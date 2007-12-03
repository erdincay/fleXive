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

import java.io.Serializable;

/**
 * An order-by property for the result preferences.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ResultOrderByInfo extends ResultColumnInfo implements Serializable {

    private static final long serialVersionUID = -6847831827485999778L;
    private final SortDirection direction;

    public ResultOrderByInfo(Table table, String propertyName, String suffix, SortDirection direction) {
        super(table, propertyName, suffix);
        FxSharedUtils.checkParameterEmpty(direction, "DIRECTION");
        this.direction = direction;
    }

    public SortDirection getDirection() {
        return direction;
    }


    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ResultOrderByInfo)) {
            return false;
        }
        return super.equals(o) && ((ResultOrderByInfo) o).direction.equals(this.direction);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return 31 * super.hashCode() + direction.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        String base = super.toString();
        if (base != null && base.length() > 0) {
            return base.substring(0, base.length() - 1) + ", " + direction.name() + "}";
        }
        return base;
    }
}
