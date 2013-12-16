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
import com.flexive.faces.components.input.RenderHelperUtils.ContainerWriter;
import com.flexive.shared.*;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.media.FxMediaSelector;
import com.flexive.shared.structure.FxProperty;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxStructureOption;
import com.flexive.shared.value.*;
import com.flexive.shared.value.renderer.FxValueRendererFactory;
import com.flexive.war.servlet.ThumbnailServlet;
import org.apache.commons.lang.StringUtils;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlGraphicImage;
import javax.faces.component.html.HtmlOutputLink;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlSelectBooleanCheckbox;
import java.io.IOException;

/**
 * Renders a FxValue component in read-only mode.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ReadOnlyModeHelper implements RenderHelper {
    protected final AbstractFxValueInput component;
    protected final String clientId;
    protected final FxValue value;

    public ReadOnlyModeHelper(AbstractFxValueInput component, String clientId, FxValue value) {
        this.component = component;
        this.clientId = clientId;
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render() throws IOException {
        RenderHelperUtils.render(this, component, clientId, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void encodeMultiLanguageField() throws IOException {
        if (component.isReadOnlyShowTranslations()) {
            //TODO: for now only binaries are implemented correctly
            for (FxLanguage language : AbstractFxValueInputRenderer.getLanguages()) {
                if (value.translationExists(language.getId())) {
                    encodeField(component, clientId, language);
                }
            }
        } else {
            encodeField(component, clientId, FxContext.getUserTicket().getLanguage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public void encodeField(UIComponent parent, String inputId, FxLanguage language) throws IOException {
        if (value == null || value.isEmpty()) {
            return;
        }
        // TODO: optional "empty" message

        boolean useHTMLEditor;
        if (value instanceof FxString && StringUtils.isNotBlank(value.getXPath())) {
            if (CacheAdmin.getEnvironment().assignmentExists(value.getXPath())) {
                FxPropertyAssignment pa = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(value.getXPath());
                useHTMLEditor = pa.getOption(FxStructureOption.OPTION_HTML_EDITOR).isValueTrue();
            } else if (CacheAdmin.getEnvironment().propertyExists(value.getXPath())) {
                FxProperty p = CacheAdmin.getEnvironment().getProperty(value.getXPath());
                useHTMLEditor = p.getOption(FxStructureOption.OPTION_HTML_EDITOR).isValueTrue();
            } else
                useHTMLEditor = value instanceof FxHTML; //fallback if no xpath is known, we assume FxHTML to be rendered with an HTML editor
        } else {
            useHTMLEditor = value instanceof FxHTML; //fallback if no xpath is known, we assume FxHTML to be rendered with an HTML editor
        }

        final FxLanguage outputLanguage = FxContext.getUserTicket().getLanguage();
        final String langInputId = inputId + (language != null ? "_" + language.getId() : "");
        String formattedValue = null;
        if (component.getValueFormatter() != null &&
                (formattedValue = component.getValueFormatter().format(value, value.getBestTranslation(language), outputLanguage)) != null) {
            // use custom formatter if retuned value != null, otherwise use default
            addOutputComponent(formattedValue, language, langInputId);
        } else if (value instanceof FxBinary && !value.isEmpty()) {
            // render preview image
            renderPreviewImage(language, langInputId);
        } else if (value instanceof FxReference && !value.isTranslationEmpty(language)) {
            // render reference preview
            final HtmlGraphicImage image = (HtmlGraphicImage) FxJsfUtils.addChildComponent(
                    parent, RenderHelperUtils.stripForm(langInputId) + "_image", HtmlGraphicImage.COMPONENT_TYPE, true
            );
            image.setUrl(ThumbnailServlet.getLink(
                    new FxMediaSelector(((FxReference) value).getDefaultTranslation()).
                            setSize(BinaryDescriptor.PreviewSizes.PREVIEW1).
                            setScaleHeight(16).
                            setScaleWidth(16)
            ));
            // render reference label
            addOutputComponent(FxValueRendererFactory.getInstance(outputLanguage).format(value, language), language, langInputId);
        } else if (value instanceof FxBoolean && !value.isTranslationEmpty(language)) {
            // render disabled checkbox
            final HtmlSelectBooleanCheckbox checkbox = (HtmlSelectBooleanCheckbox) FxJsfUtils.addChildComponent(
                    parent, RenderHelperUtils.stripForm(langInputId) + "_cb", HtmlSelectBooleanCheckbox.COMPONENT_TYPE, true
            );
            checkbox.setDisabled(true);
            checkbox.setReadonly(true);
            checkbox.setValue(value.getTranslation(language));
        } else if (component.isFilter() && !(useHTMLEditor || value instanceof FxHTML)) {
            // escape HTML code and generate <br/> tags for newlines
            addOutputComponent(FxFormatUtils.escapeForJavaScript(FxValueRendererFactory.getInstance(outputLanguage).format(value, language), true, true), language, langInputId);
        } else {
            // write the plain value
            addOutputComponent(FxValueRendererFactory.getInstance(outputLanguage).format(value, language), language, langInputId);
        }
    }

    protected ContainerWriter newContainerWriter() {
        return new RenderHelperUtils.ContainerWriter();
    }


    protected void addOutputComponent(String value, FxLanguage language, String inputId) {
        UIComponent parent = component;
        if (component.isReadOnlyShowTranslations() && component.getUIValue().isMultiLanguage()) {
            final RenderHelperUtils.ContainerWriter container = newContainerWriter();
            container.setId(RenderHelperUtils.stripForm(inputId) + "_container");
            container.setDisplayLanguage(true);
            container.setLanguage(language);
            container.setInputClientId(clientId);
            container.setMultiLanguage(true);
            parent.getChildren().add(container);
            // use container as parent for all subsequent operations
            parent = container;
        }
        final HtmlOutputText output = (HtmlOutputText) FxJsfUtils.addChildComponent(parent, RenderHelperUtils.stripForm(inputId), HtmlOutputText.COMPONENT_TYPE, true);
        if (this.value instanceof FxSelectOne)
            output.setStyle("color:" + ((FxSelectOne) this.value).getDefaultTranslation().getColor());
        output.setEscape(false);
        output.setValue(value);
    }

    protected void renderPreviewImage(FxLanguage language, String inputId) {
        if (value.isEmpty() || (language != null && !value.translationExists(language.getId()))) {
            return;
        }
        final BinaryDescriptor descriptor = ((FxBinary) value).getTranslation(language);
        if (component.isDisableLytebox()) {
            addImageComponent(component, descriptor, language, inputId);
            return;
        }
        final HtmlOutputLink link = (HtmlOutputLink) FxJsfUtils.addChildComponent(
                component, RenderHelperUtils.stripForm(inputId) + "_imglink", HtmlOutputLink.COMPONENT_TYPE, true
        );
        FxPK pk;
        try {
            pk = XPathElement.getPK(value.getXPath());
        } catch (FxRuntimeException e) {
            pk = null;
        }

        final String urlOriginal = FxJsfUtils.getServletContext().getContextPath() +
                ThumbnailServlet.getLink(pk,
                        BinaryDescriptor.PreviewSizes.SCREENVIEW,
                        value.getXPath(), descriptor.getCreationTime(), language);
        link.setValue(urlOriginal);
        link.setRel("lytebox[ce]");
        addImageComponent(link, descriptor, language, inputId);
        postRenderPreviewImage(language, inputId);
    }

    /**
     * Add additional components or includes after {@link #renderPreviewImage(FxLanguage, String)} was called.
     *
     * @param language  the output language
     * @param inputId   the input client ID
     */
    protected void postRenderPreviewImage(FxLanguage language, String inputId) {
        // nop
    }

    protected void addImageComponent(UIComponent parent, BinaryDescriptor descriptor, FxLanguage language, String inputId) {
        final HtmlGraphicImage image = (HtmlGraphicImage) FxJsfUtils.addChildComponent(parent, RenderHelperUtils.stripForm(inputId), HtmlGraphicImage.COMPONENT_TYPE, true);
        FxPK pk;
        try {
            pk = XPathElement.getPK(value.getXPath());
        } catch (FxRuntimeException e) {
            pk = null;
        }
        image.setUrl(ThumbnailServlet.getLink(pk,
                BinaryDescriptor.PreviewSizes.PREVIEW2, value.getXPath(), descriptor.getCreationTime(), language));
        if (component.isReadOnlyShowTranslations()) //TODO: might use another attribute to determine if this should be rendered
            image.setStyle("border: none; padding: 5px;");
        else
            image.setStyle("border: none;");
    }
}
