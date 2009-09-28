/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2009
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
package com.flexive.core.storage.H2;

import com.flexive.core.storage.ContentStorage;
import com.flexive.core.storage.genericSQL.GenericHierarchicalStorage;
import com.flexive.core.storage.genericSQL.GenericBinarySQLStorage;
import com.flexive.shared.exceptions.FxRuntimeException;

import java.sql.Connection;

/**
 * H2 implementation of hierarchical content handling
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class H2HierarchicalStorage extends GenericHierarchicalStorage {
    private static final H2HierarchicalStorage instance = new H2HierarchicalStorage();

    /**
     * Ctor
     */
    public H2HierarchicalStorage() {
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
    public void lockTables(Connection con, long id, int version) throws FxRuntimeException {
        //do nothing for H2 since we rely on MVCC
    }
}