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
package com.flexive.faces.components.toolkit;

import com.flexive.faces.FxJsfComponentUtils;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxData;
import com.flexive.shared.content.FxGroupData;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.structure.FxAssignment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.value.FxReference;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxValue;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.event.FacesEvent;
import javax.faces.event.FacesListener;
import javax.faces.event.PhaseId;
import javax.faces.event.AbortProcessingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p/>
 * Provides an editable view of an FxContent instance. A hashmap for accessing
 * the properties is provided by the variable <code>#{var}</code>, the content instance
 * itself can be obtained using <code>#{var}_content</code>.
 * </p>
 * <p/>
 * For example:
 * <pre>
 * &lt;fx:content pk="#{myBean.articlePk}" var="article">
 *    Title: &lt;fx:value property="title"/>
 *    &lt;h:commandLink action="#{myBean.save}">
 *      &lt;f:setPropertyActionListener target="#{myBean.content}" value="#{article_content}"/>
 *      Save article
 *    &lt;/h:commandLink>
 * &lt;/fx:content>
 * </pre>
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxContentView extends UIOutput {
    private static final Log LOG = LogFactory.getLog(FxContentView.class);

    private FxPK pk;
    private long type = -1;
    private String typeName;
    private String var;
    private FxContent content;
    private Map<FxPK, FxContent> contentMap = new HashMap<FxPK, FxContent>();
    private boolean preserveContent = false;
    private boolean explode = true;

    public FxContentView() {
        setRendererType(null);
    }

    public static String getExpression(String var, String xpath, String suffix) {
        return "#{" + var + "['" + xpath + (StringUtils.isNotBlank(suffix) ? "$" + suffix : "") + "']}";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        provideContent(context);
    }

    public void provideContent(FacesContext context) {
        final Map requestMap = context.getExternalContext().getRequestMap();
        try {
            final FxPK pk = (FxPK) getPk();
            getContent();
            if (content == null || (pk != null && !content.matchesPk(pk))) {
                final ContentEngine contentInterface = EJBLookup.getContentEngine();
                if (pk == null || pk.isNew()) {
                    setContent(contentInterface.initialize(getSelectedType().getId()));
                } else {
                    setContent(contentInterface.load(pk));
                    if (explode) {
                        content.getRootGroup().explode(true);
                    }
                }
                contentMap.put(content.getPk(), content);
            }
            //noinspection unchecked
            requestMap.put(getVar(), new ContentMap(content));
            //noinspection unchecked
            requestMap.put(getVar() + "_content", content);
        } catch (FxApplicationException e) {
            requestMap.remove(getVar());
            LOG.error("Failed to provide content: " + e.getMessage(), e);
            throw e.asRuntimeException();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        removeContent(context);
    }

    private void removeContent(FacesContext context) {
        //context.getELContext().getVariableMapper().setVariable(getVar(), null);
        context.getExternalContext().getRequestMap().remove(getVar());
//        context.getExternalContext().getRequestMap().remove(getVar() + "_content");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processDecodes(FacesContext context) {
        provideContent(context);
        super.processDecodes(context);
        removeContent(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processValidators(FacesContext context) {
        provideContent(context);
        super.processValidators(context);
        removeContent(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processUpdates(FacesContext context) {
        provideContent(context);
        super.processUpdates(context);
        removeContent(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void broadcast(FacesEvent event) throws AbortProcessingException {
        if (event instanceof WrappedEvent) {
            // unwrap and provide content variable in the event context
            final FacesEvent target = ((WrappedEvent) event).event;
            provideContent(FacesContext.getCurrentInstance());
            target.getComponent().broadcast(target);
            removeContent(FacesContext.getCurrentInstance());
        } else {
            super.broadcast(event);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queueEvent(FacesEvent event) {
        super.queueEvent(new WrappedEvent(this, event));
    }

    public Object getPk() {
        final Object pkValue = FxJsfComponentUtils.getValue(this, "pk");
        if (pkValue != null) {
            return FxPK.fromObject(pkValue);
        }
        return pk;
    }

    public void setPk(Object pk) {
        this.pk = pk != null ? FxPK.fromObject(pk) : null;
    }

    public String getVar() {
        if (var == null) {
            var = FxJsfComponentUtils.getStringValue(this, "var");
        }
        return var;
    }

    public void setVar(String var) {
        this.var = var;
    }

    public long getType() {
        if (type == -1) {
            type = FxJsfComponentUtils.getLongValue(this, "type", -1);
        }
        return type;
    }

    public void setType(long type) {
        this.type = type;
    }

    public boolean isExplode() {
        final Boolean value = FxJsfComponentUtils.getBooleanValue(this, "explode");
        if (value != null) {
            this.explode = value;
        }
        return explode;
    }

    public void setExplode(boolean explode) {
        this.explode = explode;
    }

    public String getTypeName() {
        if (typeName == null) {
            typeName = FxJsfComponentUtils.getStringValue(this, "typeName");
        }
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    private FxType getSelectedType() {
        if (getType() != -1) {
            return CacheAdmin.getEnvironment().getType(getType());
        } else if (StringUtils.isNotBlank(getTypeName())) {
            return CacheAdmin.getEnvironment().getType(getTypeName());
        } else {
            throw new FxNotFoundException("ex.jsf.contentView.type.required").asRuntimeException();
        }
    }

    public FxContent getContent() {
        if (content == null) {
            setContent((FxContent) FxJsfComponentUtils.getValue(this, "content"));
            if (content != null) {
                // we need to make sure the content instance still exists at
                // the next request, so we store it in the component
                preserveContent = true;
                // initialize empty fields
                content.getRootGroup().explode(true);
            }
        }
        return content;
    }

    public void setContent(FxContent content) {
        this.content = content;
    }

    public boolean isPreserveContent() {
        final Boolean value = FxJsfComponentUtils.getBooleanValue(this, "preserveContent");
        return value != null ? value : preserveContent;
    }

    public void setPreserveContent(boolean preserveContent) {
        this.preserveContent = preserveContent;
    }

    @Override
    public Object saveState(FacesContext facesContext) {
        Object[] state = new Object[8];
        state[0] = super.saveState(facesContext);
        state[1] = pk;  // note: don't call getPk() here - rely on the caller to provide a valid pk next time
        state[2] = var;
        state[3] = getType();
        state[4] = typeName;
        state[5] = preserveContent;
        state[6] = preserveContent ? content : null;
        state[7] = explode;
        return state;
    }

    @Override
    public void restoreState(FacesContext facesContext, Object o) {
        Object[] state = (Object[]) o;
        super.restoreState(facesContext, state[0]);
        pk = (FxPK) state[1];
        var = (String) state[2];
        type = (Long) state[3];
        typeName = (String) state[4];
        preserveContent = (Boolean) state[5];
        content = preserveContent ? (FxContent) state[6] : null;
        explode = (Boolean) state[7];
    }

    public class ContentMap extends HashMap<String, Object> {
        private static final long serialVersionUID = -6423903577633591618L;
        private final String prefix;
        private final FxContent content;

        public ContentMap(FxContent content) {
            this(content, "");
        }

        public ContentMap(FxContent content, String prefix) {
            this.content = content;
            this.prefix = prefix;
        }

        @Override
        public Object put(String key, Object value) {
            final Object oldValue = get(key);
            try {
                content.setValue(((FxValue) value).getXPath(), (FxValue) value);
            } catch (FxApplicationException e) {
                throw e.asRuntimeException();
            }
            return oldValue;
        }

        @Override
        public Object get(Object key) {
            try {
                String path = getCanonicalPath((String) key);
                if (isListRequest(path)) {
                    return getList(path, false);
                } else if (isListAllRequest(path)) {
                    return getList(path, true);
                } else if (isLabelRequest(path)) {
                    return getLabel(path);
                } else if (isNewValueRequest(path)) {
                    return getNewValue(path);
                } else if (isResolveReferenceRequest(path)) {
                    return getResolvedReference(path);
                } else if (isXPathRequest(path)) {
                    return getXPath(path);
                } else {
                    return content.getValue(path);
                }
            } catch (FxNotFoundException e) {
                return null;
            } catch (FxInvalidParameterException e) {
                return new FxString("?" + key + "?");
            } catch (FxApplicationException e) {
                throw e.asRuntimeException();
            }
        }

        private String getCanonicalPath(String path) {
            return prefix + (path.startsWith("/") ? path : "/" + path);
        }

        private boolean isLabelRequest(String path) {
            return path.endsWith("$label");
        }

        private boolean isListRequest(String path) {
            return path.endsWith("$list");
        }

        private boolean isListAllRequest(String path) {
            return path.endsWith("$listAll");
        }

        private boolean isNewValueRequest(String path) {
            return path.endsWith("$new");
        }

        private boolean isResolveReferenceRequest(String path) {
            return path.endsWith("$");
            //return path.indexOf(".@") > 0 && path.indexOf(".@") == path.lastIndexOf(".");
        }

        private boolean isXPathRequest(String path) {
            return path.endsWith("$xpath");
        }

        private FxString getLabel(String path) throws FxNotFoundException, FxInvalidParameterException {
            final long assignmentId = content.getPropertyData(StringUtils.replace(path, "$label", "")).getAssignmentId();
            return CacheAdmin.getEnvironment().getAssignment(assignmentId).getDisplayLabel();
        }

        private List<?> getList(String path, boolean includeEmpty) {
            path = StringUtils.replace(StringUtils.replace(path, "$listAll", ""), "$list", "");
            try {
                final FxAssignment assignment = CacheAdmin.getEnvironment().getType(content.getTypeId()).getAssignment(path);
                if (assignment instanceof FxPropertyAssignment) {
                    // iterate over property values
                    return content.getPropertyData(path).getValues(false);
                } else {
                    // create a content map for each child
                    List<ContentMap> rowMaps = new ArrayList<ContentMap>();
                    for (FxData row : content.getGroupData(path).getElements()) {
                        if (includeEmpty || !row.isEmpty()) {
                            rowMaps.add(new ContentMap(content, path + "[" + row.getIndex() + "]"));
                        }
                    }
                    return rowMaps;
                }
            } catch (FxNotFoundException e) {
                // return null object
                return new ArrayList<Object>(0);
            } catch (FxInvalidParameterException e) {
                throw e.asRuntimeException();
            }
        }

        private FxValue getNewValue(String path) {
            path = StringUtils.replace(path, "$new", "");
            try {
                /*final FxPropertyData data = content.getPropertyData(path);
                if (data.getAssignmentMultiplicity().getMax() > 1 && data.getCreateableElements() > 0) {
                    return ((FxPropertyData) data.createNew(FxData.POSITION_BOTTOM)).getValue();
                } */
                // TODO use content API when available
                final String[] groups = StringUtils.split(path, '/');
                if (groups.length == 0) {
                    throw new FxInvalidParameterException("PATH", "No new values may be created for " + path).asRuntimeException();
                }
                final StringBuilder newPath = new StringBuilder();
                final StringBuilder xPathSoFar = new StringBuilder();
                for (int i = 0; i < groups.length - 1; i++) {
                    final String group = groups[i];
                    xPathSoFar.append("/").append(group);
                    final FxGroupData data = content.getGroupData(xPathSoFar.toString());
                    newPath.append("/").append(group).append('[').append(data.getOccurances() + 1).append("]");
                }
                final FxType type = CacheAdmin.getEnvironment().getType(content.getTypeId());
                final FxValue value = ((FxPropertyAssignment) type.getAssignment(path)).getEmptyValue();
                // add property
                newPath.append("/").append(groups[groups.length - 1]);
                content.setValue(newPath.toString(), value);
                return content.getValue(newPath.toString());
            } catch (FxNotFoundException e) {
                return new FxString("");
            } catch (FxApplicationException e) {
                throw e.asRuntimeException();
            }
        }

        private ContentMap getResolvedReference(String path) {
            path = path.substring(0, path.length() - 1);    // stip trailing $
            // resolve referenced object
            try {
                final FxPK pk = ((FxReference) content.getValue(path)).getDefaultTranslation();
                if (contentMap.containsKey(pk)) {
                    return new ContentMap(contentMap.get(pk));
                } else if (pk.isNew()) {
                    return null;    // don't resolve empty references
                }
                final FxContent referencedContent = EJBLookup.getContentEngine().load(pk);
                contentMap.put(pk, referencedContent);
                return new ContentMap(referencedContent);
            } catch (FxApplicationException e) {
                throw e.asRuntimeException();
            }
        }

        private String getXPath(String path) {
            final String xpath = StringUtils.replace(path, "$xpath", "").toUpperCase();
            return xpath.endsWith("/") ? xpath.substring(0, xpath.length() - 1) : xpath;
        }
    }

    private static class WrappedEvent extends FacesEvent {
        private static final long serialVersionUID = 3341414461609445709L;
        
        private final FacesEvent event;

        public WrappedEvent(FxContentView component, FacesEvent event) {
            super(component);
            this.event = event;
        }

        @Override
        public PhaseId getPhaseId() {
            return event.getPhaseId();
        }

        @Override
        public void setPhaseId(PhaseId phaseId) {
            this.event.setPhaseId(phaseId);
        }

        @Override
        public boolean isAppropriateListener(FacesListener listener) {
            return false;
        }

        @Override
        public void processListener(FacesListener listener) {
            throw new IllegalStateException();
        }

        @Override
        public void queue() {
            event.queue();
        }
    }
}
