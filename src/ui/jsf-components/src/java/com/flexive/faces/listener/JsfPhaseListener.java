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
package com.flexive.faces.listener;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.beans.SystemBean;

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

/**
 * A JSF Phase listener recording the current phase of the request in a thread-local variable.
 * During a JSF request, the phase can be retrieved with {@link JsfPhaseListener#getCurrentPhase()}.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class JsfPhaseListener implements PhaseListener {
    private static final long serialVersionUID = -6499050042791431510L;
    private static ThreadLocal<PhaseId> currentPhase = new ThreadLocal<PhaseId>() {
        @Override
        protected PhaseId initialValue() {
            return PhaseId.APPLY_REQUEST_VALUES;
        }
    };

    public PhaseId getPhaseId() {
        return PhaseId.ANY_PHASE;
    }

    public void beforePhase(PhaseEvent e) {
        currentPhase.set(e.getPhaseId());
        FxJsfUtils.getManagedBean(SystemBean.class).reset();
    }

    public void afterPhase(PhaseEvent e) {
        currentPhase.set(null);
    }

    public static PhaseId getCurrentPhase() {
        return currentPhase.get();
    }

    public static boolean isInPhase(PhaseId id) {
        return id != null && id.equals(currentPhase.get());
    }
}
