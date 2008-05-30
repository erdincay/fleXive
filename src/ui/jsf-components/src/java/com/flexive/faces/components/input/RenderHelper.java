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
package com.flexive.faces.components.input;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.components.WriteWebletIncludes;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.value.BinaryDescriptor;
import com.flexive.shared.value.FxBinary;
import com.flexive.shared.value.FxValue;
import org.apache.commons.lang.StringUtils;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.component.html.HtmlOutputLink;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import java.io.IOException;

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
     * @param parent   the parent component
     * @param inputId  the input element ID (= form name)
     * @param language the language for the input field, or null if the value is not multi-language. @throws IOException if the input could not be rendered
     * @throws java.io.IOException on errors
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
            for (String weblet : weblets) {
                out.write(StringUtils.defaultString(WriteWebletIncludes.getWebletInclude(weblet)));
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

    /**
     * Renders the container for the whole fxValueInput component.
     */
    public static class ContainerWriter extends DeferredInputWriter {

        boolean displayLanguage = false;
        FxLanguage language = null;

        public void setDisplayLanguage(boolean displayLanguage) {
            this.displayLanguage = displayLanguage;
        }

        public void setLanguage(FxLanguage language) {
            this.language = language;
        }

        @Override
        public void encodeBegin(FacesContext facesContext) throws IOException {
            final ResponseWriter writer = facesContext.getResponseWriter();
            StringBuilder styleClass = new StringBuilder(300);
            styleClass.append(FxValueInputRenderer.CSS_CONTAINER).append(" ").append(getInputValue().getClass().getSimpleName()).
                    append("Input");
            if( displayLanguage && language != null )  {
                writer.writeText(language.getLabel().getBestTranslation()+":", null);
                styleClass.insert(0, FxValueInputRenderer.CSS_READONLYCONTAINER+" ");
            }
            writer.startElement("div", null);
            writer.writeAttribute("id", inputClientId, null);
            writer.writeAttribute("class", styleClass.toString(), null);
        }

        @Override
        public void encodeEnd(FacesContext facesContext) throws IOException {
            facesContext.getResponseWriter().endElement("div");
        }
    }

    /**
     * Render an image description and download link
     */
    public static class ImageDescription extends UIOutput {
        private FxLanguage language = FxLanguage.DEFAULT;


        public void setLanguage(FxLanguage language) {
            this.language = language;
        }

        protected FxValueInput getInputComponent() {
            UIComponent component = getParent();
            while (component != null && !(component instanceof FxValueInput)) {
                component = component.getParent();
            }
            return (FxValueInput) component;
        }

        public void encodeEnd(FacesContext facesContext) throws IOException {
            final FxValueInput input = getInputComponent();
            FxValue value = input.getUIValue();
            if (value == null)
                return;
            if (value instanceof FxBinary && !value.isEmpty()) {
                final ResponseWriter writer = facesContext.getResponseWriter();
                final BinaryDescriptor descriptor = ((FxBinary) value).getTranslation(language);
                writer.startElement("br", null);
                writer.writeAttribute("clear", "all", null);
                writer.endElement("br");

                writer.startElement("span", null);
                writer.writeAttribute("class", "binaryDescription", null);
                StringBuilder sb = new StringBuilder(500);
                if (input.isReadOnly() && input.isReadOnlyShowTranslations() && value.isMultiLanguage())
                    sb.append(language.getLabel().getBestTranslation()).append(": ");
                sb.append(descriptor.getName()).append(", ").append(descriptor.getSize()).append(" byte");
                if (descriptor.isImage())
                    sb.append(",").append(descriptor.getWidth()).append("x").append(descriptor.getHeight()).append(" pixel");
                writer.writeText(sb.toString(), null);
                writer.endElement("span");

                if (!descriptor.isNewBinary()) {
                    final HtmlOutputLink link = (HtmlOutputLink) FxJsfUtils.createComponent(HtmlOutputLink.COMPONENT_TYPE);
                    final String downloadURL = FxJsfUtils.getServletContext().getContextPath() +
                            "/cefiledownload/" +
                            (value.isMultiLanguage() ? "lang:" + language.getIso2digit() : "") +
                            "/xpath:" + value.getXPath().replaceAll("\\/", "|") + "/" + descriptor.getName();
                    link.setValue(downloadURL);

                    final HtmlGraphicImage image = (HtmlGraphicImage) FxJsfUtils.addChildComponent(link, HtmlGraphicImage.COMPONENT_TYPE);
                    image.setUrl("/adm/images/contentEditor/download.png");
                    image.setStyleClass("binaryDownloadIcon");
                    link.encodeAll(facesContext);
                }

                writer.startElement("br", null);
                writer.writeAttribute("clear", "all", null);
                writer.endElement("br");
            }
        }
    }

    protected UIComponent addImageDescriptionComponent(UIComponent parent, FxLanguage language) {
        RenderHelper.ImageDescription desc = new ImageDescription();
        desc.setLanguage(language);
        parent.getChildren().add(desc);
        return desc;
    }
}
