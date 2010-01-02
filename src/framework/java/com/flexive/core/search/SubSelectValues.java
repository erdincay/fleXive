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
package com.flexive.core.search;

import com.flexive.shared.exceptions.FxSqlSearchException;
import com.flexive.shared.search.SortDirection;
import com.flexive.sqlParser.Property;
import com.flexive.sqlParser.Value;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper to store subselect values
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class SubSelectValues {
    private static final Log LOG = LogFactory.getLog(SubSelectValues.class);

    private final int resultSetPos;
    private final SortDirection sortDirection;
    private final List<Item> items = new ArrayList<Item>();
    private int values = 0;
    private Value value = null;

    /**
     * Constructor.
     *
     * @param resultSetPos  the position within the resultset
     * @param sortDirection the sort direction for this column
     */
    public SubSelectValues(int resultSetPos, SortDirection sortDirection) {
        this.resultSetPos = resultSetPos;
        this.sortDirection = sortDirection;
    }

    public boolean isSorted() {
        return !SortDirection.UNSORTED.equals(sortDirection);
    }

    public boolean isSortedAscending() {
        return SortDirection.ASCENDING.equals(sortDirection);
    }

    public void addItem(String select, int resultSetPos, boolean isXpath) {
        if (!isXpath) values++;
        items.add(new Item(select, isXpath, resultSetPos, items.size()));
    }

    public List<Item> getItems() {
        return items;
    }

    public int getResultSetPos() {
        return resultSetPos;
    }

    /**
     * Returns true if this object contains more than one select that is not the XPath.
     *
     * @return true if this object contains more than one select that is not the XPath
     */
    protected boolean isMultivalue() {
        return values > 1;
    }


    /**
     * Applies any functions and reference selectors.
     *
     * @param ds    the caller
     * @param prop  the property
     * @param entry the entry
     * @return the modified object itself
     * @throws FxSqlSearchException if the function fails
     */
    public SubSelectValues prepare(DataSelector ds, Value prop, PropertyEntry entry) throws FxSqlSearchException {
        if (prop instanceof Property) {
            applySelector(ds, (Property) prop, entry);
        }
        applyWrapper(prop);
        this.value = prop;
        return this;
    }


    public Value getValue() {
        return value;
    }

    /**
     * Applies the reference selector if needed.
     *
     * @param ds    the caller
     * @param prop  the property
     * @param entry the entry
     * @throws FxSqlSearchException if the function fails
     */
    protected void applySelector(DataSelector ds, Property prop, PropertyEntry entry) throws FxSqlSearchException {
        // any selector to apply at all?
        if (!prop.hasField()) {
            return;
        }

        // Find and apply the selector
        if (isMultivalue() || entry.getTableType() != PropertyResolver.Table.T_CONTENT) {
            throw new FxSqlSearchException(LOG, "ex.sqlSearch.query.fieldNotAllowedFor", prop.getPropertyName());
        }
        final FieldSelector selector = ds.getSelectors().get(prop.getPropertyName().toUpperCase());
        if (selector == null) {
            throw new FxSqlSearchException(LOG, "ex.sqlSearch.query.noFieldsForProp",
                    prop.getField(), prop.getPropertyName());
        } else {
            for (Item it : items) {
                if (it.isXpath()) continue;
                final StringBuffer tmp = new StringBuffer(it.select);
                selector.apply(prop, entry, tmp);
                it.setSelect(tmp.toString());
            }
        }
    }

    /**
     * Applies a prefix and suffix (eg a function) to the subselect item(s).
     *
     * @param prop the property
     * @throws FxSqlSearchException if no prefix/suffix can be applied
     */
    protected void applyWrapper(Value prop) throws FxSqlSearchException {

        // anything to apply?
        if (prop.getFunctionsStart().length() == 0 && prop.getFunctionsEnd().length() == 0) return;

        if (isMultivalue()) {
            throw new FxSqlSearchException(LOG, "ex.sqlSearch.function.multipleColumns",
                    (prop instanceof Property) ? ((Property) prop).getPropertyName() : "[constant]",
                    prop.getFunctionsStart() + prop.getFunctionsEnd());
        }
        for (Item it : items) {
            if (it.isXpath()) continue;
            it.setSelect(prop.getFunctionsStart() + it.select + prop.getFunctionsEnd());
        }
    }

    public static class Item {
        private final String alias;
        private final boolean isXpath;
        private final boolean orderBy;
        private String select;

        private Item(String select, boolean isXpath, int resultSetPos, int pos) {
            this.isXpath = isXpath;
            this.select = select;
            this.orderBy = !isXpath;
            this.alias = "prop" + (isXpath ? "X" : "") + "_" + resultSetPos + "_" + pos;
        }

        public String getSelect() {
            return select;
        }

        public String getAlias() {
            return alias;
        }

        public boolean isOrderBy() {
            return orderBy;
        }

        public boolean isXpath() {
            return isXpath;
        }

        public void setSelect(String select) {
            this.select = select;
        }
    }

}
