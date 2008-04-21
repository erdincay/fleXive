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
package com.flexive.shared.tree;

import com.flexive.shared.AbstractSelectableObjectWithName;
import com.flexive.shared.FxContext;
import com.flexive.shared.interfaces.TemplateEngine;

import java.io.Serializable;

/**
 * Information about a template
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxTemplateInfo  extends AbstractSelectableObjectWithName implements Serializable {
    private static final long serialVersionUID = -7704618161235339482L;

    private String contentType;
    private long modifiedAt;
    private long modifiedBy;
    private long id;
    private String name;
    private long typeId;
    private long contentId = -1;
    private Long masterTemplate;
    private long masterTemplateModifiedAt;
    private TemplateEngine.Type templateType;
    private long parentNode;
    private long tdef;
    private boolean isLive;
    private boolean hasLiveVersion;
    private boolean inSync;


    public FxTemplateInfo(TemplateEngine.Type templateType,boolean isLive,boolean hasLiveVersion,boolean isInSync) {
        this.id = -1;
        this.contentType = "";
        this.name = "NEW_"+System.currentTimeMillis();
        this.modifiedAt = System.currentTimeMillis();
        this.modifiedBy = FxContext.get().getTicket().getUserId();
        this.templateType = templateType;
        this.masterTemplate = null;
        this.parentNode=-1;
        this.masterTemplateModifiedAt = -1;
        this.typeId = -1;
        this.isLive =isLive;
        this.hasLiveVersion = hasLiveVersion;
        this.inSync=isInSync;
    }

    public FxTemplateInfo(long id,int typeId,String name,String type,long modifiedAt,long modifiedBy,long contentId,
                          TemplateEngine.Type templateType,Long masterTemplate,long masterTemplateModifiedAt,
                          boolean isLive,boolean hasLiveVersion,boolean isInSync) {
        this.contentType = type;
        this.id = id;
        this.name = name;
        this.typeId = typeId;
        this.modifiedAt = modifiedAt;
        this.modifiedBy = modifiedBy;
        this.contentId = contentId;
        this.isLive =isLive;
        this.templateType = templateType;
        this.masterTemplateModifiedAt = masterTemplateModifiedAt;
        this.masterTemplate = masterTemplate;
        this.parentNode=-1;
        this.hasLiveVersion = hasLiveVersion;
        this.inSync=isInSync;
    }


    /**
     * @return true when the live and edit version of the template are ident.
     */
    public boolean getIsInSync() {
        return inSync;
    }

    /**
     * @return true if this template has an live version
     */
    public boolean isHasLiveVersion() {
        return hasLiveVersion;
    }

    public boolean getIsLive() {
        return isLive;
    }

    public long getParentNode() {
        return parentNode;
    }

    public void setParentNode(long parentNode) {
        this.parentNode = parentNode;
    }

    public long getMasterTemplateModifiedAt() {
        return masterTemplateModifiedAt;
    }


    public long getTdef() {
        return tdef;
    }

    public void setTdef(long tdef) {
        this.tdef = tdef;
    }

    public Long getMasterTemplate() {
        return masterTemplate;
    }

    public boolean hasMasterTemplate() {
        return masterTemplate!=null && masterTemplate >0;
    }


    public long getTypeId() {
        return typeId;
    }

    public TemplateEngine.Type getTemplateType() {
        return templateType;
    }

    /**
     * Only set if the template info was looked up by a tree request.
     *
     * @return -1, or a content id
     */
    public long getContentId() {
        return contentId;
    }

    /**
     * Sets the content id.
     *
     * @param contentId the content id.
     */
    public void setContentId(long contentId) {
        this.contentId = contentId;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the content type id of the node.
     *
     * @return the content type id of the node.
     */
    public String getContentType() {
        return contentType;
    }


    public long getId() {
        return id;
    }

    public void setContentType(String type) {
        this.contentType = type;
    }

    public long getModifiedAt() {
        return modifiedAt;
    }

    public long getModifiedBy() {
        return modifiedBy;
    }


    public void setId(long id) {
        this.id = id;
    }


    public void setName(String name) {
        this.name = name;
    }

}


