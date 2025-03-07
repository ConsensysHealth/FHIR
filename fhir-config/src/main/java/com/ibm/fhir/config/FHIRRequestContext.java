/*
 * (C) Copyright IBM Corp. 2017, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.config;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.fhir.core.HTTPHandlingPreference;
import com.ibm.fhir.core.HTTPReturnPreference;
import com.ibm.fhir.exception.FHIRException;

/**
 * This class is used to hold FHIR REST API context information.
 * There are non-static methods for maintaining individual instances, as
 * well as static methods for setting instances on and getting instances from thread local.
 * The overall strategy is for the REST API servlet filter to retrieve the request context
 * information, create an instance of this class and set it on the current thread for use
 * by the FHIR Server as it processes the request.
 */
public class FHIRRequestContext {
    private static final Logger log = Logger.getLogger(FHIRRequestContext.class.getName());

    // The tenantId. Corresponding tenantKey is retrieved from the configuration on demand
    private String tenantId;

    // The datastore to be used for this request. Usually "default"
    private String dataStoreId;
    private String requestUniqueId;
    private String originalRequestUri;
    private Map<String, List<String>> httpHeaders;

    // Set to true if the REST layer determines the entire request only needs to read from a persistence layer, not write to it
    private boolean readOnly;

    // Set to false automatically, and override when appropriate.
    private boolean bulk = false;

    // Default to the "strict" handling which means the server will reject unrecognized search parameters and elements
    private HTTPHandlingPreference handlingPreference = HTTPHandlingPreference.STRICT;

    // Default to the "minimal" representation which means create/update responses won't return the resource body
    private HTTPReturnPreference returnPreference = HTTPReturnPreference.MINIMAL;

    private Pattern validChars = Pattern.compile("[a-zA-Z0-9_\\-]+");
    private String errorMsg = "Only [a-z], [A-Z], [0-9], '_', and '-' characters are allowed.";

    private static ThreadLocal<FHIRRequestContext> contexts = new ThreadLocal<FHIRRequestContext>() {
        @Override
        public FHIRRequestContext initialValue() {
            try {
                return new FHIRRequestContext(FHIRConfiguration.DEFAULT_TENANT_ID, FHIRConfiguration.DEFAULT_DATASTORE_ID);
            } catch (FHIRException e) {
                throw new IllegalStateException("Unexpected error while initializing FHIRRequestContext");
            }
        }
    };

    public FHIRRequestContext() {
        this.requestUniqueId = UUID.randomUUID().toString();
    }

    public FHIRRequestContext(String tenantId) throws FHIRException {
        this();
        setTenantId(tenantId);
    }

    public FHIRRequestContext(String tenantId, String dataStoreId) throws FHIRException {
        this(tenantId);
        setDataStoreId(dataStoreId);
    }

    public String getTenantId() {
        return tenantId;
    }

    /**
     * Returns true iff the entire request can be processed using only reads in the persistence layer
     * Permits persistence layer implementations to leverage read-only replicas if they are available
     * @return
     */
    public boolean isReadOnly() {
        return this.readOnly;
    }

    /**
     * Setter for the readOnly flag
     * @param flag
     */
    public void setReadOnly(boolean flag) {
        this.readOnly = flag;
    }

    /**
     * the status of the FHIR request
     * @return the status of the FHIRRequest indicating bulk
     */
    public boolean isBulk() {
        return this.bulk;
    }

    /**
     * set the bulk status
     * @param bulk
     */
    public void setBulk(boolean bulk) {
        this.bulk = bulk;
    }

    public void setTenantId(String tenantId) throws FHIRException {
        Matcher matcher = validChars.matcher(tenantId);
        if (matcher.matches()) {
            this.tenantId = tenantId;
        } else {
            throw new FHIRException("Invalid tenantId. " + errorMsg);
        }
    }

    public String getDataStoreId() {
        return dataStoreId;
    }

    public void setDataStoreId(String dataStoreId) throws FHIRException {
        Matcher matcher = validChars.matcher(dataStoreId);
        if (matcher.matches()) {
            this.dataStoreId = dataStoreId;
        } else {
            throw new FHIRException("Invalid dataStoreId. " + errorMsg);
        }
    }

    /**
     * Sets the specified FHIRRequestContext instance on the current thread,
     * so that it can be used by FHIR Server downstream processing.
     * This method is called when the FHIR Server starts processing a request.
     */
    public static void set(FHIRRequestContext context) {
        contexts.set(context);
        if (log.isLoggable(Level.FINEST)) {
            log.finest("FHIRRequestContext.set: " + context.toString());
        }
    }

    /**
     * Returns the FHIRRequestContext on the current thread.
     */
    public static FHIRRequestContext get() {
        FHIRRequestContext result = contexts.get();
        if (log.isLoggable(Level.FINEST)) {
            log.finest("FHIRRequestContext.get: " + result.toString());
        }
        return result;
    }

    /**
     * Removes the FHIRRequestContext that's set on the current thread.
     * This method is called when the FHIR Server is finished processing a request.
     */
    public static void remove() {
        contexts.remove();
        log.finest("FHIRRequestContext.remove invoked.");
    }

    @Override
    public String toString() {
        return "FHIRRequestContext [tenantId=" + tenantId + ", dataStoreId=" + dataStoreId
                + ", this="+ objectHandle(this) + "]";
    }

    public String getRequestUniqueId() {
        return this.requestUniqueId;
    }

    private static String objectHandle(Object obj) {
        return '@' + Integer.toHexString(System.identityHashCode(obj));
    }

    /**
     * @return the handlingPreference
     */
    public HTTPHandlingPreference getHandlingPreference() {
        return handlingPreference;
    }

    /**
     * @param handlingPreference the handlingPreference to set
     */
    public void setHandlingPreference(HTTPHandlingPreference handlingPreference) {
        this.handlingPreference = handlingPreference;
    }

    /**
     * @return the returnPreference
     */
    public HTTPReturnPreference getReturnPreference() {
        return returnPreference;
    }

    /**
     * @param returnPreference the returnPreference to set
     */
    public void setReturnPreference(HTTPReturnPreference returnPreference) {
        this.returnPreference = returnPreference;
    }

    /**
     * @return the originalRequestUri
     */
    public String getOriginalRequestUri() {
        return originalRequestUri;
    }

    /**
     * @param originalRequestUri the originalRequestUri to set
     */
    public void setOriginalRequestUri(String originalRequestUri) {
        this.originalRequestUri = originalRequestUri;
    }

    /**
     * @return the httpHeaders
     */
    public Map<String, List<String>> getHttpHeaders() {
        return httpHeaders;
    }

    /**
     * @param httpHeaders the httpHeaders to set
     */
    public void setHttpHeaders(Map<String, List<String>> httpHeaders) {
        this.httpHeaders = httpHeaders;
    }
}