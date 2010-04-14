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

import com.flexive.chemistry.webdav.FolderResource;
import com.flexive.chemistry.webdav.ChemistryResourceFactory;
import com.bradmcevoy.http.*;
import com.flexive.shared.CacheAdmin;
import java.util.List;
import org.apache.chemistry.Folder;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import org.apache.chemistry.BaseType;

/**
 * Add lock support for text document resources.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class LockableFolder extends FolderResource implements LockableResource, LockingCollectionResource {
    public LockableFolder(ChemistryResourceFactory resourceFactory, String path, Folder object) {
        super(resourceFactory, path, object);
    }

    public LockResult lock(LockTimeout timeout, LockInfo lockInfo) {
        return FlexiveLockSupport.lock(object, timeout, lockInfo);
    }

    public LockResult refreshLock(String token) {
        return FlexiveLockSupport.refreshLock(object, token);
    }

    public void unlock(String tokenId) {
        FlexiveLockSupport.unlock(object, tokenId);
    }

    public LockToken getCurrentLock() {
        return FlexiveLockSupport.getCurrentLock(object);
    }

    public LockToken createAndLock(String name, LockTimeout timeout, LockInfo lockInfo) {
        try {
            final Resource resource = createNew(name, new ByteArrayInputStream(new byte[0]), 0L, null);
            if (resource instanceof LockableResource) {
                return ((LockableResource) resource).lock(timeout, lockInfo).getLockToken();
            } else {
                throw new IllegalArgumentException("Cannot lock resource of class " + resource.getClass().getCanonicalName());
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    protected String getDocumentType(List<String> mimeTypes) {
        // TODO: the image/document switching should be implemented in the flexive repository
        if (mimeTypes.isEmpty()) {
            return BaseType.DOCUMENT.getId();
        } else {
            return CacheAdmin.getEnvironment().getMimeTypeMatch(mimeTypes.get(0)).getName();
        }
    }



}
