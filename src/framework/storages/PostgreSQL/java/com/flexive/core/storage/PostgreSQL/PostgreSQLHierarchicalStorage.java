/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
package com.flexive.core.storage.PostgreSQL;

import com.flexive.core.DatabaseConst;
import com.flexive.core.storage.ContentStorage;
import com.flexive.core.storage.FulltextIndexer;
import com.flexive.core.storage.genericSQL.GenericHierarchicalStorage;
import com.flexive.core.storage.genericSQL.GenericBinarySQLStorage;
import com.flexive.shared.exceptions.FxDbException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.content.FxPK;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Postgres implementation of hierarchical content handling
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class PostgreSQLHierarchicalStorage extends GenericHierarchicalStorage {

    private static final Log LOG = LogFactory.getLog(PostgreSQLHierarchicalStorage.class);

    private static final PostgreSQLHierarchicalStorage instance = new PostgreSQLHierarchicalStorage();

    /**
     * Ctor
     */
    public PostgreSQLHierarchicalStorage() {
        super(new GenericBinarySQLStorage());
    }

    /**
     * Singleton getter
     *
     * @return ContentStorage
     */
    public static ContentStorage getInstance() {
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void lockTables(Connection con, long id, int version) throws FxRuntimeException {
        try {
            PreparedStatement ps = null;
            try {
                String ver = version <= 0 ? "" : " AND VER=?";
                ps = con.prepareStatement("SELECT * FROM " + DatabaseConst.TBL_CONTENT + " WHERE ID=?" + ver + " FOR UPDATE");
                ps.setLong(1, id);
                if (version > 0) ps.setInt(2, version);
                ps.executeQuery();
                ps.close();
                ps = con.prepareStatement("SELECT * FROM " + DatabaseConst.TBL_CONTENT_DATA + " WHERE ID=?" + ver + " FOR UPDATE");
                ps.setLong(1, id);
                if (version > 0) ps.setInt(2, version);
                ps.executeQuery();
                ps.close();
                ps = con.prepareStatement("SELECT * FROM " + DatabaseConst.TBL_CONTENT_BINARY + " WHERE ID=?" + ver + " FOR UPDATE");
                ps.setLong(1, id);
                if (version > 0) ps.setInt(2, version);
                ps.executeQuery();
                //fulltext table uses MyISAM engine and can not be locked
            } finally {
                if (ps != null)
                    ps.close();
            }
            if (LOG.isDebugEnabled())
                LOG.debug("Locked instances of id #" + id + (version > 0 ? " and version #" + version : " (all versions)"));
        } catch (SQLException e) {
            throw new FxDbException(LOG, e, "ex.db.sqlError", e.getMessage()).asRuntimeException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected FulltextIndexer getFulltextIndexer(FxPK pk, Connection con) {
        final PostgreSQLFulltextIndexer fti = new PostgreSQLFulltextIndexer();
        fti.init(pk, con);
        return fti;
    }
}
