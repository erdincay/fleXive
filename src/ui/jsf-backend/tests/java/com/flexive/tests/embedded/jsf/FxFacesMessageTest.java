/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
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
package com.flexive.tests.embedded.jsf;

import com.flexive.faces.messages.FxFacesMessage;
import com.flexive.shared.exceptions.FxAccountInUseException;
import com.flexive.shared.exceptions.FxLoginFailedException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.tests.embedded.TestUsers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * FxFacesMessage tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"jsf"})
public class FxFacesMessageTest {

    private static final String MSG_TEST_1 = "test.jsf.fxFacesMessage.1";

    @BeforeClass
    public void beforeClass() throws FxLoginFailedException, FxAccountInUseException {
        login(TestUsers.SUPERVISOR);
    }

    @AfterClass
    public void afterClass() throws FxLoginFailedException, FxAccountInUseException, FxLogoutFailedException {
        logout();
    }

    @Test
    public void equalsNullDetails() {
        FxFacesMessage message1 = new FxFacesMessage(FxFacesMessage.SEVERITY_ERROR, MSG_TEST_1);
        FxFacesMessage message2 = new FxFacesMessage(FxFacesMessage.SEVERITY_ERROR, MSG_TEST_1);
        assert message1.equals(message2) : "Messages not equal.";
        assert message1.hashCode() == message2.hashCode() : "Hashcodes not equal";
    }

    @Test
    public void unequalsArgs() {
        FxFacesMessage message1 = new FxFacesMessage(FxFacesMessage.SEVERITY_ERROR, MSG_TEST_1, 21);
        FxFacesMessage message2 = new FxFacesMessage(FxFacesMessage.SEVERITY_ERROR, MSG_TEST_1, 22);
        assert !message1.equals(message2) : "Messages with different object args considered equal."
                + "\nMessage 1: " + message1.getSummary() + ", Message 2: " + message2.getSummary();
        assert message1.hashCode() != message2.hashCode() : "Different messages should have different hash codes";
    }

    @Test
    public void equalsArgs() {
        FxFacesMessage message1 = new FxFacesMessage(FxFacesMessage.SEVERITY_ERROR, MSG_TEST_1, 21);
        FxFacesMessage message2 = new FxFacesMessage(FxFacesMessage.SEVERITY_ERROR, MSG_TEST_1, 21);
        assert message1.equals(message2) : "Messages with same object args considered unequal."
                + "\nMessage 1: " + message1.getSummary() + ", Message 2: " + message2.getSummary();
        assert message1.hashCode() == message2.hashCode() : "Hashcodes not equal";
    }
}
