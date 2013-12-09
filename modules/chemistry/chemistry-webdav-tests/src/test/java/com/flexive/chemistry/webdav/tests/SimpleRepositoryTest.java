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
package com.flexive.chemistry.webdav.tests;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.Request;
import com.flexive.chemistry.webdav.ChemistryResourceFactory;
import org.apache.chemistry.Connection;
import org.apache.chemistry.Repository;
import org.apache.chemistry.test.BasicHelper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class SimpleRepositoryTest extends WebdavTestCase {
    private static final Map<String, Serializable> EMPTY_PARAMS = new HashMap<String, Serializable>();
    private static final String ROOT_FOLDER = "webdavRepository";

    @Override
    protected Repository getRepository() {
        try {
            return BasicHelper.makeSimpleRepository(ROOT_FOLDER);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        } 
    }

    @Override
    protected ChemistryResourceFactory getResourceFactory(final Repository repository) {
        return new ChemistryResourceFactory() {
            @Override
            public Connection createConnection(Request request, Auth auth) {
                return repository.getConnection(EMPTY_PARAMS);
            }

            @Override
            protected boolean requireAuthentication() {
                return false;
            }
        };
    }

}
