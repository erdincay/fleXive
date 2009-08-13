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
package com.flexive.tests.embedded.persistence;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxLock;
import com.flexive.shared.FxLockType;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.structure.FxType;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.tests.embedded.TestUsers;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for locks
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "content", "lock"})
public class LockTest {

    @BeforeClass
    public void setup() throws FxApplicationException, FxLoginFailedException, FxAccountInUseException {
    }

    @Test
    public void lockInstance() throws FxApplicationException, FxLoginFailedException, FxAccountInUseException, FxLogoutFailedException {
        login(TestUsers.SUPERVISOR);
        final ContentEngine ce = EJBLookup.getContentEngine();
        FxContent co = ce.initialize(FxType.FOLDER);
        co.randomize();
        FxPK pk = co.save().getPk();
        try {
            co = ce.load(pk);
            Assert.assertFalse(co.isLocked(), "New instance should not be locked");
            ce.lock(FxLockType.Permanent, pk);
            FxLock lock1 = ce.getLock(pk);
            Assert.assertTrue(lock1.isLocked(), "Expected instance to be locked");
            Assert.assertTrue(lock1.isContentLock(), "Expected a content lock");
            Assert.assertEquals(lock1.getLockedPK(), pk, "Expected same primary key");
            Assert.assertFalse(lock1.isResourceLock(), "No resource lock expected");
            Assert.assertFalse(lock1.isExpired(), "Lock should not be expired");
            FxLock lock2 = ce.load(pk).getLock();
            Assert.assertTrue(lock2.isLocked(), "Expected instance to be locked");
            Assert.assertTrue(lock2.isContentLock(), "Expected a content lock");
            Assert.assertEquals(lock2.getLockedPK(), pk, "Expected same primary key");
            Assert.assertFalse(lock2.isResourceLock(), "No resource lock expected");
            Assert.assertFalse(lock2.isExpired(), "Lock should not be expired");
        } finally {
            ce.remove(pk);
        }
        logout();
    }

    @Test
    public void takeOverLockedIstance() throws FxApplicationException, FxLoginFailedException, FxAccountInUseException, FxLogoutFailedException {
        login(TestUsers.REGULAR);
        final ContentEngine ce = EJBLookup.getContentEngine();
        FxContent co = ce.initialize(FxType.FOLDER);
        co.randomize();
        FxPK pk = co.save().getPk();
        try {
            FxLock currLock = ce.lock(FxLockType.Permanent, pk);
            co = ce.load(pk);
            Assert.assertEquals(co.getLock().getUserId(), TestUsers.REGULAR.getUserId());
            logout();
            login(TestUsers.REGULAR2);
            try {
                ce.takeOverLock(pk);
                Assert.fail("Lock should not be possible to be taken over from normal user");
            } catch (FxLockException e) {
                //expected
            }
            logout();
            login(TestUsers.MANDATOR_SUPERVISOR);
            try {
                FxLock toLock = ce.takeOverLock(pk);
                Assert.assertEquals(toLock.getUserId(), TestUsers.MANDATOR_SUPERVISOR.getUserId());
                Assert.assertEquals(toLock.getExpiresTimestamp(), currLock.getExpiresTimestamp());
            } catch (FxLockException e) {
                Assert.fail("Lock should be possible to be taken over from mandator supervisor");
            }
            logout();
            login(TestUsers.SUPERVISOR);
            try {
                FxLock toLock = ce.takeOverLock(currLock);
                Assert.assertEquals(toLock.getUserId(), TestUsers.SUPERVISOR.getUserId());
                Assert.assertEquals(toLock.getExpiresTimestamp(), currLock.getExpiresTimestamp());
            } catch (FxLockException e) {
                Assert.fail("Lock should be possible to be taken over from global supervisor");
            }
            logout();
            login(TestUsers.REGULAR2);
            try {
                ce.takeOverLock(pk);
                Assert.fail("Lock should not be possible to be taken over from normal user, even though he set the original lock");
            } catch (FxLockException e) {
                //expected
            }
            logout();
            login(TestUsers.MANDATOR_SUPERVISOR);
            FxContent co2 = ce.load(pk);
            Assert.assertTrue(co2.isLocked());
            ce.unlock(pk);
            co2 = ce.load(pk);
            Assert.assertFalse(co2.isLocked());
            Assert.assertEquals(co2.getLock().getLockType(), FxLockType.None);
            //now lock with a loose lock
            FxLock looseLock = ce.lock(FxLockType.Loose, pk);
            logout();
            login(TestUsers.REGULAR);
            try {
                FxLock lock2 = ce.takeOverLock(pk);
                Assert.assertEquals(lock2.getUserId(), TestUsers.REGULAR.getUserId());
            } catch (FxLockException e) {
                Assert.fail("Loose lock should be possible to take over!");
            }
            logout();
            login(TestUsers.REGULAR2);
            try {
                ce.unlock(pk);
                Assert.assertEquals(ce.getLock(pk).getLockType(), FxLockType.None);
            } catch (FxLockException e) {
                Assert.fail("Loose lock should be possible to be unlocked!");
            }
        } finally {
            logout();
            login(TestUsers.SUPERVISOR);
            ce.remove(pk);
        }
        logout();
    }

    /**
     * Test automatic lock removal when instances are removed
     *
     * @throws FxAccountInUseException on errors
     * @throws FxApplicationException  on errors
     * @throws FxLoginFailedException  on errors
     * @throws FxLogoutFailedException on errors
     */
    @Test
    public void lockRemove() throws FxApplicationException, FxLoginFailedException, FxAccountInUseException, FxLogoutFailedException {
        login(TestUsers.REGULAR);
        final ContentEngine ce = EJBLookup.getContentEngine();
        FxContent co = ce.initialize(FxType.FOLDER);
        co.randomize();
        FxPK pk = co.save().getPk();
        ce.lock(FxLockType.Loose, pk);
        ce.remove(pk); //should not throw exception since locks should be removed using cascaded deletes from the database
        logout();
    }

    @Test
    public void lockExtend() throws FxApplicationException, FxLoginFailedException, FxAccountInUseException, FxLogoutFailedException {
        login(TestUsers.REGULAR);
        final ContentEngine ce = EJBLookup.getContentEngine();
        FxContent co = ce.initialize(FxType.FOLDER);
        co.randomize();
        FxPK pk = co.save().getPk();
        FxLock lock = ce.lock(FxLockType.Loose, pk);
        FxLock ext = ce.extendLock(pk, 1000);
        Assert.assertEquals(lock.getExpiresTimestamp(), ext.getExpiresTimestamp() - 1000);
        logout();
        //extend by different user, should work on loose locks
        login(TestUsers.REGULAR2);
        FxLock ext2 = ce.extendLock(pk, 2000);
        Assert.assertEquals(ext.getExpiresTimestamp(), ext2.getExpiresTimestamp() - 2000);
        ce.unlock(pk);
        lock = ce.lock(FxLockType.Permanent, pk);
        ext = ce.extendLock(pk, 1000); //same user should be able to extend
        Assert.assertEquals(lock.getExpiresTimestamp(), ext.getExpiresTimestamp() - 1000);
        logout();
        login(TestUsers.REGULAR);
        try {
            ce.extendLock(pk, 2000);
            Assert.fail("Permanent lock should not be extendable!");
        } catch (FxLockException e) {
            //ok
        }
        ce.remove(pk); //even if locked by a different user, remove should be possible
        logout();
    }
}
