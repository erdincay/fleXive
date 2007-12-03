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
package com.flexive.war.beans.admin.tree;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.beans.ActionBean;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.interfaces.TemplateEngine;
import com.flexive.shared.interfaces.TreeEngine;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.tree.FxTemplateInfo;
import com.flexive.shared.tree.FxTemplateMapping;
import com.flexive.shared.tree.FxTreeMode;
import static com.flexive.shared.tree.FxTreeMode.Edit;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.List;

public class TreeNodeEditorBean implements ActionBean {
    private static final Log LOG = LogFactory.getLog(TreeNodeEditorBean.class);

    private long nodeId;
    private TemplateEngine templateEngine;
    private TreeEngine tree;
    public final static String PAGE_EDITNODE = "treeNodeEditor";
    private long contentType;
    private FxTemplateInfo template;
    private ArrayList<FxExtendedTemplateMapping> mappings;
    private List<FxType> unmappedTypes;
    private Long contentTypeId;
    private FxPK contentPk;

    public static class FxExtendedTemplateMapping extends FxTemplateMapping {
        private String contentTypeName;
        private String templateName;

        // TODO: What if template is NOT Live, but content goes live? warning?
        public FxExtendedTemplateMapping(Long contentType, long templateId, TemplateEngine te) throws FxApplicationException {
            super(contentType, templateId);
            this.contentTypeName = contentType == null ? "*" : CacheAdmin.getEnvironment().
                    getType(contentType).getDisplayName();
            this.templateName = te.getInfo(templateId, FxTreeMode.Edit).getName();
        }

        public FxExtendedTemplateMapping(FxTemplateMapping m, TemplateEngine te) throws FxApplicationException {
            super(m.getContentType(), m.getTemplateId());
            this.contentTypeName = m.getContentType() == null ? "*" : CacheAdmin.getEnvironment().
                    getType(m.getContentType()).getDisplayName();
            this.templateName = te.getInfo(m.getTemplateId(), FxTreeMode.Edit).getName();
        }

        public String getContentTypeName() {
            return contentTypeName;
        }

        public String getTemplateName() {
            return templateName;
        }
    }

    public String getParseRequestParameters() {
        try {
            String action = FxJsfUtils.getParameter("action");
            if (StringUtils.isBlank(action)) {
                // no action requested
                return null;
            }
            if ("editNode".equals(action)) {
                setNodeId(FxJsfUtils.getLongParameter("id"));
                load();
            }
            // hack!
            FxJsfUtils.resetFaceletsComponent("frm");
        } catch (Throwable t) {
            // TODO possibly pass some error message to the HTML page
            LOG.error("Failed to parse request parameters: " + t.getMessage(), t);
        }
        return null;
    }

    public FxTemplateInfo getTemplate() {
        return template;
    }

    public void setTemplate(FxTemplateInfo data) {
        this.template = data;
    }

    public long getContentType() {
        return contentType;
    }

    public void setContentType(long contentType) {
        this.contentType = contentType;
    }

    public Long getContentTypeId() {
        return contentTypeId;
    }

    public void setContentTypeId(Long contenTypeId) {
        this.contentTypeId = contenTypeId;
    }

    public void addTemplateMapping(ActionEvent event) {
        try {
            FxExtendedTemplateMapping mp = new FxExtendedTemplateMapping(contentType != -1 ? contentType : null,
                    template.getId(), templateEngine);
            mappings.add(mp);
        } catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        } finally {
            computeSettings();
            FxJsfUtils.resetFaceletsComponent("frm:mappings");
        }
    }

    public void removeTemplateMapping(ActionEvent event) {
        try {
            if (this.mappings == null) return;
            for (FxExtendedTemplateMapping m : mappings) {
                if (m.getContentType() == null && contentTypeId != null) continue;
                if ((m.getContentType() == null && contentTypeId == null) || m.getContentType().equals(contentTypeId)) {
                    mappings.remove(m);
                    break;
                }
            }
        } catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        } finally {
            computeSettings();
            FxJsfUtils.resetFaceletsComponent("frm:mappings");
        }
    }

    public List<SelectItem> getUnmappedTypes() {
        if (hasMappingForType(null)) {
            return FxJsfUtils.asSelectListWithLabel(unmappedTypes, false);
        } else {
            return FxJsfUtils.asSelectListWithLabel(unmappedTypes, true);
        }
    }

    public boolean getHasUnmappedTypes() {
        return unmappedTypes != null && unmappedTypes.size() > 0;
    }

    public void computeSettings() {
        List<FxType> types = CacheAdmin.getFilteredEnvironment().getTypes(true, true, true, false);
        // Fill unmapped types
        if (unmappedTypes != null) {
            unmappedTypes.clear();
        } else {
            unmappedTypes = new ArrayList<FxType>(types.size());
        }
        for (FxType t : types) {
            if (!t.getName().equalsIgnoreCase("ROOT") && !hasMappingForType(t)) {
                unmappedTypes.add(t);
            }
        }
        // Fill inherited
    }

    private boolean hasMappingForType(FxType type) {
        // General (all types) mapping
        if (type == null) {
            for (FxTemplateMapping m : mappings) {
                if (m.getContentType() == null) return true;
            }
            return false;
        }
        // All other mappings
        if (mappings == null) return false;
        for (FxTemplateMapping m : mappings) {
            if (m.getContentType() == null) continue;
            if (m.getContentType() == type.getId()) return true;
        }
        return false;
    }


    public List<FxExtendedTemplateMapping> getMappings() {
        return mappings == null ? new ArrayList<FxExtendedTemplateMapping>(0) : mappings;
    }


    public void setMappings(ArrayList<FxExtendedTemplateMapping> mappings) {
        this.mappings = mappings;
        computeSettings();
    }

    /**
     * Constructor.
     * <p/>
     * <p/>
     * Initializes the EJB interfaces
     */
    public TreeNodeEditorBean() {
        try {
            tree = EJBLookup.getTreeEngine();
            templateEngine = EJBLookup.getTemplateEngine();
        } catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }


    /**
     * Initializes the beans using the value of the nodeId variable.
     *
     * @return the next page to render (always the node editor)
     */
    public String load() {
        this.mappings = null;
        this.contentTypeId = null;
        this.contentType = -1;
        this.unmappedTypes = null;
        this.contentPk = null;
        try {
            List<FxTemplateMapping> mp = templateEngine.getTemplateMappings(nodeId, FxTreeMode.Edit);
            this.mappings = new ArrayList<FxExtendedTemplateMapping>(mp.size());
            for (FxTemplateMapping m : mp) {
                this.mappings.add(new FxExtendedTemplateMapping(m, templateEngine));
            }
            try {
                this.contentPk = tree.getNode(Edit, nodeId).getReference();
            } catch (Throwable t) {
                // ignore
            }
        } catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        } finally {
            computeSettings();
        }
        return PAGE_EDITNODE;
    }


    public long getNodeId() {
        return nodeId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    public String save() {
        try {
            ArrayList<FxTemplateMapping> mp = new ArrayList<FxTemplateMapping>(mappings == null ? 0 : mappings.size());
            if (mappings != null) {
                for (FxExtendedTemplateMapping m : mappings) {
                    mp.add(new FxTemplateMapping(m.getContentType(), m.getTemplateId()));
                }
                templateEngine.setTemplateMappings(nodeId, mp);
            }
        } catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
        return load();
    }

    public String gotoContentEditor() {
        return "contentEditor";
    }
}
