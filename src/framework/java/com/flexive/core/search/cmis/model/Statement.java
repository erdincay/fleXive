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
package com.flexive.core.search.cmis.model;

import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.search.SortDirection;
import com.flexive.shared.structure.FxType;

import java.util.*;

/**
 * The root class representing a CMIS query.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class Statement {
    private final List<Selectable> selectedColumns = new ArrayList<Selectable>();
    private final List<TableReference> tables = new ArrayList<TableReference>();
    private final List<SortSpecification> orderByColumns = new ArrayList<SortSpecification>();
    private Condition rootCondition = null;

    public void addSelectedColumn(Selectable column) {
        selectedColumns.add(column);
    }

    public void addTable(TableReference table) {
        tables.add(table);
    }

    public void removeTable(TableReference table) {
        tables.remove(table);
    }

    public void addOrderByColumn(String columnName, SortDirection direction) {
        orderByColumns.add(new SortSpecification(getSelectedColumn(columnName), direction));
    }
    
    public List<Selectable> getSelectedColumns() {
        return Collections.unmodifiableList(selectedColumns);
    }

    public List<TableReference> getTables() {
        return Collections.unmodifiableList(tables);
    }

    public List<SortSpecification> getOrderByColumns() {
        return Collections.unmodifiableList(orderByColumns);
    }

    public TableReference getTable(String alias) {
        for (TableReference table : tables) {
            final TableReference result = table.findByAlias(alias);
            if (result != null) {
                return result;
            }
        }
        throw new FxInvalidParameterException("alias", "ex.cmis.model.undefined.table",
                alias, getTableAliases()).asRuntimeException();
    }

    public Selectable getSelectedColumn(String alias) {
        for (Selectable column : selectedColumns) {
            if (alias.equalsIgnoreCase(column.getAlias())) {
                return column;
            }
        }
        throw new FxInvalidParameterException("alias", "ex.cmis.model.undefined.column",
                alias, getColumnAliases()).asRuntimeException();
    }

    public Collection<FxType> getReferencedTypes() {
        final Set<FxType> types = new HashSet<FxType>();
        for (TableReference table : tables) {
            types.addAll(table.getReferencedTypes());
        }
        return types;
    }


    private List<String> getTableAliases() {
        final List<String> result = new ArrayList<String>();
        for (TableReference table : tables) {
            result.addAll(table.getReferencedAliases());
        }
        return result;
    }

    private List<String> getColumnAliases() {
        final List<String> result = new ArrayList<String>();
        for (Selectable column : selectedColumns) {
            result.add(column.getAlias());
        }
        return result;
    }

    public Condition getRootCondition() {
        return rootCondition;
    }

    public void setRootCondition(Condition rootCondition) {
        this.rootCondition = rootCondition;
    }
}
