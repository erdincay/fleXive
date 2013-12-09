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
package com.flexive.shared.configuration;

/**
 * Editable parameter data - mainly used for testing.
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @param <T>   the parameter value type
 */

public class ParameterDataEditBean<T> extends ParameterDataBean<T> {
    private static final long serialVersionUID = -5075613290682777446L;

    /**
     * Constructor.
     * 
     * @param path  path for the parameter
     * @param key   key for the parameter (unless for aggregate parameters)
     * @param defaultValue  default value
     */
    public ParameterDataEditBean(ParameterPath path, String key, T defaultValue) {
        super(path, key, defaultValue);
    }

    /**
     * Copy constructor.
     *
     * @param data  the parameter data beans to be copied
     */
    public ParameterDataEditBean(ParameterData<T> data) {
        this(data.getPath(), data.getKey(), data.getDefaultValue());
    }

    /**
     * Set the parameter path.
     * @param path  the parameter path.
     * @return  this
     */
    public synchronized ParameterDataEditBean<T> setPath(ParameterPath path) {
        this.path = path;
        return this;
    }
    
    /**
     * Set the parameter key.
     * @param key   the parameter key.
     * @return  this
     */
    public synchronized ParameterDataEditBean<T> setKey(String key) {
        this.key = key;
        return this;
    }
    
    /**
     * Set the parameter's default value. 
     * @param defaultValue  the parameter's default value.
     * @return  this
     */
    public synchronized ParameterDataEditBean<T> setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }
}
