package com.flexive.war.webdav.catalina;

import javax.naming.CompositeName;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

/**
 * Catalina sources cloned for packaging issues to the flexive source tree.
 * Refactored to JDK 1.5 compatibility.
 * Licensed under the Apache License, Version 2.0
 * <p/>
 * Parses names.
 *
 * @author Remy Maucherat
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

public class NameParserImpl implements NameParser {

    // ----------------------------------------------------- Instance Variables

    // ----------------------------------------------------- NameParser Methods


    /**
     * Parses a name into its components.
     *
     * @param name The non-null string name to parse
     * @return A non-null parsed form of the name using the naming convention
     *         of this parser.
     */
    public Name parse(String name) throws NamingException {
        return new CompositeName(name);
    }
}


