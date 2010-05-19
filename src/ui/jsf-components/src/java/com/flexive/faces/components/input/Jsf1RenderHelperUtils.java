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

import com.flexive.faces.components.WriteWebletIncludes;
import java.io.IOException;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
class Jsf1RenderHelperUtils {

    private Jsf1RenderHelperUtils() {
    }

    public static class WebletIncludeWriter extends UIOutput {
        private String[] weblets;

        public void setWeblets(String... weblets) {
            this.weblets = weblets.clone();
        }

        @Override
        public void encodeBegin(FacesContext facesContext) throws IOException {
            if (weblets == null) {
                return;
            }
            final ResponseWriter out = facesContext.getResponseWriter();
            for (String weblet : weblets) {
                out.write(StringUtils.defaultString(WriteWebletIncludes.getWebletInclude(weblet)));
            }
        }

        @Override
        public Object saveState(FacesContext context) {
            final Object[] state = new Object[2];
            state[0] = super.saveState(context);
            state[1] = weblets;
            return state;
        }

        @Override
        public void restoreState(FacesContext context, Object stateValue) {
            final Object[] state = (Object[]) stateValue;
            super.restoreState(context, state[0]);
            weblets = (String[]) state[1];
        }
    }

}
