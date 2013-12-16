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
package com.flexive.tests.embedded;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.cache.FxCacheException;
import com.flexive.shared.configuration.DivisionData;
import com.flexive.shared.interfaces.StatelessTest;
import com.flexive.shared.structure.FxEnvironment;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for testing EJB transactions.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = "ejb")
public class TransactionTest {
    private static final String PATH = "/TransactionTest";

    /**
     * An object that's never equal to other objects.
     */
    private static class NeverEqualObject implements Serializable {
        private static final long serialVersionUID = -1938180841347334758L;

        @Override
        public boolean equals(Object obj) {
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    /**
     * An object that's always equal to other objects.
     */
    private static class AlwaysEqualObject implements Serializable {
        private static final long serialVersionUID = 238120062737195505L;

        @Override
        public boolean equals(Object obj) {
            return true;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    private static class GetEnvironmentThread implements Runnable {
        private final List<String> environmentIds;

        public GetEnvironmentThread(List<String> environmentIds) {
            this.environmentIds = environmentIds;
        }

        @Override
        public void run() {
            FxContext.get().setDivisionId(DivisionData.DIVISION_TEST);
            FxContext.get().setContextPath("flexiveTest");
            FxEnvironment environment = CacheAdmin.getEnvironment();
            synchronized (environmentIds) {
                environmentIds.add(environment.toString());
            }
        }

    }

    private StatelessTest statelessTest;

    @BeforeClass
    public void beforeTestClass() {
        statelessTest = EJBLookup.getStatelessTestInterface();
    }

    @Test
    public void testSimpleTransactionRollbackCache() throws FxCacheException {
        // create an object that's always unequal to other objects
        Object value = new NeverEqualObject();
        final String key = value.getClass().toString();
        CacheAdmin.getInstance().put(PATH, key, value);
        statelessTest.cachePutRollback(PATH, key, new NeverEqualObject());
        Assert.assertTrue(CacheAdmin.getInstance().get(PATH, key) == value,
                "Transaction rollback, but original object removed from cache: " + value
                + " (cached object = " + CacheAdmin.getInstance().get(PATH, key) + ")");
    }

    @Test
    public void testSimpleTransactionRollbackCacheEqual() throws FxCacheException {
        // create an object that's always equal to other objects
        Object value = new AlwaysEqualObject();
        final String key = value.getClass().toString();
        CacheAdmin.getInstance().put(PATH, key, value);
        statelessTest.cachePutRollback(PATH, key, new AlwaysEqualObject());
        Assert.assertTrue(CacheAdmin.getInstance().get(PATH, key) == value,
                "Transaction rollback, but original object removed from cache: " + value
                + " (cached object = " + CacheAdmin.getInstance().get(PATH, key) + ")");
    }

    @Test
    public void testMultithreadedEnvironment() {
        final int numThreads = 10;
        Thread[] threads = new Thread[numThreads];
        List<String> environmentIds = new ArrayList<String>();
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(new GetEnvironmentThread(environmentIds));
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                // ignore
            }
        }
        // assert that all environments are the same object
        FxEnvironment environment = CacheAdmin.getEnvironment();
        for (String id : environmentIds) {
            Assert.assertEquals(id, environment.toString(), "Thread got different environment instance: "
                    + id + " (main thread: " + environment.toString() + ")");
        }
    }

}
