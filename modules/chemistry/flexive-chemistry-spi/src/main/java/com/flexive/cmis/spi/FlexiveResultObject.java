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

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.cmis.search.CmisResultColumnDefinition;
import com.flexive.shared.cmis.search.CmisResultRow;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.value.BinaryDescriptor;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxValue;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.chemistry.*;
import org.apache.chemistry.impl.base.BaseObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.io.IOException;
import java.util.*;
import org.apache.chemistry.impl.simple.SimpleChangeInfo;
import static com.flexive.cmis.spi.SPIUtils.notImplemented;

/**
 * An object wrapping a CMIS-SQL result row.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FlexiveResultObject extends BaseObject implements ObjectEntry {
    private static final Log LOG = LogFactory.getLog(FlexiveResultObject.class);

    private final CmisResultRow row;
    final FlexiveConnection.Context context;

    FlexiveResultObject(FlexiveConnection.Context context, CmisResultRow row) {
        this.row = row;
        this.context = context;
    }

    public Connection getConnection() {
        return context.getConnection();
    }


    public void move(Folder targetFolder, Folder sourceFolder) {
        throw new UnsupportedOperationException();
    }

    public void delete() {
        throw new UnsupportedOperationException();
    }

    public void unfile() {
        throw new UnsupportedOperationException();
    }

    public Folder getParent() {
        final Collection<Folder> parents = getParents();
        if (parents.isEmpty()) {
            return null;
        } else if (parents.size() == 1) {
            return parents.iterator().next();
        } else {
            throw new IllegalArgumentException("Cannot return single parent for multi-filed row instance: " + row);
        }
    }

    public Collection<Folder> getParents() {
        if (row.indexOf(VirtualProperties.PARENT_ID.getId()) == -1) {
            return Lists.newArrayListWithCapacity(0);
        }
        final List<Folder> result = Lists.newArrayList();
        final Collection<Long> parentIds = row.getColumn(VirtualProperties.PARENT_ID.getId()).getValues();
        for (long id : parentIds) {
            result.add(new FlexiveFolder(context, id));
        }
        return result;
    }

    public List<Relationship> getRelationships(RelationshipDirection direction, String typeId, boolean includeSubRelationshipTypes) {
        notImplemented(getClass(), "getRelationships");
        return Lists.newArrayList();
    }

    public void applyPolicy(Policy policy) {
        throw new UnsupportedOperationException();
    }

    public void removePolicy(Policy policy) {
        throw new UnsupportedOperationException();
    }

    public Collection<Policy> getPolicies() {
        notImplemented(getClass(), "getPolicies");
        return Lists.newArrayList();
    }

    public Type getType() {
        return new FlexiveType(getFxType());
    }

    public FxType getFxType() {
        final int typeIndex = Math.max(row.indexOf("typedef"), row.indexOf(VirtualProperties.TYPE_ID.getId()));
        return CacheAdmin.getEnvironment().getType(
                typeIndex == -1
                        ? FxType.ROOT_ID
                        : row.getColumn(typeIndex).getLong()
        );
    }

    @Override
    public String getTypeId() {
        return getType().getId();
    }

    public Property getProperty(String name) {

        final String colName;
        final VirtualProperties vprop = VirtualProperties.getByName(name);
        if (vprop != null) {
            return handleVirtualProperty(vprop);
        } else {
            colName = name;
        }

        final int colIndex = row.indexOf(colName);
        if (colIndex == -1) {
            return null;
        }
        final CmisResultColumnDefinition colDef = row.getColumnDefinitions().get(colIndex - 1);
        final FxPropertyAssignment assignment = colDef.getAssignmentId() == -1
                ? null
                : (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(colDef.getAssignmentId());

        return new FlexiveProperty(
                Arrays.asList(
                        convertResultValue(assignment, colName)
                ),
                assignment
        );
    }

    private Property handleVirtualProperty(VirtualProperties vprop) {
        final Serializable value;
        if (row.indexOf(vprop.getId()) != -1) {
            return new VirtualProperty(vprop, vprop.convertValue(row.getColumn(vprop.getId()).getValue()));
        } else if (vprop.getRootAssignment() != null) {
            // map virtual property to the actual assignment alias
            final String colName = vprop.getRootAssignment().getAlias();
            return row.indexOf(colName) == -1
                    // column not selected, return null
                    ? null
                    // convert column value to the type expected by CMIS
                    : new VirtualProperty(vprop, vprop.convertValue(row.getColumn(colName).getValue()));
        } else {
            final BinaryDescriptor binary = getBinary();
            switch(vprop) {
                case CONTENT_STREAM_MIME_TYPE:
                    value = binary == null ? null : binary.getMimeType();
                    break;
                case CONTENT_STREAM_LENGTH:
                    value = binary == null ? null : binary.getSize();
                    break;
                case BASE_TYPE_ID:
                    value = getBaseType();
                    break;
                case PARENT_ID:
                    // TODO: since any instance can be multi-filed, we cannot really return an ID here
                    final int parentIdIndex = row.indexOf(VirtualProperties.PARENT_ID.getId());
                    if (parentIdIndex != -1) {
                        value = String.valueOf(row.getColumn(parentIdIndex).getValues());
                    } else {
                        value = null;
                    }
                    break;
                default:
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Cannot handle virtual property " + vprop);
                    }
                    value = null;
            }
        }
        return new VirtualProperty(vprop, value);
    }

    /**
     * Return the main binary property of the row, or null if none exists.
     *
     * @return the main binary property of the row, or null if none exists.
     */
    private BinaryDescriptor getBinary() {
        // TODO: check for non-binary content streams?
        final FxPropertyAssignment assignment = getFxType().getMainBinaryAssignment();
        if (assignment == null) {
            return null;
        }
        final String alias = assignment.getAlias();
        return row.indexOf(alias) == -1
                ? null
                : row.getColumn(alias).getBinary();
    }

    @SuppressWarnings({"unchecked"})
    private FxValue convertResultValue(FxPropertyAssignment assignment, String colName) {
        final Object columnValue = row.getColumn(colName).getValue();
        if (assignment != null) {
            return assignment.getEmptyValue().setDefaultTranslation(columnValue);
        } else {
            return new FxString(false, columnValue != null ? columnValue.toString() : "");
        }
    }

    public Serializable getValue(String name) {
        final Property prop = getProperty(name);
        return prop != null ? prop.getValue() : null;
    }

    public void save() {
        throw new UnsupportedOperationException();
    }

    public Map<String, Serializable> getValues() {
        final Map<String, Property> properties = getProperties();
        final Map<String, Serializable> result = new HashMap<String, Serializable>(properties.size());
        for (Map.Entry<String, Property> entry : properties.entrySet()) {
            result.put(
                    entry.getKey(),
                    entry.getValue() != null ? entry.getValue().getValue() : null
            );
        }
        return result;
/*
        final Map<String, Serializable> values = new HashMap<String, Serializable>();
        final FxEnvironment environment = CacheAdmin.getEnvironment();
        for (CmisResultColumnDefinition column : row.getColumnDefinitions()) {
            final VirtualProperties vprop = VirtualProperties.getByBaseAssignment(column.getAssignmentId());
            values.put(
                    // map virtual properties to their CMIS name (e.g. ObjectTypeId instead of typedef)
                    vprop != null ? vprop.getDefinition().getId() : column.getAlias(),

                    // convert result value to the type expected by CMIS
                    SPIUtils.convertValue(
                            (Serializable) row.getColumn(column.getAlias()).getValue(),
                            ((FxPropertyAssignment) environment.getAssignment(column.getAssignmentId())).getProperty()
                    )
            );
        }
        return values;
*/
    }

    public Set<QName> getAllowableActions() {
        notImplemented(getClass(), "getAllowableActions");
        return Sets.newHashSet();
    }

    public Collection<ObjectEntry> getRelationships() {
        notImplemented(getClass(), "getRelationships");
        return Lists.newArrayList();
    }

    public BaseType getBaseType() {
        return getType().getBaseType();
    }

    public ContentStream getContentStream(String contentStreamId) throws IOException {
        notImplemented(getClass(), "getContentStream");
        return null;
    }

    public ChangeInfo getChangeInfo() {
        notImplemented(getClass(), "getChangeInfo");
        return new SimpleChangeInfo(ChangeType.UPDATED, Calendar.getInstance());
    }

    public String getPathSegment() {
        notImplemented(getClass(), "getPathSegment");
        return "";
    }


}
