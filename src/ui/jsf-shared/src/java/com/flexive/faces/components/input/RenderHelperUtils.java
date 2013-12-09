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
package com.flexive.faces.components.input;

import com.flexive.faces.FxJsfUtils;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.value.BinaryDescriptor;
import com.flexive.shared.value.FxBinary;
import com.flexive.shared.value.FxValue;
import com.flexive.war.servlet.DownloadServlet;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.component.html.HtmlOutputLink;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import java.io.IOException;


/**
 * Shared utility classes and methods for the JSF1 and JSF2 implementations of {@link RenderHelper}.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class RenderHelperUtils {

    private RenderHelperUtils() {
    }

    /**
     * Default implementation of {@link RenderHelper#render()}.
     *
     * @param renderHelper  the render helper instance
     * @param component     the input component
     * @param clientId      the desired clientId
     * @param value         the value to be rendered
     * @throws IOException  on I/O errors
     */
    public static void render(RenderHelper renderHelper, AbstractFxValueInput component, String clientId, FxValue value) throws IOException {
        if (value.isMultiLanguage() && !component.isDisableMultiLanguage()) {
            renderHelper.encodeMultiLanguageField();
        } else {
            renderHelper.encodeField(component, clientId + AbstractFxValueInputRenderer.INPUT, null);
        }
    }

    public static FxValue getInputValue(AbstractFxValueInput input) {
        if (input != null) {
            return input.getSubmittedValue() != null ? (FxValue) input.getSubmittedValue() : (FxValue) input.getValue();
        }
        throw new IllegalStateException("No enclosing fx:fxValueInput component found");
    }

    public static String stripForm(String inputId) {
        return inputId.substring(inputId.lastIndexOf(':') + 1);
    }

    public static String getForm(String inputId) {
        return inputId.substring(0, inputId.indexOf(':'));
    }

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

        @Override
        public boolean getRendersChildren() {
            return false;
        }

        @Override
        public boolean isTransient() {
            return true;
        }

        public String getInputClientId() {
            return inputClientId;
        }

        public void setInputClientId(String inputClientId) {
            this.inputClientId = inputClientId;
        }

        protected FxValue getInputValue() {
            return RenderHelperUtils.getInputValue(getInputComponent());
        }

        protected AbstractFxValueInput getInputComponent() {
            UIComponent component = getParent();
            while (component != null && !(component instanceof AbstractFxValueInput)) {
                component = component.getParent();
            }
            return (AbstractFxValueInput) component;
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


    /**
     * Renders the container for the whole fxValueInput component.
     */
    public static class ContainerWriter extends DeferredInputWriter {

        private boolean displayLanguage;
        private FxLanguage language;
        private boolean multiLanguage;

        public void setDisplayLanguage(boolean displayLanguage) {
            this.displayLanguage = displayLanguage;
        }

        public void setLanguage(FxLanguage language) {
            this.language = language;
        }

        public void setMultiLanguage(boolean multiLanguage) {
            this.multiLanguage = multiLanguage;
        }

        @Override
        public void encodeBegin(FacesContext facesContext) throws IOException {
            final ResponseWriter writer = facesContext.getResponseWriter();
            StringBuilder styleClass = new StringBuilder(300);
            styleClass.append(getInputValue().getClass().getSimpleName()).append("Input ")
                    .append(AbstractFxValueInputRenderer.CSS_CONTAINER);
            if (multiLanguage) {
                styleClass.append(' ').append(AbstractFxValueInputRenderer.CSS_MULTILANG);
            }
            if( displayLanguage && language != null )  {
                writer.writeText(language.getLabel().getBestTranslation()+":", null);
                styleClass.insert(0, AbstractFxValueInputRenderer.CSS_READONLYCONTAINER+" ");
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
        private boolean downloadLink = true;
        private String downloadServletPath;

        public void setDownloadLink(boolean downloadLink) {
            this.downloadLink = downloadLink;
        }

        public void setLanguage(FxLanguage language) {
            this.language = language;
        }

        public void setDownloadServletPath(String downloadServletPath) {
            this.downloadServletPath=downloadServletPath;
        }

        protected AbstractFxValueInput getInputComponent() {
            UIComponent component = getParent();
            while (component != null && !(component instanceof AbstractFxValueInput)) {
                component = component.getParent();
            }
            return (AbstractFxValueInput) component;
        }

        @Override
        public void encodeEnd(FacesContext facesContext) throws IOException {
            final AbstractFxValueInput input = getInputComponent();
            FxValue value = input.getUIValue();
            if (value == null)
                return;
            if (value instanceof FxBinary && !value.isEmpty()) {
                final ResponseWriter writer = facesContext.getResponseWriter();
                final BinaryDescriptor descriptor = ((FxBinary) value).getTranslation(language);

                if (!descriptor.isNewBinary() && !"unknown".equals(descriptor.getMd5sum())) {
                    writer.startElement("br", null);
                    writer.endElement("br");
                    writer.startElement("span", null);
                    writer.writeAttribute("class", "binaryDescription", null);
                    writer.writeText("MD5:" + descriptor.getMd5sum(), null);
                    writer.endElement("span");
                }

                writer.startElement("br", null);
                writer.endElement("br");

                writer.startElement("span", null);
                writer.writeAttribute("class", "binaryDescription", null);
                writer.writeText(getBinaryDescription(input, value, descriptor), null);
                writer.endElement("span");

                if (!descriptor.isNewBinary() && downloadLink) {
                    final HtmlOutputLink link = (HtmlOutputLink) FxJsfUtils.createComponent(HtmlOutputLink.COMPONENT_TYPE);
                    link.setId(facesContext.getViewRoot().createUniqueId());
                    final String downloadURL = DownloadServlet.getLink(downloadServletPath, value.getXPath(), descriptor.getName());
                    link.setValue(FxJsfUtils.getServletContext().getContextPath() +downloadURL);

                    final HtmlGraphicImage image = (HtmlGraphicImage) FxJsfUtils.addChildComponent(link, facesContext.getViewRoot().createUniqueId(), HtmlGraphicImage.COMPONENT_TYPE);
                    input.setPackagedImageUrl(image, "/images/download.png");
                    image.setStyleClass("binaryDownloadIcon");
                    link.encodeAll(facesContext);
                }

                writer.startElement("br", null);
                writer.endElement("br");
            }
        }

        /**
         * Build a binary description
         *
         * @param input      input component
         * @param value      FxValue
         * @param descriptor BinaryDescriptor
         * @return description
         */
        public String getBinaryDescription(AbstractFxValueInput input, FxValue value, BinaryDescriptor descriptor) {
            StringBuilder sb = new StringBuilder(500);
            if (input.isReadOnly() && input.isReadOnlyShowTranslations() && value.isMultiLanguage())
                sb.append(language.getLabel().getBestTranslation()).append(": ");
            sb.append(descriptor.getName()).append(", ").append(descriptor.getSize()).append(" byte");
            if (descriptor.isImage())
                sb.append(",").append(descriptor.getWidth()).append("x").append(descriptor.getHeight()).append(" pixel");
            return sb.toString();
        }
    }

    public static UIComponent addImageDescriptionComponent(AbstractFxValueInput component, UIComponent parent, FxLanguage language, String inputId) {
        ImageDescription desc = new ImageDescription();
        desc.setId(RenderHelperUtils.stripForm(inputId));
        desc.setLanguage(language);
        desc.setDownloadServletPath(component.getDownloadServletPath());
        parent.getChildren().add(desc);
        return desc;
    }


}
