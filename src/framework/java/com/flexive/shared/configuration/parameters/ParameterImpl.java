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
package com.flexive.shared.configuration.parameters;

import com.flexive.shared.configuration.Parameter;
import com.flexive.shared.configuration.ParameterData;
import com.flexive.shared.configuration.ParameterPath;
import com.flexive.shared.configuration.ParameterScope;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic parameter implementation. 
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @param <T> value type of the parameter
 */
public abstract class ParameterImpl<T>  implements Parameter<T>, Serializable {
    private static final long serialVersionUID = -2727270342944531283L;

    /** Parameter data */
    private ParameterData<T> data;
    private boolean frozen;

    /** Registers all known parameters */
    private static final List<Parameter> registeredParameters = new ArrayList<Parameter>();

    /**
     * Create a new parameter with the given data
     * @param parameter parameter data
     * @param registerParameter if the parameter should be registered in the static parameter table
     *  (don't do this for non-static parameter declarations)
     */
    protected ParameterImpl(ParameterData<T> parameter, boolean registerParameter) {
        this.data = parameter;
        synchronized(registeredParameters) {
            // TODO: need to rethink this - probably not very useful in its current state
            if (registerParameter && !registeredParameters.contains(this)) {
                registeredParameters.add(this);
            }
        }
    }

    /** {@inheritDoc} */
    public ParameterData<T> getData() {
        return this.data;
    }

    /** {@inheritDoc} */
    public T getDefaultValue() {
        return data.getDefaultValue();
    }
    
    /** {@inheritDoc} */
    public String getKey() {
        return data.getKey();
    }

    /** {@inheritDoc} */
    public ParameterPath getPath() {
        return data.getPath();
    }

    /** {@inheritDoc} */
    public ParameterScope getScope() {
        return data.getPath().getScope();
    }
    
    /** {@inheritDoc} */
    public boolean isValid(T value) {
        // by default all parameters of the given type are valid
        return true;
    }
    
    /** {@inheritDoc} */
    public String getDatabaseValue(T value) {
        return value != null ? String.valueOf(value) : null;
    }

    public Parameter<T> setData(ParameterData<T> parameterData) {
        if (!frozen) {
            this.data = parameterData;
        } else {
            throw new IllegalStateException("Parameter " + this + " frozen and may not be changed.");
        }
        return this;
    }

    public Parameter<T> freeze() {
        frozen = true;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "[" + data.toString() + "]";
    }
}
