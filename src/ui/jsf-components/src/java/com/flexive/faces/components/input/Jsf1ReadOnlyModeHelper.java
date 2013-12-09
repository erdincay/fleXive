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

import com.flexive.shared.FxLanguage;
import com.flexive.shared.value.FxValue;

import static com.flexive.faces.components.input.Jsf1RenderHelperUtils.WebletIncludeWriter;

/**
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class Jsf1ReadOnlyModeHelper extends ReadOnlyModeHelper {

    public Jsf1ReadOnlyModeHelper(AbstractFxValueInput component, String clientId, FxValue value) {
        super(component, clientId, value);
    }

    @Override
    protected void postRenderPreviewImage(FxLanguage language, String inputId) {
        if (component.isReadOnlyShowTranslations()) //TODO: might use another attribute to determine if this should be rendered
        {
            RenderHelperUtils.addImageDescriptionComponent(component, component, language, inputId + "_desc");
        }

        // render lytebox include
        final WebletIncludeWriter includeWriter = new WebletIncludeWriter();
        includeWriter.setWeblets(
                "com.flexive.faces.weblets/js/lytebox.js",
                "com.flexive.faces.weblets/css/lytebox.css");
        component.getChildren().add(includeWriter);
    }
}
