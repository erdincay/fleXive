/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
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
package com.flexive.war.beans.cms;

import com.flexive.faces.FxJsfUtils;
import com.flexive.shared.FxContext;
import com.flexive.shared.security.UserTicket;

import java.io.Serializable;

public class PageProviderBean implements Serializable {

    /**
     * Returns a reference to the request information object.
     *
     * @return the request information object
     */
    public FxContext getContext() {
        return FxContext.get();
    }

    /**
     * Returns the id of the page.
     *
     * @return the page id
     */
    public long getNodeId() {
        return FxJsfUtils.getRequest().getNodeId();
    }

    /**
     * Returns the ticket of the calling user.
     *
     * @return the ticket
     */
    public UserTicket getTicket() {
        return getContext().getTicket();
    }

    /**
     * Returns the path of the page, eg "/Home".
     *
     * @return the path
     */
    public String getPath() {
        return FxContext.get().getRelativeRequestURI(true);
    }

}
