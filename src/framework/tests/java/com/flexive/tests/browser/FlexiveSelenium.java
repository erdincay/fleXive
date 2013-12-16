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
package com.flexive.tests.browser;

import com.thoughtworks.selenium.CommandProcessor;
import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.SeleniumException;

/**
 * Flexive-specific extensions to the DefaultSelenium class.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FlexiveSelenium extends DefaultSelenium {
    /**
     * A simple context menu wrapper.
     */
    public class ContextMenu {
        private final String id;

        /**
         * Define a context menu.
         *
         * @param id the context menu widget ID, e.g. "frm:searchResultMenu"
         */
        public ContextMenu(String id) {
            this.id = id;
        }

        public ContextMenu clickOption(String optionId) {
            FlexiveSelenium.this.click('"' + id + ':' + optionId + '"');
            return this;
        }

        /**
         * Open the context menu for the given element. The element (or any of its parents) must have
         * a oncontextmenu handler that opens the context menu.
         *
         * @param locator the element locator where the context menu should be opened
         * @return this
         */
        public ContextMenu openAt(String locator) {
            commandProcessor.doCommand("contextMenu", new String[]{locator});
            return this;
        }
    }

    public FlexiveSelenium(String serverHost, int serverPort, String browserStartCommand, String browserURL) {
        super(serverHost, serverPort, browserStartCommand, browserURL);
    }

    public FlexiveSelenium(CommandProcessor processor) {
        super(processor);
    }

    public ContextMenu getContextMenu(String id) {
        return new ContextMenu(id);
    }

    @Override
    public String getHtmlSource() {
        return getHTMLSource("<table>", "<table ", "</table>", "<tr>", "<tr ", "</tr>", "<td>", "<td ", "</td>", "<div>", "<div ", "</div>", "<input ", "</input>");
    }

    private String getHtmlSource_() {
        if (!new Throwable().getStackTrace()[1].toString().contains("getHTMLSource"))  {
            System.out.println("[WARNING] : use getHtmlSource(String ... reps) instead" );
        }
        String src = super.getHtmlSource();
        String tmpSrc;
        int trys = 10;
        int hash = src.hashCode();
        int tmpHash;
        while (trys-->0) {
            sleep(200);
            tmpSrc = super.getHtmlSource();
            tmpHash = tmpSrc.hashCode();
            if (hash == tmpHash)
                break;
            src = tmpSrc;
            hash = tmpHash;
        }

        return src;
    }

    public String getHTMLSource(String ... reps) {
        return AbstractBackendBrowserTest.correctHTML(getHtmlSource_(), reps);
    }

    @Override
    public void waitForPageToLoad(String sec) {
        try {
            super.waitForPageToLoad(sec);
        } catch (SeleniumException se) {
            if (se.getMessage().contains("Timed out after 30000ms")) {
                System.err.println("[ERROR] : " + se.getMessage() + " --> ignoring");
                return;
            }
            throw new SeleniumException("Location : \"" + super.getLocation() + "\" ==> " + se.getMessage());
        }
    }

    @Override
    public String getEval(String script) {
        try {
            return super.getEval(script);
        } catch (SeleniumException se) {
            System.err.println("[ERROR] : " + se.getMessage() + " @ \"" + script + "\"");
            throw se;
        }
    }

    private void sleep(int i) {
        try {
            Thread.sleep(i);
        } catch (InterruptedException e) {
            // ignore it...
        }
    }
}
