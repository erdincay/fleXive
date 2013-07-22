package com.flexive.ejb.beans.configuration;

import com.flexive.core.Database;
import com.flexive.core.DatabaseConst;
import com.flexive.core.storage.DBStorage;
import com.flexive.core.storage.GroupPositionsProvider;
import com.flexive.core.storage.StorageManager;
import com.flexive.shared.FxArrayUtils;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.sql.*;

/**
 * Implementation of {@link DatabasePatchUtilsLocal}.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
@Stateless
public class DatabasePatchUtils implements DatabasePatchUtilsLocal {
    private static final Log LOG = LogFactory.getLog(DatabasePatchUtils.class);

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void migrateContentDataGroups() throws SQLException {
        final long start = System.currentTimeMillis();
        LOG.info("Migrating group positions from " + DatabaseConst.TBL_CONTENT_DATA + " to " + DatabaseConst.TBL_CONTENT);
        int instanceCount = 0;
        int versionCount = 0;

        final DBStorage storage = StorageManager.getStorageImpl();
        //                         1   2    3    4       5
        final String sql = "SELECT id, ver, pos, assign, xmult\n"
                + "FROM " + DatabaseConst.TBL_CONTENT_DATA + "\n"
                + "WHERE isgroup=" + storage.getBooleanExpression(true) + "\n"
                + "ORDER BY id, ver, assign";
        final Connection con = Database.getDbConnection();
        Statement stmt = null;
        Statement ddlStmt = null;
        PreparedStatement updateStmt = null;
        try {
            updateStmt = con.prepareStatement("UPDATE " + DatabaseConst.TBL_CONTENT + " SET group_pos=? WHERE id=? AND ver=?");

            stmt = con.createStatement();

            final ResultSet rs = stmt.executeQuery(sql);

            long id = -1;
            int version = -1;
            long assignmentId = -1;
            int updateCount = 0;
            GroupPositionsProvider.Builder builder = null;
            while (rs.next()) {
                final long nextId = rs.getLong(1);
                final int nextVer = rs.getInt(2);
                if (nextId != id || nextVer != version) {
                    if (builder != null && !builder.isEmpty()) {
                        // update FX_CONTENT
                        addMigratedGroupPos(updateStmt, id, version, builder);
                        updateCount++;
                        if (updateCount > 100) {
                            updateStmt.executeBatch();
                            updateCount = 0;
                        }
                    }

                    if (nextId != id) {
                        instanceCount++;
                    }
                    versionCount++;

                    id = nextId;
                    version = nextVer;
                    assignmentId = -1;
                    builder = GroupPositionsProvider.builder();
                }
                final int pos = rs.getInt(3);
                final long nextAssignmentId = rs.getLong(4);
                if (assignmentId != nextAssignmentId) {
                    assignmentId = nextAssignmentId;
                    builder.startAssignment(assignmentId);
                }
                final String xmult = rs.getString(5);
                try {
                    builder.addPos(FxArrayUtils.toIntArray(xmult, ','), pos);
                } catch (FxInvalidParameterException e) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Invalid xmult: " + xmult);
                    }
                }
            }

            if (builder != null && !builder.isEmpty()) {
                addMigratedGroupPos(updateStmt, id, version, builder);
            }

            updateStmt.executeBatch();

            // remove group records
            stmt.executeUpdate("DELETE FROM " + DatabaseConst.TBL_CONTENT_DATA + " WHERE isgroup=" + storage.getBooleanTrueExpression());

        } finally {
            Database.closeObjects(DivisionConfigurationEngineBean.class, stmt, updateStmt, ddlStmt);
            Database.closeObjects(DivisionConfigurationEngineBean.class, con, null);
        }

        LOG.info("Migrated " + instanceCount + " instances (in " + versionCount + " versions) in "
                + (System.currentTimeMillis() - start) + " milliseconds");
    }

    private void addMigratedGroupPos(PreparedStatement updateStmt, long id, int version, GroupPositionsProvider.Builder builder) throws SQLException {
        updateStmt.setString(1, builder.build());
        updateStmt.setLong(2, id);
        updateStmt.setInt(3, version);
        updateStmt.addBatch();
    }


}
