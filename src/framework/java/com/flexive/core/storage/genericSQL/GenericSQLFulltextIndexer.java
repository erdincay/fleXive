/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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

import com.flexive.core.DatabaseConst;
import static com.flexive.core.DatabaseConst.TBL_CONTENT_DATA_FT;
import com.flexive.core.storage.FulltextIndexer;
import com.flexive.shared.FxXMLUtils;
import com.flexive.shared.content.FxDelta;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.content.FxPropertyData;
import com.flexive.shared.value.FxBinary;
import com.flexive.shared.value.FxValue;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Fulltext indexer (generic SQL implementation)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @since 3.1
 */
public class GenericSQLFulltextIndexer implements FulltextIndexer {
    protected static final Log LOG = LogFactory.getLog(GenericSQLFulltextIndexer.class);

    protected static final String CONTENT_DATA_FT_REMOVE_VERSION = "DELETE FROM " + TBL_CONTENT_DATA_FT + " WHERE ID=? AND VER=?";
    protected static final String CONTENT_DATA_FT_REMOVE = "DELETE FROM " + TBL_CONTENT_DATA_FT + " WHERE ID=?";
    protected static final String CONTENT_DATA_FT_REMOVE_TYPE = "DELETE FROM " + TBL_CONTENT_DATA_FT + " WHERE ASSIGN IN (SELECT DISTINCT ID FROM " + DatabaseConst.TBL_STRUCT_ASSIGNMENTS + " WHERE TYPEDEF=?)";

    protected static final String CONTENT_FULLTEXT_INSERT = "INSERT INTO " + TBL_CONTENT_DATA_FT + "(ID,VER,LANG,ASSIGN,XMULT,VALUE) VALUES (?,?,?,?,?,?)";
    //                                                                                                    1          2         3          4            5           6
    protected static final String CONTENT_FULLTEXT_UPDATE = "UPDATE " + TBL_CONTENT_DATA_FT + " SET VALUE=? WHERE ID=? AND VER=? AND LANG=? AND ASSIGN=? AND XMULT=?";
    protected static final String CONTENT_FULLTEXT_DELETE_ALL = "DELETE FROM " + TBL_CONTENT_DATA_FT + " WHERE ID=? AND VER=? AND ASSIGN=?";
    protected static final String CONTENT_FULLTEXT_DELETE = "DELETE FROM " + TBL_CONTENT_DATA_FT + " WHERE ID=? AND VER=? AND ASSIGN=? AND LANG=?";


    private FxPK pk;
    private Connection con;
    private PreparedStatement psu, psi, psd, psd_all;

    /**
     * {@inheritDoc}
     */
    public void init(FxPK pk, Connection con) {
        this.pk = pk;
        this.con = con;
        if (pk != null) {
            try {
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
        } else {
            psu = null;
            psi = null;
            psd = null;
            psd_all = null;
        }
    }

    protected String getDeleteSql() {
        return CONTENT_FULLTEXT_DELETE;
    }

    protected String getDeleteAllSql() {
        return CONTENT_FULLTEXT_DELETE_ALL;
    }

    protected String getInsertSql() {
        return CONTENT_FULLTEXT_INSERT;
    }

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
        if (pk == null || psi == null || psu == null || psd == null || psd_all == null) {
            LOG.warn("Tried to index FxDeltaChange with no pk provided!");
            return;
        }
        if (change.isGroup())
            return; //only index properties
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
                        data = prepare(FxXMLUtils.getElementData(((FxBinary) value).getTranslation(lang).getMetadata(), "compressed"));
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
            ps = con.prepareStatement("UPDATE " + TBL_CONTENT_DATA_FT + " SET LANG=? WHERE LANG=? AND ASSIGN=?");
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
            ps = con.prepareStatement("UPDATE " + TBL_CONTENT_DATA_FT + " SET LANG=? WHERE ASSIGN=?");
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
    public void commitChanges() {
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
}
