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

import com.flexive.shared.FxXMLUtils;
import com.flexive.shared.configuration.Parameter;
import com.flexive.shared.configuration.ParameterData;
import com.flexive.shared.search.*;
import com.flexive.shared.search.query.AssignmentValueNode;
import com.flexive.shared.search.query.QueryOperatorNode;
import com.flexive.shared.search.query.QueryRootNode;
import com.thoughtworks.xstream.XStream;

/**
 * Generic Object parameter wrapper. Takes any java object, serializes
 * it to XML (currently using XStream), and stores it in the configuration.
 * 
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @param <T> type of the object stored in the parameter
 */
class ObjectParameter<T> extends ParameterImpl<T> {
    private static final long serialVersionUID = -1843420825643455601L;
    
    // global parameters
    
    // per-division parameters
    
    // per-user configuration parameters

    private Class<T> cls;       // the value object's class
    private transient XStream instanceStream;     // overrides default static xstream instance

    /** xstream instance used for serializing all parameters - aliases added in static constructor */
    private static final XStream xStream;

    static {
    	xStream = new XStream();
    	
    	// query node classes aliases
    	xStream.alias("queryOperatorNode", QueryOperatorNode.class);
    	xStream.alias("queryRootNode", QueryRootNode.class);
    	xStream.alias("propertyValueNode", AssignmentValueNode.class);

        // result preferences class
        xStream.alias("resultPreferences", ResultPreferences.class);
        // no distinction between immutable and editable objects for stored values
        xStream.alias("resultPreferences", ResultPreferencesEdit.class);
        xStream.alias("rpColumnInfo", ResultColumnInfo.class);
        xStream.alias("rpOrderByInfo", ResultOrderByInfo.class);
        xStream.alias("rpOrderByDirection", SortDirection.class);
    }

    /**
     * Creates a new object parameter definition.
     * @param parameter parameter data
     * @param cls   the object's class
     * @param registerParameter if the parameter should be registered in the static parameter table
     *  (don't do this for non-static parameter declarations)
     */
    public ObjectParameter(ParameterData<T> parameter, Class<T> cls, boolean registerParameter) {
        super(parameter, registerParameter);
        this.cls = cls;
    }

    /**
     * Creates a new object parameter definition.
     * @param parameter parameter data
     * @param cls   the object's class
     * @param xStream   xStream instance to be used for loading and storing values for this parameter.
     *                  If null, the default xStream instance stored in this class will be used.
     * @param registerParameter if the parameter should be registered in the static parameter table
     *  (don't do this for non-static parameter declarations)
     */
    public ObjectParameter(ParameterData<T> parameter, Class<T> cls, boolean registerParameter, XStream xStream) {
        super(parameter, registerParameter);
        this.cls = cls;
        this.instanceStream = xStream;
    }

    /** {@inheritDoc} */
    public Parameter<T> copy() {
        return new ObjectParameter<T>(getData(), cls, false, instanceStream);
    }

    protected ObjectParameter<T> create(ParameterData<T> parameterData, boolean registerParameter) {
        return new ObjectParameter<T>(parameterData, cls, registerParameter);
    }

    /** {@inheritDoc} */
    public T getValue(Object dbValue) {
        return dbValue != null
                ? cls.cast(
                (cls != Object.class && cls.isAssignableFrom(dbValue.getClass()))
                        ? dbValue
                        : getXStream().fromXML(dbValue.toString()
                ))
                : null;
    }

    /** {@inheritDoc} */
    @Override
    public String getDatabaseValue(T value) {
        // add a bit of type safety here
        if (!cls.isAssignableFrom(value.getClass())) {
            throw new ClassCastException("Invalid class for configuration parameter (" + value.getClass() 
                    + ", expected: " + cls + ")");
        }
        return FxXMLUtils.toXML(getXStream(), value);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValid(T value) {
        return value != null;
    }

    private XStream getXStream() {
        return instanceStream != null ? instanceStream : xStream;
    }

    public static XStream getDefaultXStream() {
    	return xStream;
    }

    void setXStream(XStream instance) {
        this.instanceStream = instance;
    }
}
