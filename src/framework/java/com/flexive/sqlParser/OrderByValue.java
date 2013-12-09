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
package com.flexive.sqlParser;

/**
 * OrderBy
 * 
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class OrderByValue extends Constant {

    private boolean ascending;
    private int columnIndex = -1;

    /**
     * Constructor.
     *
     * @param  ascending true if ascending, false if descending
     * @param value the value
     * @throws SqlParserException if a error occured
     */
    public OrderByValue(String value,boolean ascending) throws SqlParserException {
        super(value);
        this.ascending = ascending;
    }

    /**
     * Internal setter for the selected value belonging to this order by. If the index
     * is negative, it indicates a column selected through the "wildcard" selector, co.*.
     * If a negative column index is not found in the actual result set, an exception
     * is thrown when the final order by is assembled.
     *
     * @param columnIndex the index of the column to sort
     */
    protected void setSelectedValue(int columnIndex) {
        this.columnIndex = columnIndex;
    }


    /**
     * The position of the column to sort in the selected values list, 0 based.
     * 
     * @return The position of the column to sort in the selected values list
     */
    public int getColumnIndex() {
        return columnIndex;
    }

    /**
     * Returns the sort order for this element (ascending or descending)
     *
     * @return true if ascending, false if descending
     */
    public boolean isAscending() {
        return ascending;
    }

    /**
     * Returns true if the given {@link SelectedValue} can be used for sorting.
     *
     * @param sv    the selected value
     * @return  true if the given {@link SelectedValue} can be used for sorting.
     */
    public boolean isUsableForSorting(SelectedValue sv) {
        final String alias = getValue().charAt(0) == '#' ? getValue().substring(1) : getValue();
        return sv.getAlias().equalsIgnoreCase(alias)
                // also try table alias + column alias
                || (sv.getValue() instanceof Property
                && (((Property) sv.getValue()).getTableAlias() + "." + sv.getAlias()).equalsIgnoreCase(alias));
    }

    @Override
    public String toString() {
        return super.toString()+" "+(this.ascending?"asc":"desc");
    }
}
