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
package com.flexive.faces.components.toolkit;

import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.value.FxValue;
import com.flexive.shared.value.mapper.InputMapper;
import com.sun.facelets.FaceletContext;
import com.sun.facelets.el.VariableMapperWrapper;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import java.io.IOException;

/**
 * A facelets handler that renders a complete form element for editing or displaying
 * a property of a content instance provided by {@link FxContentView}. The output template
 * is provided by fxValueInputRow.xhtml (the component template of fx:fxValueInputRow).
 * A custom template can be specified with the <code>template</code> parameter.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxValueHandler extends TagHandler {
    private static final String TEMPLATE_ROOT = "templates/";
    private String template = "fxValueInputRow.xhtml";

    public FxValueHandler(TagConfig config) {
        super(config);
    }

    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, ELException {
        final String var;
        try {
            if (this.getAttribute("var") == null) {
                // no variable name specified, use enclosing content view
                if (!(parent instanceof FxContentView)) {
                    throw new FacesException("Facelet parent is no FxContentView instance and \"var\" attribute not specified.");
                }
                var = ((FxContentView) parent).getVar();
            } else {
                // use a custom variable name
                var = this.getAttribute("var").getValue(ctx);
            }
        } catch (FxRuntimeException e) {
            throw new FacesException("The fx:value component must be embedded in a fx:content instance.");
        }
        final boolean isNewValue = this.getAttribute("new") != null && Boolean.valueOf(this.getAttribute("new").getValue(ctx));
        final VariableMapper origMapper = ctx.getVariableMapper();
        final VariableMapperWrapper mapper = new VariableMapperWrapper(origMapper);
        try {
            ctx.setVariableMapper(mapper);

            // get property attribute
            final ValueExpression propertyExpression = this.getAttribute("property").getValueExpression(ctx, String.class);
            final String property = (String) propertyExpression.getValue(ctx);

            // assign id, label/labelKey and value based on the enclosing FxContentView instance
            // if the property contains a slash, return the part after the last slash character as unique ID
            mapper.setVariable("id", ctx.getExpressionFactory().createValueExpression(
                    property.indexOf('/') != -1 ? property.substring(property.lastIndexOf('/') + 1) : property,
                    String.class
            ));
            if (this.getAttribute("labelKey") == null) {
                // use property label
                mapper.setVariable("label", ctx.getExpressionFactory().createValueExpression(ctx, FxContentView.getExpression(var, property, "label"), String.class));
            } else {
                // use provided message key
                assignAttribute(ctx, mapper, "labelKey", String.class);
            }
            // retrieve content from content view
            mapper.setVariable("value", ctx.getExpressionFactory().createValueExpression(ctx,
                    FxContentView.getExpression(var, property, isNewValue ? "new" : null), FxValue.class));

            // passthrough other template attributes
            assignAttribute(ctx, mapper, "inputMapper", InputMapper.class);
            assignAttribute(ctx, mapper, "onchange", String.class);
            assignAttribute(ctx, mapper, "readOnly", Boolean.class);
            assignAttribute(ctx, mapper, "decorate", Boolean.class);
            assignAttribute(ctx, mapper, "filter", Boolean.class);

            // TODO: cache templates/use a facelet ResourceResolver to encapsulate this
            ctx.includeFacelet(parent, Thread.currentThread().getContextClassLoader().getResource(TEMPLATE_ROOT + template));
        } finally {
            ctx.setVariableMapper(origMapper);
        }
    }

    private void assignAttribute(FaceletContext ctx, VariableMapper mapper, String name, Class<?> expectedClass) {
        final TagAttribute attribute = getAttribute(name);
        if (attribute != null) {
            mapper.setVariable(name, attribute.getValueExpression(ctx, expectedClass));
        }
    }

    /**
     * Sets the template name (relative to /WEB-INF/templates).
     *
     * @param template the template name (relative to /WEB-INF/templates).
     */
    public void setTemplate(String template) {
        this.template = template;
    }
}
