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

import com.flexive.shared.exceptions.FxCmisSqlParseException;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;
import com.google.common.collect.Iterables;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A table reference representing a JOIN of two tables on explicitly specified columns.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class JoinedTableReference implements TableReference {
    private static final Log LOG = LogFactory.getLog(JoinedTableReference.class);

    private final TableReference first;
    private final TableReference second;
    private final ColumnReference firstColumn;
    private final ColumnReference secondColumn;

    public JoinedTableReference(TableReference first, TableReference second, ColumnReference firstColumn, ColumnReference secondColumn) throws FxCmisSqlParseException {
        if (firstColumn.isMultivalued() || secondColumn.isMultivalued()) {
            throw new FxCmisSqlParseException(
                    LOG,
                    FxCmisSqlParseException.ErrorCause.JOIN_ON_MV_COLUMN,
                    firstColumn.isMultivalued() ? firstColumn : secondColumn
            );
        }
        if (firstColumn.isMultilanguage() || secondColumn.isMultilanguage()) {
            throw new FxCmisSqlParseException(
                    LOG,
                    FxCmisSqlParseException.ErrorCause.JOIN_ON_MULTILANG_COLUMN,
                    firstColumn.isMultilanguage() ? firstColumn : secondColumn
            );
        }
        this.first = first;
        this.second = second;
        this.firstColumn = firstColumn;
        this.secondColumn = secondColumn;
    }

    public ColumnReference getFirstTableColumn() {
        return firstColumn;
    }

    public ColumnReference getSecondTableColumn() {
        return secondColumn;
    }

    public TableReference getFirstTable() {
        return first;
    }

    public TableReference getSecondTable() {
        return second;
    }

    /**
     * {@inheritDoc}
     */
    public TableReference findByAlias(String alias) {
        final TableReference result1 = first.findByAlias(alias);
        if (result1 != null) {
            return result1;
        }
        return second.findByAlias(alias);
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getReferencedAliases() {
        final List<String> result = new ArrayList<String>();
        result.addAll(first.getReferencedAliases());
        result.addAll(second.getReferencedAliases());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public List<FxType> getReferencedTypes() {
        final List<FxType> result = new ArrayList<FxType>();
        result.addAll(first.getReferencedTypes());
        result.addAll(second.getReferencedTypes());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Iterable<TableReference> getLeafTables() {
        return Iterables.concat(first.getLeafTables(), second.getLeafTables());
    }

    /**
     * {@inheritDoc}
     */
    public String getAlias() {
        throw new UnsupportedOperationException("A joined table cannot be addressed by a column alias.");
    }

    /**
     * {@inheritDoc}
     */
    public List<FxPropertyAssignment> resolvePropertyAssignment(FxEnvironment environment, String name) {
        throw new UnsupportedOperationException("A joined table cannot be addressed by a column reference.");
    }

    /**
     * {@inheritDoc}
     */
    public String getIdFilterColumn() {
        throw new UnsupportedOperationException("A joined table cannot be addressed by a column reference.");
    }

    /**
     * {@inheritDoc}
     */
    public String getVersionFilterColumn() {
        throw new UnsupportedOperationException("A joined table cannot be addressed by a column reference.");
    }

    public String getIdVersionLink(String filterTableAlias, String subSelectTableAlias) {
        throw new UnsupportedOperationException("A joined table cannot be addressed by a column reference.");
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPropertySecurityEnabled() {
        return first.isPropertySecurityEnabled() || second.isPropertySecurityEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public FxType getRootType() {
        throw new UnsupportedOperationException("A joined table has no root type.");
    }

    /**
     * {@inheritDoc}
     */
    public void accept(TableReferenceVisitor visitor) {
        visitor.visit(this);
        first.accept(visitor);
        second.accept(visitor);
    }

    /**
     * {@inheritDoc}
     */
    public List<TableReference> getSubTables() {
        return Arrays.asList(first, second);
    }
}
