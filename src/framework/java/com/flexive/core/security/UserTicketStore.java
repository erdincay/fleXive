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
package com.flexive.core.security;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.cache.FxCacheException;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLoadException;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.interfaces.AccountEngine;
import com.flexive.shared.mbeans.FxCacheMBean;
import com.flexive.shared.security.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Store for all currently logged in user(ticket)s
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class UserTicketStore {

    private static final String KEY_TICKET = "Ticket";
    private static transient Log LOG = LogFactory.getLog(UserTicketStore.class);

    /**
     * Stores a subject (and its ticket) for the current session.
     *
     * @param sub the subject to store
     */
    public static void storeSubject(Subject sub) {
        FxCacheMBean cache = CacheAdmin.getInstance();
        FxContext si = FxContext.get();
        try {
            cache.put(getCacheRoot(si), KEY_TICKET, sub);
        } catch (FxCacheException exc) {
            LOG.error("Failed to store ticket in UserTickerStore: " + exc.getMessage(), exc);
            System.err.println("Failed to store ticket in UserTickerStore: " + exc.getMessage());
        }
    }

    /**
     * Removes the subject for the current session
     */
    public static void removeSubject() {
        FxCacheMBean cache = CacheAdmin.getInstance();
        FxContext si = FxContext.get();
        try {
            cache.remove(getCacheRoot(si), KEY_TICKET);
        } catch (FxCacheException exc) {
            LOG.error("Failed to clear session in UserTickerStore: " + exc.getMessage(), exc);
            System.err.println("Failed to store ticket in UserTickerStore: " + exc.getMessage());
        }
    }

    /**
     * Computes the ticket store path of a session.
     *
     * @param si the session info
     * @return the path of the ticket for the session
     */
    protected static String getCacheRoot(final FxContext si) {
        return CacheAdmin.ROOT_USERTICKETSTORE + "/" + si.getApplicationId() +
                (si.isWebDAV() ? "_WebDav" : "") + ":" + si.getSessionId();
    }


    /**
     * Gets the ticket of the current request.
     *
     * @param si the request information
     * @return the ticket of the current request.
     */
    protected static Subject getSubject(FxContext si) {
        FxCacheMBean cache = CacheAdmin.getInstance();
        Subject sub = null;
        try {
            sub = (Subject) cache.get(getCacheRoot(si), KEY_TICKET);
        } catch (FxCacheException exc) {
            LOG.error("Failed to get ticket from UserTicketStore Cache: " + exc.getMessage(), exc);
        } catch (Exception exc) {
            LOG.error("Failed to get ticket from UserTicketStore: " + exc.getMessage(), exc);
        }
        return sub;
    }


    /**
     * Returns all subjects(tickets) in the store that match at least one of the given parameters.
     *
     * @param userId  the user id to look for
     * @param groupId the group ids to match, if the ticket is a member of at least
     *                one group it is added to the result
     * @param acls    the acls
     * @return the matching subjects
     */
    private static Subject[] getSubjects(Long userId, long[] groupId, long[] acls) {
        FxCacheMBean cache = CacheAdmin.getInstance();
        try {
            List<Subject> result = new ArrayList<Subject>(50);
            Set aSet = cache.getChildrenNames(CacheAdmin.ROOT_USERTICKETSTORE);
            if (aSet == null) return new Subject[0];
            UserTicketImpl aTicket;
            Subject sub;
            for (Object subPath : aSet) {
                sub = (Subject) cache.get(CacheAdmin.ROOT_USERTICKETSTORE + "/" + subPath, KEY_TICKET);
                if (sub == null) continue;
                try {
                    aTicket = (UserTicketImpl) FxDefaultLogin.getUserTicket(sub);
                } catch (FxNotFoundException exc) {
                    LOG.error("No UserTicket found in Subject " + sub, exc);
                    continue;
                }
                boolean match = userId != null && aTicket.getUserId() == userId;
                match = match || aTicket.isInAtLeastOneGroup(groupId);
                match = match || aTicket.hasAtLeastOneACL(acls);
                if (!match) continue;
                result.add(sub);
            }
            return result.toArray(new Subject[result.size()]);
        } catch (FxCacheException exc) {
            LOG.error("Failed to examine tickets: " + exc.getMessage(), exc);
            return new Subject[0];
        }
    }

    /**
     * Gets the user ticket for the current request.
     *
     * @return the user ticket for the current request.
     */
    public static UserTicket getTicket() {
        return getTicket(true);
    }

    /**
     * Gets the user ticket for the current request.
     *
     * @param refreshIfDirty true: syncs the ticket with the database if it is dirty
     * @return the user ticket for the current request.
     */
    public static UserTicket getTicket(boolean refreshIfDirty) {
        FxContext si = FxContext.get();
        try {
            Subject sub = getSubject(si);
            UserTicketImpl ticket;
            if (sub == null) {
                ticket = (UserTicketImpl) UserTicketImpl.getGuestTicket();
            } else {
                ticket = (UserTicketImpl) FxDefaultLogin.getUserTicket(sub);
                // Check dirty flag and sync with database if needed
                if (refreshIfDirty && ticket.isDirty()) {
                    ticket = (UserTicketImpl) getUserTicket(ticket.getLoginName());
                    FxDefaultLogin.updateUserTicket(sub, ticket);
                    storeSubject(sub);
                }
            }
            // Return ticket with system permissions if a runAsSystem flag is set
            if (si.getRunAsSystem() && !ticket.isGlobalSupervisor()) {
                ticket = ticket.cloneAsGlobalSupervisor();
            }
            return ticket;
        } catch (Exception exc) {
            LOG.fatal(exc, exc);
            return UserTicketImpl.getGuestTicket();
        }
    }

    public static UserTicket getUserTicket(String loginName) throws FxApplicationException {
        FxContext ri = FxContext.get();
        ri.runAsSystem();
        try {
            AccountEngine ae = EJBLookup.getAccountEngine();
            Account acc = ae.load(loginName);
            UserGroupList gl = ae.getGroups(acc.getId());
            Role roles[] = ae.getRoles(acc.getId(), AccountEngine.RoleLoadMode.ALL);
            ACLAssignment[] aad = ae.loadAccountAssignments(acc.getId());
            return new UserTicketImpl(ri.getApplicationId(), ri.isWebDAV(), acc, gl.toLongArray(), roles, aad, acc.getLanguage());
        } catch (FxNoAccessException exc) {
            // This should NEVER happen since we are running as system
            throw new FxLoadException(LOG, exc);
        } finally {
            ri.stopRunAsSystem();
        }
    }

    /**
     * Flags all active UserTickets with a given user id as dirty, which will
     * force them to sync with the database upon the next access.
     *
     * @param userId the user id, or null
     */
    public static void flagDirtyHavingUserId(Long userId) {
        flagDirtyHaving(userId, null, null);

    }

    /**
     * Flags all active UserTickets with a given ACL id as dirty, which will
     * force them to sync with the database upon the next access.
     *
     * @param aclId the acl
     */
    public static void flagDirtyHavingACL(long aclId) {
        flagDirtyHaving(null, null, aclId);

    }

    /**
     * Flags all active UserTickets with a given group id as dirty, which will
     * force them to sync with the database upon the next access.
     *
     * @param groupId the group id
     */
    public static void flagDirtyHavingGroupId(Long groupId) {
        flagDirtyHaving(null, groupId, null);
    }


    /**
     * Flags all active UserTickets with a given id or acl as dirty, which will
     * force them to sync with the database upon the next access.
     *
     * @param userId  the user id, or null
     * @param aclId   a acl, or null
     * @param groupId a group id, or null
     */
    private static void flagDirtyHaving(Long userId, Long groupId, Long aclId) {
        Subject[] subs = getSubjects(userId,
                groupId == null ? null : new long[]{groupId},
                aclId == null ? null : new long[]{aclId});
        for (Subject sub : subs) {
            try {
                final UserTicketImpl ticket = (UserTicketImpl) FxDefaultLogin.getUserTicket(sub);
                if (!ticket.isDirty()) {
                    // Flag as dirty
                    ticket.setDirty(true);
                    FxDefaultLogin.updateUserTicket(sub, ticket);
                    // Write back to the cluster cache
                    storeSubject(sub);
                }
            } catch (Exception exc) {
                LOG.fatal("Subject without UserTicket [" + sub + "], dirty flag update skipped", exc);
            }
        }
        if (FxContext.get() != null) {
            // update current user's ticket
            FxContext.get().setTicket(getTicket());
        }
    }

    /**
     * Removes all data matching a given user id.
     *
     * @param userId        the user id
     * @param applicationId the application id, may be null for all applications
     * @return amount of deleted entries
     */
    public static int removeUserId(final long userId, final String applicationId) {
        FxCacheMBean cache = CacheAdmin.getInstance();
        List<String> remove = new ArrayList<String>(100);
        // Find all matching nodes
        try {
            Set aSet = cache.getChildrenNames(CacheAdmin.ROOT_USERTICKETSTORE);
            if (aSet == null) return 0;
            UserTicket aTicket;
            Subject sub;
            for (Object subPath : aSet) {
                String fullNodeName = CacheAdmin.ROOT_USERTICKETSTORE + "/" + subPath;
                sub = (Subject) cache.get(fullNodeName, KEY_TICKET);
                if (sub == null) continue;
                try {
                    aTicket = FxDefaultLogin.getUserTicket(sub);
                } catch (FxNotFoundException exc) {
                    LOG.error("No UserTicket found in Subject " + sub);
                    continue;
                }
                if (aTicket.getUserId() != userId) continue;
                if (applicationId != null && !aTicket.getApplicationId().equals(applicationId)) continue;
                remove.add(fullNodeName);
            }
        } catch (FxCacheException exc) {
            LOG.error("Failed to examine tickets: " + exc.getMessage(), exc);
            return 0;
        }
        // Clear matching nodes
        int count = 0;
        for (String node : remove) {
            try {
                cache.remove(node);
                count++;
            } catch (Exception exc) {
                LOG.error("Failed to remove node [" + node + "] for userid:" + userId, exc);
            }
        }
        // Return amount of deleted entries
        return count;
    }


    /**
     * Returns all UserTickets currently in the store.
     *
     * @return all UserTickets that are in the store
     */
    public static List<UserTicket> getTickets() {
        FxCacheMBean cache = CacheAdmin.getInstance();
        try {
            List<UserTicket> result = new ArrayList<UserTicket>(1000);
            Set aSet = cache.getChildrenNames(CacheAdmin.ROOT_USERTICKETSTORE);
            if (aSet == null) return new ArrayList<UserTicket>(0);
            UserTicketImpl aTicket;
            Subject sub;
            for (Object subPath : aSet) {
                sub = (Subject) cache.get(CacheAdmin.ROOT_USERTICKETSTORE + "/" + subPath, KEY_TICKET);
                if (sub == null) continue;
                try {
                    aTicket = (UserTicketImpl) FxDefaultLogin.getUserTicket(sub);
                } catch (FxNotFoundException exc) {
                    LOG.error("No UserTicket found in Subject " + sub, exc);
                    continue;
                }
                result.add(aTicket);
            }
            return result;
        } catch (FxCacheException exc) {
            LOG.error("Failed to examine tickets: " + exc.getMessage(), exc);
            return new ArrayList<UserTicket>(0);
        }
    }

}
