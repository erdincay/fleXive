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
package com.flexive.shared.configuration.parameters;

import com.flexive.shared.configuration.Parameter;
import com.flexive.shared.configuration.ParameterData;

/**
 * Boolean parameters for flexive.
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
class BooleanParameter extends ParameterImpl<Boolean> {
    private static final long serialVersionUID = -7485943734106339003L;

    public BooleanParameter() {
        this(null, false);
    }

    /**
     * Creates a new boolean parameter definition.
     * @param parameter parameter data
     * @param registerParameter if the parameter should be registered in the static parameter table
     *  (don't do this for non-static parameter declarations)
     */
    public BooleanParameter(ParameterData<Boolean> parameter, boolean registerParameter) {
		super(parameter, registerParameter);
	}

    /** {@inheritDoc} */
    public Parameter<Boolean> copy() {
        return new BooleanParameter(getData(), false);
    }

    /** {@inheritDoc} */
	public Boolean getValue(Object dbValue) {
		return dbValue != null ? Boolean.valueOf(dbValue.toString()) : null;
	}
    
    /** {@inheritDoc} */
    @Override
    public boolean isValid(Boolean value) {
        return value != null;
    }
}
