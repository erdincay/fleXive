/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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

import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgValidationErr;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.XPathElement;
import com.flexive.shared.value.FxValue;
import org.apache.commons.lang.StringUtils;

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
        AbstractFxValueInput input = (AbstractFxValueInput) component;
        if (input.isReadOnly()) {
            return; // nothing to validate
        }
        FxValue fxValue = (FxValue) value;
        if (!fxValue.isValid()) {
            final String label;
            if (StringUtils.isNotEmpty(fxValue.getXPath())) {
                label = CacheAdmin.getEnvironment().getAssignment(fxValue.getXPath()).getDisplayName();
            } else {
                label = "?";
            }
            FxFacesMsgErr message = new FxFacesMsgValidationErr(fxValue, "FxValueInput.err.invalid", label, fxValue.getErrorValue());
            String clientId = input.getExternalId() == -1 ? input.getClientId(context) : String.valueOf(input.getExternalId());
            if (!StringUtils.isEmpty(clientId)) {
                if (clientId.indexOf(':') > 0) {
                    String[] cid = StringUtils.split(clientId, ":", 2);
                    // TODO: this doesn't really work in most JSF2 applications, where every composite component
                    // is a naming container
                    message.setForm(cid[0]);
                    message.setId(cid[1]);
                } else {
                    message.setId(clientId);
                }
            }
            message.setLocalizedDetail("FxValueInput.err.invalid.detail", fxValue.getErrorValue());
            throw new ValidatorException(message);
        }
        // check default translation
        if (input.isRequired() && !fxValue.translationExists(fxValue.getDefaultLanguage())) {
            final FxFacesMsgErr message = new FxFacesMsgErr("FxValueInput.err.emptyDefaultLanguage");
            message.setLocalizedDetail("FxValueInput.err.emptyDefaultLanguage");
            throw new ValidatorException(message);
        }
    }

    /**
     * Prepare an xpath for output
     *
     * @param fxValue value with an xpath
     * @return xpath
     */
    private String prepareXPath(FxValue fxValue) {
        if (StringUtils.isEmpty(fxValue.getXPath()))
            return "?";
        return XPathElement.stripType(fxValue.getXPath());
    }
}
