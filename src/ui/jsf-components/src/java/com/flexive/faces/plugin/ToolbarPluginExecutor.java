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
package com.flexive.faces.plugin;

import com.flexive.faces.beans.MessageBean;
import org.apache.commons.lang.StringUtils;

/**
 * <p>
 * PluginExecutor that allows to add buttons to a toolbar. Pages
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
     * Contains a definition of a single toolbar button. Currently you must either define
     * a concrete JSF action outcome in "action", or supply a EL-binding for "beans" and
     * the name of a method in "action". Javascript or absolute URI requests are currently
     * not supported.
     */
    public static class Button {
        private final String id;
        private String label;
        private String bean;
        private String action;
        private String icon;

        public Button(String id) {
            this.id = id;
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
    }
}
