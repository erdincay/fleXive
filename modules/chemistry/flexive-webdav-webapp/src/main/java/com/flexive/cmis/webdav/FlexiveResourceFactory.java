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
package com.flexive.cmis.webdav;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.Request;
import com.flexive.chemistry.webdav.*;
import com.flexive.cmis.spi.FlexiveConnection;
import com.flexive.cmis.spi.FlexiveRepository;
import com.flexive.shared.exceptions.FxLoginFailedException;
import org.apache.chemistry.Connection;
import org.apache.chemistry.Document;
import org.apache.chemistry.Folder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FlexiveResourceFactory extends ChemistryResourceFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection createConnection(Request request, Auth auth) throws LoginException {
        final Map<String, Serializable> params = new HashMap<String, Serializable>();
        if (auth != null) {
            params.put(FlexiveConnection.PARAM_USERNAME, auth.getUser());
            params.put(FlexiveConnection.PARAM_PASSWORD, auth.getPassword());
        }
        
        // browse into documents that have children, even if their document type
        // is not a subtype of folder
        params.put(FlexiveConnection.PARAM_DOCS_FOLDERS, true);

        // use session login info, if available (provided by FxFilter). This is not strictly necessary,
        // because AuthenticationFilter keeps the authentication information in the session,
        // but it avoids the login/logout calls around every request.

        params.put(FlexiveConnection.PARAM_USE_SESSION_AUTH, true);
        try {
            return new FlexiveRepository(null).getConnection(params);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof FxLoginFailedException) {
                throw new LoginException(e);
            } else {
                throw e;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean requireAuthentication() {
        return true;
    }


    @Override
    protected TextDocumentResource createTextDocument(String path, Document doc) {
        return new LockableTextDocument(this, path, doc);
    }

    @Override
    protected BinaryDocumentResource createBinaryDocument(String path, Document doc) {
        return new LockableBinaryDocument(this, path, doc);
    }

    @Override
    protected FolderResource createFolder(String path, Folder folder) {
        return new LockableFolder(this, path, folder);
    }
}
