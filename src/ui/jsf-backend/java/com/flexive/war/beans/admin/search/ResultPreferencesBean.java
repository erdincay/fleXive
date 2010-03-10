/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2010
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
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
package com.flexive.war.beans.admin.search;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.beans.MessageBean;
import com.flexive.faces.beans.SearchResultBean;
import com.flexive.faces.beans.SelectBean;
import com.flexive.faces.listener.JsfPhaseListener;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.faces.messages.FxFacesMsgWarn;
import com.flexive.shared.CacheAdmin;
import static com.flexive.shared.EJBLookup.getResultPreferencesEngine;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.search.*;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxGroupAssignment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.component.html.ext.HtmlDataTable;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.faces.model.DataModel;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import java.util.*;
import java.io.Serializable;

/**
 * Bean for creating and updating result preferences.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ResultPreferencesBean implements Serializable {
    private static final long serialVersionUID = 7084947975179682271L;
    private static final Log LOG = LogFactory.getLog(ResultPreferencesBean.class);

    /**
     * Tomahawk HtmlDataTable wrapper that exposes the internal data model.
     * We need this for other components depending on data tables that persist their
     * model in their viewState (aka preserveDataModel=true).
     */
    public static class WrappedHtmlDataTable extends HtmlDataTable {
        @SuppressWarnings({"MethodOverridesPrivateMethodOfSuperclass"})
        @Override
        public DataModel getDataModel() {
            return super.getDataModel();
        }
    }

    private long type = -1;
    private ResultViewType viewType = ResultViewType.LIST;
    private ResultLocation location = AdminResultLocations.ADMIN;
    private ResultPreferencesEdit resultPreferences = null;
    private String addPropertyName = null;
    private String addOrderByName = null;
    private SortDirection addOrderByDirection = SortDirection.ASCENDING;
    private int editColumnIndex = -1;
    private boolean forceSystemDefault;

    // cached select list
    private List<SelectItem> properties = null;
    private long cachedTypeId = -1;
    private Map<String, String> propertyLabelMap = null;
    private List<SelectItem> types;

    // access current selected columns directly from datatable component 
    private transient WrappedHtmlDataTable selectedColumnsTable;


    public ResultPreferencesBean() {
        parseRequestParameters();
    }

    /**
     * Parse the request parameters and perform actions as requested.
     * Works only if the ResultPreferencesBean remains request-scoped!
     */
    private void parseRequestParameters() {
        try {
            String action = FxJsfUtils.getParameter("action");
            if (StringUtils.isBlank(action)) {
                // no action requested
                return;
            }
            if ("loadSystemDefault".equals(action)) {
                forceSystemDefault = true;
            }
        } catch (Exception e) {
            LOG.error("Failed to parse request parameters: " + e.getMessage(), e);
        }
    }

    public String show() {
        return "resultPreferences";
    }

    public String save() {
        try {
            getResultPreferencesEngine().save(getResultPreferences(), type, viewType, location);
            new FxFacesMsgInfo("ResultPreferences.nfo.saved").addToContext();
            
            // invalidate cached result from search bean
            FxJsfUtils.getManagedBean(SearchResultBean.class).setResult(null);
        } catch (FxApplicationException e) {
            new FxFacesMsgErr("ResultPreferences.err.save", e).addToContext();
        }
        return show();
    }

    public String remove() {
        try {
            getResultPreferencesEngine().remove(type, viewType, location);
            resultPreferences = null;
            new FxFacesMsgInfo("ResultPreferences.nfo.removed").addToContext();
        } catch (FxApplicationException e) {
            new FxFacesMsgErr("ResultPreferences.err.remove", e).addToContext();
        }
        return show();
    }

    public void saveSystemDefault() {
        try {
            getResultPreferencesEngine().saveSystemDefault(getResultPreferences(), type, viewType, location);
            new FxFacesMsgInfo("ResultPreferences.nfo.savedSystemDefault").addToContext();
        } catch (FxApplicationException e) {
            new FxFacesMsgErr("ResultPreferences.err.save", e).addToContext();
        }
    }

    public void loadSystemDefault() {
        forceSystemDefault = true;
        resultPreferences = null;
    }

    public String cancel() {
        return ((SearchResultBean) FxJsfUtils.getManagedBean("fxSearchResultBean")).show();
    }

    public void addColumnProperty() {
        // split into property name and (optional) suffix
        final String[] name = StringUtils.split(addPropertyName, ".", 2);
        getResultPreferences().addSelectedColumn(
                new ResultColumnInfo(
                        Table.CONTENT,
                        name[0],
                        name.length > 1 ? name[1] : null
                )
        );
        addPropertyName = null;
        properties = null;  // refresh available properties
    }

    public void removeColumnProperty() {
        if (editColumnIndex == -1) {
            return;
        }
        try {
            final ResultColumnInfo info = getResultPreferences().removeSelectedColumn(editColumnIndex);
            // remove property from order by clause too
            List<ResultOrderByInfo> removeOrderByColumns = new ArrayList<ResultOrderByInfo>();
            for (ResultOrderByInfo orderByInfo : getResultPreferences().getOrderByColumns()) {
                if (orderByInfo.getColumnName().equals(info.getColumnName())) {
                    removeOrderByColumns.add(orderByInfo);
                }
            }
            for (ResultOrderByInfo removeInfo : removeOrderByColumns) {
                getResultPreferences().removeOrderByColumn(removeInfo);
            }
            properties = null;  // refresh available properties
        } catch (FxRuntimeException e) {
            new FxFacesMsgErr("ResultPreferences.err.removeRow", e).addToContext();
        }
    }

    public void moveColumnPropertyUp() {
        moveColumnProperty(-1);
    }

    public void moveColumnPropertyDown() {
        moveColumnProperty(1);
    }

    public void moveColumnPropertyTop() {
        moveColumnProperty(-editColumnIndex);
    }

    public void moveColumnPropertyBottom() {
        moveColumnProperty(getSelectedColumns().size() - editColumnIndex);
    }
    
    private void moveColumnProperty(int moveDelta) {
        if (editColumnIndex == -1) {
            return;
        }
        try {
            ResultColumnInfo info = getResultPreferences().removeSelectedColumn(editColumnIndex);
            getResultPreferences().addSelectedColumn(editColumnIndex + moveDelta, info);
        } catch (FxRuntimeException e) {
            new FxFacesMsgErr("ResultPreferences.err.moveRow", e).addToContext();
        }
    }

    public void moveOrderByPropertyUp() {
        moveOrderByProperty(-1);
    }

    public void moveOrderByPropertyDown() {
        moveOrderByProperty(1);
    }

    private void moveOrderByProperty(int moveDelta) {
        if (editColumnIndex == -1) {
            return;
        }
        try {
            ResultOrderByInfo info = getResultPreferences().removeOrderByColumn(editColumnIndex);
            getResultPreferences().addOrderByColumn(editColumnIndex + moveDelta, info);
        } catch (FxRuntimeException e) {
            new FxFacesMsgErr("ResultPreferences.err.moveRow", e).addToContext();
        }
    }

    public void addOrderByProperty() {
        getResultPreferences().addOrderByColumn(new ResultOrderByInfo(Table.CONTENT, addOrderByName, null,
                addOrderByDirection));
    }

    public void removeOrderByProperty() {
        if (editColumnIndex == -1) {
            return;
        }
        try {
            getResultPreferences().removeOrderByColumn(editColumnIndex);
        } catch (FxRuntimeException e) {
            new FxFacesMsgErr("ResultPreferences.err.removeRow", e).addToContext();
        }
    }

    public void reloadPreferences() {
        this.resultPreferences = null;
    }

    public long getType() {
        return type;
    }

    public void setType(long type) {
        this.type = type;
    }

    public List<SelectItem> getTypes() throws FxApplicationException {
        if (types == null) {
            types = new ArrayList<SelectItem>(FxJsfUtils.getManagedBean(SelectBean.class).getTypes());
            // remove root type
            for (Iterator<SelectItem> iterator = types.iterator(); iterator.hasNext();) {
                if (iterator.next().getValue().equals(FxType.ROOT_ID)) {
                    iterator.remove();
                    break;
                }
            }
            types.add(0, new SelectItem(-1L, FxJsfUtils.getLocalizedMessage("ResultPreferences.label.allTypes")));
        }
        return types;
    }

    public ResultViewType getViewType() {
        return viewType;
    }

    public void setViewType(ResultViewType viewType) {
        this.viewType = viewType;
    }

    public boolean isThumbnails() {
        return ResultViewType.THUMBNAILS.equals(viewType);
    }

    public ResultLocation getLocation() {
        return location;
    }

    public void setLocation(ResultLocation location) {
        this.location = location;
    }

    public String getAddPropertyName() {
        return addPropertyName;
    }

    public void setAddPropertyName(String addPropertyName) {
        this.addPropertyName = addPropertyName;
    }

    public int getEditColumnIndex() {
        return editColumnIndex;
    }

    public void setEditColumnIndex(int editColumnIndex) {
        this.editColumnIndex = editColumnIndex;
    }

    public SortDirection getAddOrderByDirection() {
        return addOrderByDirection;
    }

    public void setAddOrderByDirection(SortDirection addOrderByDirection) {
        this.addOrderByDirection = addOrderByDirection;
    }

    public String getAddOrderByName() {
        return addOrderByName;
    }

    public void setAddOrderByName(String addOrderByName) {
        this.addOrderByName = addOrderByName;
    }

    public boolean isCustomized() throws FxApplicationException {
        return getResultPreferencesEngine().isCustomized(type, viewType, location);
    }

    public boolean isForceSystemDefault() {
        return forceSystemDefault;
    }

    public void setForceSystemDefault(boolean forceSystemDefault) {
        this.forceSystemDefault = forceSystemDefault;
    }

    public ResultPreferencesEdit getResultPreferences() {
        if (resultPreferences == null) {
            // fetch result preferences from DB (the current value of ResultPreferences#selectedColumn is
            // persisted by the t:dataTable, so we do not have to remember them inside the bean)
            try {
                if (forceSystemDefault) {
                    resultPreferences = getResultPreferencesEngine().loadSystemDefault(type, viewType, location).getEditObject();
                } else {
                    resultPreferences = getResultPreferencesEngine().load(type, viewType, location).getEditObject();
                }
            } catch (FxNotFoundException e) {
                new FxFacesMsgWarn("ResultPreferences.wng.notFound").addToContext();
                resultPreferences = new ResultPreferences().getEditObject();
            } catch (FxApplicationException e) {
                new FxFacesMsgErr("ResultPreferences.err.load", e).addToContext();
            }
        }
        return resultPreferences;
    }

    public List<SelectItem> getProperties() {
        if (properties == null || cachedTypeId != getType()) {
            final FxEnvironment environment = CacheAdmin.getFilteredEnvironment();
            final FxType type = environment.getType(getType() != -1 ? getType() : FxType.ROOT_ID);
            final List<FxPropertyAssignment> contentProperties = Lists.newArrayList(type.getAssignedProperties());
            for (FxGroupAssignment groupAssignment : type.getAssignedGroups()) {
                addGroupAssignments(groupAssignment, contentProperties);
            }

            // add virtual properties...
            final MessageBean messageBean = MessageBean.getInstance();
            final SelectItemGroup virtualGroup = new SelectItemGroup(messageBean.getMessage("ResultPreferences.label.group.virtual"));
            virtualGroup.setSelectItems(new SelectItem[]{
                    new SelectItem("@pk", messageBean.getMessage("ResultPreferences.label.property.pk"), null, columnSelected("@pk")),
                    new SelectItem("@path", messageBean.getMessage("ResultPreferences.label.property.path"), null, columnSelected("@path")),
                    new SelectItem("@permissions", messageBean.getMessage("ResultPreferences.label.property.permissions"), null, columnSelected("@permissions")),
                    new SelectItem("@lock", messageBean.getMessage("ResultPreferences.label.property.lock"), null, columnSelected("@lock"))
            });
            properties = new ArrayList<SelectItem>(contentProperties.size() + 10);
            properties.add(virtualGroup);
            // add type properties
            properties.addAll(
                    filteredPropertiesGroup(contentProperties, messageBean.getMessage("ResultPreferences.label.group.type", type.getLabel().getBestTranslation()), false)
            );
            // add derived properties
            properties.addAll(
                    filteredPropertiesGroup(contentProperties, messageBean.getMessage("ResultPreferences.label.group.derived"), true)
            );

            cachedTypeId = getType();
        }
        return properties;
    }

    private boolean columnSelected(String columnName) {
        for (ResultColumnInfo columnInfo : getSelectedColumns()) {
            if (columnInfo.getColumnName().equalsIgnoreCase(columnName)) {
                return true;
            }
        }
        return false;
    }

    private void addGroupAssignments(FxGroupAssignment group, List<FxPropertyAssignment> contentProperties) {
        contentProperties.addAll(group.getAssignedProperties());
        for (FxGroupAssignment child : group.getAssignedGroups()) {
            addGroupAssignments(child, contentProperties);
        }
    }

    private List<SelectItemGroup> filteredPropertiesGroup(List<FxPropertyAssignment> contentProperties, String title, boolean includeDerived) {

        // assign properties to the type that originally defined them
        final HashMultimap<FxType, FxPropertyAssignment> byType = HashMultimap.create();
        final FxType rootType = CacheAdmin.getEnvironment().getType(FxType.ROOT_ID);
        for (FxPropertyAssignment assignment : contentProperties) {
            if (includeDerived && assignment.isDerivedAssignment()
                    || (!includeDerived && !assignment.isDerivedAssignment())) {
                // find type that first defined used or defined an assignment with our property
                FxType type = assignment.getAssignedType();
                while (type.getParent() != null && !type.getParent().getAssignmentsForProperty(assignment.getProperty().getId()).isEmpty()) {
                    type = type.getParent();
                }
                // attach system-internal properties (ID, ACL, ...) to root type
                byType.put(assignment.getProperty().isSystemInternal() ? rootType : type, assignment);
            }
        }

        final List<SelectItemGroup> result;
        if (!includeDerived) {
            // just our type, return all properties in one group
            result = Arrays.asList(createSelectItemGroup(title, byType.values()));
        } else {
            // all derived types, group by type
            result = new ArrayList<SelectItemGroup>();

            // root types come last
            final List<FxType> types = Lists.newArrayList(byType.keys().elementSet());
            Collections.sort(
                    types,
                    new Comparator<FxType>() {
                        public int compare(FxType o1, FxType o2) {
                            return o1.getParent() == null ? 1 : o1.getParent().equals(o2) ? -1 : 0;
                        }
                    }
            );
            for (FxType type : types) {
                result.add(
                        createSelectItemGroup(
                                title + ": " + type.getLabel(),
                                byType.get(type)
                        )
                );
            }
        }

        return result;
    }

    private SelectItemGroup createSelectItemGroup(String title, Collection<FxPropertyAssignment> assignments) {
        final SelectItemGroup group = new SelectItemGroup(title);
        final List<SelectItem> items = new ArrayList<SelectItem>();
        for (FxPropertyAssignment assignment : assignments) {
            items.add(createSelectItem(assignment));
        }
        Collections.sort(items, new FxJsfUtils.SelectItemSorter());
        group.setSelectItems(items.toArray(new SelectItem[items.size()]));
        return group;
    }

    private SelectItem createSelectItem(FxPropertyAssignment assignment) {
        // add parent group path
        final StringBuilder label = new StringBuilder();
        FxGroupAssignment parent = assignment.getParentGroupAssignment();
        while (parent != null) {
            label.insert(0, parent.getLabel() + "/");
            parent = parent.getParentGroupAssignment();
        }
        // add property label
        label.append(assignment.getProperty().getLabel());

        final String value;
        final String propertyName = assignment.getProperty().getName();
        if ("CAPTION".equals(propertyName)) {
            // the caption property is often renamed (which breaks assignment inheritance)
            value = "CAPTION";
        } else if ("TYPEDEF".equals(propertyName)) {
            // select the type label instead
            value = "TYPEDEF.DESCRIPTION";
        } else if ("ACL".equals(propertyName)) {
            value = "ACL.LABEL";
        } else if ("STEP".equals(propertyName)) {
            value = "STEP.LABEL";
        } else if ("CREATED_BY".equals(propertyName) || "MODIFIED_BY".equals(propertyName)) {
            value = assignment.getProperty().getName() + ".USERNAME";
        } else {
            value = "#" + assignment.getXPath();
        }
        return new SelectItem(
                value,
                label.toString(),
                null,
                // disable properties/assignments that are already selected
                columnSelected(assignment.getProperty().getName()) || columnSelected("#" + assignment.getXPath())
        );
    }

    public List<SelectItem> getSelectedProperties() {
        // caching this is not trivial because the selectedColumns list
        // is updated between phases by the datatable
        final FxEnvironment environment = CacheAdmin.getFilteredEnvironment();
        final List<ResultColumnInfo> columns = getSelectedColumns();
        List<SelectItem> result = new ArrayList<SelectItem>(columns.size());
        for (ResultColumnInfo info : columns) {
            result.add(new SelectItem(info.getPropertyName(), info.getLabel(environment)));
        }
        return result;
    }

    @SuppressWarnings({"unchecked"})
    private List<ResultColumnInfo> getSelectedColumns() {
        final List<ResultColumnInfo> columns;
        if (JsfPhaseListener.isInPhase(PhaseId.PROCESS_VALIDATIONS)) {
            columns = (List) ((WrappedHtmlDataTable) getSelectedColumnsTable()).getDataModel().getWrappedData();
        } else {
            columns = getResultPreferences().getSelectedColumns();
        }
        return columns;
    }

    public HtmlDataTable getSelectedColumnsTable() {
        if (selectedColumnsTable == null) {
            selectedColumnsTable = new WrappedHtmlDataTable();
        } else {
            UIComponent parent = selectedColumnsTable.getParent();
            while (parent.getParent() != null) {
                parent = parent.getParent();
            }
            if (parent instanceof UIViewRoot && parent != FacesContext.getCurrentInstance().getViewRoot()) {
                // create new table when view root changes
                System.out.println("View root changed - creating new wrapped table.");
                selectedColumnsTable = new WrappedHtmlDataTable();
            }
        }
        return selectedColumnsTable;
    }

    public void setSelectedColumnsTable(HtmlDataTable selectedColumnsTable) {
        if (selectedColumnsTable instanceof WrappedHtmlDataTable) {
            this.selectedColumnsTable = (WrappedHtmlDataTable) selectedColumnsTable;
        } else {
            throw new IllegalArgumentException("Invalid data table class: "
                    + (selectedColumnsTable != null ? selectedColumnsTable.getClass().getCanonicalName() : "null")
            );
        }
    }

    /**
     * Provides the property labels for the result preferences page, including virtual properties like @pk.
     *
     * @return the property labels for the result preferences page, including virtual properties like @pk.
     */
    public Map<String, String> getPropertyLabelMap() {
        if (propertyLabelMap == null) {
            propertyLabelMap = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<String, String>() {
                private static final long serialVersionUID = -1140857482270400036L;
                private FxEnvironment environment = CacheAdmin.getFilteredEnvironment();

                public String get(Object key) {
                    if (key == null) {
                        return null;
                    }
                    final String name = key.toString();
                    if (name.charAt(0) == '@') {
                        return MessageBean.getInstance().getMessage("ResultPreferences.label.property." + name.substring(1));
                    } else if (name.charAt(0) == '#') {
                        return environment.getAssignment(
                                stripTableSelectors(name.substring(1))
                        ).getDisplayName(true);
                    } else {
                        return environment.getProperty(
                                stripTableSelectors(name)
                        ).getLabel().getBestTranslation();
                    }
                }
            }, true);
        }
        return propertyLabelMap;
    }

    private String stripTableSelectors(String name) {
        final int pos = name.indexOf('.');
        return pos != -1 ? name.substring(0, pos) : name;
    }
}
