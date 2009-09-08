/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2009
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
package com.flexive.shared;

import com.flexive.shared.content.FxPK;
import com.flexive.shared.security.UserTicket;

import java.io.Serializable;
import java.util.Date;

/**
 * Lock for contents or resources in general
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxLock implements Serializable {
    private static final long serialVersionUID = 7452575469399904087L;

    private FxLockType lockType;
    private long created;
    private Date createdDate = null;
    private long expires;
    private Date expiresDate = null;
    private long userId;
    private String lockedResource;
    private FxPK lockedPK;

    /**
     * Ctor for a content lock
     *
     * @param lockType type of the lock
     * @param created  when was the lock created
     * @param expires  when does the lock expire
     * @param userId   user id that issued the lock
     * @param lockedPK FxPK of the locked content instance
     */
    public FxLock(FxLockType lockType, long created, long expires, long userId, FxPK lockedPK) {
        this.lockType = lockType;
        this.created = created;
        this.expires = expires;
        this.userId = userId;
        this.lockedPK = lockedPK;
        this.lockedResource = null;
    }

    /**
     * Ctor for a resource lock
     *
     * @param lockType       type of the lock
     * @param created        when was the lock created
     * @param expires        when does the lock expire
     * @param userId         user id that issued the lock
     * @param lockedResource unique name of the locked resource
     */
    public FxLock(FxLockType lockType, long created, long expires, long userId, String lockedResource) {
        this.lockType = lockType;
        this.created = created;
        this.expires = expires;
        this.userId = userId;
        this.lockedResource = lockedResource;
        this.lockedPK = null;
    }

    /**
     * Ctor that decides locked resource depending on class of <code>obj</code>
     *
     * @param lockType type of the lock
     * @param created  when was the lock created
     * @param expires  when does the lock expire
     * @param userId   user id that issued the lock
     * @param obj      resource to lock
     */
    public FxLock(FxLockType lockType, long created, long expires, long userId, Object obj) {
        this.lockType = lockType;
        this.created = created;
        this.expires = expires;
        this.userId = userId;
        if (obj instanceof FxPK)
            this.lockedPK = (FxPK) obj;
        else
            this.lockedResource = String.valueOf(obj);
    }

    /**
     * Getter for the lock type
     *
     * @return lock type
     */
    public FxLockType getLockType() {
        return lockType;
    }

    /**
     * Is this an actual lock?
     *
     * @return if this FxLock instance is an actual lock
     */
    public boolean isLocked() {
        return lockType != FxLockType.None;
    }

    /**
     * Getter for the user id that issued the lock
     *
     * @return user id that issued the lock
     */
    public long getUserId() {
        return userId;
    }

    /**
     * Is this lock for a content instance?
     *
     * @return if this lock is for a content instance
     */
    public boolean isContentLock() {
        return lockedPK != null;
    }

    /**
     * Is this lock for a generic resource?
     *
     * @return if this lock is for a generic resource
     */
    public boolean isResourceLock() {
        return lockedResource != null;
    }

    /**
     * Getter for the name of the locked resource
     *
     * @return name of the locked resource
     */
    public String getLockedResource() {
        return lockedResource;
    }

    /**
     * Getter for the FxPK of the locked content
     *
     * @return FxPK of the locked content
     */
    public FxPK getLockedPK() {
        return lockedPK;
    }

    /**
     * Get the timestamp when the lock was created
     *
     * @return timestamp when the lock was created
     */
    public long getCreatedTimestamp() {
        return created;
    }

    /**
     * Get the date when the lock was created
     *
     * @return date when the lock was created
     */
    public Date getCreatedDate() {
        if (createdDate == null)
            createdDate = new Date(created);
        return createdDate;
    }

    /**
     * Get the timestamp when the lock expires
     *
     * @return timestamp when the lock expires
     */
    public long getExpiresTimestamp() {
        return expires;
    }

    /**
     * Get the date when the lock expires
     *
     * @return date when the lock expires
     */
    public Date getExpiresDate() {
        if (expiresDate == null)
            expiresDate = new Date(expires);
        return expiresDate;
    }

    /**
     * Has this lock expired?
     *
     * @return lock expired
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expires || expires < created;
    }

    /**
     * Can this lock (probably) be unlocked by the calling user?
     *
     * @return if this lock can be unlocked by the calling user
     */
    public boolean isUnlockable() {
        final UserTicket ticket = FxContext.getUserTicket();
        return ticket.isGlobalSupervisor() || ticket.isMandatorSupervisor() || ticket.getUserId() == userId || lockType == FxLockType.Loose;
    }

    /**
     * Get the remaining duration of the lock
     *
     * @return remaining duration of the lock
     */
    public long getDuration() {
        return expires - System.currentTimeMillis();
    }

    final static FxLock NO_LOCK_PK = new FxLock(FxLockType.None, -1, -1, -1, new FxPK(-1, -1));
    final static FxLock NO_LOCK_RESOURCE = new FxLock(FxLockType.None, -1, -1, -1, "NONE");

    public static FxLock noLockPK() {
        return NO_LOCK_PK;
    }

    public static FxLock noLockResource() {
        return NO_LOCK_RESOURCE;
    }
}
