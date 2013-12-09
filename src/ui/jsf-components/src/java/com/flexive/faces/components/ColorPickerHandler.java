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
package com.flexive.faces.components;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.el.VariableMapperWrapper;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;
import org.apache.commons.lang.StringUtils;

import javax.el.ELException;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import java.io.IOException;

/**
 * Renders a color picker control.
 * 
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class ColorPickerHandler extends TagHandler {
    private static final String TEMPLATE = "templates/colorPicker.xhtml";

    private final TagAttribute inputId;

    public ColorPickerHandler(TagConfig config) {
        super(config);
        this.inputId = getAttribute("inputId");
    }

    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, ELException {
        VariableMapper orig = ctx.getVariableMapper();
        try {
            final VariableMapperWrapper mapper = new VariableMapperWrapper(orig);
            final String targetId = inputId != null && StringUtils.isNotBlank(inputId.getValue(ctx))
                    ? inputId.getValue(ctx) : parent.getClientId(ctx.getFacesContext());
            mapper.setVariable("inputId", ctx.getExpressionFactory().createValueExpression(targetId, String.class));
            ctx.setVariableMapper(mapper);
            ctx.includeFacelet(parent, Thread.currentThread().getContextClassLoader().getResource(TEMPLATE));
        } finally {
            ctx.setVariableMapper(orig);
        }
    }
}
