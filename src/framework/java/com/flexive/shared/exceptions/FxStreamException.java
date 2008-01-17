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
package com.flexive.shared.exceptions;

import com.flexive.stream.StreamException;
import org.apache.commons.logging.Log;

/**
 * Streaming server exception(s)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxStreamException extends FxApplicationException {
    private static final long serialVersionUID = -8754514403395400542L;

    public FxStreamException(StreamException se) {
        super(se, "ex.stream", se.getMessage());
    }

    public FxStreamException(FxApplicationException converted) {
        super(converted);
    }

    public FxStreamException(Log log, FxApplicationException converted) {
        super(log, converted);
    }

    public FxStreamException(String key, Object... values) {
        super(key, values);
    }

    public FxStreamException(Log log, String key, Object... values) {
        super(log, key, values);
    }

    public FxStreamException(Throwable cause, String key, Object... values) {
        super(cause, key, values);
    }

    public FxStreamException(Log log, Throwable cause, String key, Object... values) {
        super(log, cause, key, values);
    }

    public FxStreamException(String key) {
        super(key);
    }

    public FxStreamException(Log log, String key) {
        super(log, key);
    }

    public FxStreamException(String message, Throwable cause) {
        super(message, cause);
    }

    public FxStreamException(Log log, String message, Throwable cause) {
        super(log, message, cause);
    }

    public FxStreamException(Throwable cause) {
        super(cause);
    }

    public FxStreamException(Log log, Throwable cause) {
        super(log, cause);
    }
}
