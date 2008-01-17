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
package com.flexive.shared.configuration;

/**
 * Basic implementation of <code>ParameterData</code>. Container for the path,
 * the key and the default value of a parameter.
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 *
 * @param <T>   parameter value type
 */
public class ParameterDataBean<T> implements ParameterData<T> {
    private static final long serialVersionUID = 2602058914095136489L;
    protected ParameterPath path;
    protected String key;
    protected T defaultValue;

    /**
	 * Constructor.
	 * 
	 * @param path	path for the parameter
	 * @param key	key for the parameter (unless for aggregate parameters)
	 * @param defaultValue	default value
	 */
	public ParameterDataBean(ParameterPath path, String key, T defaultValue) {
		this.key = key;
		this.path = path;
		this.defaultValue = defaultValue;
	}
	
	
	/**
	 * Constructor for aggregate parameters (with generated keys).
	 * 
	 * @param path	path to be used for the parameters
	 * @param defaultValue	the default value
	 */
	public ParameterDataBean(ParameterPath path, T defaultValue) {
		this.path = path;
		this.key = "";
		this.defaultValue = defaultValue;
	}


    /** {@inheritDoc} */
	public ParameterPath getPath() {
		return path;
	}

    /** {@inheritDoc} */
	public String getKey() {
		return key;
	}
	
    /** {@inheritDoc} */
	public T getDefaultValue() {
		return defaultValue;
	}

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return path.toString() + "/" + key;
    }
}
