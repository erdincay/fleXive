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
package com.flexive.cmis.spi;

import org.apache.chemistry.*;

/**
 * Singleton object that stores flexive's capabilities using the current CMIS implementation.
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FlexiveRepositoryCapabilities implements RepositoryCapabilities {
    private static final FlexiveRepositoryCapabilities instance = new FlexiveRepositoryCapabilities();

    public static FlexiveRepositoryCapabilities getInstance() {
        return instance;
    }
    
    private FlexiveRepositoryCapabilities() {
    }

    public boolean hasMultifiling() {
        return true;
    }

    public boolean hasUnfiling() {
        return true;
    }

    public boolean hasVersionSpecificFiling() {
        return false;
    }

    public boolean isPWCUpdatable() {
        return true;
    }

    public boolean isPWCSearchable() {
        return false;
    }

    public boolean isAllVersionsSearchable() {
        return true;
    }

    public CapabilityQuery getQueryCapability() {
        return CapabilityQuery.BOTH_COMBINED;
    }

    public CapabilityJoin getJoinCapability() {
        return CapabilityJoin.INNER_ONLY;
    }

    public boolean hasGetDescendants() {
        return true;
    }

    public boolean isContentStreamUpdatableAnytime() {
        return true;
    }

    public CapabilityRendition getRenditionCapability() {
        return CapabilityRendition.NONE;    // TODO: what are renditions?
    }

    public CapabilityChange getChangeCapability() {
        return CapabilityChange.ALL;
    }

    public CapabilityACL getACLCapability() {
        return CapabilityACL.MANAGE;
    }

    public boolean hasGetFolderTree() {
        return true;
    }
}
