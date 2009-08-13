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
package com.flexive.shared.content;

import com.flexive.shared.FxLock;

import java.io.Serializable;

/**
 * A "cached" content, used to keep a content and its security information together for caching purposes
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxCachedContent implements Serializable {
    private static final long serialVersionUID = 188840576298309320L;
    private FxContent content;
    private FxContentSecurityInfo securityInfo;

    /**
     * Ctor
     *
     * @param content      the content
     * @param securityInfo the contents security info
     */
    public FxCachedContent(FxContent content, FxContentSecurityInfo securityInfo) {
        this.content = content;
        this.securityInfo = securityInfo;
    }

    /**
     * Getter for the content
     *
     * @return content
     */
    public FxContent getContent() {
        return content;
    }

    /**
     * Getter for the security info
     *
     * @return security info
     */
    public FxContentSecurityInfo getSecurityInfo() {
        return securityInfo;
    }

    /**
     * Update the lock of the cache instance
     *
     * @param lock the lock to set
     */
    public void updateLock(FxLock lock) {
        this.content.updateLock(lock);
    }
}
