package com.flexive.ejb.beans.configuration;

import java.sql.SQLException;

/**
 * Custom database patching scripts that require custom transaction handling
 * (and should not be exposed in "public" EJB interfaces).
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public interface DatabasePatchUtilsLocal {
    /**
     * Migration helper for DB version 2863: migrate group positions from FX_CONTENT_DATA to FX_CONTENT.
     *
     * @throws java.sql.SQLException on SQL errors
     */
    void migrateContentDataGroups() throws SQLException;
}
