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
package com.flexive.tests.browser;

import com.thoughtworks.selenium.CommandProcessor;
import com.thoughtworks.selenium.DefaultSelenium;

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
}
