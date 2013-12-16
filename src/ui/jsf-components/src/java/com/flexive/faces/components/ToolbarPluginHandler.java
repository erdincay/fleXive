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

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.beans.PluginRegistryBean;
import com.flexive.faces.plugin.AdmExtensionPoints;
import com.flexive.faces.plugin.ToolbarPluginExecutor;
import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.TemplateClient;
import com.sun.facelets.el.VariableMapperWrapper;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders all plugged-in toolbar buttons for the current page (using the commandElement.xhtml
 * facelets template).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ToolbarPluginHandler extends TagHandler implements TemplateClient {
    private static final Log LOG = LogFactory.getLog(ToolbarPluginHandler.class);

    private static final String COMMAND_TEMPLATE = "/adm/templates/commandElement.xhtml";
    private static final String SEPARATOR_TEMPLATE = "/adm/templates/toolbarSeparator.xhtml";

    /**
     * A {@link ToolbarPluginExecutor} that collects all buttons matching the current URI.
     */
    private static class Executor implements ToolbarPluginExecutor {
        private final String uri;
        private final List<Button> buttons = new ArrayList<Button>();

        private Executor(String uri) {
            this.uri = uri;
        }

        @Override
        public void addToolbarButton(String target, Button button) {
            final Matcher matcher = Pattern.compile(target.replace("*", ".*")).matcher(uri);
            if (matcher.find()) {
                buttons.add(button);
            }
        }

        @Override
        public void addToolbarSeparatorButton() {
            buttons.add(SEPARATOR);
        }

        public List<Button> getButtons() {
            return buttons;
        }
    }

    public ToolbarPluginHandler(TagConfig config) {
        super(config);
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, FaceletException, ELException {
        // run toolbar plugins
        final Executor executor = new Executor(FxJsfUtils.getRequest().getRequestURIWithoutContext());
        PluginRegistryBean.getInstance().execute(AdmExtensionPoints.ADM_TOOLBAR_PLUGINS, executor);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Added " + executor.getButtons().size() + " buttons for " + executor.uri);
        }

        final VariableMapper origMapper = ctx.getVariableMapper();
        try {
            ctx.pushClient(this);           // register ourselve to serve ui:insert requests by the template
            final ExpressionFactory factory = ctx.getExpressionFactory();
            for (ToolbarPluginExecutor.Button button : executor.getButtons()) {
                VariableMapperWrapper mapper = new VariableMapperWrapper(origMapper);
                ctx.setVariableMapper(mapper);

                if( button.isSeparator() ) {
                    // include template
                    ctx.includeFacelet(parent, SEPARATOR_TEMPLATE);
                } else {
                    // set options for commandElement
                    mapper.setVariable("id", factory.createValueExpression(button.getId(), String.class));
                    mapper.setVariable("label", factory.createValueExpression(button.getLabel(), String.class));
                    mapper.setVariable("bean", factory.createValueExpression(ctx, "#{" + button.getBean() + "}", Object.class));
                    mapper.setVariable("action", factory.createValueExpression(button.getAction(), String.class));
                    mapper.setVariable("icon", factory.createValueExpression(button.getIcon(), String.class));
                    mapper.setVariable("iconUrl", factory.createValueExpression(button.getIconUrl(), String.class));
                    mapper.setVariable("location", factory.createValueExpression("toolbar", String.class));

                    // include template
                    ctx.includeFacelet(parent, COMMAND_TEMPLATE);
                }
            }
        } finally {
            ctx.popClient(this);
            ctx.setVariableMapper(origMapper);
        }
    }

    @Override
    public boolean apply(FaceletContext ctx, UIComponent parent, String name) throws IOException, FacesException, FaceletException, ELException {
        // ignore template requests from the included COMMAND_TEMPLATE since we don't have a body
        return true;
    }
}
