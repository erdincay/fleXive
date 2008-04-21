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
package com.flexive.tests.embedded.jsf.bean;

import com.flexive.faces.beans.MessageBean;
import com.flexive.shared.exceptions.FxAccountInUseException;
import com.flexive.shared.exceptions.FxLoginFailedException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.tests.embedded.TestUsers;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = "jsf")
public class MessageBeanTest {
    private static final String KEY_1 = "test.jsf.fxFacesMessage.1";
    private static final String MSG_1 = "Test message with one parameter: {0}";
    private MessageBean messageBean;

    @BeforeClass
    public void beforeClass() throws FxLoginFailedException, FxAccountInUseException {
        messageBean = new MessageBean();
        login(TestUsers.SUPERVISOR);
    }

    @AfterClass
    public void afterClass() throws FxLoginFailedException, FxAccountInUseException, FxLogoutFailedException {
        logout();
    }

    @Test
    public void getMessageNoArgs() {
        String message = (String) messageBean.get(KEY_1);
        assert MSG_1.equals(message) : "Expected: " + MSG_1 + ", got: " + message;
    }

    @Test
    public void getMessageArgsInt() {
        String message = (String) messageBean.get(KEY_1 + ",42");
        String expected = StringUtils.replace(MSG_1, "{0}", "42");
        assert expected.equals(message) : "Expected: " + expected + ", got: " + message;
    }

    @Test
    public void getMessageArgsString() {
        String message = (String) messageBean.get(KEY_1 + ",random,string");
        String expected = StringUtils.replace(MSG_1, "{0}", "random");
        assert expected.equals(message) : "Expected: " + expected + ", got: " + message;
    }

    @Test
    public void getMessageArgsEscapedString() {
        String message = (String) messageBean.get(KEY_1 + ",'random string, with comma'");
        String expected = StringUtils.replace(MSG_1, "{0}", "random string, with comma");
        assert expected.equals(message) : "Expected: " + expected + ", got: " + message;
    }

    @Test
    public void getMessageArgsElInt() {
        String message = (String) messageBean.get(KEY_1 + ",#{41+1}");
        String expected = StringUtils.replace(MSG_1, "{0}", "42");
        assert expected.equals(message) : "Expected: " + expected + ", got: " + message;
    }

    @Test
    public void getMessageArgsElString() {
        String message = (String) messageBean.get(KEY_1 + ",#{'some string value'}");
        String expected = StringUtils.replace(MSG_1, "{0}", "some string value");
        assert expected.equals(message) : "Expected: " + expected + ", got: " + message;
    }

    @Test
    public void getMessageArgsElStringWithComma() {
        String message = (String) messageBean.get(KEY_1 + ",#{'some string value, with comma'}");
        String expected = StringUtils.replace(MSG_1, "{0}", "some string value, with comma");
        assert expected.equals(message) : "Expected: " + expected + ", got: " + message;
    }

}
