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
package com.flexive.war.filter;

import com.flexive.shared.FxContext;
import com.flexive.shared.security.UserTicket;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.Enumeration;
import java.util.Vector;

/**
 * [fleXive] provides multiple divisions (applications) in one war archive, which causes
 * them to share sessions. This wrapper is repsonsible to avoid session data mixup
 * between those divisions by adding a context prefix to all stored data keys.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxSessionWrapper implements HttpSession {

    private HttpSession wrappedSession = null;
    private boolean invalidated = false;

    protected static UserTicket getLastUserTicket(HttpSession session) {
        return (UserTicket) session.getAttribute("LAST_USERTICKET");
    }

    protected static void setLastUserTicket(HttpSession session, UserTicket lastUserTicket) {
        session.setAttribute("LAST_USERTICKET", lastUserTicket);
    }

    /**
     * Constructor
     *
     * @param original the original session
     */
    protected FxSessionWrapper(HttpSession original) {
        this.wrappedSession = original;
    }

    /**
     * Returns the time at which this session representation was created, in
     * milliseconds since midnight, January 1, 1970 UTC.
     *
     * @return the time when the session was created
     */
    @Override
    public long getCreationTime() {
        return wrappedSession.getCreationTime();
    }

    /**
     * Returns the identifier assigned to this session. An HttpSession's identifier is a
     * unique string that is created and maintained by HttpSessionContext.
     *
     * @return the identifier assigned to this session
     */
    @Override
    public String getId() {
        return wrappedSession.getId();
    }

    /**
     * Returns the last time the client sent a request carrying the identifier assigned to the session.
     * <p/>
     * Time is expressed as milliseconds since midnight, January 1, 1970 UTC.
     * Application level operations, such as getting or setting a value associated with the session,
     * does not affect the access time.<br>
     * This information is particularly useful in session management policies.
     * For example,<br>
     * <li> a session manager could leave all sessions which have not been used in a long time in a given context.</li>
     * <li> the sessions can be sorted according to age to optimize some task.</li>
     *
     * @return the last time the client sent a request carrying the identifier assigned to the session
     */
    @Override
    public long getLastAccessedTime() {
        return wrappedSession.getLastAccessedTime();
    }

    /**
     * Returns the ServletContext to which this session belongs.
     *
     * @return The ServletContext object for the web application
     * @since 2.3
     */
    @Override
    public ServletContext getServletContext() {
        return wrappedSession.getServletContext();
    }

    /**
     * Specifies the time, in seconds, between client requests before the servlets container will invalidate
     * this session. A negative time indicates the session should never timeout.
     *
     * @param intervalInSeconds An integer specifying the number of seconds
     */
    @Override
    public void setMaxInactiveInterval(int intervalInSeconds) {
        wrappedSession.setMaxInactiveInterval(intervalInSeconds);
    }

    /**
     * Returns the maximum time interval, in seconds, that the servlets container will keep this session open
     * between client accesses. After this interval, the servlets container will invalidate the session.
     * The maximum time interval can be set with the setMaxInactiveInterval method. A negative time
     * indicates the session should never timeout.
     *
     * @return an integer specifying the number of seconds this session remains open between client requests
     */
    @Override
    public int getMaxInactiveInterval() {
        return wrappedSession.getMaxInactiveInterval();
    }

    /**
     * @deprecated As of Version 2.1, this method is deprecated and has no replacement.
     *             It will be removed in a future version of the Java Servlet API.
     */
    @Override
    public HttpSessionContext getSessionContext() {
        return wrappedSession.getSessionContext();
    }

    /**
     * Invalidates this session then unbinds any objects bound to it.
     */
    @Override
    public void invalidate() {
        synchronized (this) {
            wrappedSession.invalidate();
            invalidated = true;
        }
    }

    /**
     * Returns true if the client does not yet know about the session or if the client chooses not to join the session.
     * For example, if the server used only cookie-based sessions, and the client had disabled the use of cookies,
     * then a session would be new on each request.
     *
     * @return true if the server has created a session, but the client has not yet joined
     */
    @Override
    public boolean isNew() {
        return wrappedSession.isNew();
    }

    // ---------------------------

    /**
     * Returns the object bound with the specified name in this session, or null if no object is bound under the name.
     *
     * @param name a string specifying the name of the object
     * @return the object with the specified name
     */
    @Override
    public Object getAttribute(String name) {
        return invalidated ? null : wrappedSession.getAttribute(encodeAttributeName(name));
    }

    /**
     * @param name a string specifying the name of the object
     * @return the object with the specified name
     * @deprecated As of Version 2.2, this method is replaced by getAttribute(java.lang.String).
     */
    @Override
    public Object getValue(String name) {
        return invalidated ? null : wrappedSession.getValue(encodeAttributeName(name));
    }

    /**
     * Returns an Enumeration of String objects containing the names of all the objects bound to this session.
     *
     * @return an Enumeration of String objects specifying the names of all the objects bound to this session
     */
    @Override
    public Enumeration<String> getAttributeNames() {
        Vector<String> result = new Vector<String>(25);
        if (!invalidated)
            for (Enumeration e = wrappedSession.getAttributeNames(); e.hasMoreElements();)
                result.add(decodeAttributeName((String) e.nextElement()));
        return result.elements();
    }

    /**
     * @return an array of String  objects specifying the names of all the objects bound to this session
     * @deprecated As of Version 2.2, this method is replaced by getAttributeNames().
     */
    @Override
    public String[] getValueNames() {
        Vector<String> result = new Vector<String>(25);
        if (!invalidated)
            for (Enumeration e = wrappedSession.getAttributeNames(); e.hasMoreElements();)
                result.add(decodeAttributeName((String) e.nextElement()));
        return result.toArray(new String[result.size()]);
    }

    /**
     * Binds an object to this session, using the name specified. If an object of the same name is already bound to
     * the session, the object is replaced.<br>
     * After this method executes, and if the new object implements
     * HttpSessionBindingListener, the container calls HttpSessionBindingListener.valueBound. The container then
     * notifies any HttpSessionAttributeListeners in the web application.<br>
     * If an object was already bound to this session
     * of this name that implements HttpSessionBindingListener, its HttpSessionBindingListener.valueUnbound method
     * is called.<br>
     * If the value passed in is null, this has the same effect as calling removeAttribute().
     *
     * @param name  the name to which the object is bound; cannot be null
     * @param value the object to be bound
     */
    @Override
    public void setAttribute(String name, Object value) {
        if (!invalidated)
            wrappedSession.setAttribute(encodeAttributeName(name), value);
    }

    /**
     * @param name  the name to which the object is bound; cannot be null
     * @param value the object to be bound; cannot be null
     * @deprecated As of Version 2.2, this method is replaced by setAttribute(java.lang.String, java.lang.Object)
     */
    @Override
    public void putValue(String name, Object value) {
        if (!invalidated)
            wrappedSession.putValue(encodeAttributeName(name), value);
    }

    /**
     * Removes the object bound with the specified name from this session. If the session does not have an object
     * bound with the specified name, this method does nothing.<br>
     * After this method executes, and if the object implements HttpSessionBindingListener, the container
     * calls HttpSessionBindingListener.valueUnbound. The container then notifies any HttpSessionAttributeListeners in the web application.
     *
     * @param name the name of the object to remove from this session
     * @deprecated
     */
    @Override
    public void removeAttribute(String name) {
        if (!invalidated)
            wrappedSession.removeAttribute(encodeAttributeName(name));
    }

    /**
     * @param name the name of the object to remove from this session
     * @deprecated As of Version 2.2, this method is replaced by removeAttribute(java.lang.String)
     */
    @Override
    public void removeValue(String name) {
        if (!invalidated)
            wrappedSession.removeValue(encodeAttributeName(name));
    }

    /**
     * Encodes a name to include the application (division).
     *
     * @param name the original name
     * @return the encoded name
     */
    public static String encodeAttributeName(String name) {
        FxContext ri = FxContext.get();
        return ri.getApplicationId() + ":" + name;
    }

    /**
     * Decodes a name (removes the application id).
     *
     * @param name the encoded name
     * @return the decoded name
     */
    private String decodeAttributeName(String name) {
        int idx = name.indexOf(':');
        return name.substring(idx + 1);
    }
}
