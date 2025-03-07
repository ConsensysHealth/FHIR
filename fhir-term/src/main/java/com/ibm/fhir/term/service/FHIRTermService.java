/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.term.service;

import static com.ibm.fhir.model.type.String.string;
import static com.ibm.fhir.model.util.FHIRUtil.STRING_DATA_ABSENT_REASON_UNKNOWN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import com.ibm.fhir.model.resource.CodeSystem;
import com.ibm.fhir.model.resource.CodeSystem.Concept;
import com.ibm.fhir.model.resource.ConceptMap;
import com.ibm.fhir.model.resource.ConceptMap.Group;
import com.ibm.fhir.model.resource.ConceptMap.Group.Element;
import com.ibm.fhir.model.resource.ConceptMap.Group.Element.Target;
import com.ibm.fhir.model.resource.ValueSet;
import com.ibm.fhir.model.type.Boolean;
import com.ibm.fhir.model.type.Code;
import com.ibm.fhir.model.type.CodeableConcept;
import com.ibm.fhir.model.type.Coding;
import com.ibm.fhir.model.type.String;
import com.ibm.fhir.model.type.Uri;
import com.ibm.fhir.model.type.code.CodeSystemHierarchyMeaning;
import com.ibm.fhir.model.type.code.ConceptSubsumptionOutcome;
import com.ibm.fhir.term.service.provider.DefaultTermServiceProvider;
import com.ibm.fhir.term.spi.ExpansionParameters;
import com.ibm.fhir.term.spi.FHIRTermServiceProvider;
import com.ibm.fhir.term.spi.LookupOutcome;
import com.ibm.fhir.term.spi.LookupOutcome.Designation;
import com.ibm.fhir.term.spi.LookupOutcome.Property;
import com.ibm.fhir.term.spi.LookupParameters;
import com.ibm.fhir.term.spi.TranslationOutcome;
import com.ibm.fhir.term.spi.TranslationOutcome.Match;
import com.ibm.fhir.term.spi.TranslationParameters;
import com.ibm.fhir.term.spi.ValidationOutcome;
import com.ibm.fhir.term.spi.ValidationParameters;
import com.ibm.fhir.term.util.CodeSystemSupport;
import com.ibm.fhir.term.util.ValueSetSupport;

public class FHIRTermService {
    private static final FHIRTermService INSTANCE = new FHIRTermService();
    private static final FHIRTermServiceProvider NULL_TERM_SERVICE_PROVIDER = new FHIRTermServiceProvider() {
        @Override
        public boolean isSupported(CodeSystem codeSystem) {
            return false;
        }

        @Override
        public Concept findConcept(CodeSystem codeSystem, Code code) {
            return null;
        }

        @Override
        public Concept findConcept(CodeSystem codeSystem, Concept concept, Code code) {
            return null;
        }

        @Override
        public Set<Concept> getConcepts(CodeSystem codeSystem) {
            return Collections.emptySet();
        }

        @Override
        public Set<Concept> getConcepts(CodeSystem codeSystem, Concept concept) {
            return Collections.emptySet();
        }
    };
    private final List<FHIRTermServiceProvider> providers;

    private FHIRTermService() {
        providers = new CopyOnWriteArrayList<>(loadProviders());
    }

    /**
     * Register the given {@link FHIRTermServiceProvider}
     *
     * @param provider
     *     the term service provider
     */
    public void register(FHIRTermServiceProvider provider) {
        Objects.requireNonNull(provider);
        providers.add(provider);
    }

    public static FHIRTermService getInstance() {
        return INSTANCE;
    }

    /**
     * Indicates whether the given code system is supported
     *
     * @param codeSystem
     *     the code system
     * @return
     *     true if the given code system is supported, false otherwise
     */
    public boolean isSupported(CodeSystem codeSystem) {
        if (codeSystem != null) {
            for (FHIRTermServiceProvider provider : providers) {
                if (provider.isSupported(codeSystem)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Find the concept in the provided code system that matches the specified code.
     *
     * @param codeSystem
     *     the code system to search
     * @param code
     *     the code to match
     * @return
     *     the code system concept that matches the specified code, or null if no such concept exists
     */
    public Concept findConcept(CodeSystem codeSystem, Code code) {
        return findProvider(codeSystem).findConcept(codeSystem, code);
    }

    /**
     * Find the concept in tree rooted by the provided concept that matches the specified code.
     *
     * @param codeSystem
     *     the code system
     * @param concept
     *     the root of the hierarchy to search
     * @param code
     *     the code to match
     * @return
     *     the code system concept that matches the specified code, or null if not such concept exists
     */
    public Concept findConcept(CodeSystem codeSystem, Concept concept, Code code) {
        return findProvider(codeSystem).findConcept(codeSystem, concept, code);
    }

    /**
     * Get a set containing {@link CodeSystem.Concept} instances where all structural
     * hierarchies have been flattened.
     *
     * @param codeSystem
     *     the code system
     * @return
     *     flattened list of Concept instances for the given code system
     */
    public Set<Concept> getConcepts(CodeSystem codeSystem) {
        return findProvider(codeSystem).getConcepts(codeSystem);
    }

    /**
     * Get a set containing {@link CodeSystem.Concept} instances where all structural
     * hierarchies have been flattened.
     *
     * @param codeSystem
     *     the code system
     * @param concept
     *     the root of the hierarchy containing the Concept instances to be flattened
     * @return
     *     flattened set of Concept instances for the given tree
     */
    public Set<Concept> getConcepts(CodeSystem codeSystem, Concept concept) {
        return findProvider(codeSystem).getConcepts(codeSystem, concept);
    }

    /**
     * Indicates whether the given value set is expandable
     *
     * @param valueSet
     *     the value set
     * @return
     *     true if the given value set is expandable, false otherwise
     */
    public boolean isExpandable(ValueSet valueSet) {
        return ValueSetSupport.isExpandable(valueSet);
    }

    /**
     * Expand the given value set and expansion parameters
     *
     * @param valueSet
     *     the value set to expand
     * @param parameters
     *     the expansion parameters
     * @return
     *     the expanded value set, or the original value set if already expanded or unable to expand
     */
    public ValueSet expand(ValueSet valueSet, ExpansionParameters parameters) {
        if (!ExpansionParameters.EMPTY.equals(parameters)) {
            throw new UnsupportedOperationException("Expansion parameters are not supported");
        }
        return ValueSetSupport.expand(valueSet);
    }

    /**
     * Expand the given value set
     *
     * @param valueSet
     *     the value set to expand
     * @return
     *     the expanded value set, or the original value set if already expanded or unable to expand
     */
    public ValueSet expand(ValueSet valueSet) {
        return ValueSetSupport.expand(valueSet);
    }

    /**
     * Lookup the code system concept for the given system, version, code and lookup parameters
     *
     * @param system
     *     the system
     * @param version
     *     the version
     * @param code
     *     the code
     * @param parameters
     *     the lookup parameters
     * @return
     *     the outcome of the lookup
     */
    public LookupOutcome lookup(Uri system, String version, Code code, LookupParameters parameters) {
        if (!LookupParameters.EMPTY.equals(parameters)) {
            throw new UnsupportedOperationException("Lookup parameters are not suppored");
        }
        Coding coding = Coding.builder()
                .system(system)
                .version(version)
                .code(code)
                .build();
        return lookup(coding, parameters);
    }

    /**
     * Lookup the code system concept for the given system, version, and code
     *
     * @param system
     *     the system
     * @param version
     *     the version
     * @param code
     *     the code
     * @return
     *     the outcome of the lookup
     */
    public LookupOutcome lookup(Uri system, String version, Code code) {
        return lookup(system, version, code, LookupParameters.EMPTY);
    }

    /**
     * Lookup the code system concept for the given coding and lookup parameters
     *
     * @param coding
     *     the coding to lookup
     * @param parameters
     *     the lookup parameters
     * @return
     *     the outcome of the lookup
     */
    public LookupOutcome lookup(Coding coding, LookupParameters parameters) {
        if (!LookupParameters.EMPTY.equals(parameters)) {
            throw new UnsupportedOperationException("Lookup parameters are not suppored");
        }
        Uri system = coding.getSystem();
        Code code = coding.getCode();
        if (system != null && code != null) {
            java.lang.String version = (coding.getVersion() != null) ? coding.getVersion().getValue() : null;
            java.lang.String url = (version != null) ? system.getValue() + "|" + version : system.getValue();
            CodeSystem codeSystem = CodeSystemSupport.getCodeSystem(url);
            if (codeSystem != null) {
                Concept concept = findProvider(codeSystem).findConcept(codeSystem, code);
                if (concept != null) {
                    return LookupOutcome.builder()
                            .name((codeSystem.getName() != null) ? codeSystem.getName() : STRING_DATA_ABSENT_REASON_UNKNOWN)
                            .version(codeSystem.getVersion())
                            .display((concept.getDisplay() != null) ? concept.getDisplay() : STRING_DATA_ABSENT_REASON_UNKNOWN)
                            .property(concept.getProperty().stream()
                                .map(property -> Property.builder()
                                    .code(property.getCode())
                                    .value(property.getValue())
                                    .build())
                                .collect(Collectors.toList()))
                            .designation(concept.getDesignation().stream()
                                .map(designation -> Designation.builder()
                                    .language(designation.getLanguage())
                                    .use(designation.getUse())
                                    .value(designation.getValue())
                                    .build())
                                .collect(Collectors.toList()))
                            .build();
                }
            }
        }
        return null;
    }

    /**
     * Lookup the code system concept for the given coding
     *
     * @param coding
     *     the coding to lookup
     * @return
     *     the outcome of the lookup
     */
    public LookupOutcome lookup(Coding coding) {
        return lookup(coding, LookupParameters.EMPTY);
    }

    /**
     * Perform a subsumption test to determine if the code system concept represented by the given coding "A" subsumes
     * the code system concept represented by the given coding "B"
     *
     * @param codingA
     *     the coding "A"
     * @param codingB
     *     the coding "B"
     * @return
     *     the outcome of the subsumption test
     */
    public ConceptSubsumptionOutcome subsumes(Coding codingA, Coding codingB) {
        Uri systemA = codingA.getSystem();
        java.lang.String versionA = (codingA.getVersion() != null) ? codingA.getVersion().getValue() : null;
        Code codeA = codingA.getCode();

        Uri systemB = codingB.getSystem();
        java.lang.String versionB = (codingB.getVersion() != null) ? codingB.getVersion().getValue() : null;
        Code codeB = codingB.getCode();

        if (systemA != null && systemB != null && codeA != null && codeB != null && systemA.equals(systemB)) {
            java.lang.String url = systemA.getValue();

            if (versionA != null || versionB != null) {
                if (versionA != null && versionB != null && !versionA.equals(versionB)) {
                    return null;
                }
                url = (versionA != null) ? (url + "|" + versionA) : (url + "|" + versionB);
            }

            CodeSystem codeSystem = CodeSystemSupport.getCodeSystem(url);
            if (codeSystem != null && CodeSystemHierarchyMeaning.IS_A.equals(codeSystem.getHierarchyMeaning())) {
                FHIRTermServiceProvider provider = findProvider(codeSystem);
                Concept conceptA = provider.findConcept(codeSystem, codeA);
                if (conceptA != null) {
                    Concept conceptB = provider.findConcept(codeSystem, conceptA, codeB);
                    if (conceptB != null) {
                        return conceptA.equals(conceptB) ? ConceptSubsumptionOutcome.EQUIVALENT : ConceptSubsumptionOutcome.SUBSUMES;
                    }
                    conceptB = provider.findConcept(codeSystem, codeB);
                    if (conceptB != null) {
                        conceptA = provider.findConcept(codeSystem, conceptB, codeA);
                        return (conceptA != null) ? ConceptSubsumptionOutcome.SUBSUMED_BY : ConceptSubsumptionOutcome.NOT_SUBSUMED;
                    }
                }
            }
        }

        return null;
    }

    public Set<Concept> closure(CodeSystem codeSystem, Concept concept) {
        return findProvider(codeSystem).getConcepts(codeSystem, concept);
    }

    /**
     * Generate the transitive closure for the code system concept represented by the given coding
     *
     * @param coding
     *     the coding
     * @return
     *     a set containing the transitive closure for the code system concept represented by the given coding
     */
    public Set<Concept> closure(Coding coding) {
        Uri system = coding.getSystem();
        java.lang.String version = (coding.getVersion() != null) ? coding.getVersion().getValue() : null;
        Code code = coding.getCode();

        if (system != null && code != null) {
            java.lang.String url = (version != null) ? system.getValue() + "|" + version : system.getValue();
            CodeSystem codeSystem = CodeSystemSupport.getCodeSystem(url);
            if (codeSystem != null && CodeSystemHierarchyMeaning.IS_A.equals(codeSystem.getHierarchyMeaning())) {
                FHIRTermServiceProvider provider = findProvider(codeSystem);
                Concept concept = provider.findConcept(codeSystem, code);
                if (concept != null) {
                    return provider.getConcepts(codeSystem, concept);
                }
            }
        }

        return Collections.emptySet();
    }

    /**
     * Validate a code and display using the provided code system, version and validation parameters
     *
     * @param codeSystem
     *     the code system
     * @param version
     *     the version
     * @param code
     *     the code
     * @param display
     *     the display
     * @param parameters
     *     the validation parameters
     * @return
     *     the outcome of validation
     */
    public ValidationOutcome validateCode(CodeSystem codeSystem, String version, Code code, String display, ValidationParameters parameters) {
        if (!ValidationParameters.EMPTY.equals(parameters)) {
            throw new UnsupportedOperationException("Validation parameters are not supported");
        }
        Coding coding = Coding.builder()
                .version(version)
                .code(code)
                .display(display)
                .build();
        return validateCode(codeSystem, coding, parameters);
    }

    /**
     * Validate a code and display using the provided code system and version
     *
     * @param code system
     *     the code system
     * @param version
     *     the version
     * @param code
     *     the code
     * @param display
     *     the display
     * @return
     *     the outcome of validation
     */
    public ValidationOutcome validateCode(CodeSystem codeSystem, String version, Code code, String display) {
        return validateCode(codeSystem, version, code, display, ValidationParameters.EMPTY);
    }

    /**
     * Validate a coding using the provided code system and validation parameters
     *
     * @param codeSystem
     *     the code system
     * @param coding
     *     the coding
     * @param parameters
     *     the validation parameters
     * @return
     *     the outcome of validation
     */
    public ValidationOutcome validateCode(CodeSystem codeSystem, Coding coding, ValidationParameters parameters) {
        if (!ValidationParameters.EMPTY.equals(parameters)) {
            throw new UnsupportedOperationException("Validation parameters are not supported");
        }
        LookupOutcome outcome = lookup(coding, LookupParameters.EMPTY);
        return validateCode(codeSystem, coding, (outcome != null), outcome);
    }

    /**
     * Validate a coding using the provided code system
     *
     * @param codeSystem
     *     the codeSystem
     * @param coding
     *     the coding
     * @return
     *     the outcome of validation
     */
    public ValidationOutcome validateCode(CodeSystem codeSystem, Coding coding) {
        return validateCode(codeSystem, coding, ValidationParameters.EMPTY);
    }

    /**
     * Validate a codeable concept using the provided code system and validation parameters
     *
     * @param codeSystem
     *     the code system
     * @param codeableConcept
     *     the codeable concept
     * @param parameters
     *     the validation parameters
     * @return
     *     the outcome of validation
     */
    public ValidationOutcome validateCode(CodeSystem codeSystem, CodeableConcept codeableConcept, ValidationParameters parameters) {
        if (!ValidationParameters.EMPTY.equals(parameters)) {
            throw new UnsupportedOperationException("Validation parameters are not supported");
        }
        for (Coding coding : codeableConcept.getCoding()) {
            ValidationOutcome outcome = validateCode(codeSystem, coding, parameters);
            if (Boolean.TRUE.equals(outcome.getResult())) {
                return outcome;
            }
        }
        return validateCode(null, false, null);
    }

    /**
     * Validate a codeable concept using the provided code system
     *
     * @param codeableConcept
     *     the codeable concept
     * @return
     *     the outcome of validation
     */
    public ValidationOutcome validateCode(CodeSystem codeSystem, CodeableConcept codeableConcept) {
        return validateCode(codeSystem, codeableConcept, ValidationParameters.EMPTY);
    }

    /**
     * Validate a code using the provided value set and validation parameters
     *
     * @apiNote
     *     the implementation will expand the provided value set if needed
     * @param valueSet
     *     the value set
     * @param code
     *     the code
     * @param parameters
     *     the validation parameters
     * @return
     *     the outcome of validation
     */
    public ValidationOutcome validateCode(ValueSet valueSet, Code code, ValidationParameters parameters) {
        if (!ValidationParameters.EMPTY.equals(parameters)) {
            throw new UnsupportedOperationException("Validation parameters are not supported");
        }
        boolean result = ValueSetSupport.validateCode(valueSet, code);
        return validateCode(null, Coding.builder().code(code).build(), result, null);
    }

    /**
     * Validate a code using the provided value set
     *
     * @apiNote
     *     the implementation will expand the provided value set if needed
     * @param valueSet
     *     the value set
     * @param code
     *     the code
     * @return
     *     the outcome of validation
     */
    public ValidationOutcome validateCode(ValueSet valueSet, Code code) {
        return validateCode(valueSet, code, ValidationParameters.EMPTY);
    }

    /**
     * Validate a code and display using the provided value set and validation parameters
     *
     * @apiNote
     *     the implementation will expand the provided value set if needed
     * @param valueSet
     *     the value set
     * @param system
     *     the system
     * @param version
     *     the version
     * @param code
     *     the code
     * @param display
     *     the display
     * @param parameters
     *     the validation parameters
     * @return
     *     the outcome of validation
     */
    public ValidationOutcome validateCode(ValueSet valueSet, Uri system, String version, Code code, String display, ValidationParameters parameters) {
        if (!ValidationParameters.EMPTY.equals(parameters)) {
            throw new UnsupportedOperationException("Validation parameters are not supported");
        }
        Coding coding = Coding.builder()
                .system(system)
                .version(version)
                .code(code)
                .build();
        return validateCode(valueSet, coding, parameters);
    }

    /**
     * Validate a code and display using the provided value set
     *
     * @apiNote
     *     the implementation will expand the provided value set if needed
     * @param valueSet
     *     the value set
     * @param system
     *     the system
     * @param version
     *     the version
     * @param code
     *     the code
     * @param display
     *     the display
     * @return
     *     the outcome of validation
     */
    public ValidationOutcome validateCode(ValueSet valueSet, Uri system, String version, Code code, String display) {
        return validateCode(valueSet, system, version, code, display, ValidationParameters.EMPTY);
    }

    /**
     * Validate a coding using the provided value set using the provided validation parameters
     *
     * @apiNote
     *     the implementation will expand the provided value set if needed
     * @param valueSet
     *     the value set
     * @param coding
     *     the coding
     * @param parameters
     *     the validation parameters
     * @return
     *     the outcome of validation
     */
    public ValidationOutcome validateCode(ValueSet valueSet, Coding coding, ValidationParameters parameters) {
        if (!ValidationParameters.EMPTY.equals(parameters)) {
            throw new UnsupportedOperationException("Validation parameters are not supported");
        }
        boolean result = ValueSetSupport.validateCode(valueSet, coding);
        LookupOutcome outcome = result ? lookup(coding) : null;
        return validateCode(coding, result, outcome);
    }

    /**
     * Validate a coding using the provided value set using the provided validation parameters
     *
     * @apiNote
     *     the implementation will expand the provided value set if needed
     * @param valueSet
     *     the value set
     * @param coding
     *     the coding
     * @param parameters
     *     the validation parameters
     * @return
     *     the outcome of validation
     */
    public ValidationOutcome validateCode(ValueSet valueSet, Coding coding) {
        return validateCode(valueSet, coding, ValidationParameters.EMPTY);
    }

    /**
     * Validate a codeable concept using the provided value set using the provided validation parameters
     *
     * @apiNote
     *     the implementation will expand the provided value set if needed
     * @param valueSet
     *     the value set
     * @param codeable concept
     *     the codeable concept
     * @param parameters
     *     the validation parameters
     * @return
     *     the outcome of validation
     */
    public ValidationOutcome validateCode(ValueSet valueSet, CodeableConcept codeableConcept, ValidationParameters parameters) {
        if (!ValidationParameters.EMPTY.equals(parameters)) {
            throw new UnsupportedOperationException("Validation parameters are not supported");
        }
        for (Coding coding : codeableConcept.getCoding()) {
            boolean result = ValueSetSupport.validateCode(valueSet, coding);
            if (result) {
                LookupOutcome outcome = lookup(coding);
                return validateCode(coding, result, outcome);
            }
        }
        return validateCode(null, false, null);
    }

    /**
     * Validate a codeable concept using the provided value set
     *
     * @apiNote
     *     the implementation will expand the provided value set if needed
     * @param valueSet
     *     the value set
     * @param codeable concept
     *     the codeable concept
     * @return
     *     the outcome of validation
     */
    public ValidationOutcome validateCode(ValueSet valueSet, CodeableConcept codeableConcept) {
        return validateCode(valueSet, codeableConcept, ValidationParameters.EMPTY);
    }

    /**
     * Translate the given system, version and code using the provided concept map and translation parameters
     *
     * @param conceptMap
     *     the concept map
     * @param system
     *     the system
     * @param version
     *     the version
     * @param code
     *     the code
     * @param parameters
     *     the translation parameters
     * @return
     *     the outcome of translation
     */
    public TranslationOutcome translate(ConceptMap conceptMap, Uri system, String version, Code code, TranslationParameters parameters) {
        if (!TranslationParameters.EMPTY.equals(parameters)) {
            throw new UnsupportedOperationException("Translation parameters are not supported");
        }
        Coding coding = Coding.builder()
                .system(system)
                .version(version)
                .code(code)
                .build();
        return translate(conceptMap, coding, parameters);
    }

    /**
     * Translate the given system, version and code using the provided concept map
     *
     * @param conceptMap
     *     the concept map
     * @param system
     *     the system
     * @param version
     *     the version
     * @param code
     *     the code
     * @return
     *     the outcome of translation
     */
    public TranslationOutcome translate(ConceptMap conceptMap, Uri system, String version, Code code) {
        return translate(conceptMap, system, version, code, TranslationParameters.EMPTY);
    }

    /**
     * Translate the given coding using the provided concept map and translation parameters
     *
     * @param conceptMap
     *     the concept map
     * @param coding
     *     the coding
     * @param parameters
     *     the translation parameters
     * @return
     *     the outcome of translation
     */
    public TranslationOutcome translate(ConceptMap conceptMap, Coding coding, TranslationParameters parameters) {
        if (!TranslationParameters.EMPTY.equals(parameters)) {
            throw new UnsupportedOperationException("Translation parameters are not supported");
        }
        Uri source = getSource(conceptMap);
        List<Match> match = new ArrayList<>();
        for (Group group : conceptMap.getGroup()) {
            if (group.getSource() == null || !group.getSource().equals(coding.getSystem())) {
                continue;
            }
            if (group.getSourceVersion() != null && coding.getVersion() != null && !group.getSourceVersion().equals(coding.getVersion())) {
                continue;
            }
            for (Element element : group.getElement()) {
                if (element.getCode() == null || !element.getCode().equals(coding.getCode())) {
                    // TODO: handle unmatched codes here
                    continue;
                }
                for (Target target : element.getTarget()) {
                    match.add(Match.builder()
                        .equivalence(target.getEquivalence())
                        .concept(Coding.builder()
                            .system(group.getTarget())
                            .version(group.getTargetVersion())
                            .code(target.getCode())
                            .display(target.getDisplay())
                            .build())
                        .source(source)
                        .build());
                }
            }
        }
        return TranslationOutcome.builder()
                .result(match.isEmpty() ? Boolean.FALSE : Boolean.TRUE)
                .message(match.isEmpty() ? string("No matches found") : null)
                .match(match)
                .build();
    }

    /**
     * Translate the given coding using the provided concept map
     *
     * @param conceptMap
     *     the concept map
     * @param coding
     *     the coding
     * @return
     *     the outcome of translation
     */
    public TranslationOutcome translate(ConceptMap conceptMap, Coding coding) {
        return translate(conceptMap, coding, TranslationParameters.EMPTY);
    }

    /**
     * Translate the given codeable concept using the provided concept map and translation parameters
     *
     * @param conceptMap
     *     the concept map
     * @param codeableConcept
     *     the codeable concept
     * @param parameters
     *     the translation parameters
     * @return
     *     the outcome of translation
     */
    public TranslationOutcome translate(ConceptMap conceptMap, CodeableConcept codeableConcept, TranslationParameters parameters) {
        if (!TranslationParameters.EMPTY.equals(parameters)) {
            throw new UnsupportedOperationException("Translation parameters are not supported");
        }
        for (Coding coding : codeableConcept.getCoding()) {
            TranslationOutcome outcome = translate(conceptMap, coding, parameters);
            if (Boolean.TRUE.equals(outcome.getResult())) {
                return outcome;
            }
        }
        return TranslationOutcome.builder()
                .result(Boolean.FALSE)
                .message(string("No matches found"))
                .build();
    }

    /**
     * Translate the given coding using the provided concept map
     *
     * @param conceptMap
     *     the concept map
     * @param codeable concept
     *     the codeable concept
     * @return
     *     the outcome of translation
     */
    public TranslationOutcome translate(ConceptMap conceptMap, CodeableConcept codeableConcept) {
        return translate(conceptMap, codeableConcept, TranslationParameters.EMPTY);
    }

    private List<FHIRTermServiceProvider> loadProviders() {
        List<FHIRTermServiceProvider> providers = new ArrayList<>();
        providers.add(new DefaultTermServiceProvider());
        Iterator<FHIRTermServiceProvider> iterator = ServiceLoader.load(FHIRTermServiceProvider.class).iterator();
        while (iterator.hasNext()) {
            providers.add(iterator.next());
        }
        return providers;
    }

    private FHIRTermServiceProvider findProvider(CodeSystem codeSystem) {
        for (FHIRTermServiceProvider provider : providers) {
            if (provider.isSupported(codeSystem)) {
                return provider;
            }
        }
        return NULL_TERM_SERVICE_PROVIDER;
    }

    private ValidationOutcome validateCode(Coding coding, boolean result, LookupOutcome outcome) {
        return validateCode(null, coding, result, outcome);
    }

    private ValidationOutcome validateCode(CodeSystem codeSystem, Coding coding, boolean result, LookupOutcome outcome) {
        java.lang.String message = null;
        if (!result && coding != null && coding.getCode() != null) {
            message = java.lang.String.format("Code '%s' is invalid", coding.getCode().getValue());
        }
        if (result && outcome != null && coding != null && outcome.getDisplay() != null && coding.getDisplay() != null) {
            java.lang.String system = null;
            if (coding.getSystem() != null) {
                system = coding.getSystem().getValue();
            } else if (codeSystem != null && codeSystem.getUrl() != null) {
                system = codeSystem.getUrl().getValue();
            }
            boolean caseSensitive = (codeSystem != null) ? CodeSystemSupport.isCaseSensitive(codeSystem) : false;
            if (codeSystem == null && system != null) {
                java.lang.String version = (coding.getVersion() != null) ? coding.getVersion().getValue() : null;
                java.lang.String url = (version != null) ? system + "|" + version : system;
                caseSensitive = CodeSystemSupport.isCaseSensitive(url);
            }
            result = caseSensitive ? outcome.getDisplay().equals(coding.getDisplay()) : outcome.getDisplay().getValue().equalsIgnoreCase(coding.getDisplay().getValue());
            message = !result ? java.lang.String.format("The display '%s' is incorrect for code '%s' from code system '%s'", coding.getDisplay().getValue(), coding.getCode().getValue(), system) : null;
        }
        return ValidationOutcome.builder()
                .result(result ? Boolean.TRUE : Boolean.FALSE)
                .message((message != null) ? string(message) : null)
                .display((outcome != null) ? outcome.getDisplay() : null)
                .build();
    }

    private Uri getSource(ConceptMap conceptMap) {
        StringBuilder sb = new StringBuilder(conceptMap.getUrl().getValue());
        if (conceptMap.getVersion() != null) {
            sb.append("|").append(conceptMap.getVersion().getValue());
        }
        return Uri.of(sb.toString());
    }
}
