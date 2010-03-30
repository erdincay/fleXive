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
package com.flexive.shared.cmis;

import org.apache.commons.lang.StringUtils;

/**
 * Definitions of CMIS property names (CMIS draft 0.62) and their mapping to [fleXive] properties, if available.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public enum CmisVirtualProperty {
    Id("ObjectId", "@pk"),

    Uri("Uri", null),

    TypeId("ObjectTypeId", "typedef"),
    CreatedBy("CreatedBy", "created_by"),
    CreationDate("CreationDate", "created_at"),
    LastModifiedBy("LastModifiedBy", "modified_by"),
    LastModificationDate("LastModificationDate", "modified_at"),
    ChangeToken("ChangeToken", null),

    /*
     * Document
     */
    Name("Name", "caption"),
    IsImmutable("IsImmutable", null),
    IsLatestVersion("IsLatestVersion", "ismax_ver"),
    IsMajorVersion("IsMajorVersion", null),
    IsLatestMajorVersion("IsLatestMajorVersion", null),
    VersionLabel("VersionLabel", null),
    VersionSeriesId("VersionSeriesId", null),
    IsVersionSeriesCheckedOut("IsVersionSeriesCheckedOut", null),
    VersionSeriesCheckedOutBy("VersionSeriesCheckedOutBy", null),
    VersionSeriesCheckedOutId("VersionSeriesCheckedOutId", null),
    CheckinComment("CheckinComment", null),
    ContentStreamLength("ContentStreamLength", null),
    ContentStreamMimeType("ContentStreamMimeType", null),
    ContentStreamFilename("ContentStreamFilename", null),
    ContentStreamUri("ContentStreamUri", null),

    /*
     * Folder
     */
    ParentId("ParentId", true),
    AllowedChildObjectTypeIds("AllowedChildObjectTypeIds", null);
    
    private final String cmisPropertyName;
    private final String fxPropertyName;
    private final boolean supportsQuery;

    CmisVirtualProperty(String cmisPropertyName, String fxPropertyName) {
        this.cmisPropertyName = cmisPropertyName;
        this.fxPropertyName = fxPropertyName;
        this.supportsQuery = fxPropertyName != null;
    }

    CmisVirtualProperty(String cmisPropertyName, boolean supportsQuery) {
        this.cmisPropertyName = cmisPropertyName;
        this.fxPropertyName = null;
        this.supportsQuery = supportsQuery;
    }

    public String getCmisPropertyName() {
        return "cmis:" + cmisPropertyName;
    }

    public String getFxPropertyName() {
        return fxPropertyName;
    }

    public boolean isSupportsQuery() {
        return isFxProperty() || supportsQuery;
    }

    public boolean isFxProperty() {
        return fxPropertyName != null;
    }
    
    public boolean isVirtualFxProperty() {
        return StringUtils.isNotBlank(fxPropertyName) && fxPropertyName.charAt(0) == '@';
    }
    
    /**
     * Returns the CmisVirtualProperty for the given property name, or null if none was found.
     * 
     * @param cmisPropertyName  the CMIS property name (e.g. "ObjectTypeId")
     * @return  the CmisVirtualProperty for the given property name, or null if none was found.
     */
    public static CmisVirtualProperty getByCmisName(String cmisPropertyName) {
        for (CmisVirtualProperty property : values()) {
            if (property.getCmisPropertyName().equalsIgnoreCase(cmisPropertyName)) {
                return property;
            }
        }
        return null;
    }

    /**
     * Returns the CmisVirtualProperty for the given [fleXive] property name, or null if none was found.
     *
     * @param fxPropertyName the [fleXive] property name (e.g. "typdef")
     * @return  the CmisVirtualProperty for the given property name, or null if none was found.
     */
    public static CmisVirtualProperty getByFlexiveName(String fxPropertyName) {
        for (CmisVirtualProperty property : values()) {
            if (fxPropertyName.equalsIgnoreCase(property.getFxPropertyName())) {
                return property;
            }
        }
        return null;
    }
}
