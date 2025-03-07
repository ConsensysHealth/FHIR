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

@System("http://hl7.org/fhir/history-status")
@Generated("com.ibm.fhir.tools.CodeGenerator")
public class FamilyHistoryStatus extends Code {
    /**
     * Partial
     * 
     * <p>Some health information is known and captured, but not complete - see notes for details.
     */
    public static final FamilyHistoryStatus PARTIAL = FamilyHistoryStatus.builder().value(ValueSet.PARTIAL).build();

    /**
     * Completed
     * 
     * <p>All available related health information is captured as of the date (and possibly time) when the family member 
     * history was taken.
     */
    public static final FamilyHistoryStatus COMPLETED = FamilyHistoryStatus.builder().value(ValueSet.COMPLETED).build();

    /**
     * Entered in Error
     * 
     * <p>This instance should not have been part of this patient's medical record.
     */
    public static final FamilyHistoryStatus ENTERED_IN_ERROR = FamilyHistoryStatus.builder().value(ValueSet.ENTERED_IN_ERROR).build();

    /**
     * Health Unknown
     * 
     * <p>Health information for this family member is unavailable/unknown.
     */
    public static final FamilyHistoryStatus HEALTH_UNKNOWN = FamilyHistoryStatus.builder().value(ValueSet.HEALTH_UNKNOWN).build();

    private volatile int hashCode;

    private FamilyHistoryStatus(Builder builder) {
        super(builder);
    }

    public ValueSet getValueAsEnumConstant() {
        return (value != null) ? ValueSet.from(value) : null;
    }

    /**
     * Factory method for creating FamilyHistoryStatus objects from a passed enum value.
     */
    public static FamilyHistoryStatus of(ValueSet value) {
        switch (value) {
        case PARTIAL:
            return PARTIAL;
        case COMPLETED:
            return COMPLETED;
        case ENTERED_IN_ERROR:
            return ENTERED_IN_ERROR;
        case HEALTH_UNKNOWN:
            return HEALTH_UNKNOWN;
        default:
            throw new IllegalStateException(value.name());
        }
    }

    /**
     * Factory method for creating FamilyHistoryStatus objects from a passed string value.
     * 
     * @param value
     *     A string that matches one of the allowed code values
     * @throws IllegalArgumentException
     *     If the passed string cannot be parsed into an allowed code value
     */
    public static FamilyHistoryStatus of(java.lang.String value) {
        return of(ValueSet.from(value));
    }

    /**
     * Inherited factory method for creating FamilyHistoryStatus objects from a passed string value.
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
     * Inherited factory method for creating FamilyHistoryStatus objects from a passed string value.
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
        FamilyHistoryStatus other = (FamilyHistoryStatus) obj;
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
        public FamilyHistoryStatus build() {
            return new FamilyHistoryStatus(this);
        }
    }

    public enum ValueSet {
        /**
         * Partial
         * 
         * <p>Some health information is known and captured, but not complete - see notes for details.
         */
        PARTIAL("partial"),

        /**
         * Completed
         * 
         * <p>All available related health information is captured as of the date (and possibly time) when the family member 
         * history was taken.
         */
        COMPLETED("completed"),

        /**
         * Entered in Error
         * 
         * <p>This instance should not have been part of this patient's medical record.
         */
        ENTERED_IN_ERROR("entered-in-error"),

        /**
         * Health Unknown
         * 
         * <p>Health information for this family member is unavailable/unknown.
         */
        HEALTH_UNKNOWN("health-unknown");

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
         * Factory method for creating FamilyHistoryStatus.ValueSet values from a passed string value.
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
