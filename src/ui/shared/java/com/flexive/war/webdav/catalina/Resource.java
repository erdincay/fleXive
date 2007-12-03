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

