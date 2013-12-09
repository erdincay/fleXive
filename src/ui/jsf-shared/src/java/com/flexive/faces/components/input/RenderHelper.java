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

import javax.faces.component.UIComponent;
import java.io.IOException;

/**
 * Shared interface of the render helper classes of {@code fx:fxValueInput}.
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public interface RenderHelper {
    /**
     * Render the input fields for the given value/type.
     *
     * @throws java.io.IOException if an output error occured
     */
    void render() throws IOException;

    /**
     * Render a multi-language input field for the current value.
     *
     * @throws IOException if the input could not be rendered
     */
    void encodeMultiLanguageField() throws IOException;

    /**
     * Render a input field for the given language.
     *
     * @param parent   the parent component
     * @param inputId  the input element ID (= form name)
     * @param language the language for the input field, or null if the value is not multi-language. @throws IOException if the input could not be rendered
     * @throws java.io.IOException on errors
     */
    void encodeField(UIComponent parent, String inputId, FxLanguage language) throws IOException;
}
