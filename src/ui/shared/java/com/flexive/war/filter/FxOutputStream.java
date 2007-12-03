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

import javax.servlet.ServletOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 *
 */
class FxOutputStream extends ServletOutputStream {

    ServletOutputStream sos = null;
    long length = 0;
    boolean catchContent = false;
    ByteArrayOutputStream output = null;
    boolean commited = false;
    boolean clientWriteThrough = false;


    protected boolean isCommited() {
        return commited;
    }

    /**
     * Constructor.
     *
     * @param sos          the ServletOutputStream
     * @param catchContent has to be true if getData() is called.
     */
    public FxOutputStream(ServletOutputStream sos, boolean catchContent, boolean clientWriteThrough) {
        super();
        this.sos = sos;
        this.catchContent = catchContent;
        this.clientWriteThrough = clientWriteThrough;
        if (this.catchContent) {
            output = new ByteArrayOutputStream(100000);
        }
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

    /**
     * Returns the byte count sent to the client.
     *
     * @return the byte count sent to the client.
     */
    public long getContentLength() {
        return this.length;
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (sos != null) this.sos.close();
        if (this.output != null) this.output.close();
        this.commited = true;
    }


    @Override
    public void flush() throws IOException {
        super.flush();
        if (sos != null) this.sos.flush();
        if (this.output != null) this.output.flush();
    }

    @Override
    public void write(int b) throws IOException {
        this.print((char) b);
    }

    @Override
    public void print(boolean b) throws IOException {
        this.print(b ? "true" : "false");
    }

    @Override
    public void println(boolean b) throws IOException {
        this.println(b ? "true" : "false");
    }

    @Override
    public void print(int i) throws IOException {
        this.print("" + i);
    }

    @Override
    public void print(long l) throws IOException {
        this.print("" + l);
    }

    @Override
    public void print(float v) throws IOException {
        this.print("" + v);
    }

    @Override
    public void print(double v) throws IOException {
        this.print("" + v);
    }

    @Override
    public void println(int i) throws IOException {
        this.println("" + i);
    }

    @Override
    public void println(long l) throws IOException {
        this.println("" + l);
    }

    @Override
    public void println(float v) throws IOException {
        this.println("" + v);
    }

    @Override
    public void println(double v) throws IOException {
        this.println("" + v);
    }

    @Override
    public void println() throws IOException {
        this.println("");
    }

    @Override
    public void print(char c) throws IOException {
        this.print("" + c);
    }

    @Override
    public void println(char c) throws IOException {
        this.println("" + c);
    }

    // ------------ Count operations after this point ------------

    @Override
    public void print(String s) throws IOException {
        if (sos != null) sos.print(s);
        length += s.length();
        if (catchContent) output.write(FxSharedUtils.getBytes(s));
    }

    @Override
    public void println(String s) throws IOException {
        if (sos != null) sos.println(s);
        length += s.length() + 1;
        if (catchContent) output.write(FxSharedUtils.getBytes(s + "\n"));
    }


    @Override
    public void write(byte b[]) throws IOException {
        if (sos != null) sos.write(b);
        length += b.length;
        if (catchContent) output.write(b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        if (sos != null) sos.write(b, off, len);
        length += len;
        if (catchContent) output.write(b, off, len);
    }

}

