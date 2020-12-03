/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.fhir.audit.logging.mapper.impl;

import static com.ibm.fhir.model.type.Code.code;
import static com.ibm.fhir.model.type.String.string;
import static com.ibm.fhir.model.type.Uri.uri;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.ibm.fhir.audit.logging.beans.AuditLogEntry;
import com.ibm.fhir.audit.logging.beans.AuditLogEventType;
import com.ibm.fhir.audit.logging.exception.FHIRAuditException;
import com.ibm.fhir.audit.logging.mapper.Mapper;
import com.ibm.fhir.model.format.Format;
import com.ibm.fhir.model.generator.FHIRGenerator;
import com.ibm.fhir.model.resource.AuditEvent;
import com.ibm.fhir.model.resource.AuditEvent.Agent;
import com.ibm.fhir.model.resource.AuditEvent.Entity;
import com.ibm.fhir.model.resource.AuditEvent.Source;
import com.ibm.fhir.model.type.CodeableConcept;
import com.ibm.fhir.model.type.Coding;
import com.ibm.fhir.model.type.Instant;
import com.ibm.fhir.model.type.Reference;
import com.ibm.fhir.model.type.code.AuditEventAction;
import com.ibm.fhir.model.type.code.AuditEventOutcome;

/**
 * The AuditEventMapper maps the AuditLogEntry to the FHIR standard format.
 *
 * @link https://www.hl7.org/fhir/auditevent.html
 */
public class AuditEventMapper implements Mapper {

    private static final Logger logger = java.util.logging.Logger.getLogger(CADFMapper.class.getName());
    private static final String CLASSNAME = CADFMapper.class.getName();

    //@formatter:off
    private static final Coding TYPE = Coding.builder()
            .code(code("rest"))
            .display(string("Restful Operation"))
            .system(uri("http://terminology.hl7.org/CodeSystem/audit-event-type"))
            .build();
    //@formatter:on

    //@formatter:off
    private static final Coding SUBTYPE_READ = Coding.builder()
            .code(code("read"))
            .display(string("read"))
            .system(uri("http://hl7.org/fhir/restful-interaction"))
            .build();
    //@formatter:on

    //@formatter:off
    private static final Coding SUBTYPE_VREAD = Coding.builder()
            .code(code("vread"))
            .display(string("vread"))
            .system(uri("http://hl7.org/fhir/restful-interaction"))
            .build();
    //@formatter:on

    //@formatter:off
    private static final Coding SUBTYPE_UPDATE = Coding.builder()
            .code(code("update"))
            .display(string("update"))
            .system(uri("http://hl7.org/fhir/restful-interaction"))
            .build();
    //@formatter:on

    //@formatter:off
    private static final Coding SUBTYPE_PATCH = Coding.builder()
            .code(code("patch"))
            .display(string("patch"))
            .system(uri("http://hl7.org/fhir/restful-interaction"))
            .build();
    //@formatter:on

    //@formatter:off
    private static final Coding SUBTYPE_DELETE = Coding.builder()
            .code(code("delete"))
            .display(string("delete"))
            .system(uri("http://hl7.org/fhir/restful-interaction"))
            .build();
    //@formatter:on

    //@formatter:off
    private static final Coding SUBTYPE_HISTORY = Coding.builder()
            .code(code("history"))
            .display(string("history"))
            .system(uri("http://hl7.org/fhir/restful-interaction"))
            .build();
    //@formatter:on

    //@formatter:off
    private static final Coding SUBTYPE_HISTORY_INSTANCE = Coding.builder()
            .code(code("history-instance"))
            .display(string("history-instance"))
            .system(uri("http://hl7.org/fhir/restful-interaction"))
            .build();
    //@formatter:on

    //@formatter:off
    private static final Coding SUBTYPE_HISTORY_TYPE = Coding.builder()
            .code(code("history-type"))
            .display(string("history-type"))
            .system(uri("http://hl7.org/fhir/restful-interaction"))
            .build();
    //@formatter:on

    //@formatter:off
    private static final Coding SUBTYPE_HISTORY_SYSTEM = Coding.builder()
            .code(code("history-system"))
            .display(string("history-system"))
            .system(uri("http://hl7.org/fhir/restful-interaction"))
            .build();
    //@formatter:on

    //@formatter:off
    private static final Coding SUBTYPE_CREATE = Coding.builder()
            .code(code("create"))
            .display(string("create"))
            .system(uri("http://hl7.org/fhir/restful-interaction"))
            .build();
    //@formatter:on

    //@formatter:off
    private static final Coding SUBTYPE_SEARCH = Coding.builder()
            .code(code("search"))
            .display(string("search"))
            .system(uri("http://hl7.org/fhir/restful-interaction"))
            .build();
    //@formatter:on

    //@formatter:off
    private static final Coding SUBTYPE_SEARCH_TYPE = Coding.builder()
            .code(code("search-type"))
            .display(string("search-type"))
            .system(uri("http://hl7.org/fhir/restful-interaction"))
            .build();
    //@formatter:on

    //@formatter:off
    private static final Coding SUBTYPE_SEARCH_SYSTEM = Coding.builder()
            .code(code("search-system"))
            .display(string("search-system"))
            .system(uri("http://hl7.org/fhir/restful-interaction"))
            .build();
    //@formatter:on

    //@formatter:off
    private static final Coding SUBTYPE_CAPABILITIES = Coding.builder()
            .code(code("capabilities"))
            .display(string("capabilities"))
            .system(uri("http://hl7.org/fhir/restful-interaction"))
            .build();
    //@formatter:on

    //@formatter:off
    private static final Coding SUBTYPE_TRANSACTION = Coding.builder()
            .code(code("transaction"))
            .display(string("transaction"))
            .system(uri("http://hl7.org/fhir/restful-interaction"))
            .build();
    //@formatter:on

    //@formatter:off
    private static final Coding SUBTYPE_BATCH = Coding.builder()
            .code(code("batch"))
            .display(string("batch"))
            .system(uri("http://hl7.org/fhir/restful-interaction"))
            .build();
    //@formatter:on

    //@formatter:off
    private static final Coding SUBTYPE_OPERATION = Coding.builder()
            .code(code("operation"))
            .display(string("operation"))
            .system(uri("http://hl7.org/fhir/restful-interaction"))
            .build();
    //@formatter:on

    //@formatter:off
    private static final Coding SOURCE_AUDIT = Coding.builder()
            .code(code("4"))
            .display(string("Application Server"))
            .system(uri("http://terminology.hl7.org/CodeSystem/security-source-type"))
            .build();
    //@formatter:on

    // These are loaded one time when the JVM starts up and this class is activated.
    private static final Map<String, Coding> MAP_TO_SUBTYPE = buildMapToSubtype();
    private static final Map<String, AuditEventAction> MAP_TO_ACTION = buildMapToAction();

    private AuditEvent.Builder builder = AuditEvent.builder();

    @Override
    public Mapper map(AuditLogEntry entry) {
        final String METHODNAME = "map";
        logger.entering(CLASSNAME, METHODNAME);

        // Everything on this server is an initiated RESTFUL Operation
        // @link https://www.hl7.org/fhir/valueset-audit-event-type.html
        // @formatter:off
        builder.type(TYPE)
            // We map to a single sub-type, however, in the future we may use multiple.
            // @link https://www.hl7.org/fhir/valueset-audit-event-sub-type.html#expansion
            .subtype(Arrays.asList(MAP_TO_SUBTYPE.get(entry.getEventType())))
            // Mapping our AuditEventType to a specific C-R-U-D-E
            // @link https://www.hl7.org/fhir/valueset-audit-event-action.html
            .action(MAP_TO_ACTION.get(entry.getEventType()))
            // Period involves start/end
            .period(period(entry))
            // Now, when we are crerating this AuditEvent
            .recorded(Instant.now())
            // Agent is the Actor involved in the event
            .agent(agent(entry))
            // Whether the event succeeded or failed
            .outcome(outcome(entry))
            // Description of the event outcome
            .outcomeDesc(outcomeDesc(entry))
            // The purposeOfUse of the event
            .purposeOfEvent(purposeOfEvent(entry))
            // source is the 'Audit Event Reporter' this server
            .source(source(entry))
            // Data or objects used in the audit
            .entity(entity(entry));
        // @formatter:on
        logger.exiting(CLASSNAME, METHODNAME);
        return this;
    }

    @Override
    public String serialize() throws FHIRAuditException {
        AuditEvent auditEvent = builder.build();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            FHIRGenerator.generator(Format.JSON).generate(auditEvent, out);
            return out.toString();
        } catch (Exception e) {
            throw new FHIRAuditException("Failed to serialize the Audit Event", e);
        }
    }

    // Map String to Code SubTypes
    private static Map<String, Coding> buildMapToSubtype() {
        Map<String, Coding> map = new HashMap<>();
        map.put(AuditLogEventType.FHIR_READ.value(), SUBTYPE_READ);
        map.put(AuditLogEventType.FHIR_VREAD.value(), SUBTYPE_VREAD);
        map.put(AuditLogEventType.FHIR_UPDATE.value(), SUBTYPE_UPDATE);
        map.put(AuditLogEventType.FHIR_PATCH.value(), SUBTYPE_PATCH);
        map.put(AuditLogEventType.FHIR_DELETE.value(), SUBTYPE_DELETE);
        map.put(AuditLogEventType.FHIR_HISTORY.value(), SUBTYPE_HISTORY);
        map.put(AuditLogEventType.FHIR_HISTORY_INSTANCE.value(), SUBTYPE_HISTORY_INSTANCE);
        map.put(AuditLogEventType.FHIR_HISTORY_TYPE.value(), SUBTYPE_HISTORY_TYPE);
        map.put(AuditLogEventType.FHIR_HISTORY_SYSTEM.value(), SUBTYPE_HISTORY_SYSTEM);
        map.put(AuditLogEventType.FHIR_CREATE.value(), SUBTYPE_CREATE);
        map.put(AuditLogEventType.FHIR_SEARCH.value(), SUBTYPE_SEARCH);
        map.put(AuditLogEventType.FHIR_SEARCH_SYSTEM_TYPE.value(), SUBTYPE_SEARCH_TYPE);
        map.put(AuditLogEventType.FHIR_SEARCH_SYSTEM.value(), SUBTYPE_SEARCH_SYSTEM);
        map.put(AuditLogEventType.FHIR_METADATA.value(), SUBTYPE_CAPABILITIES);
        map.put(AuditLogEventType.FHIR_TRANSACTION.value(), SUBTYPE_TRANSACTION);
        map.put(AuditLogEventType.FHIR_BUNDLE.value(), SUBTYPE_BATCH);
        map.put(AuditLogEventType.FHIR_BATCH.value(), SUBTYPE_BATCH);
        map.put(AuditLogEventType.FHIR_OPERATION.value(), SUBTYPE_OPERATION);
        map.put(AuditLogEventType.FHIR_VALIDATE.value(), SUBTYPE_OPERATION);
        map.put(AuditLogEventType.FHIR_OPERATION_BULKDATA_IMPORT.value(), SUBTYPE_OPERATION);
        map.put(AuditLogEventType.FHIR_OPERATION_BULKDATA_IMPORT.value(), SUBTYPE_OPERATION);
        map.put(AuditLogEventType.FHIR_OPERATION_BULKDATA_SEARCH.value(), SUBTYPE_OPERATION);
        return map;
    }

    // Map to AuditEventAction
    private static Map<String, AuditEventAction> buildMapToAction() {
        Map<String, AuditEventAction> map = new HashMap<>();
        map.put(AuditLogEventType.FHIR_READ.value(), AuditEventAction.R);
        map.put(AuditLogEventType.FHIR_VREAD.value(), AuditEventAction.R);
        map.put(AuditLogEventType.FHIR_UPDATE.value(), AuditEventAction.U);
        map.put(AuditLogEventType.FHIR_PATCH.value(), AuditEventAction.U);
        map.put(AuditLogEventType.FHIR_DELETE.value(), AuditEventAction.D);
        map.put(AuditLogEventType.FHIR_HISTORY.value(), AuditEventAction.R);
        map.put(AuditLogEventType.FHIR_HISTORY_INSTANCE.value(), AuditEventAction.R);
        map.put(AuditLogEventType.FHIR_HISTORY_TYPE.value(), AuditEventAction.R);
        map.put(AuditLogEventType.FHIR_HISTORY_SYSTEM.value(), AuditEventAction.R);
        map.put(AuditLogEventType.FHIR_CREATE.value(), AuditEventAction.C);
        map.put(AuditLogEventType.FHIR_SEARCH.value(), AuditEventAction.E);
        map.put(AuditLogEventType.FHIR_SEARCH_SYSTEM_TYPE.value(), AuditEventAction.E);
        map.put(AuditLogEventType.FHIR_SEARCH_SYSTEM.value(), AuditEventAction.E);
        map.put(AuditLogEventType.FHIR_METADATA.value(), AuditEventAction.E);
        map.put(AuditLogEventType.FHIR_TRANSACTION.value(), AuditEventAction.E);
        map.put(AuditLogEventType.FHIR_BUNDLE.value(), AuditEventAction.E);
        map.put(AuditLogEventType.FHIR_BATCH.value(), AuditEventAction.E);
        map.put(AuditLogEventType.FHIR_OPERATION.value(), AuditEventAction.E);
        map.put(AuditLogEventType.FHIR_VALIDATE.value(), AuditEventAction.E);
        map.put(AuditLogEventType.FHIR_OPERATION_BULKDATA_IMPORT.value(), AuditEventAction.E);
        map.put(AuditLogEventType.FHIR_OPERATION_BULKDATA_IMPORT.value(), AuditEventAction.E);
        map.put(AuditLogEventType.FHIR_OPERATION_BULKDATA_SEARCH.value(), AuditEventAction.E);
        return map;
    }

    /*
     * generate a period from the audit log entry
     */
    private com.ibm.fhir.model.type.Period period(AuditLogEntry entry) {
        // @formatter:off
        return com.ibm.fhir.model.type.Period.builder()
                    .start(com.ibm.fhir.model.type.DateTime.of(entry.getStartDate()))
                    .end(com.ibm.fhir.model.type.DateTime.of(entry.getEndDate()))
                    .build();
        // @formatter:on
    }

    private Agent agent(AuditLogEntry entry) {
        Agent.Builder builder = Agent.builder();
        builder.


        return builder.build();
    }

    private Entity entity(AuditLogEntry entry) {
        // @formatter:off
        Entity.Builder builder = Entity.builder()
                .what(what)
                .type()
                .role()
                .lifecycle()
                .securityLabel()
                .name()
                .description();
        // @formatter:on
        if searrch
            builder.query(query)

        return builder.build();
    }

    /*
     * uses the location from the audit properties as passed in to AuditLogEntrys
     */
    private Source source(AuditLogEntry entry) {
        // @formatter:off
        return Source.builder()
                .type(SOURCE_AUDIT)
                .site(string(entry.getLocation()))
                .observer(Reference.builder()
                            .reference(string(entry.getComponentId()))
                            .build())
                .build();
        // @formatter:on
    }

    private List<CodeableConcept> purposeOfEvent(AuditLogEntry entry) {
        /*
         * this may be something to make more extensible in the future.
         * @link https://www.hl7.org/fhir/v3/PurposeOfUse/vs.html
         */
        // @formatter:off
        return Arrays.asList(CodeableConcept.builder()
                    .coding(Coding.builder()
                            .code(code("PurposeOfUse"))
                            .display(string("PurposeOfUse"))
                            .system(uri("http://terminology.hl7.org/CodeSystem/v3-ActReason"))
                            .build())
                    .build());
        // @formatter:on
    }

    /*
     * currently we only log successes. In the future, we could
     * enhance this part to log the alternative outcomes.
     * This applies to the next two methods: outcomeDesc, outcome
     */
    private com.ibm.fhir.model.type.String outcomeDesc(AuditLogEntry entry) {
        return string("success");
    }

    private AuditEventOutcome outcome(AuditLogEntry entry) {
        return AuditEventOutcome.OUTCOME_0;
    }
}