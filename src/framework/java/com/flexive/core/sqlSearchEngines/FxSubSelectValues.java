/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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
package com.flexive.core.sqlSearchEngines;

import com.flexive.shared.exceptions.FxSqlSearchException;
import com.flexive.sqlParser.Property;
import com.flexive.sqlParser.Value;

import java.util.ArrayList;

/**
 * Helper to store subselect values
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxSubSelectValues {


    private ArrayList<Item> items = new ArrayList<Item>(25);
    private int values = 0;
    int resultSetPos = -1;
    Boolean ascending = null;
    private Value value = null;

    /**
     * Constructor.
     *
     * @param resultSetPos the position within the resultset
     * @param ascending    null if the subselect is not part of the orderBy,
     *                     or true if it is sorted ascending or false if it is ordered descending
     */
    public FxSubSelectValues(int resultSetPos, Boolean ascending) {
        this.ascending = ascending;
        this.resultSetPos = resultSetPos;
    }

    public boolean isSorted() {
        return ascending != null;
    }

    public boolean isSortedAscending() {
        return ascending != null && ascending.equals(Boolean.TRUE);
    }

    public void addItem(String select, int resultSetPos, boolean isXpath) {
        if (!isXpath) values++;
        items.add(new Item(select, isXpath, resultSetPos, items.size()));
    }

    public ArrayList<Item> getItems() {
        return items;
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
    public FxSubSelectValues prepare(DataSelector ds, Value prop, PropertyResolver.Entry entry) throws FxSqlSearchException {
        applySelector(ds, (Property) prop, entry);
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
    protected void applySelector(DataSelector ds, Property prop, PropertyResolver.Entry entry) throws FxSqlSearchException {

        // any selector to apply at all?
        if (!prop.hasField()) {
            return;
        }

        // Find and apply the selector
        if (isMultivalue() || entry.getTableType() != PropertyResolver.TABLE.T_CONTENT) {
            throw new FxSqlSearchException("ex.sqlSearch.query.fieldNotAllowedFor", prop.getPropertyName());
        }
        FxFieldSelector selector = ds.getSelectors().get(prop.getPropertyName());
        if (selector == null) {
            throw new FxSqlSearchException("ex.sqlSearch.query.noFieldsForProp",
                    prop.getField(), prop.getPropertyName());
        } else {
            for (Item it : items) {
                if (it.isXpath()) continue;
                StringBuffer tmp = new StringBuffer(it.select);
                selector.apply(prop, entry, tmp);
                it.select = tmp.toString();
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
            throw new FxSqlSearchException("ex.sqlSearch.query.fieldNotAllowedFor",
                    (prop instanceof Property) ? ((Property) prop).getPropertyName() : "[constant]");
        }
        for (Item it : items) {
            if (it.isXpath()) continue;
            it.select = prop.getFunctionsStart() + it.select + prop.getFunctionsEnd();
        }
    }

    public class Item {
        private String select;
        private String alias;
        private boolean isXpath;
        private boolean orderBy;

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
    }

}
