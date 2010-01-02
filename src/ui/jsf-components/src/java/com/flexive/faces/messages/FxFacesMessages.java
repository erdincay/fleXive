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
package com.flexive.faces.messages;

import org.apache.commons.lang.StringUtils;

import javax.faces.application.FacesMessage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * FxFacesMessages represents a collection of one ore more FxFacesMessage that have the same
 * summary and the same severity.
 * <p/>
 * The messages can be obtained using the getMessages() function.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxFacesMessages implements Serializable {

    private static final long serialVersionUID = 8038620276312808700L;
    private static long nextId = 0;
    private String summary;
    private FacesMessage.Severity severity;
    private List<FxFacesMessage> messages;
    private long id;

    /**
     * Generates a (server) unique message id.
     *
     * @return a (server) unique message id.
     */
    private synchronized long generateId() {
        long result = nextId++;
        if (nextId == Long.MAX_VALUE) {
            nextId = 0;
        }
        return result;
    }


    /**
     * Constructore.
     *
     * @param summary  the shared summary
     * @param severity the shared severity
     * @param message  the first message to add
     */
    public FxFacesMessages(String summary, FacesMessage.Severity severity, FxFacesMessage message) {
        this.messages = new ArrayList<FxFacesMessage>(10);
        this.summary = summary;
        this.messages.add(message);
        this.severity = severity;
        this.id = generateId();
    }

    /**
     * Returns the (server) unique id of this object.
     *
     * @return the (server) unique id of this object.
     */
    public long getId() {
        return id;
    }

    /**
     * Adds a new mesage to this FxFacesMessages collection.
     *
     * @param message the message to add
     */
    public void addMessage(FxFacesMessage message) {
        for (FxFacesMessage m : messages) {
            if (m.equals(message)) {
                // Dupe
                return;
            }
        }
        this.messages.add(message);
    }

    /**
     * Returns the shared summary with the additional information how many messages
     * the collection contains.
     *
     * @return the shared summary
     */
    public String getSummary() {
        return summary + (getSize() > 1 ? " [" + getSize() + "]" : "");
    }

    /**
     * The shared severity.
     *
     * @return the shared severity
     */
    public FacesMessage.Severity getSeverity() {
        return severity;
    }

    /**
     * The messages.
     *
     * @return the messages
     */
    public List<FxFacesMessage> getMessages() {
        return messages;
    }

    /**
     * The message count.
     *
     * @return the message count
     */
    public int getSize() {
        return this.messages.size();
    }

    /**
     * Returns true if at least one message contains a detail error message.
     *
     * @return true if at least one message contains a detail error message.
     * @see FacesMessage#getDetail
     */
    public boolean isContainsDetailMessages() {
        for (FxFacesMessage message : messages) {
            if (StringUtils.isNotBlank(message.getDetail())) {
                return true;
            }
        }
        return false;
    }
}
