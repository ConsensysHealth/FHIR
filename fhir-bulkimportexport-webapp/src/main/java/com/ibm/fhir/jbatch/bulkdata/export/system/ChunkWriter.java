/*
 * (C) Copyright IBM Corp. 2019, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.jbatch.bulkdata.export.system;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.ibm.cloud.objectstorage.services.s3.AmazonS3;
import com.ibm.cloud.objectstorage.services.s3.model.CreateBucketRequest;
import com.ibm.fhir.config.FHIRConfigHelper;
import com.ibm.fhir.config.FHIRConfiguration;
import com.ibm.fhir.core.FHIRMediaType;
import com.ibm.fhir.jbatch.bulkdata.common.BulkDataUtils;
import com.ibm.fhir.jbatch.bulkdata.common.Constants;
import com.ibm.fhir.jbatch.bulkdata.export.common.SparkParquetWriter;
import com.ibm.fhir.jbatch.bulkdata.export.common.TransientUserData;
import com.ibm.fhir.model.resource.Resource;

/**
 * Bulk export Chunk implementation - the Writer.
 * TODO: move to common?
 */
@Dependent
public class ChunkWriter extends AbstractItemWriter {
    private static final Logger logger = Logger.getLogger(ChunkWriter.class.getName());
    private AmazonS3 cosClient = null;
    private SparkParquetWriter parquetWriter = null;
    private boolean isExportPublic = true;

    /**
     * The IBM COS API key or S3 access key.
     */
    @Inject
    @BatchProperty(name = Constants.COS_API_KEY)
    String cosApiKeyProperty;

    /**
     * The IBM COS service instance id or s3 secret key.
     */
    @Inject
    @BatchProperty(name = Constants.COS_SRVINST_ID)
    String cosSrvinstId;

    /**
     * The Cos End point URL.
     */
    @Inject
    @BatchProperty(name = Constants.COS_ENDPOINT_URL)
    String cosEndpointUrl;

    /**
     * The Cos End point location.
     */
    @Inject
    @BatchProperty(name = Constants.COS_LOCATION)
    String cosLocation;

    /**
     * The Cos bucket name.
     */
    @Inject
    @BatchProperty(name = Constants.COS_BUCKET_NAME)
    String cosBucketName;

    /**
     * The Cos bucket path prefix.
     */
    @Inject
    @BatchProperty(name = Constants.EXPORT_COS_OBJECT_PATHPREFIX)
    String cosBucketPathPrefix;

    /**
     * If use IBM credential or Amazon secret keys.
     */
    @Inject
    @BatchProperty(name = Constants.COS_IS_IBM_CREDENTIAL)
    String cosCredentialIbm;

    /**
     * Fhir resource type to process.
     */
    @Inject
    @BatchProperty(name = Constants.PARTITION_RESOURCE_TYPE)
    String fhirResourceType;

    /**
     * Fhir export format.
     */
    @Inject
    @BatchProperty(name = Constants.EXPORT_FHIR_FORMAT)
    String fhirExportFormat;

    @Inject
    StepContext stepCtx;

    @Inject
    JobContext jobContext;

    public ChunkWriter() {
        super();
    }

    private void pushFhirJsonsToCos(InputStream in, int dataLength) throws Exception {
        if (cosClient == null) {
            logger.warning("pushFhirJsonsToCos: no cosClient!");
            throw new Exception("pushFhirJsonsToCos: no cosClient!");
        }

        TransientUserData chunkData = (TransientUserData) stepCtx.getTransientUserData();
        if (chunkData == null) {
            logger.warning("pushFhirJsonsToCos: chunkData is null, this should never happen!");
            throw new Exception("pushFhirJsonsToCos: chunkData is null, this should never happen!");
        }

        String itemName;
        if (cosBucketPathPrefix != null && cosBucketPathPrefix.trim().length() > 0) {
          itemName = cosBucketPathPrefix + "/" + fhirResourceType + "_" + chunkData.getUploadCount() + ".ndjson";
        } else {
          itemName = "job" + jobContext.getExecutionId() + "/" + fhirResourceType + "_" + chunkData.getUploadCount() + ".ndjson";
        }

        if (chunkData.getUploadId() == null) {
            chunkData.setUploadId(BulkDataUtils.startPartUpload(cosClient, cosBucketName, itemName, isExportPublic));
        }

        chunkData.getCosDataPacks().add(BulkDataUtils.multiPartUpload(cosClient, cosBucketName, itemName,
                chunkData.getUploadId(), in, dataLength, chunkData.getPartNum()));
        logger.info("pushFhirJsonsToCos: " + dataLength + " bytes were successfully appended to COS object - "
                + itemName);
        chunkData.setPartNum(chunkData.getPartNum() + 1);
        chunkData.getBufferStream().reset();

        if (chunkData.getPageNum() > chunkData.getLastPageNum() || chunkData.isFinishCurrentUpload()) {
            BulkDataUtils.finishMultiPartUpload(cosClient, cosBucketName, itemName, chunkData.getUploadId(),
                    chunkData.getCosDataPacks());
            // Partition status for the exported resources, e.g, Patient[1000,1000,200]
            if (chunkData.getResourceTypeSummary() == null) {
                chunkData.setResourceTypeSummary(fhirResourceType + "[" + chunkData.getCurrentUploadResourceNum());
                if (chunkData.getPageNum() > chunkData.getLastPageNum()) {
                    chunkData.setResourceTypeSummary(chunkData.getResourceTypeSummary() + "]");
                }
            } else {
                chunkData.setResourceTypeSummary(chunkData.getResourceTypeSummary() + "," + chunkData.getCurrentUploadResourceNum());
                if (chunkData.getPageNum() > chunkData.getLastPageNum()) {
                    chunkData.setResourceTypeSummary(chunkData.getResourceTypeSummary() + "]");
                    stepCtx.setTransientUserData(chunkData);
                }
            }

            if (chunkData.getPageNum() <= chunkData.getLastPageNum()) {
                chunkData.setPartNum(1);
                chunkData.setUploadId(null);
                chunkData.setCurrentUploadResourceNum(0);
                chunkData.setCurrentUploadSize(0);
                chunkData.setFinishCurrentUpload(false);
                chunkData.getCosDataPacks().clear();
                chunkData.setUploadCount(chunkData.getUploadCount() + 1);
            }
        }
    }

    private void pushFhirParquetToCos(List<Resource> resources) throws Exception {
        TransientUserData chunkData = (TransientUserData) stepCtx.getTransientUserData();
        if (chunkData == null) {
            logger.warning("pushFhirParquet2Cos: chunkData is null, this should never happen!");
            throw new Exception("pushFhirParquet2Cos: chunkData is null, this should never happen!");
        }

        String itemName;
        if (cosBucketPathPrefix != null && cosBucketPathPrefix.trim().length() > 0) {
          itemName = "cos://" + cosBucketName + ".fhir/" + cosBucketPathPrefix + "/" + fhirResourceType + "_" + chunkData.getUploadCount() + ".parquet";
        } else {
          itemName = "cos://" + cosBucketName + ".fhir/job" + jobContext.getExecutionId() + "/" + fhirResourceType + "_" + chunkData.getUploadCount() + ".parquet";
        }

        parquetWriter.writeParquet(resources, itemName);

        // Partition status for the exported resources, e.g, Patient[1000,1000,200]
        if (chunkData.getResourceTypeSummary() == null) {
            chunkData.setResourceTypeSummary(fhirResourceType + "[" + chunkData.getCurrentUploadResourceNum());
            if (chunkData.getPageNum() > chunkData.getLastPageNum()) {
                chunkData.setResourceTypeSummary(chunkData.getResourceTypeSummary() + "]");
            }
        } else {
            chunkData.setResourceTypeSummary(chunkData.getResourceTypeSummary() + "," + chunkData.getCurrentUploadResourceNum());
            if (chunkData.getPageNum() > chunkData.getLastPageNum()) {
                chunkData.setResourceTypeSummary(chunkData.getResourceTypeSummary() + "]");
                stepCtx.setTransientUserData(chunkData);
            }
        }
        if (chunkData.getPageNum() <= chunkData.getLastPageNum()) {
            chunkData.setPartNum(1);
            chunkData.setUploadId(null);
            chunkData.setCurrentUploadResourceNum(0);
            chunkData.setCurrentUploadSize(0);
            chunkData.setFinishCurrentUpload(false);
            chunkData.setUploadCount(chunkData.getUploadCount() + 1);
        }
    }

    @Override
    public void writeItems(List<java.lang.Object> resourceLists) throws Exception {
        if (cosBucketName == null) {
            cosBucketName = Constants.DEFAULT_COS_BUCKETNAME;
        }
        cosBucketName = cosBucketName.toLowerCase();
        if (!cosClient.doesBucketExistV2(cosBucketName)) {
            CreateBucketRequest req = new CreateBucketRequest(cosBucketName);
            cosClient.createBucket(req);
        }

        TransientUserData chunkData = (TransientUserData) stepCtx.getTransientUserData();
        if (chunkData == null) {
            logger.warning("writeItems: chunkData is null, this should never happen!");
            throw new Exception("writeItems: chunkData is null, this should never happen!");
        }

        try {
            switch(fhirExportFormat) {
            case FHIRMediaType.APPLICATION_PARQUET:
                boolean isTimeToWriteParquet = chunkData.getPageNum() > chunkData.getLastPageNum()
                        || chunkData.isFinishCurrentUpload();
                if (isTimeToWriteParquet) {
                    List<Resource> resources = new ArrayList<>();
                    // Is there a cleaner way to add these in a type-safe way?
                    if (resourceLists instanceof List) {
                        for (Object resourceList : resourceLists) {
                            if (resourceList instanceof Collection<?>) {
                                for (Object resource : (Collection<?>) resourceList) {
                                    if (resource instanceof Resource) {
                                        resources.add((Resource) resource);
                                    }
                                }
                            }
                        }
                    }
                    pushFhirParquetToCos(resources);
                    chunkData.setLastWritePageNum(chunkData.getPageNum());
                }
                break;
            case FHIRMediaType.APPLICATION_NDJSON:
            default:
                boolean isTimeToWrite = chunkData.getPageNum() > chunkData.getLastPageNum()
                        || chunkData.getBufferStream().size() > Constants.COS_PART_MINIMALSIZE
                        || chunkData.isFinishCurrentUpload();
                if (isTimeToWrite && chunkData.getBufferStream().size() > 0) {
                    // TODO try PipedOutputStream -> PipedInputStream instead?
                    pushFhirJsonsToCos(new ByteArrayInputStream(chunkData.getBufferStream().toByteArray()),
                            chunkData.getBufferStream().size());
                    chunkData.setLastWritePageNum(chunkData.getPageNum());
                }
            }
        } catch (Exception e) {
            // log and forward so that it appears in the server log and not just the job log
            logger.log(Level.WARNING, "Unexpected error while writing chunk", e);
        }
    }

    @Override
    public void open(Serializable checkpoint) throws Exception  {
        isExportPublic = FHIRConfigHelper.getBooleanProperty(FHIRConfiguration.PROPERTY_BULKDATA_BATCHJOB_ISEXPORTPUBLIC, true);
        boolean isCosClientUseFhirServerTrustStore = FHIRConfigHelper
                .getBooleanProperty(FHIRConfiguration.PROPERTY_BULKDATA_BATCHJOB_USEFHIRSERVERTRUSTSTORE, false);
        cosClient = BulkDataUtils.getCosClient(cosCredentialIbm, cosApiKeyProperty, cosSrvinstId, cosEndpointUrl,
                cosLocation, isCosClientUseFhirServerTrustStore);

        try {
            Class.forName("org.apache.spark.sql.SparkSession");
            parquetWriter = new SparkParquetWriter("Y".equalsIgnoreCase(cosCredentialIbm), cosEndpointUrl, cosApiKeyProperty, cosSrvinstId);
        } catch (ClassNotFoundException e) {
            logger.info("No SparkSession in classpath; skipping SparkParquetWriter initialization");
        }

        if (cosClient == null) {
            logger.warning("open: Failed to get CosClient!");
            throw new Exception("Failed to get CosClient!!");
        } else {
            logger.finer("open: Got CosClient successfully!");
        }
    }

    @Override
    public void close() throws Exception {
        logger.fine("closing the ChunkWriter");
        if (parquetWriter != null) {
            parquetWriter.close();
        }
    }
}
