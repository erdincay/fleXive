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
package com.flexive.faces.plugin;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.beans.PluginRegistryBean;
import com.flexive.faces.javascript.tree.TreeNodeWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Registers a main menu entry for the plugin runner if flexive-plugins.jar is available.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class TestRunnerPluginFactory implements PluginFactory {
    private static final Log LOG = LogFactory.getLog(TestRunnerPluginFactory.class);

    @Override
    public void initialize(PluginRegistryBean registry) {
        try {
            Class.forName("com.flexive.tests.shared.FxValueTest");
            registry.registerPlugin(AdmExtensionPoints.ADM_MAIN_NAVIGATION,
                    new Plugin<TreePluginExecutor>() {
                        @Override
                        public void apply(TreePluginExecutor executor) {
                            executor.addNode("system", new TreeNodeWriter.Node("testRunnerNode",
                                    "Test Runner", TreeNodeWriter.Node.TITLE_CLASS_LEAF, "",
                                    FxJsfUtils.getRequest().getContextPath() + "/com.flexive.faces.web/testrunner/testRunner.jsf"));
                        }
                    });
        } catch (ClassNotFoundException e) {
            LOG.info("flexive-tests.jar not found - test runner disabled");
        }
    }
}
