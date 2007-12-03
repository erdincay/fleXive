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
package com.flexive.war.beans.cms;

import com.flexive.faces.FxJsfUtils;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.tree.FxTemplateInfo;

import java.util.Hashtable;

public class ContentProviderBean extends Hashtable<Object, Object> {

    FxContent content;

    public ContentProviderBean() {
        FxTemplateInfo tf = FxJsfUtils.getRequest().getTemplateInfo();
        if (tf != null) {
            long contentId = tf.getContentId();
            if (contentId != -1) {
                try {
                    //FxEnvironment environment = CacheAdmin.getEnvironment();
                    FxPK pk = new FxPK(contentId);
                    ContentEngine co = EJBLookup.getContentEngine();
                    content = co.load(pk);
                } catch (Throwable t) {
                    System.err.println("Failed to load the content: " + t.getMessage());
                }
            }
        }
    }


    public Object get(Object xpath) {
        String sXpath = String.valueOf(xpath).toUpperCase();
        if (sXpath.equals("@PK")) {
            return content.getPk();
        }
        if (sXpath.equals("/")) {
            return content.getRootGroup();
        }
        try {
            // Simple addressing
            if (!sXpath.contains("/")) {
                sXpath = "/" + sXpath;
            }
            return String.valueOf(content.getValue(sXpath).getBestTranslation());
        } catch (Exception exc) {
            return exc.getLocalizedMessage();
        }
    }
}
