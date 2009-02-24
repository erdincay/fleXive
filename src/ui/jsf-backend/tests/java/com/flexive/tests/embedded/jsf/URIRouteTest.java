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

import com.flexive.faces.*;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxRuntimeException;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Tests for the {@link com.flexive.faces.ContentURIRoute} class.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class URIRouteTest {
    @Test(groups="jsf")
    public void idContentUriTest() {
        final ContentURIRoute contactMapper = new ContentURIRoute("", "/contact/${id}.xhtml", "CONTACTDATA");
        assertEquals(contactMapper.getMappedUri(new FxPK(21, 1)), "/contact/21.xhtml");
        final URIMatcher contactMatcher = contactMapper.getMatcher("/contact/42.xhtml");
        assertEquals(contactMatcher.isMatched(), true);
        assertEquals(contactMatcher.getParameter("id"), "42");
    }

    @Test(groups="jsf")
    public void pkContentUriTest() {
        final ContentURIRoute contactMapper = new ContentURIRoute("", "/contact/${pk}/${id}.xhtml", "CONTACTDATA");
        assertEquals(contactMapper.getMappedUri(new FxPK(21, 5)), "/contact/21.5/21.xhtml");
        final ContentURIMatcher contactMatcher = contactMapper.getMatcher("/contact/21.5/21.xhtml");
        assertEquals(contactMatcher.isMatched(), true);
        assertEquals(contactMatcher.getParameter("id"), "21");
        assertEquals(contactMatcher.getId(), 21);
        assertEquals(FxPK.fromString(contactMatcher.getParameter("pk")), new FxPK(21, 5));
        assertEquals(contactMatcher.getPk(), new FxPK(21, 5));
    }

    @Test(groups="jsf")
    public void pkContentUriTest2() {
        final ContentURIRoute contactMapper = new ContentURIRoute("", "/contact/${pk}/${id}.xhtml", "CONTACTDATA");
        assertEquals(contactMapper.getMappedUri(new FxPK(21, FxPK.MAX)), "/contact/21.MAX/21.xhtml");
        final ContentURIMatcher contactMatcher = contactMapper.getMatcher("/contact/21.LIVE/21.xhtml");
        assertEquals(contactMatcher.isMatched(), true);
        assertEquals(contactMatcher.getParameter("id"), "21");
        assertEquals(contactMatcher.getId(), 21);
        assertEquals(FxPK.fromString(contactMatcher.getParameter("pk")), new FxPK(21, FxPK.LIVE));
        assertEquals(contactMatcher.getPk(), new FxPK(21, FxPK.LIVE));
    }

    @Test(groups="jsf")
    public void invalidContentUriTest() {
        for (String pattern: new String[] {
                "/contact/${id.xhtml", "/contact/${}.xhtml", "/contact/${  }.xhtml", "/contact/${unknown}.xhtml",
                "/contact/${id}.${id}.xhtml"
        }) {
            try {
                new ContentURIRoute("", pattern, "TEST");
                Assert.fail( "Pattern " + pattern + " is invalid, but ContentURIMapper constructor succeeds.");
            } catch (FxRuntimeException e) {
                // pass
            } 
        }
    }

    private static class TestUriRoute extends URIRoute {
        static {
            PARAMETERS.put("param1", "(v\\d)");
            PARAMETERS.put("param2", "(w\\d)");
        }
        private TestUriRoute(String format) {
            super("", format);
        }
    }

    @Test(groups="jsf")
    public void basicUriMapperTest() {
        final URIRoute route = new TestUriRoute("/${param1}/misc/${param2}.xhtml");
        final HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("param1", "v1");
        parameters.put("param2", "w2");
        assertEquals(route.getMappedUri(parameters), "/v1/misc/w2.xhtml");
        parameters.put("param3", "ignored");
        assertEquals(route.getMappedUri(parameters), "/v1/misc/w2.xhtml");
        parameters.remove("param1");
        try {
            route.getMappedUri(parameters);
            Assert.fail( "Not all parameters specified");
        } catch (FxRuntimeException e) {
            // pass
        }
        final URIMatcher matcher = route.getMatcher("/v8/misc/w3.xhtml");
        assertTrue(matcher.isMatched());
        assertEquals(matcher.getParameter("param1"), "v8");
        assertEquals(matcher.getParameter("param2"), "w3");
    }

    @Test(groups="jsf")
    public void routeCollectionTest() {
        final ContentURIRoute contactRoute = new ContentURIRoute("/contact.xhtml", "/c${id}.xhtml", "CONTACTDATA");
        final ContentURIRoute folderRoute = new ContentURIRoute("/folder.xhtml", "/f${id}.xhtml", "FOLDER");
        final URIRouteCollection<ContentURIRoute> coll = new URIRouteCollection<ContentURIRoute>(Arrays.asList(contactRoute, folderRoute));
        assertEquals(coll.findForTarget("/contact.xhtml"), contactRoute);
        assertEquals(coll.findForTarget("/folder.xhtml"), folderRoute);
        assertTrue(coll.getMatcher("/c25.xhtml").isMatched());
        assertEquals(coll.findForUri("/c25.xhtml"), contactRoute);
        assertEquals(coll.findForUri("/f25.xhtml"), folderRoute);
    }
}
