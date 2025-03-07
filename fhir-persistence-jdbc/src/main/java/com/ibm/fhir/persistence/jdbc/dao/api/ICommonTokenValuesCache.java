/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.persistence.jdbc.dao.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.ibm.fhir.persistence.jdbc.dao.impl.ResourceTokenValueRec;
import com.ibm.fhir.persistence.jdbc.dto.CommonTokenValue;

/**
 * An interface for a cache of code system and related token values. The
 * cache is specialized in that it supports some specific operations to
 * process list of objects with minimal locking.
 *
 * The code-systems cache can be pre-filled because it is reasonable to
 * expect that it can be sized to accommodate every value. There are
 * likely to be too many unique token-values to cache, so these need
 * to be retrieved on-demand and managed as LRU.
 */
public interface ICommonTokenValuesCache {

    /**
     * Take the records we've touched in the current thread and update the
     * shared LRU maps.
     */
    void updateSharedMaps();

    /**
     * Lookup all the database values we have cached for the code-system names
     * in the given collection. Put any objects with cache misses into the corresponding
     * miss lists (so that we know which records we need to generate inserts for)
     * @param xrefs
     * @param misses the objects we couldn't find in the cache
     */
    void resolveCodeSystems(Collection<ResourceTokenValueRec> tokenValues,
        List<ResourceTokenValueRec> misses);

    /**
     * Look up the ids for the common token values. Must be preceded by
     * resolveCodeSystems to make sure we have code-system ids set for each
     * record. This also means that code-systems which don't yet exist must
     * be created before this method can be called (because we need the id)
     * @param commonTokenValues
     * @param misses the objects we couldn't find in the cache
     */
    void resolveTokenValues(Collection<ResourceTokenValueRec> tokenValues,
        List<ResourceTokenValueRec> system);

     /**
      * Look up the id of the named codeSystem
      * @param codeSystem
      * @return the database identity of the code system, or null if no record was found
      */
     Integer getCodeSystemId(String codeSystem);

    /**
     * Add the id to the local cache
     * @param externalSystemName
     * @param id
     */
    void addCodeSystem(String codeSystem, int id);

     /**
      * Add the CommonTokenValue and id to the local cache
      * @param key
      * @param id
      */
    public void addTokenValue(CommonTokenValue key, long id);

    /**
     * Clear any thread-local cache maps (probably because a transaction was rolled back)
     */
    void clearLocalMaps();

    /**
     * Clear the thread-local and shared caches (for test purposes)
     */
    void reset();

    /**
     * Add the contents of the given codeSystems map to the shared cache. It is assumed
     * that all of these ids are already committed in the database, not newly inserted
     * as part of the current transaction.
     * @param codeSystems
     */
    void prefillCodeSystems(Map<String, Integer> codeSystems);

    /**
     * Get the database common_token_value_id for the given code system and
     * token value.
     * @param codeSystem
     * @param tokenValue
     * @return
     */
    Long getCommonTokenValueId(String codeSystem, String tokenValue);
}
