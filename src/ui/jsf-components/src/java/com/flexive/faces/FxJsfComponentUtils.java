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
package com.flexive.faces;

import org.apache.commons.lang.StringUtils;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.FacesException;

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
     * Evaluate the string attribute of a component.
     *
     * @param component     a JSF component
     * @param attributeName the attribute name to be evaluated, e.g. "title"
     * @param defaultValue  the default value to be used if no value is bound
     * @return the bound value, or null if no value is bound
     */
    public static String getStringValue(UIComponent component, String attributeName, String defaultValue) {
        return StringUtils.defaultIfEmpty((String) getValue(component, attributeName), defaultValue);
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
    public static int getIntegerValue(UIComponent component, String attributeName, int defaultValue) {
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

    /**
     * Throws a FacesException if the given value is not set.
     *
     * @param attributeName the attribute name, e.g. "var"
     * @param componentName the component name, e.g. "fx:value"
     * @param value the value set on the component
     * @throws javax.faces.FacesException   if value is null or a blank string
     */
    public static void requireAttribute(String componentName, String attributeName, Object value) throws FacesException {
        if (value == null || (value instanceof String && StringUtils.isBlank((String) value))) {
            throw new FacesException("Attribute '" + attributeName + "' of " + componentName + " not set.");
        }
    }
}
