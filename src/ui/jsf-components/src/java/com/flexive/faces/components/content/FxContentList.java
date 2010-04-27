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
package com.flexive.faces.components.content;

import com.flexive.faces.FxJsfComponentUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxUpdateException;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.query.QueryRootNode;
import com.flexive.shared.search.query.SqlQueryBuilder;
import groovy.lang.GroovyShell;
import org.apache.commons.lang.StringUtils;

import javax.faces.component.EditableValueHolder;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.FacesEvent;
import javax.faces.event.FacesListener;
import javax.faces.event.PhaseId;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <p>
 * Provides a list of content instances matching the given search criterias.
 * Currently, search conditions can be specified in the "groovyQuery" facet,
 * which will then be dynamically evaluated with
 * a {@link com.flexive.shared.scripting.groovy.GroovyQueryBuilder GroovyQueryBuilder}.
 * </p>
 * <p/>
 * <p>
 * <b>Facets</b>
 * <ul>
 * <li><b>groovyQuery</b>: an embedded groovy query that selects the PKs to be displayed.</li>
 * <li><b>header</b>: defines an optional header to be rendered before the first result row.</li>
 * <li><b>empty</b>: facet to be rendered instead of header and results when the query returns no results.</li>
 * </ul>
 * </p>
 * <p/>
 * <p>
 * <b>FIXME:</b> it turns out that a "render-time" iterator tag is pretty complicated to implement.
 * Thus this component contains large portions of Facelets' ui:repeat tag, and should be
 * refactored into either extending UIRepeat or converted to a taghandler that
 * uses a template with ui:repeat (or a similar tag).
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxContentList extends UIComponentBase implements NamingContainer {
    public static final String COMPONENT_TYPE = "flexive.FxContentList";
    public static final String COMPONENT_FAMILY = "flexive";

    private String var;
    private String indexVar;
    private SqlQueryBuilder queryBuilder;
    private FxResultSet result;
    private int index;
    private boolean explode = true;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getRendersChildren() {
        return true;
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Override
    public void encodeChildren(FacesContext context) throws IOException {
        process(context, PhaseId.RENDER_RESPONSE);
    }

    @Override
    public void processDecodes(FacesContext context) {
        this.childState = null;
        process(context, PhaseId.APPLY_REQUEST_VALUES);
    }

    @Override
    public void processUpdates(FacesContext context) {
        process(context, PhaseId.UPDATE_MODEL_VALUES);
    }

    @Override
    public void processValidators(FacesContext context) {
        process(context, PhaseId.PROCESS_VALIDATIONS);
    }

    @Override
    public void queueEvent(FacesEvent event) {
        super.queueEvent(new IndexedEvent(this, event, index));
    }

    @Override
    public String getClientId(FacesContext facesContext) {
        return super.getClientId(facesContext) + NamingContainer.SEPARATOR_CHAR + index;
    }

    @Override
    public void broadcast(FacesEvent event) throws AbortProcessingException {
        if (event instanceof IndexedEvent) {
            final IndexedEvent indexedEvent = (IndexedEvent) event;
            final int prevIndex = getIndex();
            try {
                this.setIndex(indexedEvent.getIndex());
                final FacesEvent target = indexedEvent.getTarget();
                target.getComponent().broadcast(target);
            } finally {
                setIndex(prevIndex);
            }
        } else {
            super.broadcast(event);
        }
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private void process(FacesContext context, PhaseId phase) {
        final Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        final String resultVar = getVar() + "_result";
        try {
            final FxResultSet result = getResult(context);
            final FxContentView contentView;
            if (phase == PhaseId.RENDER_RESPONSE && (getChildCount() == 0 ||
                    !(getChildren().get(0) instanceof FxContentView))) {
                // create content view, attach our children
                contentView = new FxContentView();
                contentView.setVar(getVar());
                contentView.setExplode(isExplode());
                // attach our children to the content view
                contentView.getChildren().addAll(getChildren());
                // set content view as our only child
                this.getChildren().clear();
                this.getChildren().add(contentView);
            } else if (getChildCount() > 1 && getChildren().get(0) instanceof FxContentView) {
                // move all other children that got appended by facelets (TODO: investigate why)
                contentView = (FxContentView) getChildren().get(0);
                contentView.getChildren().clear();
                while (getChildren().size() > 1) {
                    contentView.getChildren().add(getChildren().remove(1));
                }
            } else {
                // get contentview child
                contentView = (FxContentView) getChildren().get(0);
            }
            if (result.getRowCount() == 0) {
                if (getFacet("empty") != null) {
                    applyPhase(context, getFacet("empty"), phase);
                }
                return;
            }
            // process result
            setIndex(0);
            if (result.getRowCount() > 0) {
                // initialize data
                provideContent();
            }
            requestMap.put(resultVar, getResult(context));
            if (getFacet("header") != null) {
                applyPhase(context, getFacet("header"), phase);
            }
            for (int i = 0; i < result.getRowCount(); i++) {
                setIndex(i);
                applyPhase(context, contentView, phase);
            }
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        } catch (IOException e) {
            throw new FxUpdateException("ex.jsf.contentView.render", e).asRuntimeException();
        } finally {
            if (requestMap.containsKey(resultVar)) {
                requestMap.remove(resultVar);
            }
            if (StringUtils.isNotBlank(getIndexVar()) && requestMap.containsValue(getIndexVar())) {
                requestMap.remove(getIndexVar());
            }
        }
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private void applyPhase(FacesContext context, UIComponent component, PhaseId phase) throws IOException {
        if (PhaseId.RENDER_RESPONSE.equals(phase)) {
            component.encodeAll(context);
        } else if (PhaseId.APPLY_REQUEST_VALUES.equals(phase)) {
            component.processDecodes(context);
        } else if (PhaseId.UPDATE_MODEL_VALUES.equals(phase)) {
            component.processUpdates(context);
        } else if (PhaseId.PROCESS_VALIDATIONS.equals(phase)) {
            component.processValidators(context);
        } else {
            throw new FxInvalidParameterException("phase", "ex.jsf.contentView.phase.invalid", phase).asRuntimeException();
        }
    }

    private FxResultSet getResult(FacesContext context) throws IOException, FxApplicationException {
        if (result != null) {
            return result;
        }
        final UIComponent groovyQuery = getFacet("groovyQuery");
        this.queryBuilder = this.getQueryBuilder() != null ? this.getQueryBuilder() : new SqlQueryBuilder();
        if (!queryBuilder.getColumnNames().contains("@pk")) {
            queryBuilder.select("@pk");
        }
        if (groovyQuery != null) {
            // get embedded groovy query
            final ResponseWriter oldWriter = context.getResponseWriter();
            try {
                final StringWriter queryWriter = new StringWriter();
                context.setResponseWriter(context.getResponseWriter().cloneWithWriter(queryWriter));
                groovyQuery.encodeAll(context);
                context.setResponseWriter(oldWriter);
                final GroovyShell shell = new GroovyShell();
                shell.setVariable("builder", queryBuilder);
                final QueryRootNode result = (QueryRootNode) shell.parse(
                        "new com.flexive.shared.scripting.groovy.GroovyQueryBuilder(builder).select([\"@pk\"]) {"
                                + StringUtils.trim(queryWriter.toString()).replace("&quot;", "\"")
                                + "}").run();
                result.buildSqlQuery(queryBuilder);
            } finally {
                context.setResponseWriter(oldWriter);
            }
        }
        result = queryBuilder.getResult();
        return result;
    }

    public String getVar() {
        if (var == null) {
            return FxJsfComponentUtils.getStringValue(this, "var");
        }
        return var;
    }

    public void setVar(String var) {
        this.var = var;
    }

    public String getIndexVar() {
        if (indexVar == null) {
            return FxJsfComponentUtils.getStringValue(this, "indexVar");
        }
        return indexVar;
    }

    public void setIndexVar(String indexVar) {
        this.indexVar = indexVar;
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


    public SqlQueryBuilder getQueryBuilder() {
        if (queryBuilder == null) {
            return (SqlQueryBuilder) FxJsfComponentUtils.getValue(this, "queryBuilder");
        }
        return queryBuilder;
    }

    public void setQueryBuilder(SqlQueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        saveChildState();
        this.index = index;
        provideContent();

        restoreChildState();
    }

    private void provideContent() {
        // store current PK in request
        final FxContentView contentView = (FxContentView) getChildren().get(0);
        contentView.setPk(result.getResultRow(index).getPk(result.getColumnIndex("@pk")));
        contentView.provideContent(FacesContext.getCurrentInstance());
        if (StringUtils.isNotBlank(getIndexVar())) {
            FacesContext.getCurrentInstance().getExternalContext().getRequestMap().put(getIndexVar(), index);
        }
    }

    @Override
    public Object saveState(FacesContext facesContext) {
        final Object[] state = new Object[7];
        state[0] = super.saveState(facesContext);
        state[1] = var;
        state[2] = queryBuilder;
        state[3] = childState;
        state[4] = explode;
        state[5] = result;
        state[6] = indexVar;
        return state;
    }

    @Override
    public void restoreState(FacesContext facesContext, Object o) {
        final Object[] state = (Object[]) o;
        super.restoreState(facesContext, state[0]);
        this.var = (String) state[1];
        this.queryBuilder = (SqlQueryBuilder) state[2];
        this.childState = (Map) state[3];
        this.explode = (Boolean) state[4];
        this.result = (FxResultSet) state[5];
        this.indexVar = (String) state[6];
    }

    // ------------------- All code below copied from com.sun.facelets.component.UIRepeat -------------------
    private Map childState;

    private Map getChildState() {
        if (this.childState == null) {
            this.childState = new HashMap();
        }
        return this.childState;
    }

    private void saveChildState() {
        if (this.getChildCount() > 0) {

            FacesContext faces = FacesContext.getCurrentInstance();

            for (UIComponent uiComponent : this.getChildren()) {
                this.saveChildState(faces, uiComponent);
            }
        }
    }

    private void saveChildState(FacesContext faces, UIComponent c) {
        if (c instanceof EditableValueHolder && !c.isTransient()) {
            String clientId = c.getClientId(faces);
            SavedState ss = (SavedState) this.getChildState().get(clientId);
            if (ss == null) {
                ss = new SavedState();
                //noinspection unchecked
                this.getChildState().put(clientId, ss);
            }
            ss.populate((EditableValueHolder) c);
        }

        // continue hack
        Iterator itr = c.getFacetsAndChildren();
        while (itr.hasNext()) {
            saveChildState(faces, (UIComponent) itr.next());
        }
    }

    private void restoreChildState() {
        if (this.getChildCount() > 0) {
            FacesContext faces = FacesContext.getCurrentInstance();

            for (UIComponent uiComponent : this.getChildren()) {
                this.restoreChildState(faces, uiComponent);
            }
        }
    }

    private void restoreChildState(FacesContext faces, UIComponent c) {
        // reset id
        String id = c.getId();
        c.setId(id);

        // hack
        if (c instanceof EditableValueHolder) {
            EditableValueHolder evh = (EditableValueHolder) c;
            String clientId = c.getClientId(faces);
            SavedState ss = (SavedState) this.getChildState().get(clientId);
            if (ss != null) {
                ss.apply(evh);
            } else {
                NullState.apply(evh);
            }
        }

        // continue hack
        Iterator itr = c.getFacetsAndChildren();
        while (itr.hasNext()) {
            restoreChildState(faces, (UIComponent) itr.next());
        }
    }

    /**
     * Taken from Facelets' UIRepeat component
     */
    private static class IndexedEvent extends FacesEvent {
        private static final long serialVersionUID = 5274895541939738723L;
        private final FacesEvent target;
        private final int index;

        public IndexedEvent(FxContentList owner, FacesEvent target, int index) {
            super(owner);
            this.target = target;
            this.index = index;
        }

        @Override
        public PhaseId getPhaseId() {
            return (this.target.getPhaseId());
        }

        @Override
        public void setPhaseId(PhaseId phaseId) {
            this.target.setPhaseId(phaseId);
        }

        @Override
        public boolean isAppropriateListener(FacesListener listener) {
            return this.target.isAppropriateListener(listener);
        }

        @Override
        public void processListener(FacesListener listener) {
            FxContentList owner = (FxContentList) this.getComponent();
            int prevIndex = owner.index;
            try {
                owner.setIndex(this.index);
                this.target.processListener(listener);
            } finally {
                owner.setIndex(prevIndex);
            }
        }

        public int getIndex() {
            return index;
        }

        public FacesEvent getTarget() {
            return target;
        }

    }

    private final static SavedState NullState = new SavedState();

    // from RI
    private final static class SavedState implements Serializable {

        private Object submittedValue;

        private static final long serialVersionUID = 2920252657338389849L;

        Object getSubmittedValue() {
            return (this.submittedValue);
        }

        void setSubmittedValue(Object submittedValue) {
            this.submittedValue = submittedValue;
        }

        private boolean valid = true;

        boolean isValid() {
            return (this.valid);
        }

        void setValid(boolean valid) {
            this.valid = valid;
        }

        private Object value;

        Object getValue() {
            return (this.value);
        }

        public void setValue(Object value) {
            this.value = value;
        }

        private boolean localValueSet;

        boolean isLocalValueSet() {
            return (this.localValueSet);
        }

        public void setLocalValueSet(boolean localValueSet) {
            this.localValueSet = localValueSet;
        }

        public String toString() {
            return ("submittedValue: " + submittedValue + " value: " + value
                    + " localValueSet: " + localValueSet);
        }

        public void populate(EditableValueHolder evh) {
            this.value = evh.getValue();
            this.valid = evh.isValid();
            this.submittedValue = evh.getSubmittedValue();
            this.localValueSet = evh.isLocalValueSet();
        }

        public void apply(EditableValueHolder evh) {
            evh.setValue(this.value);
            evh.setValid(this.valid);
            evh.setSubmittedValue(this.submittedValue);
            evh.setLocalValueSet(this.localValueSet);
        }
    }

}
