/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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
package com.flexive.faces.beans;

import com.flexive.shared.configuration.ParameterMap;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.configuration.Parameter;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxApplicationException;

import java.io.Serializable;

/**
 * Provides access to miscellaneous user configuration parameters.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class UserConfigurationBean {
    protected final ParameterMap cachedParameters = new ParameterMap();
    private long inputLanguageId = -1;

    public long getDefaultInputLanguageId() {
        return getParameter(SystemParameters.USER_DEFAULTINPUTLANGUAGE);
    }

    /**
     * A writable property that stores the (global) input language ID for the current page.
     *
     * @return  the current input language ID
     */
    public long getInputLanguageId() {
        if (inputLanguageId == -1) {
            final long defaultLanguageId = getDefaultInputLanguageId();
            return defaultLanguageId != -1 ? defaultLanguageId : FxContext.get().getTicket().getLanguage().getId();
        }
        return inputLanguageId;
    }

    /**
     * A writable property that stores the (global) input language ID for the current page.
     *
     * @param inputLanguageId   the language ID
     */
    public void setInputLanguageId(long inputLanguageId) {
        this.inputLanguageId = inputLanguageId;
    }

    /**
     * Returns the given user parameter and stores in the backing bean's cache to avoid
     * multiple EJB calls during a request.
     *
     * @param parameter the parameter value
     * @return  the parameter value
     */
    private <T extends Serializable> T getParameter(Parameter<T> parameter) {
        T value = cachedParameters.get(parameter);
        if (value == null) {
            try {
                value = EJBLookup.getConfigurationEngine().get(parameter);
                cachedParameters.put(parameter, value);
            } catch (FxApplicationException e) {
                throw e.asRuntimeException();
            }
        }
        return value;
    }
}
