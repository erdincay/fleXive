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

import com.flexive.shared.FxArrayUtils;
import com.flexive.shared.search.query.VersionFilter;
import org.apache.commons.lang.StringUtils;

import java.util.StringTokenizer;

/**
 * Filter
 * 
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class Filter {
    public enum TYPE {
        VERSION,
        IGNORE_CASE,
        MAX_RESULTROWS,
        SEARCH_LANGUAGES,
        /*RESULT_LANGUAGES,*/
        BRIEFCASE
    }

    private TYPE type;
    private String value;
    private Table table;



    /**
     * Constructor.
     *
     * @param stmt the statement the filter belongs to
     * @param type the type of the filter, case insensitive
     * @param value the value of the filter
     * @throws SqlParserException if a error occured
     */
    protected Filter(FxStatement stmt,String type,String value) throws SqlParserException {

        // Trim away any whitespaces.
        // The parser uses '|' as list separator, but in most cases we want ','.
        value = value.trim().replace('|',',');
        this.value = value;

        if (type.indexOf(".")>0) {
            initTableFilter(stmt, type, value);
        } else {
            type=type.toUpperCase();
            if (type.equalsIgnoreCase(String.valueOf(TYPE.BRIEFCASE))) {
                try {
                    stmt.setBriefcaseFilter(FxArrayUtils.toLongArray(value,','));
                    stmt.setMaxResultRows(Integer.MAX_VALUE);
                } catch(Throwable t) {
                    throw new SqlParserException("ex.sqlSearch.filter.invalidNumber",
                            type,String.valueOf(TYPE.MAX_RESULTROWS));
                }
            } else if (type.equalsIgnoreCase(String.valueOf(TYPE.IGNORE_CASE))) {
                stmt.setIgnoreCase(getValueAsBoolean());
            } else if (type.equalsIgnoreCase(String.valueOf(TYPE.MAX_RESULTROWS))) {
                this.type = TYPE.MAX_RESULTROWS;
                try {
                    final int maxResultRows = Integer.valueOf(value);
                    if (maxResultRows < 0) {
                        throw new SqlParserException("ex.sqlSearch.filter.negativeNumber", TYPE.MAX_RESULTROWS);
                    }
                    if (stmt.getBriefcaseFilter().length == 0) {
                        stmt.setMaxResultRows(maxResultRows);
                    }
                } catch(Exception exc) {
                    throw new SqlParserException("ex.sqlSearch.filter.invalidNumber",
                            type,String.valueOf(TYPE.MAX_RESULTROWS));
                }
            } else {
                // use content filters as fallback
                initTableFilter(stmt, "co." + type, value);
            }
            stmt.addFilter(this);
        }

    }

    private void initTableFilter(FxStatement stmt, String type, String value) throws SqlParserException {
        StringTokenizer st = new StringTokenizer(type,".",false);
        String sTable = st.nextToken().toUpperCase();
        this.table = stmt.getTableByAlias(sTable);
        type = st.nextToken();
        type = type.toUpperCase();
        if (this.table ==null) {
            throw new SqlParserException("ex.sqlSearch.filter.unknownTable",sTable,type);
        }
        // ----------- VERSION -------------------------------------------------------------------------------------
        if (type.equalsIgnoreCase(String.valueOf(TYPE.VERSION))) {
            _processVersionFilter(stmt);
        }
        // ----------- CONTENT -------------------------------------------------------------------------------------
        else if (type.equalsIgnoreCase("TYPE")) {
            stmt.setContentTypeFilter(value);
        }
        // ----------- LANGUAGES -----------------------------------------------------------------------------------
        else if (type.equalsIgnoreCase(String.valueOf(TYPE.SEARCH_LANGUAGES))) {
            this.type = TYPE.SEARCH_LANGUAGES;
            table.setSearchLanguages(value.split(","));
        }
        // ----------- INVALID -------------------------------------------------------------------------------------
        else {
            throw new SqlParserException("ex.sqlSearch.filter.unknownTableFilter",
                    type, StringUtils.join(TYPE.values(), ", "));
        }
        this.table.addFilter(this);
    }

    /**
     * Processes the VERSION filter.
     *
     * @param stmt the statement
     * @throws SqlParserException if the version filter value is invalid
     */
    private void _processVersionFilter(FxStatement stmt) throws SqlParserException {
        this.type = TYPE.VERSION;
        VersionFilter ver;
        value = value.toUpperCase();
        if (value.equals(VersionFilter.AUTO.toString())) {
            // TODO: AUTO depends on the user/session setting
            value = VersionFilter.MAX.toString();
        }
        if (value.equals(VersionFilter.MAX.toString())) {
            ver = VersionFilter.MAX;
        } else if (value.equals(VersionFilter.LIVE.toString())) {
            ver = VersionFilter.LIVE;
        } else if (value.equals(VersionFilter.ALL.toString())) {
            ver = VersionFilter.ALL;
        } else {
              throw new SqlParserException("ex.sqlSearch.filter.unknownVersionFilter",value);
        }
        stmt.setVersionFilter(ver);
    }

    /**
     * Returns the type of the filter.
     *
     * @return the type of the filter
     */
    public TYPE getType() {
        return type;
    }


    /**
     * The value of the filter.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * The value of the filter as integer.
     *
     * @return the valuee
     */
    public int getValueAsInt() {
        return Integer.valueOf(value);
    }

    /**
     * The value of the filter as integer.
     *
     * @return the valuee
     */
    public boolean getValueAsBoolean() {
        return (value!=null && (value.equalsIgnoreCase("T") || value.equalsIgnoreCase("TRUE")));
    }


    /**
     * The table the filter is assigned to, or null if it is a general filter.
     *
     * @return the table
     */
    public Table getTable() {
        return table;
    }

}
