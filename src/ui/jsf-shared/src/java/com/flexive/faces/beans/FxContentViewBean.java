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
package com.flexive.faces.beans;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxData;
import com.flexive.shared.content.FxPK;
import org.apache.commons.lang.StringUtils;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.Map;

/**
 * Provides actions that may be useful for (not only) pages using
 * the {@link com.flexive.faces.components.content.FxContentView FxContentView} component.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxContentViewBean implements Serializable {
    private static final long serialVersionUID = 3463665467310989039L;
    /**
     * Request attribute key where the PK of the saved content will be stored on success.
     */
    public static final String REQUEST_PK = FxContentViewBean.class.getName() + ".PK";
    /**
     * Request attribute key where the content instance will be stored on success.
     */
    public static final String REQUEST_CONTENT = FxContentViewBean.class.getName() + ".CONTENT";
    /**
     * Request attribute for a boolean flag that is set to true when a new content
     * instance was created.
     */
    public static final String REQUEST_ISNEW = FxContentViewBean.class.getName() + ".ISNEW";
    private FxContent content;
    private String xpath;
    private String successMessage;
    private Map<String, String> propertyNameMapper;

    /**
     * Persist the instance stored in <code>fxContentViewBean.content</code> to the DB.
     *
     * @return <ul>
     *         <li><code>success</code> if the content could be saved successfully</li>
     *         <li><code>failure</code> if an error occured<li>
     *         </ul>
     */
    public String save() {
        try {
            final FxPK pk = EJBLookup.getContentEngine().save(content.copy());
            final Map<String, Object> requestMap = FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
            requestMap.put(REQUEST_PK, pk);
            requestMap.put(REQUEST_CONTENT, content);
            requestMap.put(REQUEST_ISNEW, content.getPk().getId() == -1);
            if (StringUtils.isNotBlank(successMessage)) {
                FxJsfUtils.addMessage(new FacesMessage(successMessage));
            }
            return "success";
        } catch (Exception e) {
            new FxFacesMsgErr(e).addToContext();
            return "failure";
        }
    }

    /**
     * Add an empty element or group for the given XPath.
     *
     * @return <ul>
     *         <li><code>success</code> if the element or group was added successfully</li>
     *         <li><code>failure</code> if an error occured<li>
     *         </ul>
     */
    public String add() {
        try {
            content.getGroupData(xpath).createNew(FxData.POSITION_BOTTOM);
            return "success";
        } catch (Exception e) {
            new FxFacesMsgErr(e).addToContext();
            return "failure";
        }
    }

    /**
     * Remove the given XPath from the content instance.
     *
     * @return <ul>
     *         <li><code>success</code> if the group or element was removed successfully</li>
     *         <li><code>failure</code> if an error occured<li>
     *         </ul>
     */
    public String remove() {
        try {
            content.remove(xpath);
            return "success";
        } catch (Exception e) {
            new FxFacesMsgErr(e).addToContext();
            return "failure";
        }
    }

    public FxContent getContent() {
        return content;
    }

    public void setContent(FxContent content) {
        this.content = content;
    }

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }

    /**
     * Returns only the property name of a given XPath.<br/>
     * E.g. #{fxContentView.propertyName['id']} => 'id',<br/>
     * #{fxContentView.propertyName['group/id']} => 'id'
     *
     * @return  the property name of a given XPath.
     */
    public Map<String, String> getPropertyName() {
        if (propertyNameMapper == null) {
            propertyNameMapper = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<String, String>() {
                @Override
                public String get(Object key) {
                    if (!(key instanceof String)) {
                        return null;
                    }
                    final String value = (String) key;
                    final int pos = value.lastIndexOf('/');
                    return pos != -1 ? value.substring(pos + 1) : value;
                }
            }, true);
        }
        return propertyNameMapper;
    }
}
