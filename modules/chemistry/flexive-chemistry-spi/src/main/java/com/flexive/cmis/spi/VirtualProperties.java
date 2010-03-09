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

import com.flexive.shared.tree.FxTreeNode;
import com.flexive.shared.search.FxPaths;
import static com.flexive.shared.CacheAdmin.getEnvironment;
import com.flexive.shared.structure.FxPropertyAssignment;
import org.apache.chemistry.Property;
import org.apache.chemistry.PropertyDefinition;
import org.apache.chemistry.PropertyType;
import static org.apache.chemistry.PropertyType.*;
import org.apache.chemistry.Updatability;
import org.apache.chemistry.impl.simple.SimplePropertyDefinition;

import java.io.Serializable;

/**
 * An enumeration of all "virtual properties" required by CMIS (i.e. properties that are not user-defined
 * type properties, but rather system-internal properties with a fixed name that should be available
 * on every object).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
enum VirtualProperties {
    // object
    OBJECT_ID(Property.ID),
    //URI(Property.URI),
    TYPE_ID(Property.TYPE_ID, "TYPEDEF", PropertyType.ID) {
        @Override
        public Serializable convertValue(Object flexiveValue) {
            return getEnvironment().getType((Long) flexiveValue).getName().toLowerCase();
        }
    },
    BASE_TYPE_ID(Property.BASE_TYPE_ID),
    CREATED_BY(Property.CREATED_BY, "CREATED_BY", PropertyType.ID),
    CREATION_DATE(Property.CREATION_DATE, "CREATED_AT", PropertyType.DATETIME),
    LAST_MODIFIED_BY(Property.LAST_MODIFIED_BY, "MODIFIED_BY", PropertyType.ID),
    LAST_MODIFICATION_DATE(Property.LAST_MODIFICATION_DATE, "MODIFIED_AT", PropertyType.DATETIME),
    CHANGE_TOKEN(Property.CHANGE_TOKEN),
    PATH(Property.PATH),

    // document
    NAME(Property.NAME),
    IS_IMMUTABLE(Property.IS_IMMUTABLE, BOOLEAN),
    IS_LATEST_VERSION(Property.IS_LATEST_VERSION, BOOLEAN),
    IS_MAJOR_VERSION(Property.IS_MAJOR_VERSION, BOOLEAN),
    IS_LATEST_MAJOR_VERSION(Property.IS_LATEST_MAJOR_VERSION, BOOLEAN),
    VERSION_LABEL(Property.VERSION_LABEL),
    VERSION_SERIES_ID(Property.VERSION_SERIES_ID, "VERSION", PropertyType.INTEGER),
    IS_VERSION_SERIES_CHECKED_OUT(Property.IS_VERSION_SERIES_CHECKED_OUT, BOOLEAN),
    VERSION_SERIES_CHECKED_OUT_BY(Property.VERSION_SERIES_CHECKED_OUT_BY),
    VERSION_SERIES_CHECKED_OUT_ID(Property.VERSION_SERIES_CHECKED_OUT_ID),
    //CHECKIN_COMMENT(Property.CHECKIN_COMMENT),
    CONTENT_STREAM_LENGTH(Property.CONTENT_STREAM_LENGTH, INTEGER, 64),
    CONTENT_STREAM_MIME_TYPE(Property.CONTENT_STREAM_MIME_TYPE),
    //CONTENT_STREAM_FILENAME(Property.CONTENT_STREAM_FILENAME),
    //CONTENT_STREAM_URI(Property.CONTENT_STREAM_URI, PropertyType.URI),
    
    // folder (also has a NAME)
    PARENT_ID(Property.PARENT_ID, PropertyType.ID) {
        @Override
        public Serializable convertValue(Object flexiveValue) {
            final FxPaths paths = (FxPaths) flexiveValue;
            if (paths == null || paths.getPaths().isEmpty()) {
                return null;
            }
            // select first returned path - the API needs to override this behaviour when returning multiple
            // parents
            final FxPaths.Path path = paths.getPaths().get(0);
            return String.valueOf(path.getItems().size()  > 1
                    ? path.getItems().get(path.getItems().size() - 2).getNodeId()
                    : FxTreeNode.ROOT_NODE
            );
        }},
    // TODO: define multiple property
    ALLOWED_CHILD_OBJECT_IDS(Property.ALLOWED_CHILD_OBJECT_TYPE_IDS);

    private final PropertyDefinition definition;

    VirtualProperties(String id) {
        this(id, STRING);
    }

    VirtualProperties(String id, PropertyType type) {
        this.definition = new VirtualPropertyDefinition(id, type, -1, getUpdatability(id));
    }

    VirtualProperties(String id, PropertyType type, int precision) {
        this.definition = new VirtualPropertyDefinition(id, type, precision, getUpdatability(id));
    }

    VirtualProperties(String name, String rootPropertyName, PropertyType virtualType) {
        this.definition = new FlexivePropertyDefinition(
                getEnvironment().getPropertyAssignment("ROOT/" + rootPropertyName),
                name,
                virtualType
        );
    }

    private Updatability getUpdatability(String id) {
        return Property.NAME.equals(id)
                ? Updatability.READ_WRITE
                : Updatability.READ_ONLY;
    }

    /**
     * Return the virtual property with the given name, or null if none exists.
     *
     * @param id  the property name (e.g. ParentId)
     * @return      the virtual property with the given name, or null if none exists.
     */
    public static VirtualProperties getByName(String id) {
        for (VirtualProperties prop : values()) {
            if (id.equals(prop.getId())) {
                return prop;
            }
        }
        return null;
    }

    /**
     * Return the virtual property that matches the given base assignment ID, or null if none exists.
     *
     * @param assignmentId  the assignment ID
     * @return  the virtual property that matches the given base assignment ID, or null if none exists.
     */
    public static VirtualProperties getByBaseAssignment(long assignmentId) {
        for (VirtualProperties prop : VirtualProperties.values()) {
            if (prop.getRootAssignment() != null && prop.getRootAssignment().getId() == assignmentId) {
                return prop;
            }
        }
        return null;
    }

    public String getId() {
        return definition.getId();
    }

    public PropertyDefinition getDefinition() {
        return definition;
    }

    /**
     * Return the given value from the original flexive content representation to the representation
     * required by CMIS.
     *
     * @param flexiveValue  the value to be converted
     * @return              the value converted to the CMIS representation
     */
    public Serializable convertValue(Object flexiveValue) {
        return SPIUtils.convertValue(flexiveValue, definition.getType());
    }

    /**
     * Returns the root assignment for this virtual property, or null if this virtual property
     * is not mapped to a real fleXive property.
     *
     * @return  the root assignment for this virtual property, or null if none exists
     */
    public FxPropertyAssignment getRootAssignment() {
        return definition instanceof FlexivePropertyDefinition
                ? ((FlexivePropertyDefinition) definition).getAssignment()
                : null;
    }

    private static class VirtualPropertyDefinition extends SimplePropertyDefinition {
        public VirtualPropertyDefinition(String name, PropertyType type, int precision, Updatability updatability) {
            super(
                    name, name, null, name, "", "", false,
                    type, false, null, true, true, null,
                    updatability,
                    false, false,
                    precision, null, null, -1, null
            );
        }

        @Override
        public String toString() {
            return "VPDef[" + getLocalName() + "]";
        }
    }
}
