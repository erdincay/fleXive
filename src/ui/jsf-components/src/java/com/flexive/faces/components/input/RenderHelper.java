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
package com.flexive.faces.components.input;

import com.flexive.shared.FxLanguage;
import com.flexive.shared.value.FxValue;

import javax.faces.context.ResponseWriter;
import javax.faces.component.UIComponent;
import java.io.IOException;

/**
 * Renders the given FxValue.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
abstract class RenderHelper {

    protected final FxValueInput component;
    protected final ResponseWriter writer;
    protected final String clientId;
    protected final FxValue value;

    /**
     * @param writer    the current response writer
     * @param clientId  the component's client ID
     * @param value     the initial input value
     * @param component the input component
     */
    protected RenderHelper(ResponseWriter writer, FxValueInput component, String clientId, FxValue value) {
        this.component = component;
        this.writer = writer;
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
     * @param parent  the parent component
     * @param inputId  the input element ID (= form name)
     * @param language the language for the input field, or null if the value is not multi-language. @throws IOException if the input could not be rendered
     */
    protected abstract void encodeField(UIComponent parent, String inputId, FxLanguage language) throws IOException;
}
