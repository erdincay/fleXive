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

import com.flexive.shared.value.FxBinary;
import com.flexive.shared.value.FxValue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.faces.context.FacesContext;

/**
 * JSF2 renderer for {@code fx:fxValueInput}.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class Jsf2FxValueInputRenderer extends AbstractFxValueInputRenderer<Jsf2FxValueInput> {
    private static final Log LOG = LogFactory.getLog(Jsf2FxValueInputRenderer.class);

    @Override
    public RenderHelper getRenderHelper(FacesContext context, Jsf2FxValueInput component, FxValue value, boolean editMode) {
        return editMode
                ? new Jsf2EditModeHelper(component, component.getClientId(context), value)
                : new Jsf2ReadOnlyModeHelper(component, component.getClientId(context), value);
    }


    @Override
    protected void processBinary(FacesContext context, AbstractFxValueInput input, String inputId, FxBinary value, long languageId) {
        if (LOG.isWarnEnabled()) {
            LOG.warn("Jsf2FxValueInputRenderer#processBinary not implemented");
        }
        // TODO
    }

}
