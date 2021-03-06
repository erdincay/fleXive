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
package com.flexive.core.search.cmis.model;

import com.flexive.core.search.PropertyEntry;
import com.flexive.shared.cmis.CmisVirtualProperty;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxProperty;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * A reference to a single table, with an optional alias.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class SingleTableReference implements TableReference {
    private static final Log LOG = LogFactory.getLog(SingleTableReference.class);

    private final String alias;
    private final List<FxType> referencedTypes;
    private final FxType baseType;

    public SingleTableReference(FxEnvironment env, String name, String alias) {
        this.alias = escapeTableAlias(alias);
        final List<FxType> types;
        if ("root".equalsIgnoreCase(name)) {
            // treat root type as a special case, because the API won't return any derived types
            this.baseType = env.getType(FxType.ROOT_ID);
            types = Lists.newArrayList();
            types.add(baseType);
            for (FxType type : env.getTypes()) {
                if (type.getId() != FxType.ROOT_ID) {
                    types.add(type);
                }
            }
        } else if ("cmis:document".equalsIgnoreCase(name)) {
            // select all types that are not folders
            this.baseType = env.getType(FxType.ROOT_ID);
            types = Lists.newArrayList();
            for (FxType type : env.getTypes()) {
                if (!type.isDerivedFrom(FxType.FOLDER)) {
                    types.add(type);
                }
            }
        } else if ("cmis:folder".equalsIgnoreCase(name)) {
            this.baseType = env.getType(FxType.FOLDER);
            types = Lists.newArrayList(baseType.getDerivedTypes(true, true));
        } else {
            // normal type, add type and all subtypes
            this.baseType = env.getType(name);
            types = Lists.newArrayList(baseType.getDerivedTypes(true, true));
        }
        this.referencedTypes = Collections.unmodifiableList(types);
    }

    public SingleTableReference(FxEnvironment env, String name) {
        this(env, name, name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TableReference findByAlias(String alias) {
        return this.alias.equals(alias) ? this : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAlias() {
        return alias;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getReferencedAliases() {
        return Arrays.asList(alias);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxType> getReferencedTypes() {
        return referencedTypes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPropertySecurityEnabled() {
        for (FxType type : referencedTypes) {
            if (type.isUsePropertyPermissions()) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxPropertyAssignment> resolvePropertyAssignment(FxEnvironment environment, String name) {
        final List<FxPropertyAssignment> result = Lists.newArrayList();

        // select the assignment in the root type
        final CmisVirtualProperty cmisProperty = CmisVirtualProperty.getByCmisName(name);

        final String alias = getAssignmentAlias(environment, name, cmisProperty);
        if (alias != null) {
            if (baseType.getId() == FxType.ROOT_ID && 
                    ("caption".equalsIgnoreCase(alias) || "fqn".equalsIgnoreCase(alias))) {
                // caption and FQN is a special case since they are the only "real" properties that can be
                // selected from the root type
                result.addAll(environment.getReferencingPropertyAssignments(
                        environment.getProperty(alias).getId()
                ));
            } else {
                final FxPropertyAssignment base = baseType.getPropertyAssignment("/" + alias);
                result.add(base);

                // add all derived assignments
                result.addAll(base.getDerivedAssignments(environment));
            }
        }

        return result;
    }

    private String getAssignmentAlias(FxEnvironment environment, String name, CmisVirtualProperty cmisProperty) {
        if (cmisProperty != null) {
            if (cmisProperty.getFxPropertyName() != null && cmisProperty.getFxPropertyName().charAt(0) == '@') {
                // FxSQL virtual property selected, use first read column
                return PropertyEntry.Type.createForProperty(cmisProperty.getFxPropertyName()).getReadColumns()[0];
            }
            try {
                final String assignmentXPath = "/" + name;
                baseType.getPropertyAssignment(assignmentXPath);
                // the CMIS property is already mapped in the type, ignore the CMIS mapping
                if (LOG.isDebugEnabled()) {
                    LOG.debug(
                            "CMIS property '" + name + "' is already mapped in " + baseType + ", using assignment "
                            + baseType.getPropertyAssignment(assignmentXPath).getXPath()
                    );
                }
                return name;
            } catch (FxRuntimeException e) {
                // not found, pass
            }

            // CMIS base property selected, not hidden by a flexive property assignment
            if (cmisProperty.getFxPropertyName() != null) {
                final FxProperty property = environment.getProperty(cmisProperty.getFxPropertyName());
                final List<FxPropertyAssignment> assignments = baseType.getAssignmentsForProperty(property.getId());
                return assignments.isEmpty() ? null : assignments.get(0).getAlias();
            } else {
                throw new IllegalArgumentException("Cannot select CMIS property " + name
                        + " because it is not yet mapped to the [fleXive] repository.");
            }
        } else {
            // normal flexive property assignment selected
            return name;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdFilterColumn() {
        return alias + "_id";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersionFilterColumn() {
        return alias + "_ver";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdVersionLink(String filterTableAlias, String subSelectTableAlias) {
        return "("
                + filterTableAlias + "." + getIdFilterColumn() + "=" + subSelectTableAlias + ".id"
                + " AND " + filterTableAlias + "." + getVersionFilterColumn() + "=" + subSelectTableAlias + ".ver"
                + ")";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TableReference> getSubTables() {
        return new ArrayList<TableReference>(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<TableReference> getLeafTables() {
        return new Iterable<TableReference>() {
            @Override
            public Iterator<TableReference> iterator() {
                return Arrays.asList((TableReference) SingleTableReference.this).iterator();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxType getRootType() {
        return baseType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(TableReferenceVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return alias;
    }

    /**
     * Escape a table alias that may be valid in CMIS SQL, but invalid in SQL92.
     *
     * @param alias the alias to be escaped
     * @return      the escaped alias
     */
    public static String escapeTableAlias(String alias) {
        return alias.replace(':', '_');
    }
}
