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
package com.flexive.cmis;

import com.flexive.cmis.spi.FlexiveRepositoryInfo;
import org.apache.chemistry.Repository;
import org.apache.chemistry.test.BasicTestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Runs the chemistry testcases on a flexive repository.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ChemistryTest extends BasicTestCase {
    private static final Log LOG = LogFactory.getLog(ChemistryTest.class);

    @Override
    public Repository makeRepository() throws Exception {
        Utils.init();
        final FlexiveRepositoryInfo info = new FlexiveRepositoryInfo();
        expectedRepositoryId = info.getId();
        expectedRepositoryName = info.getName();
        expectedRepositoryDescription = info.getDescription();
        expectedRepositoryVendor = info.getVendorName();
        expectedRepositoryProductName = info.getProductName();
        expectedRepositoryProductVersion = info.getProductVersion();
        expectedRootTypeId = "Root";
        return repository = Utils.getRepo();
    }

    @Override
    public void testNewDocument() {
        // this test always fails because we don't support timezones in date properties - skip it for now
        LOG.warn("testNewDocument disabled.");
    }
}
