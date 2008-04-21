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

import com.flexive.shared.ContentLinkFormatter;
import com.flexive.shared.content.FxPK;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Basic tests for the content link mapper.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups="jsf")
public class ContentLinkFormatterTest {
    @Test
    public void basicIdUriTest() {
        Assert.assertEquals(ContentLinkFormatter.getInstance().format("/content/%{id}.html", new FxPK(42, 1)),
                "/content/42.html");
        Assert.assertEquals(ContentLinkFormatter.getInstance().format("http://www.test.com/%{id}/", new FxPK(10, 1)),
                "http://www.test.com/10/");
    }

    @Test
    public void basicVersionUriTest() {
        Assert.assertEquals(ContentLinkFormatter.getInstance().format("/content/version/%{version}.html", new FxPK(-1, 10)),
                "/content/version/10.html");
    }

    @Test
    public void basicPkUriTest() {
        Assert.assertEquals(ContentLinkFormatter.getInstance().format("/content/%{pk}", new FxPK(42, 10)),
                "/content/42.10");
    }
}
