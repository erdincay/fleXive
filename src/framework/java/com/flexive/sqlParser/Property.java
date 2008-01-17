/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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
     * @param property   the property
     * @param field      null, or the field to use from the property (eg. "m.mandator.name" where name is the field to
     *                   use from the mandator)
     */
    public Property(String tableAlias, String property, String field) {
        super(tableAlias + "." + property);
        this.tableAlias = tableAlias.toUpperCase();
        this.property = property.toUpperCase();
        this.field = field == null ? null : field.trim().toUpperCase();
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
    public synchronized boolean isAssignment() {
        if( assignment == null )
            this.assignment = property.indexOf('/') > 0 || property.charAt(0) == '#';
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
        return this.property.equals("*");
    }

}
