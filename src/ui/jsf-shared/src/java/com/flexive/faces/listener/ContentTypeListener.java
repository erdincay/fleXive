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
package com.flexive.faces.listener;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

/**
 * <p>Forces the Facelets content type to be text/html, even if none was specified (or no f:view tag exists).</p>
 *
 * <p>Currently our pages and components don't work with the default content type of
 * <code>application/xml+xhtml</code> in <i>some</i> browsers (most notably Firefox up to 2.x, Bug FX-298).
 * </p>
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class ContentTypeListener implements PhaseListener {
    private static final long serialVersionUID = -4146936347609763113L;
    private static final String CONTENT_TYPE = "text/html";

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.RENDER_RESPONSE;
    }

    @Override
    public void beforePhase(PhaseEvent event) {
        FacesContext.getCurrentInstance().getExternalContext()
                .getRequestMap().put("facelets.ContentType", CONTENT_TYPE);
    }

    @Override
    public void afterPhase(PhaseEvent event) {
        // nop
    }
}
