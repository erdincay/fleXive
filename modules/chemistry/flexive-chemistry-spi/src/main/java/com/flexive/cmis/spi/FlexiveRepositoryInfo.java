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
package com.flexive.cmis.spi;

import com.flexive.shared.FxContext;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.tree.FxTreeNode;
import org.apache.chemistry.*;
import org.w3c.dom.Document;

import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * Returns basic information about this content repository. 
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FlexiveRepositoryInfo implements RepositoryInfo {
    private static final ObjectId rootFolderId = new ObjectId() {
        public String getId() {
            return String.valueOf(FxTreeNode.ROOT_NODE);
        }
    };

    public String getDescription() {
        return FxSharedUtils.getFlexiveEditionFull();
    }

    public ObjectId getRootFolderId() {
        return rootFolderId;
    }

    public String getVendorName() {
        return "unique computing solutions GmbH";
    }

    public String getProductName() {
        return "fleXive";
    }

    public String getProductVersion() {
        return FxSharedUtils.getFlexiveVersion();
    }

    public String getVersionSupported() {
        return "0.61";
    }

    public Document getRepositorySpecificInformation() {
        return null;
    }

    public RepositoryCapabilities getCapabilities() {
        return FlexiveRepositoryCapabilities.getInstance();
    }

    public Collection<RepositoryEntry> getRelatedRepositories() {
        return null;
    }

    public String getId() {
        return String.valueOf(FxContext.get().getDivisionId());
    }

    public String getName() {
        return getId();
    }

    public URI getURI() {
        return null;
    }

    public String getRelationshipName() {
        return null;
    }

    public ACLCapabilityType getACLCapabilityType() {
        return new FlexiveACLCapabilityType();
    }

    public Set<BaseType> getChangeLogBaseTypes() {
        return new HashSet<BaseType>(Arrays.asList(BaseType.DOCUMENT));
    }

    public boolean isChangeLogIncomplete() {
        return false;   // TODO: ?
    }

    public String getLatestChangeLogToken() {
        return null;
    }

    public URI getThinClientURI() {
        return null;    // TODO
    }
}
