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
package com.flexive.shared.search;

import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.structure.FxEnvironment;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * A displayed column info object for the result preferences.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ResultColumnInfo implements Serializable {
    private static final long serialVersionUID = -3605807783224372957L;

    private final Table table;
    private final String propertyName;
    private final String suffix;

    /**
     * Create a displayed column info object.
     *
     * @param table         the table containing the type/assignment
     * @param propertyName      the type name
     * @param suffix        an (optional) suffix
     */
    public ResultColumnInfo(Table table, String propertyName, String suffix) {
        FxSharedUtils.checkParameterEmpty(table, "TABLE");
        FxSharedUtils.checkParameterEmpty(propertyName, "TYPENAME");
        this.table = table;
        this.propertyName = propertyName;
        this.suffix = suffix;
    }

    public Table getTable() {
        return table;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getSuffix() {
        return suffix;
    }

    /**
     * Returns the label to be used e.g. as result column header.
     *
     * @param environment the current environment
     * @return  the label to be used e.g. as result column header.
     */
    public String getLabel(FxEnvironment environment) {
        if (isProperty()) {
            try {
                return environment.getProperty(propertyName).getLabel().getBestTranslation();
            } catch (FxRuntimeException e) {
                return propertyName;
            }
        }
        return propertyName;
    }

    /**
     * Return the full column name to be used in SQL queries.
     * For example:<br/>
     * <code>new ColumnInfo(Table.CONTENT, "mandator", "name").getColumnName() -> co.mandator.name</code>
     * @return the full column name to be used in SQL queries.
     */
    public String getColumnName() {
        return table.getAlias() + "." + propertyName + (StringUtils.isNotBlank(suffix) ? "." + suffix : "");
    }

    /**
     * Return true if the object info's property is a "real" structure
     * property.
     * @return  true if this info object is for a "real" structure property
     */
    public boolean isProperty() {
        return !propertyName.startsWith("@");
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResultColumnInfo that = (ResultColumnInfo) o;

        return StringUtils.equalsIgnoreCase(propertyName, that.propertyName)
                && table.equals(that.table)
                && StringUtils.equalsIgnoreCase(suffix, that.suffix);

    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int result;
        result = table.hashCode();
        result = 31 * result + propertyName.hashCode();
        result = 31 * result + (suffix != null ? suffix.hashCode() : 0);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "ColumnInfo{" + table + ", " + propertyName + (StringUtils.isNotBlank(suffix) ? ", " + suffix : "") + "}";
    }
}
