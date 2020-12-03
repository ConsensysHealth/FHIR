/*
 * (C) Copyright IBM Corp. 2016,2019
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.audit.logging.beans;

/**
 * This enum defines allowable types of audit log events that can be logged using
 * Audit Log Service.
 */
public enum AuditLogEventType {

    FHIR_CREATE("fhir-create"),

    FHIR_UPDATE("fhir-update"),

    FHIR_PATCH("fhir-patch"),

    FHIR_DELETE("fhir-delete"),

    FHIR_READ("fhir-read"),

    FHIR_VREAD("fhir-version-read"),

    FHIR_HISTORY("fhir-history"),

    FHIR_HISTORY_INSTANCE("fhir-history-instance"),

    FHIR_HISTORY_TYPE("fhir-history-type"),

    FHIR_HISTORY_SYSTEM("fhir-history-system"),

    FHIR_SEARCH("fhir-search"),

    FHIR_SEARCH_SYSTEM_TYPE("fhir-search-type"),

    FHIR_SEARCH_SYSTEM("fhir-search-system"),

    FHIR_BUNDLE("fhir-bundle"),

    FHIR_TRANSACTION("fhir-transaction"),

    FHIR_BATCH("fhir-batch"),

    FHIR_VALIDATE("fhir-validate"),

    FHIR_METADATA("fhir-metadata"),

    FHIR_CONFIGDATA("fhir-configdata"),

    FHIR_OPERATION("fhir-operation"),

    FHIR_OPERATION_BULKDATA_IMPORT("fhir-operation-bulkdata-import"),

    FHIR_OPERATION_BULKDATA_EXPORT("fhir-operation-bulkdata-export"),

    FHIR_OPERATION_BULKDATA_SEARCH("fhir-operation-bulkdata-search");

    private String value = null;

    AuditLogEventType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static AuditLogEventType fromValue(String value) {
        for (AuditLogEventType entryType : AuditLogEventType.values()) {
            if (entryType.value.equalsIgnoreCase(value)) {
                return entryType;
            }
        }
        throw new IllegalArgumentException("No constant with value " + value + " found.");
    }
}