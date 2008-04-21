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
package com.flexive.faces.components.input;

import com.flexive.faces.FxJsfComponentUtils;
import com.flexive.shared.value.FxValue;
import com.flexive.shared.value.mapper.IdentityInputMapper;
import com.flexive.shared.value.mapper.InputMapper;
import com.flexive.shared.value.renderer.FxValueFormatter;

import javax.faces.component.UIInput;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;

/**
 * Input fields for FxValue variables, including multi-language support.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxValueInput extends UIInput {
    /**
     * The JSF component type for a FxValue component.
     */
    public static final String COMPONENT_TYPE = "flexive.FxValueInput";

    private static ThreadLocal<Integer> idCounter = new ThreadLocal<Integer>();

    private Boolean disableMultiLanguage;
    private Long externalId;   // an optional external id for identifying this input (ignoring the clientId)
    private Boolean readOnly;
    private Boolean forceLineInput;
    private Boolean filter;
    private InputMapper inputMapper;
    private String onchange;
    private FxValueFormatter valueFormatter;
    private Boolean disableLytebox;

    private int configurationMask = -1;

    /**
     * Default constructor
     */
    public FxValueInput() {
        setRendererType("flexive.FxValueInput");
    }

    @Override
    public void validate(FacesContext context) {
        // automatically register a FxValueInputValidator, unless it is already present
        boolean validatorFound = false;
        for (Validator validator: getValidators()) {
            if (validator instanceof FxValueInputValidator) {
                validatorFound = true;
                break;
            }
        }
        if (!validatorFound) {
            addValidator(new FxValueInputValidator());
        }
        FxValueInputRenderer.buildComponent(context, this);
        super.validate(context);
    }

    /**
     * Disables multi language support even if the FxValue
     * object is multilingual (e.g. for search query editors).
     *
     * @return if multi language support should be disabled
     */
    public boolean isDisableMultiLanguage() {
        if (disableMultiLanguage == null) {
            return FxJsfComponentUtils.getBooleanValue(this, "disableMultiLanguage", false);
        }
        return disableMultiLanguage;
    }

    public void setDisableMultiLanguage(boolean disableMultiLanguage) {
        this.disableMultiLanguage = disableMultiLanguage;
    }

    /**
     * Sets the component to read-only mode. In read-only mode, the embedded FxValue
     * is written directly to the response.
     *
     * @return true if read-only mode is enabled
     */
    public boolean isReadOnly() {
        if (readOnly == null) {
            return FxJsfComponentUtils.getBooleanValue(this, "readOnly", false);
        }
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Return an arbitrary external ID assigned to this input component. This field
     * can be used for identifying a component independent of its client ID.
     * If the external ID is set (i.e. != -1), it replaces the clientId in the
     * {@link com.flexive.faces.messages.FxFacesMessage#getId()} property when
     * a validation error message is created.
     *
     * @return the external ID assigned to this component, or -1 if there is none
     */
    public long getExternalId() {
        if (externalId == null) {
            externalId = FxJsfComponentUtils.getLongValue(this, "externalId");
        }
        return externalId == null ? -1 : externalId;
    }

    /**
     * Set an external ID used for identifying this component.
     *
     * @param externalId an external ID
     * @see #getExternalId()
     */
    public void setExternalId(long externalId) {
        this.externalId = externalId;
    }

    /**
     * Return true if the input field must be rendered in a single line. For example,
     * HTML editors are disabled by this setting.
     *
     * @return true if the input field must be rendered in a single line.
     */
    public boolean isForceLineInput() {
        if (forceLineInput == null) {
            return FxJsfComponentUtils.getBooleanValue(this, "forceLineInput", false);
        }
        return forceLineInput;
    }

    /**
     * Force single-line rendering for the input component. For example,
     * HTML editors are disabled by this setting.
     *
     * @param forceLineInput true if the component must be rendered in a single line
     */
    public void setForceLineInput(boolean forceLineInput) {
        this.forceLineInput = forceLineInput;
    }

    /**
     * Return true if the output filter is enabled (usually only applies
     * to the component in read-only mode). If enabled, text will be filtered
     * and HTML entities will be used for sensitive characters (e.g. "&amp;gt;" instead of "&gt;").
     *
     * @return true if the output filter is enabled
     */
    public boolean isFilter() {
        if (filter == null) {
            return FxJsfComponentUtils.getBooleanValue(this, "filter", true);
        }
        return filter;
    }

    public void setFilter(boolean filter) {
        this.filter = filter;
    }

    /**
     * Returns true if the <a href="http://www.dolem.com/lytebox/">Lytebox</a> javascript library
     * used for inline previews of images should not be used.
     *
     * @return  true if Lytebox is disabled
     */
    public boolean isDisableLytebox() {
        if (disableLytebox == null) {
            return FxJsfComponentUtils.getBooleanValue(this, "disableLytebox", false);
        }
        return disableLytebox;
    }

    public void setDisableLytebox(boolean disableLytebox) {
        this.disableLytebox = disableLytebox;
    }

    /**
     * Return the input mapper to be used. If no input mapper was defined for the component,
     * the identity mapper is used.
     *
     * @return the input mapper
     */
    public InputMapper getInputMapper() {
        if (inputMapper == null) {
            inputMapper = (InputMapper) FxJsfComponentUtils.getValue(this, "inputMapper");
            if (inputMapper == null) {
                inputMapper = new IdentityInputMapper();    // use dummy mapper
            }
        }
        return inputMapper;
    }

    public void setInputMapper(InputMapper inputMapper) {
        this.inputMapper = inputMapper;
    }

    /**
     * Return the (optional) onchange javascript to be called when an
     * input element changed its value.
     *
     * @return the (optional) onchange javascript to be called if an input element changed its value.
     */
    public String getOnchange() {
        if (onchange == null) {
            onchange = FxJsfComponentUtils.getStringValue(this, "onchange");
        }
        return onchange;
    }

    public void setOnchange(String onchange) {
        this.onchange = onchange;
    }


    /**
     * Return the (optional) {@link FxValueFormatter} to be used for formatting the output
     * in read-only mode.
     *
     * @return the (optional) {@link FxValueFormatter} to be used for formatting the output
     *         in read-only mode.
     */
    public FxValueFormatter getValueFormatter() {
        if (valueFormatter == null) {
            valueFormatter = (FxValueFormatter) FxJsfComponentUtils.getValue(this, "valueFormatter");
        }
        return valueFormatter;
    }

    public void setValueFormatter(FxValueFormatter valueFormatter) {
        this.valueFormatter = valueFormatter;
    }

    @Override
    public String getClientId(FacesContext facesContext) {
        if (getId() != null && getId().startsWith(UIViewRoot.UNIQUE_ID_PREFIX)) {
            // use autogenerated ID - don't use JSF's sequencers since we need the id to be rendered to the client
            if (idCounter.get() == null) {
                idCounter.set(0);
            }
            setId("fvi" + idCounter.get());
            idCounter.set(idCounter.get() + 1);
        }
        return super.getClientId(facesContext);
    }

    /**
     * Return the input's current value and include the specified inputMapper, if present.
     * If an input mapper was used, this component's value is mapped and returned. Otherwise,
     * the component's current value is returned.
     *
     * @return the current value as rendered in the UI
     */
    public FxValue getUIValue() {
        if (getInputMapper() != null) {
            //noinspection unchecked
            return getInputMapper().encode((FxValue) getValue());
        } else {
            return (FxValue) getValue();
        }
    }

    /**
     * Returns the configuration bitmask of the current component.
     *
     * @return  the configuration bitmask of the current component.
     * @see #calcConfigurationMask()
     */
    public int getConfigurationMask() {
        return configurationMask;
    }

    /**
     * Calculates a bitmask for the current component configuration (i.e. read-only mode,
     * line input mode). Used to detect configuration changes between page views by the
     * {@link com.flexive.faces.components.input.FxValueInputRenderer}.
     *
     * @return  the bitmask of the current component configuration
     */
    public int calcConfigurationMask() {
        return (isReadOnly() ? 1 : 0)
                + ((isForceLineInput() ? 1 : 0) << 2)
                + ((isFilter() ? 1 : 0) << 4);
    }

    /**
     * Resets the configuration bitmask.
     * 
     * @see #calcConfigurationMask()
     */
    public void resetConfigurationMask() {
        this.configurationMask = calcConfigurationMask();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        int index = 0;
        super.restoreState(context, values[index++]);
        this.disableMultiLanguage = (Boolean) values[index++];
        this.externalId = (Long) values[index++];
        this.readOnly = (Boolean) values[index++];
        this.forceLineInput = (Boolean) values[index++];
        this.onchange = (String) values[index++];
        this.filter = (Boolean) values[index++];
        //noinspection UnusedAssignment
        this.configurationMask = (Integer) values[index++];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[8];
        int index = 0;
        values[index++] = super.saveState(context);
        values[index++] = disableMultiLanguage;
        values[index++] = externalId;
        values[index++] = readOnly;
        values[index++] = forceLineInput;
        values[index++] = onchange;
        values[index++] = filter;
        //noinspection UnusedAssignment
        values[index++] = configurationMask >= 0 ? configurationMask : calcConfigurationMask();
        return values;
	}
	
	
}
