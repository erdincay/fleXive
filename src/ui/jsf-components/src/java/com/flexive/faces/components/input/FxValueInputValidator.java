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

import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.shared.value.FxValue;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

/**
 * Validator for FxValue inputs.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxValueInputValidator implements Validator {

    /**
     * {@inheritDoc}
     */
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        FxValueInput input = (FxValueInput) component;
        if (input.isReadOnly()) {
            return; // nothing to validate
        }
        FxValue fxValue = (FxValue) value;
        if (!fxValue.isValid()) {
            // TODO: add input component label to the error message
            FxFacesMsgErr message = new FxFacesMsgErr("FxValueInput.err.invalid", fxValue.getErrorValue());
            message.setId(input.getExternalId() == -1 ? input.getClientId(context) : String.valueOf(input.getExternalId()));
            throw new ValidatorException(message);
        }
        // check default translation
        if (input.isRequired() && !fxValue.translationExists(fxValue.getDefaultLanguage())) {
            throw new ValidatorException(new FxFacesMsgErr("FxValueInput.err.emptyDefaultLanguage"));
        }
    }
}
