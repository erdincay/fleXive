package com.flexive.core.storage.PostgreSQL;

import com.flexive.core.DatabaseConst;
import com.flexive.core.storage.DBStorage;
import com.flexive.core.storage.GenericDivisionImporter;
import com.flexive.core.storage.StorageManager;

import java.sql.Connection;
import java.sql.Statement;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * PostgreSQL specific importer implementation.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class PostgreSQLDivisionImporter extends GenericDivisionImporter {
    private static final PostgreSQLDivisionImporter INSTANCE = new PostgreSQLDivisionImporter();

    private PostgreSQLDivisionImporter() {
    }

    public static PostgreSQLDivisionImporter getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean importRequiresNonTXConnection() {
        // need custom transaction management in importAssignments
        return true;
    }

    @Override
    protected void importAssignments(Connection con, ZipFile zip, ZipEntry ze, Statement stmt) throws Exception {
        DBStorage storage = StorageManager.getStorageImpl();
        boolean success = false;
        try {
            // start a transaction since disabling foreign keys only affects transactions
            stmt.execute("BEGIN");

            stmt.execute(storage.getReferentialIntegrityChecksStatement(false));

            importTable(stmt, zip, ze, "structures/assignments/assignment", DatabaseConst.TBL_STRUCT_ASSIGNMENTS, true, true, "base:0", "parentgroup:%id", "pos:@", "KEY:id");

            con.commit();
            success = true;
        } finally {
            if (!success) {
                con.rollback();
            }
            stmt.execute(storage.getReferentialIntegrityChecksStatement(true));
        }
    }
}
