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
package com.flexive.war.beans;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.beans.MessageBean;

import java.util.ArrayList;
import java.util.List;

/**
 * This beans handles the main navigation menu.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class NavigationMenuBean {
    private static List<Item> items = new ArrayList<Item>(10);

    static {
        addItem("Admin.tab.content", "adm/content.jsf", "adm/content/navigation.jsf", "contentNavigation", true);
        addItem("Admin.tab.structure", "adm/content.jsf", "adm/structure/navigation.jsf", "structureNavigation", true);
        addItem("Admin.tab.briefcase", "adm/main/briefcase/bcOverview.jsf", "adm/briefcase/navigation.jsf", "briefcaseNavigation", true);
        addItem("Admin.tab.admin", "adm/main/userGroup/overview.jsf", "adm/main/navigation.jsf", "mainNavigation", false);
        //addItem("menu4","content4.jsf","content/navigation.jsf","contentNavigation",false);
    }

    private int activeIdx = 0;


    /**
     * Items that are displayed as navigation.
     */
    public static class Item {

        private int id;
        private String target;
        private String display;
        private String navigation;
        private String navigationTarget;
        private boolean renderReloadButton;

        public Item(int id, String display, String target, String nav, String navigationTarget,
                    boolean renderReloadButton) {
            this.renderReloadButton = renderReloadButton;
            this.target = target;
            this.display = display;
            this.id = id;
            this.navigation = nav;
            this.navigationTarget = navigationTarget;
        }


        public boolean getRenderReloadButton() {
            return renderReloadButton;
        }

        public String getNavigationTarget() {
            return navigationTarget;
        }

        public void setNavigationTarget(String navigationTarget) {
            this.navigationTarget = navigationTarget;
        }

        public String getTarget() {
            return target;
        }

        public String getDisplay() {
            // auto-translate display label for the current user
            MessageBean messageBean = (MessageBean) FxJsfUtils.getManagedBean("fxMessageBean");
            return (String) messageBean.get(display);
        }

        public int getId() {
            return id;
        }


        public String getNavigation() {
            return navigation + "?activeIdx=" + id;
        }
    }

    public void setActiveIdx(int idx) {
        if (idx < 0 || (idx > items.size() - 1)) {
            idx = 0;
        }
        this.activeIdx = idx;
    }

    public int getActiveIdx() {
        return this.activeIdx;
    }

    public Item getActiveItem() {
        return items.get(activeIdx);
    }

    private static synchronized void addItem(String display, String target, String nav, String navigationTarget,
                                             boolean renderReloadButton) {
        items.add(new Item(items.size(), display, target, nav, navigationTarget, renderReloadButton));
    }


    public List getItems() {
        return items;
    }

    public List getTopItems() {
        // TODO add caching
        ArrayList<Item> result = new ArrayList<Item>(activeIdx + 1);
        for (int i = 0; i < activeIdx; i++) {
            result.add(items.get(i));
        }
        return result;
    }

    public List getBottomItems() {
        // TODO add caching
        ArrayList<Item> result = new ArrayList<Item>(items.size() - (activeIdx + 1));
        for (int i = (activeIdx + 1); i < items.size(); i++) {
            result.add(items.get(i));
        }
        return result;
    }

    public String gotoMenu(int pos) {
        setActiveIdx(pos);
        return "main";
    }

    public String toggleMenu() {
        return items.get(activeIdx).getNavigationTarget();
    }

}
