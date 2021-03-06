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
    @Override
    public ParameterData<T> getData() {
        return this.data;
    }

    /** {@inheritDoc} */
    @Override
    public T getDefaultValue() {
        return data.getDefaultValue();
    }
    
    /** {@inheritDoc} */
    @Override
    public String getKey() {
        return data.getKey();
    }

    /** {@inheritDoc} */
    @Override
    public ParameterPath getPath() {
        return data.getPath();
    }

    /** {@inheritDoc} */
    @Override
    public ParameterScope getScope() {
        return data.getPath().getScope();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCached() {
        return data.isCached();
    }


    /** {@inheritDoc} */
    @Override
    public boolean isValid(T value) {
        // by default all parameters of the given type are valid
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    public String getDatabaseValue(T value) {
        return value != null ? String.valueOf(value) : null;
    }

    @Override
    public Parameter<T> setData(ParameterData<T> parameterData) {
        if (!frozen) {
            this.data = parameterData;
        } else {
            throw new IllegalStateException("Parameter " + this + " frozen and may not be changed.");
        }
        return this;
    }

    @Override
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
