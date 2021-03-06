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
package com.flexive.shared.interfaces;

import com.flexive.shared.FxHistory;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.structure.FxType;

import javax.ejb.Remote;
import java.util.Date;
import java.util.List;

/**
 * History tracker service
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface HistoryTrackerEngine {

    /**
     * Write a history entry
     *
     * @param key  key of the message (history. ...)
     * @param args arguments needed by the key
     */
    void track(String key, Object... args);

    /**
     * Write a history entry with custom data
     *
     * @param data custom data/message
     * @param key  key of the message (history. ...)
     * @param args arguments needed by the key
     */
    void trackData(String data, String key, Object... args);

    /**
     * Write a history entry for a type
     *
     * @param type affected type
     * @param key  key of the message (history. ...)
     * @param args arguments needed by the key
     */
    void track(FxType type, String key, Object... args);

    /**
     * Write a history entry with the option to override specific attributes.
     * Blank or <code>null</code> arguments will be replaced by defaults if not set.
     *
     * @param mandator mandator
     * @param typeName type name
     * @param loginname login name
     * @param application application name
     * @param session session id
     * @param remoteHost remote host
     * @param message message text
     * @param data data
     * @param key message key
     * @param args message arguments
     */
    void track(Long mandator, String typeName, String loginname, String application, String session, String remoteHost, String message, String data, String key, Object... args);

    /**
     * Write a history entry for a content instance
     *
     * @param type affected type
     * @param data (optional) data
     * @param pk   affected content
     * @param key  key of the message (history. ...)
     * @param args arguments needed by the key
     */
    void track(FxType type, FxPK pk, String data, String key, Object... args);

    /**
     * Get a list of entries for a content identified by its id.
     * If no content with this id exists, an empty list is returned.
     *
     * @param contentId requested content id
     * @return date ordered (descending) list of entries for the requested content
     */
    List<FxHistory> getContentEntries(long contentId);

    /**
     * Get a list of history entries that matches the requested options (<code>null</code> means any)
     *
     * @param keyMatch     message key
     * @param accountMatch account id
     * @param typeMatch    type id
     * @param contentMatch content id
     * @param startDate    start date
     * @param endDate      end date
     * @param maxEntries   maximum number of entries to return
     * @return date ordered (descending) list of matched entries
     */
    List<FxHistory> getEntries(String keyMatch, Long accountMatch, Long typeMatch, Long contentMatch, Date startDate, Date endDate, int maxEntries);

}
