/*
 * (C) Copyright IBM Corp. 2019, 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.model.type.code;

import com.ibm.fhir.model.annotation.System;
import com.ibm.fhir.model.type.Code;
import com.ibm.fhir.model.type.Extension;
import com.ibm.fhir.model.type.String;

import java.util.Collection;
import java.util.Objects;

import javax.annotation.Generated;

@System("http://hl7.org/fhir/explanationofbenefit-status")
@Generated("com.ibm.fhir.tools.CodeGenerator")
public class ExplanationOfBenefitStatus extends Code {
    /**
     * Active
     * 
     * <p>The resource instance is currently in-force.
     */
    public static final ExplanationOfBenefitStatus ACTIVE = ExplanationOfBenefitStatus.builder().value(ValueSet.ACTIVE).build();

    /**
     * Cancelled
     * 
     * <p>The resource instance is withdrawn, rescinded or reversed.
     */
    public static final ExplanationOfBenefitStatus CANCELLED = ExplanationOfBenefitStatus.builder().value(ValueSet.CANCELLED).build();

    /**
     * Draft
     * 
     * <p>A new resource instance the contents of which is not complete.
     */
    public static final ExplanationOfBenefitStatus DRAFT = ExplanationOfBenefitStatus.builder().value(ValueSet.DRAFT).build();

    /**
     * Entered In Error
     * 
     * <p>The resource instance was entered in error.
     */
    public static final ExplanationOfBenefitStatus ENTERED_IN_ERROR = ExplanationOfBenefitStatus.builder().value(ValueSet.ENTERED_IN_ERROR).build();

    private volatile int hashCode;

    private ExplanationOfBenefitStatus(Builder builder) {
        super(builder);
    }

    public ValueSet getValueAsEnumConstant() {
        return (value != null) ? ValueSet.from(value) : null;
    }

    /**
     * Factory method for creating ExplanationOfBenefitStatus objects from a passed enum value.
     */
    public static ExplanationOfBenefitStatus of(ValueSet value) {
        switch (value) {
        case ACTIVE:
            return ACTIVE;
        case CANCELLED:
            return CANCELLED;
        case DRAFT:
            return DRAFT;
        case ENTERED_IN_ERROR:
            return ENTERED_IN_ERROR;
        default:
            throw new IllegalStateException(value.name());
        }
    }

    /**
     * Factory method for creating ExplanationOfBenefitStatus objects from a passed string value.
     * 
     * @param value
     *     A string that matches one of the allowed code values
     * @throws IllegalArgumentException
     *     If the passed string cannot be parsed into an allowed code value
     */
    public static ExplanationOfBenefitStatus of(java.lang.String value) {
        return of(ValueSet.from(value));
    }

    /**
     * Inherited factory method for creating ExplanationOfBenefitStatus objects from a passed string value.
     * 
     * @param value
     *     A string that matches one of the allowed code values
     * @throws IllegalArgumentException
     *     If the passed string cannot be parsed into an allowed code value
     */
    public static String string(java.lang.String value) {
        return of(ValueSet.from(value));
    }

    /**
     * Inherited factory method for creating ExplanationOfBenefitStatus objects from a passed string value.
     * 
     * @param value
     *     A string that matches one of the allowed code values
     * @throws IllegalArgumentException
     *     If the passed string cannot be parsed into an allowed code value
     */
    public static Code code(java.lang.String value) {
        return of(ValueSet.from(value));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ExplanationOfBenefitStatus other = (ExplanationOfBenefitStatus) obj;
        return Objects.equals(id, other.id) && Objects.equals(extension, other.extension) && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = Objects.hash(id, extension, value);
            hashCode = result;
        }
        return result;
    }

    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.id(id);
        builder.extension(extension);
        builder.value(value);
        return builder;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Code.Builder {
        private Builder() {
            super();
        }

        @Override
        public Builder id(java.lang.String id) {
            return (Builder) super.id(id);
        }

        @Override
        public Builder extension(Extension... extension) {
            return (Builder) super.extension(extension);
        }

        @Override
        public Builder extension(Collection<Extension> extension) {
            return (Builder) super.extension(extension);
        }

        @Override
        public Builder value(java.lang.String value) {
            return (value != null) ? (Builder) super.value(ValueSet.from(value).value()) : this;
        }

        public Builder value(ValueSet value) {
            return (value != null) ? (Builder) super.value(value.value()) : this;
        }

        @Override
        public ExplanationOfBenefitStatus build() {
            return new ExplanationOfBenefitStatus(this);
        }
    }

    public enum ValueSet {
        /**
         * Active
         * 
         * <p>The resource instance is currently in-force.
         */
        ACTIVE("active"),

        /**
         * Cancelled
         * 
         * <p>The resource instance is withdrawn, rescinded or reversed.
         */
        CANCELLED("cancelled"),

        /**
         * Draft
         * 
         * <p>A new resource instance the contents of which is not complete.
         */
        DRAFT("draft"),

        /**
         * Entered In Error
         * 
         * <p>The resource instance was entered in error.
         */
        ENTERED_IN_ERROR("entered-in-error");

        private final java.lang.String value;

        ValueSet(java.lang.String value) {
            this.value = value;
        }

        /**
         * @return
         *     The java.lang.String value of the code represented by this enum
         */
        public java.lang.String value() {
            return value;
        }

        /**
         * Factory method for creating ExplanationOfBenefitStatus.ValueSet values from a passed string value.
         * 
         * @param value
         *     A string that matches one of the allowed code values
         * @throws IllegalArgumentException
         *     If the passed string cannot be parsed into an allowed code value
         */
        public static ValueSet from(java.lang.String value) {
            for (ValueSet c : ValueSet.values()) {
                if (c.value.equals(value)) {
                    return c;
                }
            }
            throw new IllegalArgumentException(value);
        }
    }
}
