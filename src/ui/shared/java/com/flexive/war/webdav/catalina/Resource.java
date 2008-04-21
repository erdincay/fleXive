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
package com.flexive.war.webdav.catalina;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Catalina sources cloned for packaging issues to the flexive source tree.
 * Refactored to JDK 1.5 compatibility.
 * Licensed under the Apache License, Version 2.0
 * <p/>
 * Encapsultes the contents of a resource.
 *
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class Resource {

    // ----------------------------------------------------------- Constructors


    public Resource() {
    }


    public Resource(InputStream inputStream) {
        setContent(inputStream);
    }


    public Resource(byte[] binaryContent) {
        setContent(binaryContent);
    }

    // ----------------------------------------------------- Instance Variables


    /**
     * Binary content.
     */
    protected byte[] binaryContent = null;


    /**
     * Input stream.
     */
    protected InputStream inputStream = null;

    // ------------------------------------------------------------- Properties


    /**
     * Content accessor.
     *
     * @return InputStream
     */
    public InputStream streamContent()
            throws IOException {
        if (binaryContent != null) {
            return new ByteArrayInputStream(binaryContent);
        }
        return inputStream;
    }


    /**
     * Content accessor.
     *
     * @return binary content
     */
    public byte[] getContent() {
        return binaryContent;
    }


    /**
     * Content mutator.
     *
     * @param inputStream New input stream
     */
    public void setContent(InputStream inputStream) {
        this.inputStream = inputStream;
    }


    /**
     * Content mutator.
     *
     * @param binaryContent New bin content
     */
    public void setContent(byte[] binaryContent) {
        this.binaryContent = binaryContent;
    }


}

