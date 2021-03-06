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

import com.flexive.shared.interfaces.SearchEngine;

import java.util.Collection;
import java.util.StringTokenizer;

/**
 * Property reference class.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class Property extends Value {

    private String tableAlias;
    private String property;
    private String field;
    private Boolean assignment = null;

    /**
     * Constructor.
     *
     * @param tableAlias the table the property belongs to
     * @param property   the property or assignment (e.g. #folder/caption)
     * @param field      null, or the field to use from the property (eg. "m.mandator.name" where name is the field to
     *                   use from the mandator)
     */
    public Property(String tableAlias, String property, String field) {
        super(property.startsWith("#") ? property.substring(1) : property);
        this.assignment = property.startsWith("#");
        this.tableAlias = tableAlias.toUpperCase();
        this.property = (this.assignment ? property.substring(1) : property).toUpperCase();
        this.field = field == null ? null : field.trim().toUpperCase();
    }

    public Property(FxStatement stmt, String value) {
        super(null);
        // decode value
        assignment = value.charAt(0) == '#';
        final StringTokenizer st = new StringTokenizer(assignment ? value.substring(1) : value, ".", false);
        tableAlias = st.nextToken().toUpperCase();
        property = st.hasMoreTokens() ? st.nextToken().toUpperCase() : null;
        field = st.hasMoreTokens() ? st.nextToken().toUpperCase() : null;
        if (property == null) {
            // only a property name was specified, use default table
            property = tableAlias;
            tableAlias = "co";
        }
        setValue(property);
    }

    public void publishTableAliases(Collection<String> aliases) {
        // treat unknown table aliases as property names
        for (String stmtAlias: aliases) {
            if (tableAlias.equalsIgnoreCase(stmtAlias)) {
                return; // table alias ok
            }
        }
        if (field == null) {
            // table --> property, property --> field
            field = property;
            property = tableAlias;
            setValue(property);
            tableAlias = "co";
        }
        // do nothing if tableAlias, property and field are set - in this case
        // the alias is actually invalid and an error message should be displayed
    }
    /**
     * The field that should be used from the property, or null.
     * <p/>
     * If set the value is always a uppercase string
     *
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * Returns true if a field is set.
     *
     * @return true if a field is set
     */
    public boolean hasField() {
        return field != null && field.length() > 0;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object
     */
    public String toString() {
        return this.getFunctionsStart() + tableAlias + "." + property + this.getFunctionsEnd();
    }

    /**
     * Returns the table alias, this is a empty string the the value info
     * represents a constant value.
     *
     * @return the table alias
     */
    public String getTableAlias() {
        return this.tableAlias;
    }

    /**
     * Is this a property or an assignment?
     *
     * @return property or assignment?
     */
    public boolean isAssignment() {
        if( assignment == null )
            this.assignment = property.charAt(0) == '#';
        return assignment;
    }

    /**
     * Get the name of the property/assignment
     *
     * @return name of the property/assignment
     */
    public String getPropertyName() {
        return this.property;
    }

    /**
     * Returns true if this property is a wildcard.
     *
     * @return true if this property is a wildcard
     */
    public boolean isWildcard() {
        return SearchEngine.PROP_WILDCARD.equals(this.property);
    }

    /**
     * Returns true if this property is the "user properties wildcard" that selects all
     * user-defined propertie.s
     *
     * @return  true if this property is the "user properties wildcard" 
     */
    public boolean isUserPropsWildcard() {
        return SearchEngine.PROP_USERWILDCARD.equals(this.property);
    }

    public boolean isCustomSql() {
        return SearchEngine.PROP_CUSTOM_SQL.equals(this.property);
    }
}
