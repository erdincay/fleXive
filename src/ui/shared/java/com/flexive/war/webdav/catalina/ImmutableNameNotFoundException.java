package com.flexive.war.webdav.catalina;

import javax.naming.Name;
import javax.naming.NameNotFoundException;

/**
 * Catalina sources cloned for packaging issues to the flexive source tree.
 * Refactored to JDK 1.5 compatibility.
 * Licensed under the Apache License, Version 2.0
 * <p/>
 * Immutable exception to avoid useless object creation by the proxy context.
 * This should be used only by the proxy context. Actual contexts should return
 * properly populated exceptions.
 *
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class ImmutableNameNotFoundException
        extends NameNotFoundException {

    public void appendRemainingComponent(String name) {
    }

    public void appendRemainingName(Name name) {
    }

    public void setRemainingName(Name name) {
    }

    public void setResolverName(Name name) {
    }

    public void setRootCause(Throwable e) {
    }

}
