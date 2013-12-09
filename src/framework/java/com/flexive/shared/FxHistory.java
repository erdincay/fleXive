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
package com.flexive.shared;

import java.io.Serializable;

/**
 * A history entry
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxHistory implements Serializable {
    private static final long serialVersionUID = -610739322976575887L;

    private long timestp;
    private long accountId;
    private String loginName;
    private String key;
    private String[] args;
    private long typeId;
    private long contentId;
    private int contentVersion;
    private String application;
    private String host;
    private String data;
    private String session;

    /**
     * Ctor
     *
     * @param timestp        timestamp
     * @param accountId      account id
     * @param loginName      login name
     * @param key            message key
     * @param args           message arguments separated by '|'
     * @param typeId         type id
     * @param contentId      content id
     * @param contentVersion content version
     * @param application    application name
     * @param host           remote host
     * @param data           optional data
     * @param session        session id
     */
    public FxHistory(long timestp, long accountId, String loginName, String key, String[] args, long typeId, long contentId,
                     int contentVersion, String application, String host, String data, String session) {
        this.timestp = timestp;
        this.accountId = accountId;
        this.loginName = loginName;
        this.key = key;
        this.args = args != null ? args.clone() : null;
        this.typeId = typeId;
        this.contentId = contentId;
        this.contentVersion = contentVersion;
        this.application = application;
        this.host = host;
        this.data = data;
        this.session = session;
    }

    public long getTimestp() {
        return timestp;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getLoginName() {
        return loginName;
    }

    public long getTypeId() {
        return typeId;
    }

    public long getContentId() {
        return contentId;
    }

    public int getContentVersion() {
        return contentVersion;
    }

    public String getApplication() {
        return application;
    }

    public String getHost() {
        return host;
    }

    public String getData() {
        return data;
    }

    public String getSession() {
        return session;
    }

    /**
     * Is additional data available?
     *
     * @return additional data available
     */
    public boolean hasData() {
        return data != null && !"".equals(data);
    }

    /**
     * Bean compliant hasDate for JSF ...
     *
     * @return data is available
     */
    public boolean getHasData() {
        return hasData();
    }

    /**
     * Is this entry related to a content?
     *
     * @return entry related to a content
     */
    public boolean isContentSpecific() {
        return contentId > 0;
    }

    /**
     * Is this entry related to a type?
     *
     * @return entry related to a type
     */
    public boolean isTypeSpecific() {
        return typeId >= 0;
    }

    /**
     * Get the message formatted in the calling users locale (if available)
     *
     * @return message formatted in the calling users locale (if available)
     */
    public String getMessage() {
        FxLanguage lang = FxContext.getUserTicket().getLanguage();
        return FxSharedUtils.getLocalizedMessage("History", lang.getId(), lang.getIso2digit(), key, (Object[]) args);
    }

}
