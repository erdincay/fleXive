package com.flexive.faces.beans;

import com.flexive.shared.configuration.ParameterMap;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.configuration.Parameter;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;

import java.io.Serializable;

/**
 * Provides access to miscellaneous user configuration parameters.
 *
 * @author Daniel Lichtenberger, UCS
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
            return getDefaultInputLanguageId();
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
