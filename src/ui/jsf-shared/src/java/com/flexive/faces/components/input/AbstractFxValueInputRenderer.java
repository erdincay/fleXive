/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
package com.flexive.faces.components.input;

import com.flexive.shared.CacheAdmin;
import static com.flexive.shared.FxFormatUtils.toDate;
import static com.flexive.shared.FxFormatUtils.toDateTime;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.security.ACL;
import com.flexive.shared.value.*;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.render.Renderer;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Base renderer class for the FxValueInput component.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @param <T>   concrete (base) type supported for rendering
 */
public abstract class AbstractFxValueInputRenderer<T extends AbstractFxValueInput> extends Renderer {
    private static final Log LOG = LogFactory.getLog(AbstractFxValueInputRenderer.class);

    protected static final String LANG_CONTAINER = "_language_";
    protected static final String DEFAULT_LANGUAGE = "_defaultLanguage";
    protected static final String LANG_SELECT = "_languageSelect";
    protected static final String INPUT = "_input_";
    protected static final String CSS_CONTAINER = "fxValueInput";
    protected static final String CSS_READONLYCONTAINER = "fxValueInputReadOnly";
    protected static final String CSS_LANG_CONTAINER = "fxValueInputRow";
    protected static final String CSS_LANG_CONTAINER_FIRST = "firstRow";
    protected static final String CSS_VALUE_INPUT_FIELD = "fxValueField";   // marker class for actual data input fields exposed to the user 
    protected static final String CSS_TEXT_INPUT = "fxValueTextInput";
    protected static final String CSS_TEXTAREA = "fxValueTextArea";
    protected static final String CSS_TEXTAREA_HTML = "fxValueTextAreaHtml";
    protected static final String CSS_TEXTAREA_HTML_OUTER = "fxValueHtmlEditorContainer";
    protected static final String CSS_TEXTAREA_OUTER = "fxValueTextAreaContainer";
    protected static final String CSS_INPUTELEMENTWIDTH = "fxValueInputElementWidth";
    protected static final String CSS_LANG_ICON = "fxValueInputLanguageIcon";
    protected static final String CSS_SINGLE_LANG = "singleLanguage";
    protected static final String CSS_FIND_REFERENCES = "findReferencesIcon";
    protected static final String CSS_RESIZEABLE = "fxResizeable";
    protected static final String CSS_EMPTY = "fxValueEmpty";
    private static final String DBG = "fxValueInput: ";

    /**
     * {@inheritDoc}
     */
    @Override
    public void encodeBegin(FacesContext context, UIComponent input) throws IOException {
//        if (component.calcConfigurationMask() != component.getConfigurationMask()) {
        buildComponent(context, (T) input);
//        }
    }

    /**
     * Return the {@link RenderHelper} for rendering this component.
     *
     * @param context   the Faces context
     * @param component the component to be rendered
     * @param value     the value to be rendered
     * @param editMode  if the component should be rendered in edit mode
     * @return          a RenderHelper instance
     */
    public abstract RenderHelper getRenderHelper(FacesContext context, T component, FxValue value, boolean editMode);

    public void buildComponent(FacesContext context, T component) {

        final String clientId = component.getClientId(context);
        //noinspection unchecked
        final FxValue value = component.isReadOnly()
                ? getFxValue(context, component)
                : component.getInputMapper().encode(getFxValue(context, component));

        RenderHelper helper = getRenderHelper(context, component, value, !(component.isReadOnly() || value.isReadOnly()));

        /*if (!component.getChildren().isEmpty()) {
            LOG.warn(DBG + "Component " + clientId + " already has " + component.getChildren().size()
                    + " children which will be discarded. Please don't use fx:fxValueInput inside render-time tags like ui:repeat.");
        }*/
        component.getChildren().clear();
        if (!(value instanceof FxVoid)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(DBG + "Rendering " + (component.isReadOnly() ? "read only" : "editable")
                        + " component " + clientId + " for value=" + value);
            }
            try {
                helper.render();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        component.resetConfigurationMask();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decode(FacesContext context, UIComponent component) {
        T input = (T) component;
        buildComponent(context, input);
        if (!(input.getValue() instanceof FxVoid) && !input.isReadOnly()
                && !(input.getValue() != null && ((FxValue) input.getValue()).isReadOnly())) {
            input.setSubmittedValue(decodeFxValue(context, component));
        }
    }

    /**
     * Return the FxValue used as input for the component.
     *
     * @param context the faces context
     * @param input   the FxValueInput component @return the FxValue used as input for the component.
     * @return the FxValue stored in the input component
     */
    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public static FxValue getFxValue(FacesContext context, UIInput input) {
        Object o = input.getSubmittedValue() != null ? input.getSubmittedValue() : input.getValue();
        if (o == null) {
            throw new FxInvalidParameterException("VALUE", "ex.jsf.valueInput.null", input.getClientId(context)).asRuntimeException();
        } else if (!(o instanceof FxValue)) {
            throw new FxInvalidParameterException("VALUE", "ex.jsf.valueInput.invalidType",
                    o.getClass().getCanonicalName()).asRuntimeException();
        }
        return (FxValue) o;
    }

    /**
     * Process the result of a file upload.
     * 
     * @param context   the faces context
     * @param input     the input component
     * @param inputId   the input ID to be decoded
     * @param value     the binary value container
     * @param languageId    the language to be updated
     */
    protected abstract void processBinary(FacesContext context,AbstractFxValueInput input, String inputId, FxBinary value, long languageId);

    /**
     * Decode FxValue items posted for this component.
     *
     * @param context   the current faces context
     * @param component the component to be rendered
     * @return FxValue items posted for this component.
     */
    private FxValue decodeFxValue(FacesContext context, UIComponent component) {
        final AbstractFxValueInput input = (AbstractFxValueInput) component;
        final Map parameters = context.getExternalContext().getRequestParameterMap();
        final Map parameterValues = context.getExternalContext().getRequestParameterValuesMap();
        final String clientId = component.getClientId(context);
        @SuppressWarnings({"unchecked"})
        final FxValue value = input.getInputMapper().encode(getFxValue(context, input).copy());

        if (LOG.isDebugEnabled()) {
            LOG.debug(DBG + "Decoding value for " + clientId + ", component value=" + value);
        }
        if (value.isMultiLanguage() && !input.isDisableMultiLanguage()) {
            // set the FxValue default language
            final String defaultLanguage = (String) parameters.get(clientId + DEFAULT_LANGUAGE);
            if (isNotBlank(defaultLanguage)) {
                final int defaultLanguageId = Integer.parseInt(defaultLanguage);
                value.setDefaultLanguage(defaultLanguageId, true);
            } else {
                value.clearDefaultLanguage();
            }
            // update all languages
            for (FxLanguage language : getLanguages()) {
                final String inputId = clientId + INPUT + language.getId();
                updateTranslation(context, input, value, inputId, language.getId(), parameters, parameterValues);
            }
        } else {
            updateTranslation(context, input, value, clientId + AbstractFxValueInputRenderer.INPUT, value.getDefaultLanguage(), parameters, parameterValues);
        }
        @SuppressWarnings({"unchecked"})
        final FxValue mappedValue = input.getInputMapper().decode(value);
        if (LOG.isDebugEnabled()) {
            LOG.debug(DBG + "Decoded value for " + clientId + ": " + value + ", mapped value: " + mappedValue);
        }
        return mappedValue;
    }

    @SuppressWarnings({"unchecked"})
    private void updateTranslation(FacesContext context, AbstractFxValueInput input, FxValue value, String inputId, long languageId, Map parameters, Map parameterValues) {
        if (value instanceof FxSelectMany) {
            final String selectedOptions = StringUtils.join((String[]) parameterValues.get(inputId), ',');
            if (isNotBlank(selectedOptions)) {
                // update selection
                value.setTranslation(languageId, selectedOptions);
            } else if (value.getTranslation(languageId) != null) {
                // remove all selected items
                ((FxSelectMany) value).getTranslation(languageId).deselectAll();
            }
        } else if (value instanceof FxBinary) {
            processBinary(context, input, inputId, (FxBinary) value, languageId);
        } else if (value instanceof FxBoolean) {
            if ("true".equals(parameters.get(inputId + "_empty"))) {
                // empty flag of tristate checkbox set
                value.removeLanguage(languageId);
            } else {
                value.setTranslation(languageId, isNotBlank((String) parameters.get(inputId)));
            }
        } else if (value instanceof FxDateRange || value instanceof FxDateTimeRange) {
            final boolean withTime = value instanceof FxDateTimeRange;
            final String postedLower = (String) parameters.get(inputId + "_1");
            final String postedUpper = (String) parameters.get(inputId + "_2");
            if (isBlank(postedLower) || isBlank(postedUpper)) {
                value.removeLanguage(languageId);
            } else {
                value.setTranslation(languageId, new DateRange(
                        withTime ? addTimeFields(parameters, inputId + "_1", toDateTime(postedLower)) : toDate(postedLower),
                        withTime ? addTimeFields(parameters, inputId + "_2", toDateTime(postedUpper)) : toDate(postedUpper))
                );
            }
        } else {
            // use FxValue string representation
            final String postedValue = (String) parameters.get(inputId);
            
            if (isBlank(postedValue) || (value instanceof FxSelectOne && "-1".equals(postedValue))) {
                value.removeLanguage(languageId);
            } else if (isNotBlank(postedValue)) {
                value.setTranslation(languageId, postedValue);
                if (value instanceof FxReference && value.isValid()) {
                    // store translation
                    final ReferencedContent reference = ((FxReference) value).getTranslation(languageId);
                    value.setTranslation(languageId, new ReferencedContent(reference, (String) parameters.get(inputId + "_caption"), null, Lists.<ACL>newArrayList()));
                }
            } 

            if (value instanceof FxDateTime && !value.isTranslationEmpty(languageId) && value.isValid()) {
                value.setTranslation(languageId, addTimeFields(parameters, inputId, ((FxDateTime) value).getTranslation(languageId)));
            }
        }
    }

    private Date addTimeFields(Map parameters, String inputId, Date date) {
        // get additional time information
        final String hours = (String) parameters.get(inputId + "_hh");
        final String minutes = (String) parameters.get(inputId + "_mm");
        final String seconds = (String) parameters.get(inputId + "_ss");
        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        try {
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hours));
            cal.set(Calendar.MINUTE, Integer.parseInt(minutes));
            cal.set(Calendar.SECOND, Integer.parseInt(seconds));
        } catch (NumberFormatException e) {
            // ignore invalid time
        }
        return cal.getTime();
    }

    /**
     * Load all available languages.
     *
     * @return all available languages.
     */
    protected static List<FxLanguage> getLanguages() {
        return CacheAdmin.getEnvironment().getLanguages();
    }

    /**
     * Strips all naming containers from our client ID (e.g. forms, iterator components).
     * This is usful for finding components by id within our FxValueInput component.
     *
     * @param clientId the client ID
     * @return the client ID without naming containers
     */
    protected static String stripNamingContainers(String clientId) {
        return clientId.indexOf(NamingContainer.SEPARATOR_CHAR) != -1
                ? clientId.substring(clientId.lastIndexOf(NamingContainer.SEPARATOR_CHAR) + 1) : clientId;
    }
}
