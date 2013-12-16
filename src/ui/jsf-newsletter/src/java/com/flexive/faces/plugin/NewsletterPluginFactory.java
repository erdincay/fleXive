/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2012
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

/**
 * PluginFactory for the Newsletter plugin
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev: 1295 $
 */
public class NewsletterPluginFactory implements PluginFactory {

    private static class NavigationMenuPlugin implements Plugin<TreePluginExecutor> {
        @Override
        public void apply(TreePluginExecutor executor) {
            final TreeNodeWriter.Node root = new TreeNodeWriter.Node(
                    "Newsletter",
                    "Newsletter",
                    TreeNodeWriter.Node.TITLE_CLASS_NODE,
                    TreeNodeWriter.Node.formatWebletIcon("newsletter_plugin.weblet", "/images/newsletter.png")
                    );
            final TreeNodeWriter.Node overview = new TreeNodeWriter.Node(
                    "NewsletterOverview",
                    "Overview",
                    TreeNodeWriter.Node.TITLE_CLASS_LEAF, "",
                    FxJsfUtils.getRequest().getContextPath() + "/com.flexive.faces.web/newsletter/nlOverview.jsf");

            executor.addNode(null, root);
            final TreeNodeWriter.Node newsletters = new TreeNodeWriter.Node(
                    "NewsletterMgmt",
                    "Newsletters",
                    TreeNodeWriter.Node.TITLE_CLASS_LEAF, "",
                    FxJsfUtils.getRequest().getContextPath() + "/com.flexive.faces.web/newsletter/nlNewsletters.jsf");
            final TreeNodeWriter.Node newslettersMSG = new TreeNodeWriter.Node(
                    "NewsletterMSG_Mgmt",
                    "Newsletters Messages",
                    TreeNodeWriter.Node.TITLE_CLASS_LEAF, "",
                    FxJsfUtils.getRequest().getContextPath() + "/com.flexive.faces.web/newsletter/nlNewsletterMSG.jsf");
            final TreeNodeWriter.Node newslettersNewMSG = new TreeNodeWriter.Node(
                    "NewsletterMSG_Mgm2t",
                    "Newsletters New Messages",
                    TreeNodeWriter.Node.TITLE_CLASS_LEAF, "",
                    FxJsfUtils.getRequest().getContextPath() + "/com.flexive.faces.web/newsletter/newMessage.jsf");
            executor.addNode("Newsletter", newsletters);
            executor.addNode("Newsletter", overview);
            executor.addNode("Newsletter", newslettersMSG);
            executor.addNode("Newsletter", newslettersNewMSG);
        }
    }

    @Override
    public void initialize(PluginRegistryBean registry) {
        registry.registerPlugin(
                AdmExtensionPoints.ADM_MAIN_NAVIGATION,
                new NavigationMenuPlugin()
        );

    }
}
