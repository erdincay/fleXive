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
package com.flexive.war.beans.admin.main;

import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.interfaces.TemplateEngine;
import com.flexive.shared.tree.FxTemplateInfo;
import com.flexive.shared.tree.FxTreeMode;

import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.List;

public class ContentTemplateBean {

    private TemplateEngine template = null;
    private FxTemplateInfo info = null;
    private String content;
    private boolean inUse = false;

    protected String getOverviewPage() {
        return "contentTemplateOverview";
    }

    protected String getEditPage() {
        return "contentTemplateEdit";
    }

    public TemplateEngine.Type getTemplateType() {
        return TemplateEngine.Type.CONTENT;
    }

    public ContentTemplateBean() {
        template = EJBLookup.getTemplateEngine();
    }

    public boolean getIsInUse() {
        return inUse;
    }

    public List<SelectItem> getTypeOptions() {
        ArrayList<SelectItem> options = new ArrayList<SelectItem>(3);
        options.add(new SelectItem("html", "html"));
        options.add(new SelectItem("jsf", "jsf"));
        options.add(new SelectItem("txt", "txt"));
        options.add(new SelectItem("xhtml", "xhtml"));
        options.add(new SelectItem("jsp", "jsp"));
        return options;
    }

    public List<FxTemplateInfo> getList() {
        try {
            return template.list(getTemplateType());
        } catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
            return new ArrayList<FxTemplateInfo>(0);
        }
    }

    public boolean isNew() {
        return info == null || info.getId() == -1;
    }


    public FxTemplateInfo getInfo() {
        if (info == null) info = new FxTemplateInfo(getTemplateType(), false, false, false); // TODO
        return info;
    }

    public void setInfo(FxTemplateInfo info) {
        this.info = info;
    }

    public String gotoNewScreen() {
        return getEditPage();
    }

    public String create() {
        try {
            long id = template.create(info.getName(), getTemplateType(), info.getContentType(), content);
            info.setId(id);
            return load();
        } catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
            return getEditPage();
        }
    }

    public String load() {
        try {
            this.info = template.getInfo(this.getInfo().getId(), FxTreeMode.Edit);      // TODO: LIVE/EDIT
            this.inUse = template.templateIsReferenced(this.getInfo().getId());
            if (this.info != null) {
                this.content = template.getContent(info.getId(), FxTreeMode.Edit);  // TODO: LIVE/EDIT
            }
            return getEditPage();
        } catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
            return getOverviewPage();
        }
    }

    public String save() {
        try {
            template.setContent(info.getId(), content, info.getContentType(), FxTreeMode.Edit);  // TODO
            template.setName(info.getId(), info.getName());
            return load();
        } catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
            return getEditPage();
        }
    }


    public String activate() {
        try {
            template.activate(getInfo().getId());
        } catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
        return null;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
