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
package com.flexive.sqlParser;

import java.util.HashMap;
import java.util.Locale;

/**
 * Table
 * 
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class Table {

    public enum TYPE { CONTENT }
    private String sAlias;
    private TYPE tType;
    private HashMap<Filter.TYPE,Filter> filters;
    private String searchLanguages[];

    /**
     * Returns the languages that should be searched in.
     *
     * @return the languages that should be searched in
     */
    public String[] getSearchLanguages() {
        return searchLanguages==null?new String[0]:searchLanguages;
    }

    /**
     * Returns the version filter for the table.
     * <p />
     * Possible return values are:<br>
     * "MAX","LIVE","ALL" or an integer array (eg "1,4,7")
     *
     * @return the version filter for the table.
     */
    public String getVersionFilter() {
        Filter f = getFilter(Filter.TYPE.VERSION);
        if (f==null) {
            return Filter.VERSION.MAX.toString();
        } else {
            return f.getValue();
        }
    }

    /**
     * Sets the available languages for the table.
     * <p />
     *
     * @param isoCodes the iso codes
     * @throws SqlParserException
     */
    protected void setSearchLanguages(String isoCodes[]) throws SqlParserException {
        // searchLanguages contains all available options, filter out all unused
        // entries.
        if (isoCodes==null) {
            isoCodes = new String[0];
        }
        for (String code:isoCodes) {
            boolean found = false;
            for (String loc:Locale.getISOLanguages()) {
                if (code.equalsIgnoreCase(loc)) {
                    found =true;
                    break;
                }
            }
            if (!found) {
                throw new SqlParserException("ex.sqlSearch.languages.unknownIsoCode",code);
            }
        }
        this.searchLanguages = isoCodes;
    }

    /**
     * Constructor.
     *
     * @param type the type
     * @param alias the alias
     * @throws SqlParserException
     */
    protected Table(String type,String alias) throws SqlParserException {
        if (!type.equalsIgnoreCase("CONTENT")) {
            throw new SqlParserException("ex.sqlSearch.table.typeNotSupported",type.toUpperCase());
        }
        //this.searchLanguages = getAvailableLanguages();
        this.tType = TYPE.CONTENT;
        this.sAlias = alias.toUpperCase();
        this.filters = new HashMap<Filter.TYPE,Filter>(5);
    }

    /**
     * Adds a table specific filter.
     *
     * @param f the filter to add
     */
    protected void addFilter(Filter f) {
        this.filters.put(f.getType(),f);
    }

    /**
     * Returns the table alias.
     *
     * @return the table alias.
     */
    public String getAlias() {
        return sAlias;
    }

    /**
     * Returns the table type.
     *
     * @return the table type
     */
    public TYPE getType() {
        return tType;
    }

    /**
     * Returns a string representation of the table.
     *
     * @return a string representation of the table.
     */
    public String toString() {
        return "table[alias:"+sAlias+";type:"+tType+"]";
    }

    /**
     * Returns the desired filter, or null if the filter was not specified and has no
     * default value.
     *
     * @param t the filter to get
     * @return the filter
     */
    public Filter getFilter(Filter.TYPE t) {
        return this.filters.get(t);
    }

}
