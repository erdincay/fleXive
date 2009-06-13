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
package com.flexive.faces.components.input;

import com.flexive.faces.FxJsfComponentUtils;
import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.model.FxJSFSelectItem;
import com.flexive.shared.*;
import com.flexive.shared.scripting.FxScriptInfo;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxSelectList;
import com.flexive.shared.structure.FxSelectListItem;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.el.ValueExpression;
import javax.faces.component.*;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import javax.faces.render.Renderer;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A special renderer for SelectOneListbox and SelectManyListbox based on MenuRenderer from the JSF RI 1.2
 * Changed behaviour: if readonly or disabled on the text is rendered and colors are taken from
 * the SelectItem's if their value class extends ObjectWithColor or a style is provided in FxJSFSelectItem's
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @see com.flexive.shared.ObjectWithColor
 */
public class FxSelectRenderer extends Renderer {

    private static final Log LOG = LogFactory.getLog(FxSelectRenderer.class);

    /**
     * constant if no value is assigned
     */
    public static final Object NO_VALUE = "";

    /**
     * Scripting support (debugging)
     */
    private static final boolean ALLOW_SCRIPTING = false;
    private static final String readOnlyScript = "renderSelectReadOnly.groovy";
    private static final String editScript = "renderSelectEdit.groovy";

    /**
     * {@inheritDoc}
     */
    @Override
    public String convertClientId(FacesContext context, String clientId) {
        return clientId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getRendersChildren() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decode(FacesContext context, UIComponent component) {
        if (!isEditable(component)) {
            //dont decode a readonly or disabled listbox
            return;
        }
        String clientId = component.getClientId(context);
        if (component instanceof UISelectMany) {
            Map<String, String[]> requestParameterValuesMap =
                    context.getExternalContext().
                            getRequestParameterValuesMap();
            if (requestParameterValuesMap.containsKey(clientId)) {
                String newValues[] = requestParameterValuesMap.
                        get(clientId);
                ((UIInput) component).setSubmittedValue(newValues);
            } else {
                // Use the empty array, not null, to distinguish
                // between an deselected UISelectMany and a disabled one
                ((UIInput) component).setSubmittedValue(new String[0]);
            }
        } else {
            Map<String, String> requestParameterMap = context.getExternalContext().getRequestParameterMap();
            if (requestParameterMap.containsKey(clientId)) {
                String newValue = requestParameterMap.get(clientId);
                ((UIInput) component).setSubmittedValue(newValue);
            } else {
                // there is no value, but this is different from a null value.
                ((UIInput) component).setSubmittedValue(NO_VALUE);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        //do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        if (isEditable(component)) {
            if (ALLOW_SCRIPTING) {
                if (CacheAdmin.getEnvironment().scriptExists(editScript)) {
                    try {
                        LOG.info("Rendering using script " + editScript);
                        FxScriptInfo scriptInfo = CacheAdmin.getEnvironment().getScript(editScript);
                        GroovyShell shell = new GroovyShell();
                        Script script = shell.parse(scriptInfo.getCode());
                        script.setProperty("context", context);
                        script.setProperty("component", component);
                        script.run();
                        LOG.info("Finished " + editScript);
                    } catch (Exception e) {
                        LOG.error("Error executing render script " + editScript + ": " + e.getMessage(), e);
                        renderSelect(context, component);
                    }
                    return;
                }
            }
            renderSelect(context, component);
        } else {
            if (ALLOW_SCRIPTING) {
                if (CacheAdmin.getEnvironment().scriptExists(readOnlyScript)) {
                    try {
                        LOG.info("Rendering using script " + readOnlyScript);
                        FxScriptInfo scriptInfo = CacheAdmin.getEnvironment().getScript(readOnlyScript);
                        GroovyShell shell = new GroovyShell();
                        Script script = shell.parse(scriptInfo.getCode());
                        script.setProperty("context", context);
                        script.setProperty("component", component);
                        script.run();
                        LOG.info("Finished " + readOnlyScript);
                    } catch (Exception e) {
                        LOG.error("Error executing render script " + readOnlyScript + ": " + e.getMessage(), e);
                        renderText(context, component);
                    }
                    return;
                }
            }
            renderText(context, component);
        }
    }

    /**
     * Convert a SelectOne value to the correct value class
     *
     * @param context     faces context
     * @param uiSelectOne the select one component
     * @param newValue    the value to convert
     * @return converted value
     * @throws ConverterException on errors
     */
    public Object convertSelectOneValue(FacesContext context, UISelectOne uiSelectOne, String newValue) throws ConverterException {
        if (NO_VALUE.equals(newValue) || newValue == null)
            return null;
        return FxJsfComponentUtils.getConvertedValue(context, uiSelectOne, newValue);
    }

    /**
     * Convert SelectManys value to the correct value classes
     *
     * @param context      faces context
     * @param uiSelectMany the select many component
     * @param newValues    the new values to convert
     * @return converted values
     * @throws ConverterException on errors
     */
    public Object convertSelectManyValue(FacesContext context, UISelectMany uiSelectMany, String[] newValues)
            throws ConverterException {
        // if we have no local value, try to get the valueExpression.
        ValueExpression valueExpression = uiSelectMany.getValueExpression("value");

        Object result = newValues; // default case, set local value
        boolean throwException = false;

        // If we have a ValueExpression
        if (null != valueExpression) {
            Class modelType = valueExpression.getType(context.getELContext());
            // Does the valueExpression resolve properly to something with
            // a type?
            if (modelType != null)
                result = convertSelectManyValuesForModel(context, uiSelectMany, modelType, newValues);
            // If it could not be converted, as a fall back try the type of
            // the valueExpression's current value covering some edge cases such
            // as where the current value came from a Map.
            if (result == null) {
                Object value = valueExpression.getValue(context.getELContext());
                if (value != null)
                    result = convertSelectManyValuesForModel(context, uiSelectMany, value.getClass(), newValues);
            }
            if (result == null)
                throwException = true;
        } else {
            // No ValueExpression, just use Object array.
            result = convertSelectManyValues(context, uiSelectMany, Object[].class, newValues);
        }
        if (throwException) {
            StringBuffer values = new StringBuffer();
            if (null != newValues) {
                for (int i = 0; i < newValues.length; i++) {
                    if (i == 0)
                        values.append(newValues[i]);
                    else
                        values.append(' ').append(newValues[i]);
                }
            }
            throw new ConverterException("Error converting expression [" + valueExpression.getExpressionString() + "] of " + String.valueOf(values));
        }
        return result;
    }

    /*
    * Converts the provided string array and places them into the correct provided model type.
    */
    protected Object convertSelectManyValuesForModel(FacesContext context, UISelectMany uiSelectMany, Class modelType, String[] newValues) {
        Object result = null;
        if (modelType.isArray())
            result = convertSelectManyValues(context, uiSelectMany, modelType, newValues);
        else if (List.class.isAssignableFrom(modelType)) {
            Object[] values = (Object[]) convertSelectManyValues(context, uiSelectMany, Object[].class, newValues);
            // perform a manual copy as the Array returned from
            // Arrays.asList() isn't mutable.  It seems a waste
            // to also call Collections.addAll(Arrays.asList())
            List<Object> l = new ArrayList<Object>(values.length);
            //noinspection ManualArrayToCollectionCopy
            for (Object v : values)
                l.add(v);
            result = l;
        }
        return result;
    }

    /**
     * Convert select many values to given array class
     *
     * @param context      faces context
     * @param uiSelectMany select many component
     * @param arrayClass   the array class
     * @param newValues    new values to convert
     * @return converted values
     * @throws ConverterException on errors
     */
    protected Object convertSelectManyValues(FacesContext context, UISelectMany uiSelectMany, Class arrayClass, String[] newValues)
            throws ConverterException {

        Object result;
        Converter converter;
        int len = (null != newValues ? newValues.length : 0);

        Class elementType = arrayClass.getComponentType();

        // Optimization: If the elementType is String, we don't need
        // conversion.  Just return newValues.
        if (elementType.equals(String.class))
            return newValues;

        try {
            result = Array.newInstance(elementType, len);
        } catch (Exception e) {
            throw new ConverterException(e);
        }

        // bail out now if we have no new values, returning our
        // oh-so-useful zero-length array.
        if (null == newValues)
            return result;

        // obtain a converter.

        // attached converter takes priority
        if (null == (converter = uiSelectMany.getConverter())) {
            // Otherwise, look for a by-type converter
            if (null == (converter = FxJsfComponentUtils.getConverterForClass(elementType, context))) {
                // if that fails, and the attached values are of Object type,
                // we don't need conversion.
                if (elementType.equals(Object.class))
                    return newValues;
                StringBuffer valueStr = new StringBuffer();
                for (int i = 0; i < len; i++) {
                    if (i == 0)
                        valueStr.append(newValues[i]);
                    else
                        valueStr.append(' ').append(newValues[i]);
                }
                throw new ConverterException("Could not get a converter for " + String.valueOf(valueStr));
            }
        }

        if (elementType.isPrimitive()) {
            for (int i = 0; i < len; i++) {
                if (elementType.equals(Boolean.TYPE)) {
                    Array.setBoolean(result, i, ((Boolean) converter.getAsObject(context, uiSelectMany, newValues[i])));
                } else if (elementType.equals(Byte.TYPE)) {
                    Array.setByte(result, i, ((Byte) converter.getAsObject(context, uiSelectMany, newValues[i])));
                } else if (elementType.equals(Double.TYPE)) {
                    Array.setDouble(result, i, ((Double) converter.getAsObject(context, uiSelectMany, newValues[i])));
                } else if (elementType.equals(Float.TYPE)) {
                    Array.setFloat(result, i, ((Float) converter.getAsObject(context, uiSelectMany, newValues[i])));
                } else if (elementType.equals(Integer.TYPE)) {
                    Array.setInt(result, i, ((Integer) converter.getAsObject(context, uiSelectMany, newValues[i])));
                } else if (elementType.equals(Character.TYPE)) {
                    Array.setChar(result, i, ((Character) converter.getAsObject(context, uiSelectMany, newValues[i])));
                } else if (elementType.equals(Short.TYPE)) {
                    Array.setShort(result, i, ((Short) converter.getAsObject(context, uiSelectMany, newValues[i])));
                } else if (elementType.equals(Long.TYPE)) {
                    Array.setLong(result, i, ((Long) converter.getAsObject(context, uiSelectMany, newValues[i])));
                }
            }
        } else {
            for (int i = 0; i < len; i++)
                Array.set(result, i, converter.getAsObject(context, uiSelectMany, newValues[i]));
        }
        return result;
    }


    /**
     * Should the component be rendered as editable?
     * Checks the disabled and reaonly attributes.
     *
     * @param component the component to check
     * @return editable
     */
    protected boolean isEditable(UIComponent component) {
        return !(Boolean.valueOf(String.valueOf(component.getAttributes().get("disabled"))) ||
                Boolean.valueOf(String.valueOf(component.getAttributes().get("readonly"))));
    }

    /**
     * Render the component as static text (when readonly or disabled)
     *
     * @param context   faces context
     * @param component the component
     * @throws IOException on errors
     */
    protected void renderText(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        Object[] items = FxJsfComponentUtils.getCurrentSelectedValues(component);
        List<SelectItem> selectedItems = FxJsfComponentUtils.getSelectItems(context, component);
        for (Object item : items) {
            writer.startElement("div", component);
            for (SelectItem currItem : selectedItems) {
                if (currItem.getValue().equals(item)) {
                    item = currItem;
                    break;
                } else if (currItem instanceof FxJSFSelectItem &&
                        item instanceof Long &&
                        currItem.getValue() instanceof SelectableObject &&
                        ((SelectableObject) currItem.getValue()).getId() == (Long) item) {
                    item = currItem;
                    break;
                }
            }

            String color = null;
            if (item instanceof FxJSFSelectItem) {
                writer.writeAttribute("style", ((FxJSFSelectItem) item).getStyle(), "style");
            } else if (item instanceof ObjectWithColor) {
                ObjectWithColor oc = (ObjectWithColor) item;
                if (!StringUtils.isEmpty(oc.getColor()))
                    color = oc.getColor();
            } else if (item instanceof SelectItem) {
                if (((SelectItem) item).getValue() instanceof ObjectWithColor) {
                    ObjectWithColor oc = (ObjectWithColor) ((SelectItem) item).getValue();
                    if (!StringUtils.isEmpty(oc.getColor()))
                        color = oc.getColor();
                }
            }
            if (color != null) {
                //if no styleClass is explicitly set, apply contrast background color if too light
                if (null == component.getAttributes().get("styleClass") && FxFormatUtils.lackOfContrast(color))
                    color += ";background-color:" + FxFormatUtils.CONTRAST_BACKGROUND_COLOR;
                writer.writeAttribute("style", "color:" + color, "style");
            }

            if (item instanceof SelectableObjectWithLabel) {
                if (item instanceof FxJSFSelectItem && ((FxJSFSelectItem) item).isFxSelectListItem()) {
                    //we have an FxSelectListItem -> check if cascaded
                    final FxEnvironment env = CacheAdmin.getEnvironment();
                    final Long itemId = (Long) ((FxJSFSelectItem) item).getValue();
                    FxSelectList list = env.getSelectListItem(itemId).getList();

                    writer.write(list.getItem(itemId).getLabelBreadcrumbPath());
                } else
                    writer.write(((SelectableObjectWithLabel) item).getLabel().getBestTranslation());

            } else if (item instanceof SelectableObjectWithName)
                writer.write(((SelectableObjectWithName) item).getName());
            else if (item instanceof SelectItem)
                writer.write(((SelectItem) item).getLabel());
            else
                writer.write(String.valueOf(item)); //last exit ...
            writer.endElement("div");
        }

    }

    /**
     * Render the component as select list box
     *
     * @param context   faces context
     * @param component the component
     * @throws IOException on errors
     */
    protected void renderSelect(FacesContext context, UIComponent component) throws IOException {
        List<SelectItem> items = FxJsfComponentUtils.getSelectItems(context, component);

        boolean restrictToGroups = false;
        if ((component instanceof UISelectOne || component instanceof UISelectMany) &&
                items.size() > 0 &&
                items.get(0) instanceof FxJSFSelectItem &&
                ((FxJSFSelectItem) items.get(0)).isFxSelectListItem()) {
            //we have an FxSelectListItem in a SelectOne -> check if cascaded
            final long itemId = (Long) items.get(0).getValue();
            if (itemId >= 0) {
                FxSelectList list = CacheAdmin.getEnvironment().getSelectListItem(itemId).getList();
                restrictToGroups = list.isOnlySameLevelSelect();
                if (list.isCascaded()) {
                    List<SelectItem> converted = new ArrayList<SelectItem>(items.size());
                    for (SelectItem check : items) {
                        final Long currentItemId = (Long) check.getValue();
                        if (currentItemId < 0) {
                            converted.add(check);
                            continue;
                        }
                        final FxSelectListItem listItem = list.getItem(currentItemId);
                        final boolean forceDisplay = ((FxJSFSelectItem) check).isForceDisplay();
                        if (!listItem.getHasChildren() || forceDisplay) {
                            if (!forceDisplay)
                                check.setLabel(listItem.getLabelBreadcrumbPath());
                            else
                                restrictToGroups = false; //if forceDisplay is enabled, do not restrict to group!
                            check.setDescription(listItem.hasParentItem() ? String.valueOf(listItem.getParentItem().getId()) : "0");
                            converted.add(check);
                        }
                    }
                    items = converted;
                }
            }
        }

        ResponseWriter writer = context.getResponseWriter();
        Map attrMap = component.getAttributes();

        if( component instanceof UISelectMany && restrictToGroups) {
/*
full js code:
    var grpData_xx = [["1", 1],["2",1],["3",2],["4",2],["5",3],["6",1]];

    function processChange() {
        var s = document.getElementById('xx');
        function gg(v) {
            for(var x in grpData_xx)
                if(grpData_xx[x][0]==v)
                    return grpData_xx[x][1];
            return 0;
        }
        var g = -1;
        for(var o in s.childNodes) {
            if(!s.childNodes[o])
                continue;
            if(s.childNodes[o].selected) {
                g = gg(s.childNodes[o].value);
                break;
            }
        }
        if (g == -1)
            return;
        for(var c in s.childNodes) {
            if(!s.childNodes[c])
                continue;
            if(gg(s.childNodes[c].value)!=g && s.childNodes[c].selected!=undefined)
                s.childNodes[c].selected = false;
            }
        }
    }
*/            
          StringBuilder sb = new StringBuilder(750);
          sb.append("<script type=\"text/javascript\" language=\"JavaScript\">");
          sb.append("var msGrpData_").append(component.getId()).append("=[");
          if( items.size() > 0 ) {
            for(SelectItem check: items) {
              String grp = check.getDescription() == null ? "0" : check.getDescription();
              sb.append("[\"").append(check.getValue()).append("\",").append(Long.valueOf(grp)).append("],");
            }
            sb.deleteCharAt(sb.length()-1);
          }
          sb.append("];");
          sb.append("function pc_").append(component.getId()).append("(){");
          sb.append("var s=document.getElementById('").append(component.getClientId(context));
          sb.append("');function gg(v){for(var x in msGrpData_").append(component.getId()).append(")if(msGrpData_");
          sb.append(component.getId()).append("[x][0]==v)return msGrpData_").append(component.getId()).append("[x][1];return 0;}");
          sb.append("var g=-1;for(var o in s.childNodes){if(!s.childNodes[o])continue;if(s.childNodes[o].selected){g=gg(s.childNodes[o].value);break;}}");
          sb.append("if(g==-1)return;for(var c in s.childNodes){if(!s.childNodes[c])continue;if(gg(s.childNodes[c].value)!=g&&s.childNodes[c].selected!=undefined){s.childNodes[c].selected=false;}}");
          if (!StringUtils.isEmpty(String.valueOf(attrMap.get("onchange")))) {
            //execute the original onchange event
            sb.append(String.valueOf(attrMap.get("onchange"))).append(";");
          }  
          sb.append("}</script>\n");
          writer.write(sb.toString());
        }

        writer.startElement("select", component);
        if(restrictToGroups)
          writer.writeAttribute("onchange", "pc_"+component.getId()+"();", null);
        String id;
        if (null != (id = component.getId()) && !id.startsWith(UIViewRoot.UNIQUE_ID_PREFIX))
            //noinspection UnusedAssignment
            writer.writeAttribute("id", id = component.getClientId(context), "id");

        writer.writeAttribute("name", component.getClientId(context), "clientId");
        // render styleClass attribute if present.
        String styleClass;
        if (null != (styleClass = (String) component.getAttributes().get("styleClass"))) {
            writer.writeAttribute("class", styleClass, "styleClass");
        }

        if (component instanceof UISelectMany)
            writer.writeAttribute("multiple", true, "multiple");

        // If "size" is *not* set explicitly, we have to default it correctly
        Integer size = (Integer) component.getAttributes().get("size");
        if (size == null || size == Integer.MIN_VALUE) {
            // Determine how many option(s) we need to render, and update
            // the component's "size" attribute accordingly;  The "size"
            // attribute will be rendered as one of the "pass thru" attributes
            int itemCount;
            if ("javax.faces.Listbox".equals(component.getRendererType()))
                itemCount = 1; //only 1 item if we are a listbox
            else
                itemCount = FxJsfComponentUtils.getSelectlistOptionCount(items);
            size = itemCount;
        }
        writer.writeAttribute("size", size, "size");

        //render the components default attributes if present
        for (String att : FxJsfComponentUtils.SELECTLIST_ATTRIBUTES) {
            if (restrictToGroups && "onchange".equals(att))
                continue;
            if (attrMap.get(att) != null)
                writer.writeAttribute(att, attrMap.get(att), att);
        }

        //render each option
        renderOptions(context, component, items);
        writer.endElement("select");
    }

    /**
     * Render all present options
     *
     * @param context   faces context
     * @param component the component
     * @param items     all items to render
     * @throws IOException on errors
     */
    protected void renderOptions(FacesContext context, UIComponent component, List<SelectItem> items) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        assert (writer != null);

        Converter converter = null;
        if (component instanceof ValueHolder)
            converter = ((ValueHolder) component).getConverter();

        if (!items.isEmpty()) {
            Object currentSelections = FxJsfComponentUtils.getCurrentSelectedValues(component);
            Object[] submittedValues = FxJsfComponentUtils.getSubmittedSelectedValues(component);
            for (SelectItem item : items) {
                if (item instanceof SelectItemGroup) {
                    // render OPTGROUP
                    writer.startElement("optgroup", component);
                    writer.writeAttribute("label", item.getLabel(), "label");

                    // if the component is disabled, "disabled" attribute would be rendered
                    // on "select" tag, so don't render "disabled" on every option.
                    boolean componentDisabled = Boolean.TRUE.equals(component.getAttributes().get("disabled"));
                    if ((!componentDisabled) && item.isDisabled())
                        writer.writeAttribute("disabled", true, "disabled");

                    // render options of this group.
                    SelectItem[] itemsArray = ((SelectItemGroup) item).getSelectItems();
                    for (SelectItem currentOption : itemsArray)
                        renderOption(context, component, converter, currentOption, currentSelections, submittedValues);
                    writer.endElement("optgroup");
                } else
                    renderOption(context, component, converter, item, currentSelections, submittedValues);
            }
        }
    }

    /**
     * Render a single option
     *
     * @param context           faces context
     * @param component         the current component
     * @param converter         the converter
     * @param curItem           the item to render
     * @param currentSelections the current selections
     * @param submittedValues   all submitted values
     * @throws IOException on errors
     */
    protected void renderOption(FacesContext context, UIComponent component, Converter converter, SelectItem curItem,
                                Object currentSelections, Object[] submittedValues) throws IOException {
        ResponseWriter writer = context.getResponseWriter();

        writer.writeText("\t", component, null);
        writer.startElement("option", component);

        String valueString = getFormattedValue(context, component, curItem.getValue(), converter);
        writer.writeAttribute("value", valueString, "value");
        if (curItem instanceof FxJSFSelectItem) {
            //apply the style attribute if it is available
            writer.writeAttribute("style", ((FxJSFSelectItem) curItem).getStyle(), null);
        } else if (curItem.getValue() instanceof ObjectWithColor) {
            //apply the color to the style attribute
            String color = ((ObjectWithColor) (curItem.getValue())).getColor();
            if (!StringUtils.isEmpty(color))
                writer.writeAttribute("style", "color:" + color, null);
        }

        Object valuesArray;
        Object itemValue;
        boolean containsValue;
        if (submittedValues != null) {
            containsValue = FxJsfComponentUtils.containsaValue(submittedValues);
            if (containsValue) {
                valuesArray = submittedValues;
                itemValue = valueString;
            } else {
                valuesArray = currentSelections;
                itemValue = curItem.getValue();
            }
        } else {
            valuesArray = currentSelections;
            itemValue = curItem.getValue();
        }

        if (FxJsfComponentUtils.isSelectItemSelected(context, component, itemValue, valuesArray, converter)) {
            writer.writeAttribute("selected", true, "selected");
        }

        Boolean disabledAttr = (Boolean) component.getAttributes().get("disabled");
        boolean componentDisabled = disabledAttr != null && disabledAttr.equals(Boolean.TRUE);

        // if the component is disabled, "disabled" attribute would be rendered
        // on "select" tag, so don't render "disabled" on every option.
        if ((!componentDisabled) && curItem.isDisabled())
            writer.writeAttribute("disabled", true, "disabled");

        String labelClass;
        if (componentDisabled || curItem.isDisabled())
            labelClass = (String) component.getAttributes().get("disabledClass");
        else
            labelClass = (String) component.getAttributes().get("enabledClass");

        if (labelClass != null)
            writer.writeAttribute("class", labelClass, "labelClass");

        if (curItem.isEscape()) {
            String label = curItem.getLabel();
            if (label == null)
                label = valueString;
            writer.writeText(label, component, "label");
        } else
            writer.write(curItem.getLabel());

        writer.endElement("option");
        writer.writeText("\n", component, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getConvertedValue(FacesContext context, UIComponent component, Object submittedValue) throws ConverterException {
        if (component instanceof UISelectMany)
            return convertSelectManyValue(context, ((UISelectMany) component), (String[]) submittedValue);
        else
            return convertSelectOneValue(context, ((UISelectOne) component), (String) submittedValue);
    }

    /**
     * Overloads getFormattedValue to take a advantage of a previously
     * obtained converter.
     *
     * @param context      the FacesContext for the current request
     * @param component    UIComponent of interest
     * @param currentValue the current value of <code>component</code>
     * @param converter    the component's converter
     * @return the currentValue after any associated Converter has been
     *         applied
     * @throws ConverterException if the value cannot be converted
     */
    protected String getFormattedValue(FacesContext context, UIComponent component, Object currentValue, Converter converter)
            throws ConverterException {

        // formatting is supported only for components that support
        // converting value attributes.
        if (!(component instanceof ValueHolder)) {
            if (currentValue != null)
                return currentValue.toString();
            return null;
        }

        if (converter == null) {
            // If there is a converter attribute, use it to to ask application
            // instance for a converter with this identifer.
            converter = ((ValueHolder) component).getConverter();
        }

        if (converter == null) {
            // if value is null and no converter attribute is specified, then
            // return a zero length String.
            if (currentValue == null) {
                return "";
            }
            // Do not look for "by-type" converters for Strings
            if (currentValue instanceof String) {
                return (String) currentValue;
            }

            // if converter attribute set, try to acquire a converter
            // using its class type.
            try {
                converter = FxJsfUtils.getApplication().createConverter(currentValue.getClass());
            } catch (Exception e) {
                converter = null;
            }

            // if there is no default converter available for this identifier,
            // assume the model type to be String.
            if (converter == null) {
                return currentValue.toString();
            }
        }
        return converter.getAsString(context, component, currentValue);
    }
}
