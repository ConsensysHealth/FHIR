/*
 * (C) Copyright IBM Corp. 2019, 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.database.utils.api;

import java.sql.SQLException;
import java.util.Properties;

import com.ibm.fhir.database.utils.model.DbType;

/**
 * Lets us adjust DDL/DML/SQL statements to match the target database. This
 * is needed because DB2 and Derby have a few differences, and we need to
 * tweak the SQL in order to support all the unit tests we want/need
 */
public interface IDatabaseTranslator {

    /**
     * Are we working with a Derby database
     * @return
     */
    boolean isDerby();

    /**
     * Append FOR UPDATE/FOR UPDATE WITH RS depending on the target DB type
     * @param sql
     * @return
     */
    String addForUpdate(String sql);

    /**
     * Get the proper table name based on the type of database we
     * are connected to. Derby has its own handling of temp tables
     * @param tableName
     * @return
     */
    String globalTempTableName(String tableName);

    /**
     * @param ddl
     * @return
     */
    String createGlobalTempTable(String ddl);

    /**
     * Compose a select statement to obtain the next value from the
     * named sequence
     * @param schemaName
     * @param sequenceName
     * @return
     */
    String selectSequenceNextValue(String schemaName, String sequenceName);

    /**
     * Statement for getting the next value (for use in a select list or
     * insert values)
     * @param schemaName
     * @param sequenceName
     * @return
     */
    String nextValue(String schemaName, String sequenceName);

    /**
     * Check the exception to see if it is reporting a duplicate value constraint violation
     * @param x
     * @return
     */
    boolean isDuplicate(SQLException x);

    /**
     * Check the exception to see if it is reporting that THE NAME OF THE OBJECT TO BE CREATED
     * OR THE TARGET OF A RENAME STATEMENT IS IDENTICAL TO THE EXISTING NAME OF THE OBJECT TYPE
     * @param x
     * @return
     */
    boolean isAlreadyExists(SQLException x);

    /**
     * Database timed out waiting to get a lock. This is not the same as a deadlock, of course
     * @param x
     * @return
     */
    boolean isLockTimeout(SQLException x);

    /**
     * Was this statement the victim of a deadlock
     * @param x
     * @return
     */
    boolean isDeadlock(SQLException x);

    /**
     * Returns true if the exception represents a connection error
     * @param x
     * @return
     */
    boolean isConnectionError(SQLException x);

    /**
     * Get an appropriate instance of ReplicatorException to throw
     * depending on the details of SQLException
     * @param x
     * @return
     */
    DataAccessException translate(SQLException x);

    /**
     * Returns true if the SQLException is indicating an object is undefined
     * (e.g. "DROP TABLE foo.bar", where table "foo.bar" doesn't exist)
     * @param x
     * @return
     */
    boolean isUndefinedName(SQLException x);

    /**
     * Configure the properties using information from the ConnectionDetails
     * @param p
     * @param cd
     */
    void fillProperties(Properties p, ConnectionDetails cd);

    /**
     * Returns an expression which computes the timestamp difference
     * between left and right in seconds
     * @param left
     * @param right
     * @param alias adds " AS alias " if alias is not null
     * @return
     */
    String timestampDiff(String left, String right, String alias);

    /**
     * Get the "CURRENT TIMESTAMP" string for the database type
     * @return
     */
    String currentTimestampString();

    /**
     * Craft the DDL for a CREATE SEQUENCE statement
     * @param name
     * @param cache the number of sequence values to cache, if supported by the database
     * @return
     */
    String createSequence(String name, int cache);

    /**
     * Return the REORG TABLE command if supported, or null otherwise
     * @param tableName
     * @return
     */
    String reorgTableCommand(String tableName);

    /**
     * Get the driver class to use for connections
     * @return
     */
    String getDriverClassName();

    /**
     * Get the correct LIMIT/FETCH NEXT ROWS clause for the database
     * @param arg a literal int or ? bind-variable marker
     * @return
     */
    String limit(String arg);

    /**
     * Get the JDBC connection URL based on the properties
     * @param connectionProperties
     * @return
     */
    String getUrl(Properties connectionProperties);

    /**
     * Does the database support inlining for clobs
     * @return
     */
    boolean clobSupportsInline();

    /**
     * The main type of the database
     * @return
     */
    DbType getType();

    /**
     * The name of the "DUAL" table...that special table giving us one row/column.
     * @return the name of the "DUAL" table for the database, or null if not supported
     */
    String dualTableName();

    /**
     * Generate the DDL for dropping the named FK constraint from the given table
     * @param qualifiedTableName such as schema.foo_tab
     * @param constraintName the constraint name of the FK
     * @return
     */
    String dropForeignKeyConstraint(String qualifiedTableName, String constraintName);

    /**
     * Does this database use the schema prefix when defining indexes
     * @return
     */
    default boolean isIndexUseSchemaPrefix() { return true; }
}
