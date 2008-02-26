/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
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
package com.flexive.faces.components.input;

import com.flexive.shared.FxLanguage;
import com.flexive.shared.value.FxValue;
import com.flexive.faces.components.WriteWebletIncludes;

import javax.faces.context.ResponseWriter;
import javax.faces.context.FacesContext;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;

/**
 * Renders the given FxValue.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
abstract class RenderHelper {

    protected final FxValueInput component;
    protected final String clientId;
    protected final FxValue value;

    /**
     * @param clientId  the component's client ID
     * @param value     the initial input value
     * @param component the input component
     */
    protected RenderHelper(FxValueInput component, String clientId, FxValue value) {
        this.component = component;
        this.clientId = clientId;
        this.value = value;
    }

    /**
     * Render the input fields for the given value/type.
     *
     * @throws java.io.IOException if an output error occured
     */
    protected void render() throws IOException {
        if (value.isMultiLanguage() && !component.isDisableMultiLanguage()) {
            encodeMultiLanguageField();
        } else {
            encodeField(component, clientId + FxValueInputRenderer.INPUT, null);
        }
    }

    /**
     * Render a multi-language input field for the current value.
     *
     * @throws IOException if the input could not be rendered
     */
    protected abstract void encodeMultiLanguageField() throws IOException;

    /**
     * Render a input field for the given language.
     *
     * @param parent  the parent component
     * @param inputId  the input element ID (= form name)
     * @param language the language for the input field, or null if the value is not multi-language. @throws IOException if the input could not be rendered
     */
    protected abstract void encodeField(UIComponent parent, String inputId, FxLanguage language) throws IOException;

    /**
     * The base class of all primitive input renderer components.
     * These inputs are not composed of more complex input elements (like date pickers),
     * but render a single HTML form element with the given
     * {@link EditModeHelper.DeferredInputWriter#inputClientId}
     * that will be decoded by the
     * {@link FxValueInputRenderer}.
     */
    public static abstract class DeferredInputWriter extends UIOutput {
        protected String inputClientId;

        public String getInputClientId() {
            return inputClientId;
        }

        public void setInputClientId(String inputClientId) {
            this.inputClientId = inputClientId;
        }

        protected FxValue getInputValue() {
            final FxValueInput input = getInputComponent();
            if (input != null) {
                return input.getSubmittedValue() != null ? (FxValue) input.getSubmittedValue() : (FxValue) input.getValue();
            }
            throw new IllegalStateException("No enclosing fx:fxValueInput component found");
        }

        protected FxValueInput getInputComponent() {
            UIComponent component = getParent();
            while (component != null && !(component instanceof FxValueInput)) {
                component = component.getParent();
            }
            return (FxValueInput) component;
        }

        @Override
        public void restoreState(FacesContext context, Object stateValue) {
            final Object[] state = (Object[]) stateValue;
            super.restoreState(context, state[0]);
            this.inputClientId = (String) state[1];
        }

        @Override
        public Object saveState(FacesContext context) {
            final Object[] state = new Object[2];
            state[0] = super.saveState(context);
            state[1] = inputClientId;
            return state;
        }
    }

    public static class WebletIncludeWriter extends UIOutput {
        private String[] weblets;

        public void setWeblets(String... weblets) {
            this.weblets = weblets;
        }

        @Override
        public void encodeBegin(FacesContext facesContext) throws IOException {
            if (weblets == null) {
                return;
            }
            final ResponseWriter out = facesContext.getResponseWriter();
            for (String weblet: weblets) {
                out.write(StringUtils.defaultString(WriteWebletIncludes.renderWeblet(weblet)));
            }
        }

        @Override
        public Object saveState(FacesContext context) {
            final Object[] state = new Object[2];
            state[0] = super.saveState(context);
            state[1] = weblets;
            return state;
        }

        @Override
        public void restoreState(FacesContext context, Object stateValue) {
            final Object[] state = (Object[]) stateValue;
            super.restoreState(context, state[0]);
            weblets = (String[]) state[1];
        }
    }
}
