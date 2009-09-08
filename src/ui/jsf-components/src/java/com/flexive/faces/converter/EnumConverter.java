/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.faces.converter;

import org.apache.commons.lang.StringUtils;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

/**
 * Generic JSF Enum converter. Stores the Enum class in the value (unlike the JSF 1.2 Enum converter,
 * where the target class must be passed in the constructor), thus no further configuration is necessary. 
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class EnumConverter implements Converter {

    /**
     * {@inheritDoc}
     */
    public Object getAsObject(FacesContext facesContext, UIComponent uiComponent, String value) {
        return getValue(value);
    }

    @SuppressWarnings({"unchecked"})
    public static Enum getValue(String value) {
        if (StringUtils.isBlank(value) || "-1".equals(value)) {
            return null;
        }
        String[] values = value.split("::");
        if (values.length != 2) {
            throw new IllegalArgumentException("Invalid argument for enum converter: " + value);
        }
        try {
            return Enum.valueOf((Class<? extends Enum>) Class.forName(values[0]), values[1]);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getAsString(FacesContext facesContext, UIComponent uiComponent, Object object) {
        if (object == null) {
            return null;
        }
        return encodeEnum((Enum) object);

    }

    /**
     * Encode an Enum value to a string that can be decoded using {@link EnumConverter#getValue(String)}.
     * <p>This method is exposed in JSF-EL as <code>fx:encodeEnum</code>.</p>
     *
     * @param value the Enum value to be encoded
     * @return  the encoded string representation
     */
    public static String encodeEnum(Enum value) {
        // replace inner class suffixes of class name and append enum name
        return value.getClass().getName().replaceFirst("\\$\\d+$", "") + "::" + value.name();
    }
}
