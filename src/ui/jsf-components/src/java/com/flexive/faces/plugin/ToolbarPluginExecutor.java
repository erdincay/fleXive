/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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

import com.flexive.faces.beans.MessageBean;
import org.apache.commons.lang.StringUtils;

/**
 * <p>
 * PluginExecutor that allows to add buttons to the backend administration toolbars. Pages
 * are identified by their URI relative to the administration application root (/adm).
 * For example:
 * </p>
 * <table>
 * <tr>
 * <th>Target</th>
 * <th>Matches</th>
 * </tr>
 * <tr>
 * <td>search/query.xhtml</td>
 * <td>/adm/search/query.xhtml</td>
 * </tr>
 * <tr>
 * <td>search/q*.xhtml</td>
 * <td>/adm/search/query.xhtml</td>
 * </tr>
 * <tr>
 * <td>search/*</td>
 * <td>/adm/search/query.xhtml<br/>
 * /adm/search/resultPreferences.xhtml
 * </td>
 * </tr>
 * <tr>
 * <td>*</td>
 * <td>every admin page (not recommended)</td>
 * </tr>
 * </table>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public interface ToolbarPluginExecutor extends PluginExecutor {

    /**
     * Add the given toolbar button to all pages matching the given target.
     *
     * @param target the target URL. Look at the {@link ToolbarPluginExecutor} Javadoc for examples.
     * @param button the button to be added.
     */
    void addToolbarButton(String target, Button button);

    /**
     * Add a separator button
     */
    void addToolbarSeparatorButton();

    String SEPARATOR_ID = "@@separator@@";

    /**
     * A toolbar separator
     */
    Button SEPARATOR = new Button(SEPARATOR_ID);


    /**
     * Contains a definition of a single toolbar button. Currently you must either define
     * a concrete JSF action outcome in "action", or supply a EL-binding for "beans" and
     * the name of a method in "action". Javascript or absolute URI requests are currently
     * not supported.
     * The url of the icon to be displayed in the toolbar can be set via <code>setIconUrl(String iconUrl)</code>
     * (i.e /flexive/myImages/myIcon.png).
     * Alternatively as shorthand, the icon can be set via <code>setIcon(String iconName)</code> which
     * is mapped to the url adm/images/toolbar/iconName.png.
     */
    public static class Button {
        private final String id;
        private String label;
        private String bean;
        private String action;
        private String icon;
        private String iconUrl;
        private boolean separator;

        public Button(String id) {
            this.id = id;
            this.separator = SEPARATOR_ID.equals(id);
        }

        public boolean isValid() {
            return StringUtils.isNotBlank(label) && StringUtils.isNotBlank(action);
        }

        public Button setLabel(String label) {
            this.label = label;
            return this;
        }

        public Button setLabelKey(String labelKey) {
            this.label = (String) MessageBean.getInstance().get(labelKey);
            return this;
        }

        public Button setBean(String bean) {
            this.bean = bean;
            return this;
        }

        public Button setAction(String action) {
            this.action = action;
            return this;
        }

        public Button setIcon(String icon) {
            this.icon = icon;
            return this;
        }

        public Button setIconUrl(String iconUrl) {
            this.iconUrl = iconUrl;
            return this;
        }

        public String getIconUrl() {
            return iconUrl;
        }

        public String getLabel() {
            return label;
        }

        public String getBean() {
            return bean;
        }

        public String getAction() {
            return action;
        }

        public String getIcon() {
            return icon;
        }

        public String getId() {
            return id;
        }

        public boolean isSeparator() {
            return separator;
        }
    }
}
