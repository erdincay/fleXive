/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License version 2.1 or higher as published by the Free Software Foundation.
 *
 *  The GNU Lesser General Public License can be found at
 *  http://www.gnu.org/licenses/lgpl.html.
 *  A copy is found in the textfile LGPL.txt and important notices to the
 *  license from the author are found in LICENSE.txt distributed with
 *  these libraries.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  For further information about UCS - unique computing solutions gmbh,
 *  please see the company website: http://www.ucs.at
 *
 *  For further information about [fleXive](R), please see the
 *  project website: http://www.flexive.org
 *
 *
 *  This copyright notice MUST APPEAR in all copies of the file!
 ***************************************************************/
package com.flexive.core.storage.genericSQL;

import com.flexive.core.Database;
import com.flexive.core.DatabaseConst;
import com.flexive.core.flatstorage.FxFlatStorageInfo;
import com.flexive.core.storage.ContentStorage;
import com.flexive.core.storage.DBStorage;
import com.flexive.core.storage.FulltextIndexer;
import com.flexive.core.storage.StorageManager;
import com.flexive.shared.FxXMLUtils;
import com.flexive.shared.XPathElement;
import com.flexive.shared.content.FxDelta;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.content.FxPropertyData;
import com.flexive.shared.exceptions.FxDbException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.structure.FxDataType;
import com.flexive.shared.structure.FxProperty;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.TypeStorageMode;
import com.flexive.shared.value.FxBinary;
import com.flexive.shared.value.FxValue;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.*;

import static com.flexive.core.DatabaseConst.TBL_CONTENT_DATA_FT;
import static com.flexive.core.flatstorage.FxFlatStorageInfo.Type.TypeGroups;

/**
 * Fulltext indexer (generic SQL implementation)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @since 3.1
 */
@SuppressWarnings({"ThrowableInstanceNeverThrown"})
public class GenericSQLFulltextIndexer implements FulltextIndexer {
    protected static final Log LOG = LogFactory.getLog(GenericSQLFulltextIndexer.class);

    protected static final String CONTENT_DATA_FT_REMOVE = "DELETE FROM " + TBL_CONTENT_DATA_FT + " WHERE ID=?";
    protected static final String CONTENT_DATA_FT_REMOVE_TYPE = "DELETE FROM " + TBL_CONTENT_DATA_FT + " WHERE ASSIGN IN (SELECT DISTINCT ID FROM " + DatabaseConst.TBL_STRUCT_ASSIGNMENTS + " WHERE TYPEDEF=?)";
    protected static final String CONTENT_FULLTEXT_REMOVE_ALL = "DELETE FROM " + TBL_CONTENT_DATA_FT;
    protected static final String CONTENT_FULLTEXT_REMOVE_PROPERTY = "DELETE FROM " + TBL_CONTENT_DATA_FT +
            " WHERE ASSIGN IN (SELECT DISTINCT ID FROM " + DatabaseConst.TBL_STRUCT_ASSIGNMENTS + " WHERE APROPERTY=?)";
    protected static final String CONTENT_FULLTEXT_INSERT = "INSERT INTO " + TBL_CONTENT_DATA_FT + "(ID,VER,LANG,ASSIGN,XMULT,VALUE) VALUES (?,?,?,?,?,?)";
    //                                                                                                    1          2         3          4            5           6
    protected static final String CONTENT_FULLTEXT_UPDATE = "UPDATE " + TBL_CONTENT_DATA_FT + " SET VALUE=? WHERE ID=? AND VER=? AND LANG=? AND ASSIGN=? AND XMULT=?";
    protected static final String CONTENT_FULLTEXT_DELETE = "DELETE FROM " + TBL_CONTENT_DATA_FT + " WHERE ID=? AND VER=? AND ASSIGN=?";
    protected static final String CONTENT_FULLTEXT_DELETE_LANG = "DELETE FROM " + TBL_CONTENT_DATA_FT + " WHERE ID=? AND VER=? AND ASSIGN=? AND LANG=?";
    protected static final String CONTENT_FULLTEXT_SET_LANG = "UPDATE " + TBL_CONTENT_DATA_FT + " SET LANG=? WHERE ASSIGN=?";
    protected static final String CONTENT_FULLTEXT_CHANGE_LANG = "UPDATE " + TBL_CONTENT_DATA_FT + " SET LANG=? WHERE LANG=? AND ASSIGN=?";


    private FxPK pk;
    protected Connection con;
    private PreparedStatement psu, psi, psd, psd_all;
    private boolean initializedStatements;

    /**
     * {@inheritDoc}
     */
    public void init(FxPK pk, Connection con) {
        this.pk = pk;
        this.con = con;
    }

    /**
     * Get the delete statement, allows storage specific implementations to provide different statements
     *
     * @return delete statement
     */
    protected String getDeleteSql() {
        return CONTENT_FULLTEXT_DELETE_LANG;
    }

    /**
     * Get the delete all statement, allows storage specific implementations to provide different statements
     *
     * @return delete all statement
     */
    protected String getDeleteAllSql() {
        return CONTENT_FULLTEXT_DELETE;
    }

    /**
     * Get the insert statement, allows storage specific implementations to provide different statements
     *
     * @return insert statement
     */
    protected String getInsertSql() {
        return CONTENT_FULLTEXT_INSERT;
    }

    /**
     * Get the update statement, allows storage specific implementations to provide different statements
     *
     * @return update statement
     */
    protected String getUpdateSql() {
        return CONTENT_FULLTEXT_UPDATE;
    }

    /**
     * Prepare a value to for indexing (uppercase, trim, etc)
     *
     * @param value the value to prepare
     * @return prepared value
     */
    protected String prepare(String value) {
        if (value == null)
            value = "";
        return value.trim().toUpperCase();
    }

    /**
     * {@inheritDoc}
     */
    public void index(FxPropertyData data) {
        initStatements();
        if (pk == null || psi == null || psu == null || psd == null || psd_all == null) {
            LOG.warn("Tried to index FxPropertyData with no pk provided!");
            return;
        }
        FxValue value = data.getValue();
        try {
            psi.setLong(4, data.getAssignmentId());
            psi.setString(5, StringUtils.join(ArrayUtils.toObject(data.getIndices()), ','));
            for (long lang : value.getTranslatedLanguages()) {
                String idata;
                try {
                    if (value instanceof FxBinary)
                        idata = prepare(FxXMLUtils.getElementData(((FxBinary) value).getTranslation(lang).getMetadata(), "text"));
                    else
                        idata = prepare(String.valueOf(value.getTranslation(lang)));
                } catch (Exception e) {
                    LOG.error("Failed to fetch indexing data for " + data);
                    continue;
                }
                if (idata.length() == 0)
                    continue;
                psi.setLong(3, lang);
                psi.setString(6, idata);
                psi.executeUpdate();
            }
        } catch (SQLException e) {
            LOG.error(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void index(FxDelta.FxDeltaChange change) {
        initStatements();
        if (pk == null || psi == null || psu == null || psd == null || psd_all == null) {
            LOG.warn("Tried to index FxDeltaChange with no pk provided!");
            return;
        }
        if (change.isGroup())
            return; //only index properties
        try {
            FxProperty prop = change.getNewData() != null
                    ? ((FxPropertyAssignment) change.getNewData().getAssignment()).getProperty()
                    : ((FxPropertyAssignment) change.getOriginalData().getAssignment()).getProperty();
            if (!prop.getDataType().isTextType())
                return; //make sure only text types are fulltext indexed
            if (!prop.isFulltextIndexed()) {
                return; // respect fulltext flag for updates
            }
        } catch (Exception e) {
            //could not get the property, return
            LOG.error("Could not retrieve the used FxProperty for change " + change + "!");
            return;
        }
        if (change.getNewData() == null) {
            //data removed
            try {
                psd_all.setLong(3, change.getOriginalData().getAssignmentId());
                psd_all.executeUpdate();
            } catch (SQLException e) {
                LOG.error(e);
            }
            return;
        }

        if (change.getOriginalData() == null) {
            //add
            index((FxPropertyData) change.getNewData());
            return;
        }

        //update, try to update and if not found insert
        try {
            final String xmult = StringUtils.join(ArrayUtils.toObject(change.getNewData().getIndices()), ',');
            psu.setLong(5, change.getNewData().getAssignmentId());
            psu.setString(6, xmult);
            psi.setLong(4, change.getNewData().getAssignmentId());
            psi.setString(5, xmult);
            FxValue value = ((FxPropertyData) change.getNewData()).getValue();
            String data;
            long[] newLang = value.getTranslatedLanguages();
            for (long lang : newLang) {
                try {
                    if (value instanceof FxBinary)
                        data = prepare(FxXMLUtils.getElementData(((FxBinary) value).getTranslation(lang).getMetadata(), "text"));
                    else
                        data = prepare(String.valueOf(value.getTranslation(lang)));
                } catch (Exception e) {
                    LOG.error("Failed to fetch indexing data for " + change);
                    continue;
                }
                if (data.length() == 0)
                    continue;
                psu.setString(1, data);
                psu.setLong(4, lang);
                if (psu.executeUpdate() == 0) {
                    psi.setLong(3, lang);
                    psi.setString(6, data);
                    psi.executeUpdate();
                }
            }
            for (long lang : ((FxPropertyData) change.getOriginalData()).getValue().getTranslatedLanguages()) {
                if (!ArrayUtils.contains(newLang, lang)) {
                    //delete lang entry
                    psd.setLong(3, change.getOriginalData().getAssignmentId());
                    psd.setLong(4, lang);
                    psd.executeUpdate();
                }
            }
        } catch (SQLException e) {
            LOG.error(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void remove() {
        if (pk == null) {
            LOG.warn("Tried to remove a fulltext version with no pk provided!");
            return;
        }
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(CONTENT_DATA_FT_REMOVE + " AND VER=?");
            ps.setLong(1, pk.getId());
            ps.setInt(2, pk.getVersion());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.error(e);
        } finally {
            try {
                if (ps != null) ps.close();
            } catch (SQLException e) {
                LOG.error(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void remove(long assignmentId) {
        if (pk == null) {
            LOG.warn("Tried to remove a fulltext version with no pk provided!");
            return;
        }
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(CONTENT_DATA_FT_REMOVE + " AND VER=? AND ASSIGN=?");
            ps.setLong(1, pk.getId());
            ps.setInt(2, pk.getVersion());
            ps.setLong(3, assignmentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.error(e);
        } finally {
            try {
                if (ps != null) ps.close();
            } catch (SQLException e) {
                LOG.error(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeAllVersions() {
        if (pk == null) {
            LOG.warn("Tried to remove all fulltext versions with no pk provided!");
            return;
        }
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(CONTENT_DATA_FT_REMOVE);
            ps.setLong(1, pk.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.error(e);
        } finally {
            try {
                if (ps != null) ps.close();
            } catch (SQLException e) {
                LOG.error(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeType(long typeId) {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(CONTENT_DATA_FT_REMOVE_TYPE);
            ps.setLong(1, typeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.error(e);
        } finally {
            try {
                if (ps != null) ps.close();
            } catch (SQLException e) {
                LOG.error(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void changeLanguage(long assignmentId, long oldLanguage, long newLanguage) {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(CONTENT_FULLTEXT_CHANGE_LANG);
            ps.setLong(1, oldLanguage);
            ps.setLong(2, newLanguage);
            ps.setLong(3, assignmentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.error(e);
        } finally {
            try {
                if (ps != null) ps.close();
            } catch (SQLException e) {
                LOG.error(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setLanguage(long assignmentId, long lang) {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(CONTENT_FULLTEXT_SET_LANG);
            ps.setLong(1, lang);
            ps.setLong(2, assignmentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.error(e);
        } finally {
            try {
                if (ps != null) ps.close();
            } catch (SQLException e) {
                LOG.error(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void commitChanges() throws SQLException {
        //this implementation commits on index(..)
    }

    /**
     * {@inheritDoc}
     */
    public void cleanup() {
        try {
            if (psi != null) psi.close();
            if (psu != null) psu.close();
            if (psd != null) psd.close();
            if (psd_all != null) psd_all.close();
        } catch (SQLException e) {
            LOG.error(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void rebuildIndex() {
        removeIndexForProperty(-1);
        rebuildIndexForProperty(-1);
    }

    /**
     * {@inheritDoc}
     */
    public void rebuildIndexForProperty(long propertyId) {
        PreparedStatement ps = null;
        Statement stmtAssignment = null;
        Statement stmtFetch = null;
        try {
            final DBStorage storage = StorageManager.getStorageImpl();
            final ContentStorage contentStorage = storage.getContentStorage(TypeStorageMode.Hierarchical);
            final String TRUE = storage.getBooleanTrueExpression();
            stmtAssignment = con.createStatement();
            stmtFetch = con.createStatement();
            //                                                   1     2           3         4          5          6    7        8     9              10         11
            ResultSet rs = stmtAssignment.executeQuery("SELECT a.id, p.datatype, m.typeid, m.tblname, m.colname, m.lvl, a.xpath, p.id, p.sysinternal, s.tbltype, m.group_assid " +
                    "FROM " + DatabaseConst.TBL_STRUCT_PROPERTIES + " p, " + DatabaseConst.TBL_STRUCT_ASSIGNMENTS + " a LEFT JOIN " +
                    DatabaseConst.TBL_STRUCT_FLATSTORE_MAPPING + " m ON (m.assid=a.id) LEFT JOIN " +
                    DatabaseConst.TBL_STRUCT_FLATSTORE_INFO + " s ON (m.tblname=s.tblname)" +
                    "WHERE p.ISFULLTEXTINDEXED=" + TRUE + " AND a.APROPERTY=p.id" +
                    (propertyId >= 0 ? " AND p.id=" + propertyId : "") +
                    " ORDER BY p.datatype");
            //1..ID,2..VER,3..LANG,4..ASSIGN,5..XMULT,6..VALUE
            ps = con.prepareStatement(getInsertSql());
            int batchCounter;
            long totalCount = 0;
            while (rs != null && rs.next()) {
                long assignmentId = rs.getLong(1);
                ps.setLong(4, assignmentId); //assignment id
                long currentPropertyId = rs.getLong(8);
                FxDataType dataType = FxDataType.getById(rs.getInt(2));
                boolean systemInternalProperty = rs.getBoolean(9);
                if (systemInternalProperty || !dataType.isTextType())
                    continue; //do not index system internal properties or non-text properties
                long flatTypeId = rs.getLong(3);
                if (!rs.wasNull()) {
                    //flatstorage
                    String xpath = rs.getString(7);
                    final String xmult;
                    int xpathDepth = 1;
                    if (!StringUtils.isEmpty(xpath)) {
                        xpathDepth = XPathElement.getDepth(xpath);
                        xmult = XPathElement.addDefaultIndices("", xpathDepth);
                    } else {
                        xmult = "1";
                    }
                    ps.setString(5, xmult);
                    final FxFlatStorageInfo.Type flatType = FxFlatStorageInfo.Type.forId(rs.getInt(10));
                    //                                                1  2   3      4
                    ResultSet rsData = stmtFetch.executeQuery("SELECT ID,VER,LANG," + rs.getString(5) +
                            //                          5
                            (flatType == TypeGroups ? ",XMULT" : "") +
                            " FROM " + rs.getString(4) +
                            " WHERE TYPEID=" + flatTypeId + " AND LVL=" + rs.getInt(6) +
                            (flatType == TypeGroups ? " AND GROUP_ASSID=" + rs.getLong(11) : ""));
                    batchCounter = 0;
                    while (rsData != null && rsData.next()) {
                        String value = rsData.getString(4);
                        if (rsData.wasNull() || StringUtils.isEmpty(value))
                            continue;
                        ps.setLong(1, rsData.getLong(1));
                        ps.setInt(2, rsData.getInt(2));
                        ps.setInt(3, rsData.getInt(3));
                        if (flatType == TypeGroups) {
                            // xmult is based on the base group's xmult
                            final String groupXMult = rsData.getString(5);
                            final int missingIndices = Math.max(0, xpathDepth - XPathElement.getDepth(groupXMult));
                            ps.setString(5, XPathElement.addDefaultIndices(groupXMult, missingIndices));
                        }
                        ps.setString(6, value.trim().toUpperCase());
                        ps.addBatch();
                        batchCounter++;
                        totalCount++;
                        if (batchCounter % 1000 == 0)
                            ps.executeBatch(); //insert every 1000 rows
                    }
                    if (rsData != null)
                        rsData.close();
                    ps.executeBatch();
                } else {
                    //hierarchical storage
                    String[] columns = contentStorage.getColumns(currentPropertyId, systemInternalProperty, dataType);

                    final String columnSelect = dataType == FxDataType.Binary
                            ? "b.XMLMETA"
                            : "d." + columns[0]; //first column is the one responsible for indexing
                    final String tableAppend = dataType == FxDataType.Binary
                            ? "," + DatabaseConst.TBL_CONTENT_BINARY + " b"
                            : "";
                    final String whereAppend = dataType == FxDataType.Binary
                            ? " AND b.ID=d." + columns[0] + " AND b.VER=1 AND b.QUALITY=1"
                            : "";
                    //                                                  1    2     3      4               5
                    ResultSet rsData = stmtFetch.executeQuery("SELECT d.ID,d.VER,d.LANG,d.XMULT," + columnSelect +
                            " FROM " + DatabaseConst.TBL_CONTENT_DATA + " d" + tableAppend +
                            " WHERE d.ASSIGN=" + assignmentId + whereAppend);
                    batchCounter = 0;
                    while (rsData != null && rsData.next()) {
                        String value = rsData.getString(4);
                        if (rsData.wasNull() || StringUtils.isEmpty(value))
                            continue;
                        ps.setLong(1, rsData.getLong(1));
                        ps.setInt(2, rsData.getInt(2));
                        ps.setInt(3, rsData.getInt(3));
                        ps.setString(5, rsData.getString(4));
                        final String ftValue;
                        switch (dataType) {
                            case Binary:
                                final String xmlMeta = rsData.getString(5);
                                ftValue = FxXMLUtils.getElementData(xmlMeta, "text");
                                break;
                            case HTML:
                            case String1024:
                            case Text:
                                ftValue = rsData.getString(5);
                                break;
                            default:
                                ftValue = null;
                        }
                        if (!StringUtils.isEmpty(ftValue)) {
                            ps.setString(6, ftValue.trim().toUpperCase());
                            ps.addBatch();
                            batchCounter++;
                            totalCount++;
                        }
                        if (batchCounter % 1000 == 0)
                            ps.executeBatch(); //insert every 1000 rows
                    }
                    if (rsData != null)
                        rsData.close();
                    ps.executeBatch();
                }


            }
            LOG.info("Added " + totalCount + " entries to fulltext index.");
            commitChanges();
        } catch (SQLException e) {
            throw new FxDbException(e, "ex.db.sqlError", e.getMessage()).asRuntimeException();
        } catch (FxNotFoundException e) {
            //ContentStorage was not found
            throw e.asRuntimeException();
        } finally {
            Database.closeObjects(GenericSQLFulltextIndexer.class, ps, stmtAssignment, stmtFetch);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeIndexForProperty(long propertyId) {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement((propertyId < 0
                    ? CONTENT_FULLTEXT_REMOVE_ALL
                    : CONTENT_FULLTEXT_REMOVE_PROPERTY));
            if (propertyId >= 0)
                ps.setLong(1, propertyId);
            int removed = ps.executeUpdate();
            LOG.info("Removed " + removed + " fulltext entries.");
            commitChanges();
        } catch (SQLException e) {
            throw new FxDbException(e, "ex.db.sqlError", e.getMessage()).asRuntimeException();
        } finally {
            Database.closeObjects(GenericSQLFulltextIndexer.class, ps);
        }
    }

    private void initStatements() {
        if (!initializedStatements && pk != null) {
            try {
                initializedStatements = true;
                psu = con.prepareStatement(getUpdateSql());
                psu.setLong(2, pk.getId());
                psu.setInt(3, pk.getVersion());
                psi = con.prepareStatement(getInsertSql());
                psi.setLong(1, pk.getId());
                psi.setInt(2, pk.getVersion());
                psd_all = con.prepareStatement(getDeleteAllSql());
                psd_all.setLong(1, pk.getId());
                psd_all.setInt(2, pk.getVersion());
                psd = con.prepareStatement(getDeleteSql());
                psd.setLong(1, pk.getId());
                psd.setInt(2, pk.getVersion());
            } catch (SQLException e) {
                LOG.error(e);
                psu = null;
                psi = null;
                psd = null;
                psd_all = null;
            }
        }
    }
}
