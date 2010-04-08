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

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.XPathElement;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPropertyData;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.security.LifeCycleInfo;
import com.flexive.shared.security.PermissionSet;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;
import org.apache.chemistry.*;
import org.apache.chemistry.impl.base.BaseObject;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;

import static org.apache.chemistry.AllowableAction.*;

/**
 * Base class for {@link FlexiveDocument} and {@link FlexiveFolder}. It implements the common functions
 * to provide access to the properties of a single FxContent instance.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public abstract class FlexiveObjectEntry extends BaseObject implements ObjectEntry, CMISObject {
    //private static final Log LOG = LogFactory.getLog(FlexiveObjectEntry.class);

    protected final FlexiveConnection.Context context;
    protected final Map<String, Property> properties = new HashMap<String, Property>();

    FlexiveObjectEntry(FlexiveConnection.Context context) {
        this.context = context;
    }

    /**
     * Return the wrapped content instance. The instance should be cached.
     *
     * @return  the wrapped content instance. 
     */
    protected abstract FxContent getContent();

    @Override
    public boolean equals(Object o) {
        return this == o ||
                (o instanceof FlexiveObjectEntry && getId().equals(((FlexiveObjectEntry) o).getId()));

    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    public Connection getConnection() {
        return context.getConnection();
    }


    @Override
    public String getTypeId() {
        return CacheAdmin.getEnvironment().getType(
                getFxTypeId()
        ).getName().toLowerCase();
    }

    public Serializable getValue(String name) {
        if (VirtualProperties.NAME.getId().equals(name)) {
            return getName();
        } else if (VirtualProperties.getByName(name) != null) {
            final Property property = getProperties().get(name);
            return property != null ? property.getValue() : null;
        }
        final String xpath = createXPath(name);
        try {
            // fail-fast in case the assignment doesn't exist
            getFxType().getPropertyAssignment(xpath);

            final FxPropertyData data = getContent().getPropertyData(xpath);
            return SPIUtils.convertValue(
                    data.getValue(), data.getPropertyAssignment().getProperty()
            );
        } catch (FxRuntimeException e) {
            return null;    // property not set
        }
    }

    @Override
    public void setValue(String name, Serializable value) {
        if (value instanceof Calendar) {
            // unwrap calendar date
            value = ((Calendar) value).getTime();
        }
        final VirtualProperties vprop = VirtualProperties.getByName(name);
        if (vprop != null && !SPIUtils.typeContainsAssignment(getFxType(), "/" + name)) {
            if (vprop == VirtualProperties.NAME) {
                setName((String) value);
            } else if (vprop == VirtualProperties.TYPE_ID && value != null) {
                final String typeName = SPIUtils.getFxTypeName(value.toString());
                if (!typeName.equalsIgnoreCase(getValue(name).toString())) {
                    throw new IllegalArgumentException(
                            "The object type (" + VirtualProperties.TYPE_ID.getId() + ") cannot be changed. "
                            + "(Current value: " + getValue(name) + ", new value: " + typeName + ")"
                    );
                }
            } else {
                if (value != null && !value.equals(getValue(name))) {
                    // try to update the virtual property, although this currently always fails
                    getProperties().get(name).setValue(value);
                }
            }
        } else {
            // set XPath in content
            getContent().setValue(createXPath(name), value);
        }
        resetProperties();
    }

    public final Map<String, Serializable> getValues() {
        final Map<String, Property> properties = getProperties();
        final Map<String, Serializable> result = new HashMap<String, Serializable>(properties.size());
        for (Map.Entry<String, Property> entry : properties.entrySet()) {
            result.put(
                    entry.getKey(),
                    entry.getValue().getValue()
            );
        }
        return result;
    }

    @Override
    public final Map<String, Property> getProperties() {
        if (properties.isEmpty()) {
            // rebuild property map
            final FxContent content = getContent();
            content.getRootGroup().explode(true);
            for (FxPropertyData data : content.getPropertyData(-1, true)) {
                final FxPropertyAssignment assignment = data.getPropertyAssignment();
                if (!assignment.isSystemInternal()) {
                    final String cmisName = createName(data.getXPathFull());
                    if (!properties.containsKey(cmisName)) {
                        properties.put(
                                cmisName,
                                new FlexiveProperty(data.getValues(false), assignment)
                        );
                    }
                }
            }
            //addVirtualProperty(result, VirtualProperties.URI, "");    // URI handling not implemented yet
            addVirtualProperty(properties, VirtualProperties.TYPE_ID, getTypeId());
            addVirtualProperty(properties, VirtualProperties.BASE_TYPE_ID, getBaseTypeId());

            final LifeCycleInfo lci = getLifeCycleInfo();
            addVirtualProperty(properties, VirtualProperties.CREATED_BY, String.valueOf(lci.getCreatorId()));
            addVirtualProperty(properties, VirtualProperties.CREATION_DATE, new Date(lci.getCreationTime()));
            addVirtualProperty(properties, VirtualProperties.LAST_MODIFIED_BY, String.valueOf(lci.getModificatorId()));
            addVirtualProperty(properties, VirtualProperties.LAST_MODIFICATION_DATE, new Date(lci.getModificationTime()));
            
            addCustomProperties(properties);
        }

        return properties;
    }

    /**
     * Implement this method to add properties to the map returned by {@link #getProperties()}.
     *
     * @param result    the target map
     */
    protected void addCustomProperties(Map<String, Property> result) {
    }

    /**
     * Reset the cached property map returned by {@link #getProperties()}.
     */
    protected void resetProperties() {
        properties.clear();
    }

    protected void addVirtualProperty(Map<String, Property> result, VirtualProperties virtualProperty, Serializable value) {
        result.put(virtualProperty.getId(), new VirtualProperty(virtualProperty, value));
    }

    @Override
    public void setValues(Map<String, Serializable> values) {
        for (Map.Entry<String, Serializable> entry : values.entrySet()) {
            if (entry.getValue() == null) {
                // remove it
                getContent().remove(createXPath(entry.getKey()));
            } else {
                // update it
                setValue(entry.getKey(), entry.getValue());
            }
        }
        resetProperties();
    }


    public Set<QName> getAllowableActions() {
        final PermissionSet permissions = getContent().getPermissions();
        final Set<QName> allowableActions = new HashSet<QName>();
        if (permissions.isMayRead()) {
            allowableActions.addAll(Arrays.asList(
                    CAN_GET_PROPERTIES, CAN_GET_OBJECT_RELATIONSHIPS, CAN_GET_OBJECT_PARENTS,
                    CAN_GET_FOLDER_PARENT, CAN_GET_DESCENDANTS,
                    CAN_ADD_OBJECT_TO_FOLDER, CAN_REMOVE_OBJECT_FROM_FOLDER,
                    CAN_GET_CONTENT_STREAM, CAN_GET_ACL
            ));
        }
        if (permissions.isMayEdit()) {
            allowableActions.addAll(Arrays.asList(
                    CAN_UPDATE_PROPERTIES, CAN_MOVE_OBJECT,
                    CAN_CHECK_OUT, CAN_CANCEL_CHECK_OUT, CAN_CHECK_IN,
                    CAN_SET_CONTENT_STREAM, CAN_DELETE_CONTENT_STREAM,
                    CAN_APPLY_ACL, CAN_APPLY_POLICY
            ));
        }
        if (permissions.isMayDelete()) {
            allowableActions.addAll(Arrays.asList(
                    CAN_DELETE_OBJECT
            ));
        }
        if (permissions.isMayRelate()) {
            allowableActions.addAll(Arrays.asList(
                    CAN_CREATE_RELATIONSHIP
            ));
        }
        return allowableActions;
    }

    public Collection<ObjectEntry> getRelationships() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getId() {
        return getContent().getPk().toString();
    }

    @Override
    public Calendar getDateTime(String name) {
        final Serializable value = getValue(name);
        if (value == null) {
            return null;
        } else if (value instanceof Calendar) {
            return (Calendar) value;
        } else if (value instanceof Date) {
            return SPIUtils.convertDate(value);
        } else {
            throw new IllegalArgumentException(
                    "'" + name + "' is not a date or datetime property, but of type "
                            + value.getClass().getCanonicalName() + "."
            );
        }
    }

    public BaseType getBaseType() {
        return SPIUtils.getFolderTypeIds().contains(getFxTypeId())
                ? BaseType.FOLDER
                : BaseType.DOCUMENT;
    }

    /**
     * Create an XPath valid for the current content, based on the CMIS name.
     *
     * @param name  the CMIS property name
     * @return      the corresponding XPath for the current content instance
     */
    protected String createXPath(String name) {
        return "/" + name;
    }

    /**
     * Create a suitable CMIS property name from the given XPath. The name should contain all information
     * from the XPath, i.e. <code>xpath.equalsIgnoreCase(createXPath(createName(xpath)))</code>.
     *
     * @param xpath the content XPath
     * @return      a suitable CMIS property name
     */
    protected String createName(String xpath) {
        // we need to strip the multiplicity to match the XPath returned by the PropertyDefinition
        // multiplicities are probably handled by getStrings etc.
        return XPathElement.toXPathNoMult(xpath)
                // strip first slash
                .substring(1)
                .toLowerCase();
    }

    /**
     * Returns the XPath of the caption property for this content, or null if no caption is assigned
     * to the content type.
     *
     * @return the XPath of the caption property, or null if no caption is assigned
     *         to the content type.
     */
    protected final String getCaptionXPath() {
        final FxEnvironment environment = CacheAdmin.getEnvironment();
        final List<FxPropertyAssignment> assignments = environment
                .getType(getFxTypeId())
                .getAssignmentsForProperty(
                        environment.getProperty("CAPTION").getId()
                );

        return assignments.isEmpty() ? null : assignments.get(0).getXPath();
    }

    protected FxType getFxType() {
        return CacheAdmin.getEnvironment().getType(getFxTypeId());
    }

    protected long getFxTypeId() {
        return getContent().getTypeId();
    }

    protected LifeCycleInfo getLifeCycleInfo() {
        return getContent().getLifeCycleInfo();
    }

    public ChangeInfo getChangeInfo() {
        throw new UnsupportedOperationException();
    }

    protected void checkUniqueName(FxTreeNode parent, String name) {
        for (FxTreeNode child : parent.getChildren()) {
            if (child.getLabel() != null && child.getLabel().toString().equals(name)) {

                // a child (folder or document) with this label already exists
                throw new IllegalArgumentException(
                        parent.getPath() + "/" + name + " already exists."
                );

            }
        }
    }

    protected FxTreeNode getNodeWithChildren(FxTreeNode node) {
        try {
            return node.getChildren().isEmpty()
                    ? EJBLookup.getTreeEngine().getTree(FxTreeMode.Edit, node.getId(), 1) : node;
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }
}
