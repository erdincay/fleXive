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
package com.flexive.core.storage;

import com.flexive.shared.FxLock;
import com.flexive.shared.FxLockType;
import com.flexive.shared.exceptions.FxLockException;
import com.flexive.shared.content.FxPK;

import java.sql.Connection;
import java.util.List;

/**
 * Lock storage
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public interface LockStorage {
    /**
     * Lock an instance
     *
     * @param con      an open and valid connection
     * @param lockType type of the lock
     * @param pk       primary key
     * @return FxLock
     * @throws FxLockException on errors
     */
    FxLock lock(Connection con, FxLockType lockType, FxPK pk) throws FxLockException;

    /**
     * Lock an instance
     *
     * @param con      an open and valid connection
     * @param lockType type of the lock
     * @param pk       primary key
     * @param duration duration in [ms] of the lock
     * @return FxLock
     * @throws FxLockException on errors
     */
    FxLock lock(Connection con, FxLockType lockType, FxPK pk, long duration) throws FxLockException;

    /**
     * Lock a resource
     *
     * @param con      an open and valid connection
     * @param lockType type of the lock
     * @param resource name of the resource to lock
     * @return FxLock
     * @throws FxLockException on errors
     */
    FxLock lock(Connection con, FxLockType lockType, String resource) throws FxLockException;

    /**
     * Lock a resource
     *
     * @param con      an open and valid connection
     * @param lockType type of the lock
     * @param resource name of the resource to lock
     * @param duration duration in [ms] of the lock
     * @return FxLock
     * @throws FxLockException on errors
     */
    FxLock lock(Connection con, FxLockType lockType, String resource, long duration) throws FxLockException;

    /**
     * Get the lock for a primary key. If the instance is not locked a lock with <code>FxLockType.None</code> is returned
     *
     * @param con an open and valid connection
     * @param pk  primary key
     * @return FxLock or a lock with <code>FxLockType.None</code>
     * @throws FxLockException on errors
     */
    FxLock getLock(Connection con, FxPK pk) throws FxLockException;

    /**
     * Get the lock for a resource. If the resource is not locked a lock with <code>FxLockType.None</code> is returned
     *
     * @param con      an open and valid connection
     * @param resource name of the resource to get the lock for
     * @return FxLock or a lock with <code>FxLockType.None</code>
     * @throws FxLockException on errors
     */
    FxLock getLock(Connection con, String resource) throws FxLockException;

    /**
     * Unlock a locked instance. If the instance is not locked, no exception will be thrown
     *
     * @param con an open and valid connection
     * @param pk  primary key
     * @throws FxLockException on errors
     */
    void unlock(Connection con, FxPK pk) throws FxLockException;

    /**
     * Unlock a locked resource. If the resource is not locked, no exception will be thrown
     *
     * @param con      an open and valid connection
     * @param resource name of the resource to unlock
     * @throws FxLockException on errors
     */
    void unlock(Connection con, String resource) throws FxLockException;

    /**
     * Extend an existing lock for the given duration (duration will be added to current expire time)
     *
     * @param con      an open and valid connection
     * @param lock     the lock to extend
     * @param duration duration in [ms] to extend the original expire time
     * @return extended lock
     * @throws FxLockException on errors
     */
    FxLock extend(Connection con, FxLock lock, long duration) throws FxLockException;

    /**
     * Take over a lock held by another user (if permitted)
     *
     * @param con  an open and valid connection
     * @param lock the lock to take over
     * @return FxLock
     * @throws FxLockException on errors
     */
    FxLock takeOver(Connection con, FxLock lock) throws FxLockException;

    /**
     * Take over a lock held by another user (if permitted) and extend its duration
     *
     * @param con      an open and valid connection
     * @param lock     the lock to take over
     * @param duration duration in [ms] to extend the original expire time
     * @return FxLock
     * @throws FxLockException on errors
     */
    FxLock takeOver(Connection con, FxLock lock, long duration) throws FxLockException;

    /**
     * Get all locks held by the given user
     *
     * @param con    an open and valid connection
     * @param userId id of the user to get held locks for
     * @return list of all locks from the given user
     */
    List<FxLock> getUserLocks(Connection con, long userId);

    /**
     * Remove all locks that are held for a given type
     *
     * @param con    an open and valid connection
     * @param typeId id of the type
     * @throws FxLockException on errors
     */
    void unlockType(Connection con, long typeId) throws FxLockException;
}
