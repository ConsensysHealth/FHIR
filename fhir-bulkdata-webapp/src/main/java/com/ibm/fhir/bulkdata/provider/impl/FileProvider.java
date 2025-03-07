/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.bulkdata.provider.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.fhir.bulkdata.dto.ReadResultDTO;
import com.ibm.fhir.bulkdata.jbatch.export.data.ExportTransientUserData;
import com.ibm.fhir.bulkdata.jbatch.load.data.ImportTransientUserData;
import com.ibm.fhir.bulkdata.provider.Provider;
import com.ibm.fhir.exception.FHIRException;
import com.ibm.fhir.model.format.Format;
import com.ibm.fhir.model.generator.FHIRGenerator;
import com.ibm.fhir.model.parser.FHIRParser;
import com.ibm.fhir.model.parser.exception.FHIRParserException;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.operation.bulkdata.config.ConfigurationAdapter;
import com.ibm.fhir.operation.bulkdata.config.ConfigurationFactory;

/**
 * Wraps behaviors on the File objects on the local volumes.
 */
public class FileProvider implements Provider {

    private static final Logger logger = Logger.getLogger(FileProvider.class.getName());

    private static final long MAX_RES = ConfigurationFactory.getInstance().getCoreCosObjectResourceCountThreshold();
    private static final long MAX_BUFFER = ConfigurationFactory.getInstance().getCoreCosObjectSizeThreshold();

    private String source = null;
    private long parseFailures = 0l;
    @SuppressWarnings("unused")
    private ImportTransientUserData transientUserData = null;
    private List<Resource> resources = new ArrayList<>();
    private String fhirResourceType = null;
    private String fileName = null;
    private int total = 0;
    private String cosBucketPathPrefix = null;

    private ExportTransientUserData chunkData = null;
    private long bSize = 0;

    private OutputStream out = null;
    private BufferedReader br = null;

    private ConfigurationAdapter configuration = ConfigurationFactory.getInstance();

    public FileProvider(String source) throws Exception {
        this.source = source;
    }

    private String getFilePath(String workItem) {
        return configuration.getBaseFileLocation(source) + "/" + workItem;
    }

    @Override
    public long getSize(String workItem) throws FHIRException {
        // This may be error prone as the file itself may be compressed or on a compressed volume.
        try {
            return Files.size(new File(getFilePath(workItem)).toPath());
        } catch (IOException e) {
            throw new FHIRException("Files size is not computable '" + workItem + "'", e);
        }
    }

    @Override
    public void readResources(long numOfLinesToSkip, String workItem) throws FHIRException {
        resources = new ArrayList<>();
        try {
            long line = 0;
            try {
                if (br == null) {
                    br = Files.newBufferedReader(Paths.get(getFilePath(workItem)));
                }
                for (int i = 0; i <= numOfLinesToSkip; i++) {
                    line++;
                    br.readLine(); // We know the file has at least this number.
                }

                String resourceStr = br.readLine();
                int chunkRead = 0;
                int maxRead = configuration.getImportNumberOfFhirResourcesPerRead(null);
                while (resourceStr != null && chunkRead <= maxRead) {
                    line++;
                    chunkRead++;
                    try {
                        resources.add(FHIRParser.parser(Format.JSON).parse(new StringReader(resourceStr)));
                    } catch (FHIRParserException e) {
                        // Log and skip the invalid FHIR resource.
                        parseFailures++;
                        logger.log(Level.INFO, "readResources: " + "Failed to parse line " + line + " of [" + source + "].", e);
                    }
                    resourceStr = br.readLine();
                }
            } finally {
                if (br != null) {
                    br.close();
                }
            }
        } catch (Exception e) {
            throw new FHIRException("Unable to read from Local File", e);
        }
    }

    @Override
    public List<Resource> getResources() throws FHIRException {
        return resources;
    }

    @Override
    public long getNumberOfParseFailures() throws FHIRException {
        return parseFailures;
    }

    @Override
    public void registerTransient(ImportTransientUserData transientUserData) {
        this.transientUserData = transientUserData;
    }

    @Override
    public long getNumberOfLoaded() throws FHIRException {
        return this.resources.size();
    }

    @Override
    public void registerTransient(long executionId, ExportTransientUserData transientUserData, String cosBucketPathPrefix, String fhirResourceType,
        boolean isExportPublic) throws Exception {
        if (transientUserData == null) {
            logger.warning("registerTransient: chunkData is null, this should never happen!");
            throw new Exception("registerTransient: chunkData is null, this should never happen!");
        }

        this.chunkData = transientUserData;
        this.fhirResourceType = fhirResourceType;
        this.cosBucketPathPrefix = cosBucketPathPrefix;
    }

    @Override
    public void close() throws Exception {
        if (out != null) {
            out.close();
        }
    }

    @Override
    public void writeResources(String mediaType, List<ReadResultDTO> dtos) throws Exception {
        if (out == null) {
            boolean parquet = configuration.isStorageProviderParquetEnabled(source);
            String ext;
            if (parquet) {
                ext = ".parquet";
            } else {
                ext = ".ndjson";
            }
            this.fileName = cosBucketPathPrefix + File.separator + fhirResourceType + "_" + chunkData.getUploadCount() + ext;
            String base = configuration.getBaseFileLocation(source);

            String folder = base + File.separator + cosBucketPathPrefix + File.separator;
            Path folderPath = Paths.get(folder);
            try {
                Files.createDirectories(folderPath);
            } catch(IOException ioe) {
                if (!Files.exists(folderPath)) {
                    throw ioe;
                }
            }

            String fn = base + File.separator + fileName;
            Path p1 = Paths.get(fn);
            try {
                // This is a trap. Be sure to mark CREATE and APPEND.
                out = Files.newOutputStream(p1, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                logger.warning("Error creating a file '" + fn + "'");
                throw e;
            }
            bSize = 0;
        }

        for (ReadResultDTO dto : dtos) {
            total += dto.size();
            bSize += dto.size();
            for (Resource r : dto.getResources()) {
                FHIRGenerator.generator(Format.JSON).generate(r, out);
                out.write(configuration.getEndOfFileDelimiter(source));
            }

            // This replaces the existing numbers each time.
            chunkData.setCurrentUploadResourceNum(total);
            StringBuilder output = new StringBuilder();
            output.append(fhirResourceType);
            output.append('[');
            output.append(total);
            output.append(']');

            chunkData.setResourceTypeSummary(output.toString());
        }

        if (chunkData.isFinishCurrentUpload() && (bSize > MAX_RES
                || chunkData.getBufferStream().size() > MAX_BUFFER)) {
            out.close();
            out = null;
            chunkData.setUploadCount(chunkData.getUploadCount() + 1);
            chunkData.getBufferStream().reset();
            bSize = 0;
        }
    }
}