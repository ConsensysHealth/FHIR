/*
 * (C) Copyright IBM Corp. 2019
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.database.utils.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.ibm.fhir.database.utils.api.IDatabaseAdapter;
import com.ibm.fhir.database.utils.common.CreateIndexStatement;

/**
 * Definition of an index on a table
 */
public class IndexDef {
        
    // The name of the index
    private final String indexName;
    
    // The list of columns comprising the index
    private final List<OrderedColumnDef> indexColumns = new ArrayList<>();
    
    // Is this a unique index?
    private final boolean unique;
    
    // The list of include columns associated with the index
    private final List<String> includeColumns = new ArrayList<>();
    
    @Override
    public String toString() {
        return this.indexName + "(" + indexColumns.stream().map(c -> c.toString()).collect(Collectors.joining(",")) + ")";
    }
    
    public IndexDef(String indexName, Collection<OrderedColumnDef> indexColumns, boolean unique) {
        this.indexName = indexName;
        this.unique = unique;
        this.indexColumns.addAll(indexColumns);
    }
    
    /**
     * Construct an index definition for a unique index with include columns. Note that it only
     * makes sense for an index with include columns to be unique, so the unique flag is set true
     * @param indexName
     * @param indexColumns
     * @param includeColumns
     */
    public IndexDef(String indexName, Collection<OrderedColumnDef> indexColumns, Collection<String> includeColumns) {
        this.indexName = indexName;
        this.unique = true;
        this.indexColumns.addAll(indexColumns);
        this.includeColumns.addAll(includeColumns);
    }

    /**
     * Getter for the unique flag
     * @return
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * Apply this object to the given database target
     * @param tableName
     * @param target
     */
    public void apply(String schemaName, String tableName, String tenantColumnName, IDatabaseAdapter target) {
        if (includeColumns != null && includeColumns.size() > 0) {
            target.createUniqueIndex(schemaName, tableName, indexName, tenantColumnName, indexColumns, includeColumns);
        }
        else if (unique) {
            target.createUniqueIndex(schemaName, tableName, indexName, tenantColumnName, indexColumns);
        }
        else {
            target.createIndex(schemaName, tableName, indexName, tenantColumnName, indexColumns);
        }
    }

    /**
     * Drop this index
     * @param schemaName
     * @param target
     */
    public void drop(String schemaName, IDatabaseAdapter target) {
        target.dropIndex(schemaName, indexName);
    }

    /**
     * Create a statement which can be used to create vendor-specific DDL
     * @param schemaName
     * @param tableName
     * @param tenantColumnName
     * @return
     */
    public CreateIndexStatement createStatement(String schemaName, String tableName, String tenantColumnName) {
        return new CreateIndexStatement(schemaName, indexName, tableName, tenantColumnName, indexColumns);
    }
}
