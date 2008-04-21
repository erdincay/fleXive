/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.faces.converter;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.beans.SelectBean;
import com.flexive.shared.FxLanguage;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.model.SelectItem;


public class FxLanguageConverter implements Converter {

    public Object getAsObject(FacesContext facesContext, UIComponent uiComponent, String string) {
        try {
            SelectBean sb = (SelectBean) FxJsfUtils.getManagedBean("fxSelectBean");
            for (SelectItem lang : sb.getLanguages()) {
                FxLanguage curLang = (FxLanguage) lang.getValue();
                if (curLang.getIso2digit().equals(string)) {
                    return curLang;
                }
            }
            throw new ConverterException("Invalid Language: '" + string + "'");
        } catch (Throwable exc) {
            throw new ConverterException("Invalid Language: " + exc.getMessage());
        }
    }

    public String getAsString(FacesContext facesContext, UIComponent uiComponent, Object object) {
        return ((FxLanguage) object).getIso2digit();
    }


}
