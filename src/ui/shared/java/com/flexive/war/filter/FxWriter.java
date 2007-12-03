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
package com.flexive.war.filter;

import com.flexive.shared.FxSharedUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Print Writer for the FxResponseWrapper.
 * <p/>
 * This file is part of the fleXive 3.x framework.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */

final class FxWriter extends Writer {
    private static final transient Log LOG = LogFactory.getLog(FxWriter.class);
    private final PrintWriter pw;
    private final boolean catchContent;
    private final ByteArrayOutputStream output;

    private int len = 0;
    private boolean commited = false;


    protected boolean isCommited() {
        return commited;
    }

    public FxWriter(PrintWriter pw, boolean catchContent) {
        this.pw = pw;
        this.catchContent = catchContent;
        output = this.catchContent ? new ByteArrayOutputStream(10000) : null;
    }

    /**
     * Returns the byte count sent to the client.
     *
     * @return the byte count sent to the client.
     */
    public long getContentLength() {
        return this.len;
    }

    /**
     * Returns the data sent to the client if the catchContent option was true.
     * <p/>
     * if the catchContent option was false a empty array is returned.
     *
     * @return the data sent to the client.
     */
    public byte[] getData() {
        if (output == null) return new byte[0];
        return output.toByteArray();
    }

    @Override
    public void write(char buf[], int off, int len) {
        if (pw != null) pw.write(buf, off, len);
        this.len += len;
        if (catchContent) writeToBuffer(FxSharedUtils.getBytes(new String(buf, off, len)));
    }

    @Override
    public void flush() {
        try {
            if (output != null) output.flush();
        } catch (IOException exc) {
            LOG.error(this.getClass() + " output buffer flush error:" + exc.getMessage(), exc);
        }
        if (pw != null) pw.flush();
    }

    @Override
    public void close() {
        if (pw != null) pw.close();
        try {
            if (output != null) output.close();
        } catch (IOException exc) {
            LOG.error(this.getClass() + " output buffer close error:" + exc.getMessage(), exc);
        }
        commited = true;
    }

    private void writeToBuffer(byte[] b) {
        try {
            output.write(b);
        } catch (IOException exc) {
            LOG.error(this.getClass() + " is unable to write to output buffer: " + exc.getMessage(), exc);
        }
    }

}

