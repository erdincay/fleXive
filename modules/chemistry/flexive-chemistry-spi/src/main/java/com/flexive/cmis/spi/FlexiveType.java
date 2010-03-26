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

import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.structure.FxTypeRelation;
import com.flexive.shared.CacheAdmin;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import org.apache.chemistry.BaseType;
import org.apache.chemistry.ContentStreamPresence;
import org.apache.chemistry.PropertyDefinition;
import org.apache.chemistry.Type;

import java.util.Collection;
import java.util.List;
import java.net.URI;

/**
 * A flexive type.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FlexiveType implements Type {
    public static final String ROOT_TYPE_ID = "Root";

    private final FxType type;

    public FlexiveType(String typeName) {
        this.type = CacheAdmin.getEnvironment().getType(SPIUtils.getFxTypeName(typeName));
    }
    
    public FlexiveType(long typeId) {
        this.type = CacheAdmin.getEnvironment().getType(typeId);
    }

    public FlexiveType(FxType type) {
        this.type = type;
    }

    public String getId() {
        return type.getId() == FxType.ROOT_ID ? ROOT_TYPE_ID : type.getName().toLowerCase();
    }

    public String getQueryName() {
        return SPIUtils.getTypeName(type);
    }

    public String getDisplayName() {
        return type.getLabel().getBestTranslation();
    }

    public String getParentId() {
        if (type.getParent() == null) {
            if (FxType.FOLDER.equals(type.getName())) {
                return BaseType.FOLDER.getId();
            }
            return SPIUtils.getFolderTypeIds().contains(type.getId())
                    ? BaseType.FOLDER.getId()
                    : BaseType.DOCUMENT.getId();
        } else {
            return SPIUtils.getTypeName(type.getParent());
        }
    }

    public BaseType getBaseType() {
        if (SPIUtils.getFolderTypeIds().contains(type.getId()) || type.getId() == FxType.ROOT_ID) {
            return BaseType.FOLDER;
        } else {
            return type.isRelation() ? BaseType.RELATIONSHIP : BaseType.DOCUMENT;
        }
    }

    public String getDescription() {
        return type.getLabel().getBestTranslation();
    }

    public boolean isCreatable() {
        return true;
    }

    public boolean isQueryable() {
        return true;
    }

    public boolean isControllable() {
        return type.isUseInstancePermissions();
    }

    public boolean isIncludedInSuperTypeQuery() {
        return true;    // TODO: FX-627
    }

    public boolean isFileable() {
        return true;
    }

    public boolean isVersionable() {
        return true;
    }

    public ContentStreamPresence getContentStreamAllowed() {
        final FxPropertyAssignment bin = SPIUtils.getContentStreamAssignment(type);
        if (bin == null) {
            return ContentStreamPresence.NOT_ALLOWED;
        } else {
            return bin.getMultiplicity().getMin() > 0 ? ContentStreamPresence.REQUIRED  : ContentStreamPresence.ALLOWED;
        }
    }

    public String[] getAllowedSourceTypes() {
        if (getBaseType() != BaseType.RELATIONSHIP) {
            return null;
        }
        final List<String> result = newArrayListWithCapacity(type.getRelations().size());
        for (FxTypeRelation relation : type.getRelations()) {
            result.add(relation.getSource().getName());
        }
        return result.toArray(new String[result.size()]);
    }

    public String[] getAllowedTargetTypes() {
        if (getBaseType() != BaseType.RELATIONSHIP) {
            return null;
        }
        final List<String> result = newArrayListWithCapacity(type.getRelations().size());
        for (FxTypeRelation relation : type.getRelations()) {
            result.add(relation.getDestination().getName());
        }
        return result.toArray(new String[result.size()]);
    }

    public Collection<PropertyDefinition> getPropertyDefinitions() {
        final List<PropertyDefinition> result = newArrayListWithCapacity(type.getAllProperties().size());

        for (FxPropertyAssignment assignment : type.getAllProperties()) {
            if (!assignment.isSystemInternal()) {
                result.add(new FlexivePropertyDefinition(assignment));
            }
        }

        for (VirtualProperties virtualProperty : VirtualProperties.values()) {
            result.add(virtualProperty.getDefinition());
        }
        
        return result;
    }

    public PropertyDefinition getPropertyDefinition(String name) {
        final VirtualProperties virtualProperty = VirtualProperties.getByName(name);
        if (virtualProperty != null) {
            return virtualProperty.getDefinition();
        }
        // TODO: doesn't work for nested property assignments
        return new FlexivePropertyDefinition(type.getPropertyAssignment("/" + name));
    }

    public String getLocalName() {
        return type.getName();
    }

    public URI getLocalNamespace() {
        return null;
    }

    public boolean isControllablePolicy() {
        return false;
    }

    public boolean isControllableACL() {
        return type.isUseInstancePermissions();
    }

    public boolean isFulltextIndexed() {
        for (FxPropertyAssignment assignment : type.getAllProperties()) {
            if (assignment.getProperty().isFulltextIndexed()) {
                return true;
            }
        }
        return false;
    }
}
