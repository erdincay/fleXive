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
package com.flexive.tests.embedded;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxAccountInUseException;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLoginFailedException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.tutorial.persistance.ContentTutorialExample;
import com.flexive.tutorial.persistance.StructureTutorialExample;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test the tutorial classes if the actually work
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "tutorial"})
public class TutorialTest {

    private StructureTutorialExample st;
    private ContentTutorialExample ct;

    @BeforeClass
    public void prepare() throws FxLoginFailedException, FxAccountInUseException {
        st = new StructureTutorialExample();
        login(TestUsers.SUPERVISOR);
    }

    @AfterClass
    public void shutdown() throws FxLogoutFailedException {
        logout();
    }

    @Test
    public void tutorial() throws FxApplicationException {
        st.createType();
        long typeId = CacheAdmin.getEnvironment().getType(StructureTutorialExample.BASE_TYPE).getId();
        st.addNameProperty(typeId);
        st.reuseCaptionProperty(typeId);
        st.createAddressGroup(typeId);
        st.reuseGroup();
        st.deriveType();
        st.createProducts();
        st.relation();
        try {
            ct = new ContentTutorialExample();
            FxPK pk = ct.createInstance();
            pk = ct.createVersion(pk);
            ct.changeWorkflowStep(pk);
            ct.addDeliveryAddress(pk);
            ct.changeDeliveryAddress(pk);
            ct.relate();
        } finally {
            st.cleanUp();
            st.deleteCustomer();
        }
    }

}
