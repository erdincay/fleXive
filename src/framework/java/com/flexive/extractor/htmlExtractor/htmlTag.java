/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.extractor.htmlExtractor;

import java.util.Hashtable;

/**
 * HTML Text Extractor, html tag storage.
 * Part of the fleXive 3.X Framework
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
class htmlTag {

    private String name;
    Hashtable keyValuePairs;
    HtmlExtractor parent;


    protected htmlTag(HtmlExtractor parent,String name) {
        this.name = name.trim().toUpperCase();
        this.keyValuePairs = new Hashtable(5);
        this.parent = parent;
    }

    protected void add(Token key,StringBuffer value) {
        String attribute = key.image.toUpperCase().trim();
        String svalue = value.toString();
        // Append interresting contents to the extracted string
        if (attribute.equals("TITLE") || attribute.equals("ALT")) {
            this.parent.appendTagText(svalue);
        }
        // Remember key values
        //noinspection unchecked
        keyValuePairs.put(attribute,svalue);
    }

    protected void close() {
        if (name.equals("META")) {
            String metaName = (String)keyValuePairs.get("NAME");
            String metaContent = (String)keyValuePairs.get("CONTENT");
            String httpEquiv = (String)keyValuePairs.get("HTTP-EQUIV");
            if (metaContent!=null) {
                metaContent = metaContent.trim();
                if (httpEquiv!=null) {
                    if (httpEquiv.equalsIgnoreCase("created")) {
                        parent.addMeta(HtmlExtractor.META_CREATED,metaContent);
                    } else if (httpEquiv.equalsIgnoreCase("last_modified")) {
                        parent.addMeta(HtmlExtractor.META_LAST_MODIFIED,metaContent);
                    }
                } else if (metaName!=null) {
                    //noinspection ForLoopReplaceableByForEach
                    for (int i=0;i<HtmlExtractor.META_TAGS.length;i++) {
                        String knownMetaTag=HtmlExtractor.META_TAGS[i];
                        if (knownMetaTag.equalsIgnoreCase(metaName) ||
                                knownMetaTag.equalsIgnoreCase("DC."+metaName) /*Dublin Core Format*/) {
                            parent.addMeta(knownMetaTag,metaContent);
                        }
                    }
                }
            }
        }
    }

}
