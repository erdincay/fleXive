package com.flexive.faces.components;

import com.sun.facelets.tag.TagHandler;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.el.VariableMapperWrapper;

import javax.faces.component.UIComponent;
import javax.faces.FacesException;
import javax.el.ELException;
import javax.el.VariableMapper;
import javax.el.ValueExpression;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;

/**
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
