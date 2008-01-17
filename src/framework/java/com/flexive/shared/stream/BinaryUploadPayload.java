/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.shared.stream;

import com.flexive.shared.FxContext;

import java.io.Serializable;

/**
 * Payload for binary uploads.
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public final class BinaryUploadPayload implements Serializable {
    private static final long serialVersionUID = -54895840376576935L;

    private String handle = null; //handle and optional error message
    private long timeToLive = 0;
    private long expectedLength = 0;
    private boolean serverError = false;
    private int division = -1;
    private boolean finished = false;

    /**
     * Constructor for the 'calling user'
     *
     * @param expectedLength expected length of the stream/binary
     * @param timeToLive     desired TTL
     */
    public BinaryUploadPayload(long expectedLength, long timeToLive) {
        this.expectedLength = expectedLength;
        this.timeToLive = timeToLive;
        this.division = FxContext.get().getDivisionId();
    }

    /**
     * Server side constructor
     *
     * @param handle db handle assigned to the binary
     */
    public BinaryUploadPayload(String handle) {
        this.handle = handle;
    }

    /**
     * Server side constructor incase of errors
     *
     * @param serverError error occured
     * @param msg         error message
     */
    public BinaryUploadPayload(boolean serverError, String msg) {
        this.serverError = serverError;
        this.handle = msg;
    }

    public BinaryUploadPayload() {
        this.finished = true;
    }

    public long getExpectedLength() {
        return expectedLength;
    }

    public String getHandle() {
        return handle;
    }

    public boolean isServerError() {
        return serverError;
    }

    public String getErrorMessage() {
        return handle;
    }

    public long getTimeToLive() {
        return timeToLive;
    }

    public int getDivision() {
        return division;
    }

    public boolean isFinished() {
        return finished;
    }
}
