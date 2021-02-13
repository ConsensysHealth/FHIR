/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.jbatch.bulkdata.export.patient;

import static com.ibm.fhir.jbatch.bulkdata.common.Constants.EXPORT_MAX_PARTITIONPROCESSING_THREADNUMBER;
import static com.ibm.fhir.jbatch.bulkdata.common.Constants.FHIR_RESOURCETYPES;
import static com.ibm.fhir.jbatch.bulkdata.common.Constants.PARTITION_RESOURCE_TYPE;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionMapper;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.ibm.fhir.search.compartment.CompartmentUtil;

@Dependent
public class PatientExportPartitionMapper implements PartitionMapper {

    @Inject
    @BatchProperty(name = FHIR_RESOURCETYPES)
    String fhirResourceType;

    public PatientExportPartitionMapper() {
        // No Operation
    }

    @Override
    public PartitionPlan mapPartitions() throws Exception {
        List<String> resourceTypes;
        List <String> allCompartmentResourceTypes = CompartmentUtil.getCompartmentResourceTypes("Patient");
        if (fhirResourceType == null ) {
            resourceTypes = allCompartmentResourceTypes;
        } else {
            List<String> tmpResourceTypes = Arrays.asList(fhirResourceType.split("\\s*,\\s*"));
            resourceTypes = tmpResourceTypes.stream().filter(item-> allCompartmentResourceTypes.contains(item)).collect(Collectors.toList());
            if (resourceTypes == null || resourceTypes.isEmpty()) {
                throw new Exception("open: None of the input resource types is valid!");
            }
        }

        PartitionPlanImpl pp = new PartitionPlanImpl();
        pp.setPartitions(resourceTypes.size());
        pp.setThreads(Math.min(EXPORT_MAX_PARTITIONPROCESSING_THREADNUMBER, resourceTypes.size()));
        Properties[] partitionProps = new Properties[resourceTypes.size()];

        int propCount = 0;
        for (String resourceType : resourceTypes) {
            Properties p = new Properties();
            p.setProperty(PARTITION_RESOURCE_TYPE, resourceType);
            partitionProps[propCount++] = p;
        }
        pp.setPartitionProperties(partitionProps);

        return pp;
    }
}