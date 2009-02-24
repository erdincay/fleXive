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
package com.flexive.tests.embedded;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.cache.FxCacheException;
import com.flexive.shared.mbeans.FxCacheMBean;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.util.Set;

/**
 * Tree cache tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "cache"})
public class CacheTest {
    private static final String PATH_TEST = "/unitTests/" + CacheTest.class;

    @Test
    public void testGlobalPutGetRemove() throws FxCacheException {
        FxCacheMBean cache = CacheAdmin.getInstance();
        Assert.assertTrue(!cache.globalExists(PATH_TEST, 0), PATH_TEST + " not empty");
        for (int i = 0; i < 100; i++) {
            cache.globalPut(PATH_TEST, i, "Test value " + i);
            Assert.assertTrue(cache.globalExists(PATH_TEST, i));
        }
        Set keys = cache.globalGetKeys(PATH_TEST);
        Assert.assertTrue(keys.size() == 100);
        for (int i = 0; i < 100; i++) {
            Assert.assertTrue(keys.contains(i), "Key " + i + " not returned by globalGetKeys.");
            Assert.assertTrue(("Test value " + i).equals(cache.globalGet(PATH_TEST, i)), "Got unexpected cache value: " + cache.globalGet(PATH_TEST, i));
            cache.globalPut(PATH_TEST, i, "Test: " + i);
            Assert.assertTrue(cache.globalExists(PATH_TEST, i));
            Assert.assertTrue(("Test: " + i).equals(cache.globalGet(PATH_TEST, i)), "Got unexpected cache value: " + cache.globalGet(PATH_TEST, i));
            cache.globalRemove(PATH_TEST, i);
            Assert.assertTrue(!cache.globalExists(PATH_TEST, i), "Removed key still exists: " + i);
        }
        cache.globalPut(PATH_TEST, 0, "Test value");
        cache.globalPut(PATH_TEST, 1, "Test value");
        cache.globalRemove(PATH_TEST);
        Assert.assertTrue(!cache.globalExists(PATH_TEST, 0), "Global cache remove did not clear key 0");
        Assert.assertTrue(!cache.globalExists(PATH_TEST, 1), "Global cache remove did not clear key 1");
    }

    @Test
    public void testPutGetRemove() throws FxCacheException {
        FxCacheMBean cache = CacheAdmin.getInstance();
        Assert.assertTrue(!cache.exists(PATH_TEST, 0), PATH_TEST + " not empty");
        for (int i = 0; i < 100; i++) {
            cache.put(PATH_TEST, i, "Test value " + i);
            Assert.assertTrue(cache.exists(PATH_TEST, i));
        }
        Set keys = cache.getKeys(PATH_TEST);
        Assert.assertTrue(keys.size() == 100);
        for (int i = 0; i < 100; i++) {
            Assert.assertTrue(keys.contains(i), "Key " + i + " not returned by getKeys.");
            Assert.assertTrue(("Test value " + i).equals(cache.get(PATH_TEST, i)), "Got unexpected cache value: " + cache.get(PATH_TEST, i));
            cache.put(PATH_TEST, i, "Test: " + i);
            Assert.assertTrue(cache.exists(PATH_TEST, i));
            Assert.assertTrue(("Test: " + i).equals(cache.get(PATH_TEST, i)), "Got unexpected cache value: " + cache.get(PATH_TEST, i));
            cache.remove(PATH_TEST, i);
            Assert.assertTrue(!cache.exists(PATH_TEST, i), "Removed key still exists: " + i);
        }
        cache.put(PATH_TEST, 0, "Test value");
        cache.put(PATH_TEST, 1, "Test value");
        cache.remove(PATH_TEST);
        Assert.assertTrue(!cache.exists(PATH_TEST, 0), "Cache remove did not clear key 0");
        Assert.assertTrue(!cache.exists(PATH_TEST, 1), "Cache remove did not clear key 1");
    }

    @Test
    public void testAttributes() {
        FxCacheMBean cache = CacheAdmin.getInstance();
        Assert.assertTrue(StringUtils.isNotBlank(cache.getDeploymentId()), "Deployment ID not set.");
        Assert.assertTrue(cache.getSystemStartTime() < System.currentTimeMillis()
                && cache.getSystemStartTime() > System.currentTimeMillis() - 60 * 3600 * 1000
                , "System start time in the future or more than one hour in the past: " + cache.getSystemStartTime());
        Assert.assertTrue(cache.getNodeStartTime() < System.currentTimeMillis()
                && cache.getNodeStartTime() > System.currentTimeMillis() - 60 * 3600 * 1000
                , "Node start time in the future or more than one hour in the past: " + cache.getNodeStartTime());
        Assert.assertTrue(cache.getSystemStartTime() <= cache.getNodeStartTime(), "System start time later than node start time.");
    }
}
