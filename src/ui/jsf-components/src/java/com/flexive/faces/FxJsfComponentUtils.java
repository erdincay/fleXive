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
package com.flexive.faces;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

/**
 * Utility functions for JSF/Facelets components.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxJsfComponentUtils {

    /**
     * Private constructor to avoid instantiation.
     */
    private FxJsfComponentUtils() {
    }

    /**
     * Evaluate the string attribute of a component.
     *
     * @param component     a JSF component
     * @param attributeName the attribute name to be evaluated, e.g. "title"
     * @return the bound value, or null if no value is bound
     */
    public static String getStringValue(UIComponent component, String attributeName) {
        return (String) getValue(component, attributeName);
    }

    /**
     * Evaluate the integer attribute of a component.
     *
     * @param component     a JSF component
     * @param attributeName the attribute name to be evaluated, e.g. "title"
     * @return the bound value, or null if no value is bound
     */
    public static Integer getIntegerValue(UIComponent component, String attributeName) {
        return (Integer) getValue(component, attributeName);
    }

    /**
     * Evaluate the long attribute of a component.
     *
     * @param component     a JSF component
     * @param attributeName the attribute name to be evaluated, e.g. "title"
     * @param defaultValue  the default value to be used if no value is bound
     * @return the bound value, or <code>defaultValue</code> if no value is bound
     */
    public static int getLongValue(UIComponent component, String attributeName, int defaultValue) {
        final Integer intValue = (Integer) getValue(component, attributeName);
        return intValue != null ? intValue : defaultValue;
    }

    /**
     * Evaluate the long attribute of a component.
     *
     * @param component     a JSF component
     * @param attributeName the attribute name to be evaluated, e.g. "title"
     * @return the bound value, or null if no value is bound
     */
    public static Long getLongValue(UIComponent component, String attributeName) {
        return (Long) getValue(component, attributeName);
    }

    /**
     * Evaluate the long attribute of a component.
     *
     * @param component     a JSF component
     * @param attributeName the attribute name to be evaluated, e.g. "title"
     * @param defaultValue  the default value to be used if no value is bound
     * @return the bound value, or <code>defaultValue</code> if no value is bound
     */
    public static long getLongValue(UIComponent component, String attributeName, long defaultValue) {
        final Long longValue = (Long) getValue(component, attributeName);
        return longValue != null ? longValue : defaultValue;
    }

    /**
     * Evaluate the boolean attribute of a component.
     *
     * @param component     a JSF component
     * @param attributeName the attribute name to be evaluated, e.g. "title"
     * @return the bound value, or null if no value is bound
     */
    public static Boolean getBooleanValue(UIComponent component, String attributeName) {
        return (Boolean) getValue(component, attributeName);
    }

    /**
     * Evaluate the boolean attribute of a component.
     *
     * @param component     a JSF component
     * @param attributeName the attribute name to be evaluated, e.g. "title"
     * @param defaultValue  the default value to be used when the attribute is null
     * @return the bound value, or null if no value is bound
     */
    public static boolean getBooleanValue(UIComponent component, String attributeName, boolean defaultValue) {
        final Boolean value = (Boolean) getValue(component, attributeName);
        return value != null ? value : defaultValue;
    }

    public static Object getValue(UIComponent component, String attributeName) {
        ValueExpression ve = component.getValueExpression(attributeName);
        if (ve != null) {
            return ve.getValue(FacesContext.getCurrentInstance().getELContext());
		} 
		return null;
	}
}
