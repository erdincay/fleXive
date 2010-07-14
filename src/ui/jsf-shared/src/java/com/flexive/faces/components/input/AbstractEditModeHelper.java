/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.beans.MessageBean;
import com.flexive.faces.beans.UserConfigurationBean;
import static com.flexive.faces.components.input.AbstractFxValueInputRenderer.*;
import static com.flexive.faces.components.input.RenderHelperUtils.*;
import com.flexive.faces.javascript.FxJavascriptUtils;
import static com.flexive.faces.javascript.FxJavascriptUtils.*;
import com.flexive.shared.*;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.*;
import com.flexive.war.FxRequest;
import com.flexive.war.JsonWriter;
import com.flexive.war.servlet.ThumbnailServlet;
import org.apache.commons.lang.StringUtils;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UISelectItems;
import javax.faces.component.UISelectMany;
import javax.faces.component.html.*;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.model.SelectItem;
import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Renders an FxValueInput component in edit mode.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public abstract class AbstractEditModeHelper implements RenderHelper {
    private static final String JS_OBJECT = "fxValue";

    protected final AbstractFxValueInput component;
    protected final String clientId;
    protected final FxValue value;

    protected boolean multiLine = false;
    protected boolean useHTMLEditor = false;
    protected int rows = -1;
    protected boolean selectManyCheckboxes;
    protected boolean needEmptyElementsInSelectList;

    protected FxEnvironment environment;

    public AbstractEditModeHelper(AbstractFxValueInput component, String clientId, FxValue value) {
        this.component = component;
        this.clientId = clientId;
        this.value = value;
        environment = CacheAdmin.getEnvironment();
        if (value != null && StringUtils.isNotBlank(value.getXPath())) {
            if (CacheAdmin.getEnvironment().assignmentExists(value.getXPath())) {
                FxPropertyAssignment pa = (FxPropertyAssignment) environment.getAssignment(value.getXPath());
                if (value instanceof FxString) {
                    multiLine = pa.isMultiLine();
                    if (multiLine) {
                        rows = pa.getMultiLines();
                        if (rows <= 1)
                            rows = -1;
                    }
                    useHTMLEditor = pa.getOption(FxStructureOption.OPTION_HTML_EDITOR).isValueTrue();
                } else if (value instanceof FxSelectMany) {
                    selectManyCheckboxes = pa.getOption(FxStructureOption.OPTION_SELECTMANY_CHECKBOXES).isValueTrue();
                    needEmptyElementsInSelectList = pa.getMultiplicity().isOptional();
                } else if (value instanceof FxSelectOne) {
                    needEmptyElementsInSelectList = pa.getMultiplicity().isOptional();
                }
            }
            else if (CacheAdmin.getEnvironment().propertyExists(value.getXPath())) {
                FxProperty p = CacheAdmin.getEnvironment().getProperty(value.getXPath());
                if (value instanceof FxString) {
                    multiLine = p.getOption(FxStructureOption.OPTION_MULTILINE).isValueTrue();
                    if (multiLine) {
                        rows = p.getMultiLines();
                        if (rows <= 1)
                            rows = -1;
                    }
                    useHTMLEditor = p.getOption(FxStructureOption.OPTION_HTML_EDITOR).isValueTrue();
                } else if (value instanceof FxSelectMany) {
                    selectManyCheckboxes = p.getOption(FxStructureOption.OPTION_SELECTMANY_CHECKBOXES).isValueTrue();
                    needEmptyElementsInSelectList = p.getMultiplicity().isOptional();
                } else if (value instanceof FxSelectOne) {
                    needEmptyElementsInSelectList = p.getMultiplicity().isOptional();
                }
            }
        }

        if (useHTMLEditor && !(value instanceof FxString))
            useHTMLEditor = false; //prevent showing HTML editor for non-string types

        if (value instanceof FxHTML && !useHTMLEditor) {
            //if no xpath is available, always show the HTML editor for FxHTML values
            if (StringUtils.isEmpty(value.getXPath()))
                useHTMLEditor = true;
        }
    }

    /**
     * Add an upload component with the given input ID. The result of this component must then be decoded
     * in {@link FxValueInputRenderer#processBinary(javax.faces.context.FacesContext, com.flexive.faces.components.input.AbstractFxValueInput, java.lang.String, com.flexive.shared.value.FxBinary, long) }.
     *
     * @param parent    the parent component
     * @param inputId   the input ID for the upload component
     */
    protected abstract void renderUploadComponent(UIComponent parent, String inputId);

    protected ContainerWriter newContainerWriter() {
        return new ContainerWriter();
    }

    protected DefaultLanguageRadioWriter newDefaultLanguageRadioWriter() {
        return new DefaultLanguageRadioWriter();
    }

    protected LanguageContainerWriter newLanguageContainerWriter() {
        return new LanguageContainerWriter();
    }

    protected LanguageSelectWriter newLanguageSelectWriter() {
        return new LanguageSelectWriter();
    }

    protected TextAreaWriter newTextAreaWriter() {
        return new TextAreaWriter();
    }

    protected YuiDateInputWriter newYuiDateInputWriter() {
        return new YuiDateInputWriter();
    }

    protected YuiAutocompleteWriter newYuiAutocompleteWriter() {
        return new YuiAutocompleteWriter();
    }


    /**
     * {@inheritDoc}
     */
    public void encodeMultiLanguageField() throws IOException {
        final List<FxLanguage> languages = AbstractFxValueInputRenderer.getLanguages();
        //ensureDefaultLanguageExists(value, languages);
        final String radioName = clientId + AbstractFxValueInputRenderer.DEFAULT_LANGUAGE;

        final ContainerWriter container = newContainerWriter();
        container.setId(stripForm(clientId) + "_container");
        container.setInputClientId(clientId);
        component.getChildren().add(container);

        final List<UIComponent> rows = new ArrayList<UIComponent>();
        final HashMap<Long, LanguageSelectWriter.InputRowInfo> rowInfos =
                new HashMap<Long, LanguageSelectWriter.InputRowInfo>(languages.size());
        boolean first = true;
        for (final FxLanguage language : languages) {
            final String containerId = clientId + AbstractFxValueInputRenderer.LANG_CONTAINER + language.getId();
            final String inputId = clientId + AbstractFxValueInputRenderer.INPUT + language.getId();
            rowInfos.put(language.getId(), new LanguageSelectWriter.InputRowInfo(containerId, inputId));

            final LanguageContainerWriter languageContainer = newLanguageContainerWriter();
            languageContainer.setId(stripForm(inputId) + "_langContainer");
            languageContainer.setContainerId(containerId);
            languageContainer.setLanguageId(language.getId());
            languageContainer.setFirstRow(first);
            rows.add(languageContainer);

            encodeDefaultLanguageRadio(languageContainer, clientId, radioName, language);
            encodeField(languageContainer, inputId, language);
            first = false;
        }

        final LanguageSelectWriter languageSelect = newLanguageSelectWriter();
        languageSelect.setId(stripForm(clientId) + "_langSelect");
        languageSelect.setInputClientId(clientId);
        languageSelect.setRowInfos(rowInfos);
        languageSelect.setDefaultLanguageId(value.getDefaultLanguage());
        container.getChildren().add(languageSelect);
        // add children to language select because the language select needs to write code before and after the input rows
        languageSelect.getChildren().addAll(rows);
    }

    /**
     * Render the default language radiobutton for the given language.
     *
     * @param parent    the parent component
     * @param clientId  the client ID
     * @param radioName name of the radio input control
     * @param language  the language for which this input should be rendered @throws IOException if a io error occured
     */
    protected void encodeDefaultLanguageRadio(LanguageContainerWriter parent, String clientId, final String radioName, final FxLanguage language) {
        final DefaultLanguageRadioWriter radio = newDefaultLanguageRadioWriter();
        radio.setId(stripForm(clientId) + "_defaultLanguageRadio_" + language.getId());
        radio.setRadioName(radioName);
        radio.setLanguageId(language.getId());
        radio.setContainerId(parent.getContainerId());
        radio.setLanguageCode(language.getIso2digit());
        radio.setInputClientId(clientId);
        parent.getChildren().add(radio);
    }


    /**
     * {@inheritDoc}
     */
    public void render() throws IOException {
        RenderHelperUtils.render(this, component, clientId, value);
    }

    /**
     * {@inheritDoc}
     */
    public void encodeField(UIComponent parent, String inputId, FxLanguage language) throws IOException {
        if (language == null) {
            final ContainerWriter container = newContainerWriter();
            container.setId(stripForm(clientId) + "_" + "container");
            container.setInputClientId(clientId);
            parent.getChildren().add(container);
            // use container as parent for all subsequent operations
            parent = container;
        }
        if (useHTMLEditor || multiLine) {
            renderTextArea(parent, inputId, language, rows, useHTMLEditor);
        } else if (value instanceof FxSelectOne) {
            renderSelectOne(parent, inputId, language);
        } else if (value instanceof FxSelectMany) {
            renderSelectMany(parent, inputId, language);
        } else if (value instanceof FxDate) {
            renderDateInput(parent, inputId, language);
        } else if (value instanceof FxDateTime) {
            renderDateTimeInput(parent, inputId, language);
        } else if (value instanceof FxDateRange) {
            renderDateRangeInput(parent, inputId, language);
        } else if (value instanceof FxDateTimeRange) {
            renderDateTimeRangeInput(parent, inputId, language);
        } else if (value instanceof FxReference) {
            renderReferenceSelect(parent, inputId, language);
        } else if (value instanceof FxBinary) {
            renderBinary(parent, inputId, language);
        } else if (value instanceof FxBoolean) {
            renderCheckbox(parent, inputId, language);
        } else {
            renderTextInput(parent, inputId, language);
        }
    }

    protected void renderTextInput(UIComponent parent, String inputId, FxLanguage language) throws IOException {
        renderTextInput(component, parent, value, inputId, language != null ? language.getId() : -1);
        if (getInputValue(component) instanceof FxReference) {
            // add a browse reference popup button
            renderReferencePopupButton(parent, inputId);
        }
    }

    protected void renderTextInput(AbstractFxValueInput inputComponent, UIComponent parent, FxValue value, final String inputId, long languageId) throws IOException {
        renderBasicTextInput(inputComponent, parent, newYuiAutocompleteWriter(), value, inputId, languageId);
    }
    
    protected static void renderBasicTextInput(AbstractFxValueInput inputComponent, UIComponent parent,
            YuiAutocompleteWriter autocompleteWriter, FxValue value, final String inputId, long languageId) throws IOException {

        final HtmlInputText input = (HtmlInputText) FxJsfUtils.addChildComponent(parent, stripForm(inputId), HtmlInputText.COMPONENT_TYPE, true);
        addHtmlAttributes(inputComponent, input);
        if (value.getMaxInputLength() > 0) {
            input.setMaxlength(value.getMaxInputLength());
        }
        input.setValue(getTextValue(value, languageId));
        input.setStyleClass(CSS_VALUE_INPUT_FIELD + " " + CSS_TEXT_INPUT + singleLanguageStyle(languageId));

        // add autocomplete YUI component
        if (StringUtils.isNotBlank(inputComponent.getAutocompleteHandler())) {
            autocompleteWriter.setId(stripForm(inputId) + "_autocomplete");
            autocompleteWriter.setInputClientId(inputId);
            autocompleteWriter.setAutocompleteHandler(inputComponent.getAutocompleteHandler());
            parent.getChildren().add(parent.getChildren().size() - 1, autocompleteWriter);
        }
    }

    protected void renderTextArea(UIComponent parent, final String inputId, final FxLanguage language, final int rows, final boolean useHTMLEditor) throws IOException {
        final TextAreaWriter textArea = newTextAreaWriter();
        textArea.setInputClientId(inputId);
        textArea.setLanguageId(language != null ? language.getId() : -1);
        textArea.setRows(rows);
        textArea.setUseHTMLEditor(useHTMLEditor);
        parent.getChildren().add(textArea);
    }

    /**
     * Render additional HTML attributes passed to the FxValueInput component.
     *
     * @param component the input component
     * @param writer    the output writer
     * @throws IOException if the output could not be written
     */
    protected static void writeHtmlAttributes(AbstractFxValueInput component, ResponseWriter writer) throws IOException {
        if (StringUtils.isNotBlank(component.getOnchange())) {
            writer.writeAttribute("onchange", component.getOnchange(), null);
        }
    }

    protected void renderSelectOne(UIComponent parent, String inputId, FxLanguage language) throws IOException {
        final FxSelectOne selectValue = (FxSelectOne) value;
        // create selectone component
        final HtmlSelectOneListbox listbox = (HtmlSelectOneListbox) createUISelect(parent, inputId, HtmlSelectOneListbox.COMPONENT_TYPE);
        listbox.setSize(1);
        listbox.setStyleClass(CSS_VALUE_INPUT_FIELD + " " + AbstractFxValueInputRenderer.CSS_INPUTELEMENTWIDTH + singleLanguageStyle(language));
        // update posted value
        if (!selectValue.isEmpty() && selectValue.getTranslation(language) != null) {
            listbox.setValue(selectValue.getTranslation(language).getId());
        }
        storeSelectItems(listbox, selectValue.getSelectList(), needEmptyElementsInSelectList);
    }

    protected void renderSelectMany(UIComponent parent, String inputId, FxLanguage language) {
        final FxSelectMany selectValue = (FxSelectMany) value;
        final SelectMany sm = selectValue.getTranslation(language) != null ? selectValue.getTranslation(language) : new SelectMany(selectValue.getSelectList());
        final Long[] selected = new Long[sm.getSelected().size()];
        for (int i = 0; i < selected.length; i++) {
            selected[i] = sm.getSelected().get(i).getId();
        }
        final UISelectMany input;
        if (selectManyCheckboxes) {
            // render as checkboxes
            final HtmlSelectManyCheckbox checkboxes = (HtmlSelectManyCheckbox) createUISelect(parent, inputId, HtmlSelectManyCheckbox.COMPONENT_TYPE);
            checkboxes.setLayout("pageDirection");
            input = checkboxes;
        } else {
            // render a "multiple" select list
            final HtmlSelectManyListbox listbox = (HtmlSelectManyListbox) createUISelect(parent, inputId, HtmlSelectManyListbox.COMPONENT_TYPE);
            listbox.setStyleClass(CSS_VALUE_INPUT_FIELD + " " + AbstractFxValueInputRenderer.CSS_INPUTELEMENTWIDTH + singleLanguageStyle(language));
            // automatically limit select list rows for very long lists. "Real" single-line input is not possible with a
            // standard listbox widget if multiple selection should still be possible,
            // so we're only restraining the height a bit stronger
            listbox.setSize(Math.min(selectValue.getSelectList().getItems().size(), component.isForceLineInput() ? 3 : 7));
            input = listbox;
        }
        storeSelectItems(input, selectValue.getSelectList(), false);
        input.setSelectedValues(selected);
    }

    protected static void addHtmlAttributes(AbstractFxValueInput component, UIComponent target) {
        if (component.getOnchange() != null) {
            //noinspection unchecked
            target.getAttributes().put("onchange", component.getOnchange());
        }
    }

    protected UIInput createUISelect(UIComponent parent, String inputId, String componentType) {
        final UIInput listbox = (UIInput) FxJsfUtils.addChildComponent(parent, stripForm(inputId), componentType, true);
        addHtmlAttributes(component, listbox);
        return listbox;
    }

    protected void storeSelectItems(UIInput listbox, FxSelectList selectList, boolean includeEmptyElement) {
        if (selectList == null) {
            throw new FxInvalidParameterException("selectList", "ex.jsf.valueInput.select.emptyList",
                    component.getClientId(FacesContext.getCurrentInstance())).asRuntimeException();
        }
        FxSelectList _selectList = selectList;      // fallback if the id is -1 keep the items
        if (selectList.getId() > 0) // references have id -1 and are not in cache
            _selectList = CacheAdmin.getEnvironment().getSelectList(selectList.getId()); //load the list to ensure it is up-to-date
        // store available items in select component
        final UISelectItems selectItems = (UISelectItems) FxJsfUtils.createComponent(UISelectItems.COMPONENT_TYPE);
        selectItems.setId(stripForm(listbox.getId()) + "_items");
        selectItems.setTransient(true);
        final List<SelectItem> items = FxJsfUtils.asSelectList(_selectList);
        if (includeEmptyElement) {
            // include only if no empty element (with ID -1) exists
            if (!FxSharedUtils.getSelectableObjectIdList(_selectList.getItems()).contains(-1L)) {
                items.add(0, new SelectItem(-1L, ""));
            } else {
                // if an empty element exist, we need to find it, and remove it so that we could add one at the begining
                int emptyIndex = -1;
                int index = 0;
                for (SelectItem si : items) {
                    if (si.getValue().equals(-1L)) {
                        emptyIndex = index;
                        break;
                    }
                    index++;
                }
                if (emptyIndex > 0) {
                    items.remove(emptyIndex);
                    items.add(0, new SelectItem(-1L, ""));
                }
            }
        }
        selectItems.setValue(items);
        listbox.setConverter(FxJsfUtils.getApplication().createConverter(Long.class));
        listbox.getChildren().add(selectItems);
    }

    protected void renderDateInput(UIComponent parent, String inputId, FxLanguage language) {
        final Date date = value.isTranslationEmpty(language) || !value.isValid(language) ? null : (Date) value.getTranslation(language);
        createDateInput(parent, inputId, date);
    }

    protected void createDateInput(UIComponent parent, String inputId, Date date) {
        final HtmlInputText input = (HtmlInputText) FxJsfUtils.addChildComponent(parent, stripForm(inputId), HtmlInputText.COMPONENT_TYPE, true);
        input.setSize(10);
        input.setMaxlength(10);
        input.setValue(date == null ? "" : FxFormatUtils.toString(date));
        input.setStyleClass(CSS_VALUE_INPUT_FIELD);

        final HtmlGraphicImage img = (HtmlGraphicImage) FxJsfUtils.addChildComponent(
                parent, "calendarButton_" + stripForm(inputId), HtmlGraphicImage.COMPONENT_TYPE, true
        );
        component.setPackagedImageUrl(img, "/images/calendar.gif");
        img.setStyleClass("button");

        final YuiDateInputWriter diw = newYuiDateInputWriter();
        diw.setId(stripForm(inputId) + "_yuidate");
        diw.setInputClientId(inputId);
        diw.setButtonId(img.getClientId(FacesContext.getCurrentInstance()));
        diw.setDate(date);
        parent.getChildren().add(diw);
    }


    protected void renderDateRangeInput(UIComponent parent, String inputId, FxLanguage language) {
        final DateRange range = value.isTranslationEmpty(language) ? null : (DateRange) value.getTranslation(language);
        createDateInput(parent, inputId + "_1", range != null ? range.getLower() : null);
        renderLiteral(parent, " - ", inputId + "_sep");
        createDateInput(parent, inputId + "_2", range != null ? range.getUpper() : null);
    }


    protected void renderDateTimeInput(UIComponent parent, String inputId, FxLanguage language) {
        final Date date =
                value.isTranslationEmpty(language) || (!value.isValid(language))
                        ? null      // no or invalid translation - do not set date
                        : (Date) value.getTranslation(language);
        createDateTimeInput(parent, inputId, date);
    }

    protected void createDateTimeInput(UIComponent parent, String inputId, Date date) {
        createDateInput(parent, inputId, date);
        final Calendar cal = date != null ? Calendar.getInstance() : null;
        if (cal != null) {
            cal.setTime(date);
        }
        renderTimeInput(parent, inputId, "_hh", cal != null ? cal.get(Calendar.HOUR_OF_DAY) : -1);
        renderLiteral(parent, ":", inputId + "_hsep");
        renderTimeInput(parent, inputId, "_mm", cal != null ? cal.get(Calendar.MINUTE) : -1);
        renderLiteral(parent, ":", inputId + "_msep");
        renderTimeInput(parent, inputId, "_ss", cal != null ? cal.get(Calendar.SECOND) : -1);
    }

    protected void renderTimeInput(UIComponent parent, String inputId, String suffix, int value) {
        final HtmlInputText input = (HtmlInputText) FxJsfUtils.addChildComponent(parent, stripForm(inputId) + suffix, HtmlInputText.COMPONENT_TYPE, true);
        input.setSize(2);
        input.setMaxlength(2);
        input.setStyleClass(CSS_VALUE_INPUT_FIELD);
        if (value != -1) {
            input.setValue(String.format("%02d", value));
        }
    }

    protected void renderDateTimeRangeInput(UIComponent parent, String inputId, FxLanguage language) {
        final DateRange range = value.isTranslationEmpty(language) ? null : (DateRange) value.getTranslation(language);
        createDateTimeInput(parent, inputId + "_1", range != null ? range.getLower() : null);
        renderLiteral(parent, " -<br/>", inputId + "_rangesep").setEscape(false);
        createDateTimeInput(parent, inputId + "_2", range != null ? range.getUpper() : null);
    }

    protected void renderReferenceSelect(UIComponent parent, String inputId, FxLanguage language) throws IOException {
        // render hidden input that contains the actual reference
        final HtmlInputHidden inputPk = (HtmlInputHidden) FxJsfUtils.addChildComponent(parent, stripForm(inputId), HtmlInputHidden.COMPONENT_TYPE, true);
        // render hidden input where the caption is stored
        final HtmlInputHidden inputCaption = (HtmlInputHidden) FxJsfUtils.addChildComponent(parent, stripForm(inputId) + "_caption", HtmlInputHidden.COMPONENT_TYPE, true);

        // render popup button
        renderReferencePopupButton(parent, inputId);

        // render image container (we need this since the image id attribute does not get rendered)
        final HtmlOutputText captionContainer = (HtmlOutputText) FxJsfUtils.addChildComponent(parent, stripForm(inputId) + "_preview", HtmlOutputText.COMPONENT_TYPE, true);

        // render caption
        if (!value.isEmpty() && ((language != null && !value.isTranslationEmpty(language)) || language == null)) {
            final ReferencedContent reference = ((FxReference) value).getTranslation(language);
            final String caption = reference.getCaption();
            captionContainer.setValue(caption);
            inputCaption.setValue(caption);
            inputPk.setValue(reference.toString());
        }
        // render the image itself
        /*final HtmlGraphicImage image = (HtmlGraphicImage) FxJsfUtils.addChildComponent(captionContainer, HtmlGraphicImage.COMPONENT_TYPE);
        image.setStyle("border:0");
        if (!value.isEmpty() && ((language != null && !value.isTranslationEmpty(language)) || language == null)) {
            // render preview image
            final FxPK translation = referenceValue.getTranslation(language);
            image.setUrl(ThumbnailServlet.getUrl(translation, BinaryDescriptor.PreviewSizes.PREVIEW2));
            hidden.setValue(translation);
        } else {
            image.setUrl("/pub/images/empty.gif");
        }*/
    }

    protected void renderReferencePopupButton(UIComponent parent, String inputId) {
        final HtmlOutputLink link = (HtmlOutputLink) FxJsfUtils.addChildComponent(parent, stripForm(inputId) + "_refButtonLink", HtmlOutputLink.COMPONENT_TYPE, true);
        link.setValue("javascript:flexive.input.openReferenceQueryPopup('" + StringUtils.defaultString(value.getXPath()) + "', '"
                + inputId + "', '" + getForm(inputId) + "')");
        final HtmlGraphicImage button = (HtmlGraphicImage) FxJsfUtils.addChildComponent(link, stripForm(inputId) + "_refButton", HtmlGraphicImage.COMPONENT_TYPE, true);
        component.setPackagedImageUrl(button, "/images/findReferences.png");
        button.setStyle("border:0");
        button.setStyleClass(AbstractFxValueInputRenderer.CSS_FIND_REFERENCES);
    }

    protected void renderBinary(UIComponent parent, String inputId, FxLanguage language) throws IOException {
        if (!value.isEmpty() && (language == null || value.translationExists((int) language.getId()))) {
            final BinaryDescriptor descriptor = ((FxBinary) value).getTranslation(language);
            if (!descriptor.isNewBinary()) {
                final HtmlGraphicImage image = (HtmlGraphicImage) FxJsfUtils.addChildComponent(parent, stripForm(clientId) + "_thumb", HtmlGraphicImage.COMPONENT_TYPE, true);
                image.setUrl(ThumbnailServlet.getLink(XPathElement.getPK(value.getXPath()),
                        BinaryDescriptor.PreviewSizes.PREVIEW2, value.getXPath(), descriptor.getCreationTime(), language));
                if (component.isReadOnlyShowTranslations()) {
                    //TODO: might add another attribute to indicate if description should be visible
                    image.setStyle("padding: 5px;");
                    addImageDescriptionComponent(component, parent, language, inputId + "_desc");
                }
            } else
                addImageDescriptionComponent(component, parent, language, inputId + "_desc");
        }
        renderUploadComponent(parent, inputId);
    }

    protected void renderCheckbox(UIComponent parent, String inputId, FxLanguage language) throws IOException {
        MessageBean mb = MessageBean.getInstance();
        String[] toolTips = {mb.getMessage("FxValueInput.selectBox.value.true"),
            mb.getMessage("FxValueInput.selectBox.value.false"),
            mb.getMessage("FxValueInput.selectBox.value.empty")};

        final HtmlSelectBooleanCheckbox checkbox = (HtmlSelectBooleanCheckbox) FxJsfUtils.addChildComponent(parent, stripForm(inputId), HtmlSelectBooleanCheckbox.COMPONENT_TYPE, true);
        Boolean b = (Boolean) value.getTranslation(language);
        checkbox.setValue(b);
        checkbox.setTitle(value.isTranslationEmpty(language) ? toolTips[2] : b ? toolTips[0] : toolTips[1]);
        addHtmlAttributes(component, checkbox);
        checkbox.setStyleClass(
                CSS_VALUE_INPUT_FIELD
                + (value.isTranslationEmpty(language) ? " " + CSS_EMPTY : "")
        );

        checkbox.setOnclick("flexive.input.onTristateCheckboxChanged('" + inputId + "', new Array('"+StringUtils.join(toolTips,"' ,'") +"')" +")");
        // render hidden input to represent "empty"
        final HtmlInputHidden hidden = (HtmlInputHidden) FxJsfUtils.addChildComponent(parent, stripForm(inputId) + "_empty", HtmlInputHidden.COMPONENT_TYPE, true);
        hidden.setValue(value.isTranslationEmpty(language));
    }

    protected HtmlOutputText renderLiteral(UIComponent parent, String value, String inputId) {
        HtmlOutputText output = (HtmlOutputText) FxJsfUtils.addChildComponent(parent, stripForm(inputId), HtmlOutputText.COMPONENT_TYPE, true);
        output.setValue(value);
        return output;
    }

    protected static String getTextValue(FxValue value, long languageId) {
        if (value.isEmpty()) {
            return "";
        }
        final Object writeValue = getWriteValue(value, languageId);
        //noinspection unchecked
        return value.isValid() ? value.getStringValue(writeValue) :
                (writeValue != null ? writeValue.toString() : "");
    }

    protected static Object getWriteValue(FxValue value, long languageId) {
        final Object writeValue;
        if (languageId != -1) {
            //noinspection unchecked
            writeValue = value.isTranslationEmpty(languageId) ? value.getEmptyValue() : value.getTranslation(languageId);
        } else {
            //noinspection unchecked
            writeValue = value.getDefaultTranslation();
        }
        return writeValue;
    }

    protected static String singleLanguageStyle(long languageId) {
        return (languageId == -1 ? " " + AbstractFxValueInputRenderer.CSS_SINGLE_LANG : "");
    }

    protected static String singleLanguageStyle(FxLanguage language) {
        return singleLanguageStyle(language != null ? language.getId() : -1);
    }

    /**
     * Renders the container of a single language input for multilanguage input components.
     * Remember to add all elements of the language row to this component, not the parent.
     */
    public static class LanguageContainerWriter extends DeferredInputWriter {
        private long languageId;
        private String containerId;
        private boolean firstRow;

        public long getLanguageId() {
            return languageId;
        }

        public void setLanguageId(long languageId) {
            this.languageId = languageId;
        }

        public String getContainerId() {
            return containerId;
        }

        public void setContainerId(String containerId) {
            this.containerId = containerId;
        }

        public boolean isFirstRow() {
            return firstRow;
        }

        public void setFirstRow(boolean firstRow) {
            this.firstRow = firstRow;
        }

        @Override
        public void encodeBegin(FacesContext facesContext) throws IOException {
            final ResponseWriter writer = facesContext.getResponseWriter();
            writer.startElement("div", null);
            writer.writeAttribute("id", containerId, null);
            writer.writeAttribute("class",
                    AbstractFxValueInputRenderer.CSS_LANG_CONTAINER
                    + (firstRow ? " " + AbstractFxValueInputRenderer.CSS_LANG_CONTAINER_FIRST : ""),
                    null
            );
            if (languageId != getInputValue().getDefaultLanguage()) {
                writer.writeAttribute("style", "display:none", null);
            }
        }

        @Override
        public void encodeEnd(FacesContext facesContext) throws IOException {
            facesContext.getResponseWriter().endElement("div");
        }

        @Override
        public Object saveState(FacesContext context) {
            final Object[] state = new Object[4];
            state[0] = super.saveState(context);
            state[1] = languageId;
            state[2] = containerId;
            state[3] = firstRow;
            return state;
        }

        @Override
        public void restoreState(FacesContext context, Object stateValue) {
            final Object[] state = (Object[]) stateValue;
            super.restoreState(context, state[0]);
            this.languageId = (Long) state[1];
            this.containerId = (String) state[2];
            this.firstRow = (Boolean) state[3];
        }
    }

    /**
     * Renders the language select for multilanguage components and adds a Javascript-based
     * row switcher.
     */
    public static class LanguageSelectWriter extends DeferredInputWriter {
        public static class InputRowInfo implements Serializable {
            private static final long serialVersionUID = -2146282630839499609L;
            
            private final String rowId;
            private final String inputId;

            public InputRowInfo(String rowId, String inputId) {
                this.rowId = rowId;
                this.inputId = inputId;
            }

            public String getRowId() {
                return rowId;
            }

            public String getInputId() {
                return inputId;
            }
        }

        private Map<Long, InputRowInfo> rowInfos;
        private String languageSelectId;
        private long defaultLanguageId;

        public Map<Long, InputRowInfo> getRowInfos() {
            return rowInfos;
        }

        public void setRowInfos(Map<Long, InputRowInfo> rowInfos) {
            this.rowInfos = rowInfos;
        }

        public void setDefaultLanguageId(long defaultLanguageId) {
            this.defaultLanguageId = defaultLanguageId;
        }

        @Override
        public void encodeBegin(FacesContext facesContext) throws IOException {
            final ResponseWriter writer = facesContext.getResponseWriter();
            languageSelectId = inputClientId + AbstractFxValueInputRenderer.LANG_SELECT;
            writer.startElement("select", null);
            writer.writeAttribute("name", languageSelectId, null);
            writer.writeAttribute("id", languageSelectId, null);
            writer.writeAttribute("class", "languages", null);
            writer.writeAttribute("onchange", "document.getElementById('" + inputClientId + "')."
                    + JS_OBJECT + ".onLanguageChanged(this)", null);
            // use the current page input language 
            final long inputLanguageId = UserConfigurationBean.getUserInputLanguageId();
            writer.startElement("option", null);
            writer.writeAttribute("value", "-2", null);
            writer.writeText(MessageBean.getInstance().getMessage("FxValueInput.language.all.short"), null);
            writer.endElement("option");
            for (FxLanguage language : AbstractFxValueInputRenderer.getLanguages()) {
                writer.startElement("option", null);
                writer.writeAttribute("value", language.getId(), null);
                if ((inputLanguageId == -1 && language.getId() == getInputValue().getDefaultLanguage())
                        || (inputLanguageId == language.getId())) {
                    writer.writeAttribute("selected", "selected", null);
                }
                writer.writeText(language.getIso2digit(), null);
                writer.endElement("option");
            }
            writer.endElement("select");

//            writer.startElement("br", null);
//            writer.writeAttribute("clear", "all", null);
//            writer.endElement("br");
        }

        @Override
        public void encodeEnd(FacesContext facesContext) throws IOException {
            final ResponseWriter writer = facesContext.getResponseWriter();
            // attach JS handler object to container div
            final JsonWriter jsonWriter = new JsonWriter().startMap();
            for (Map.Entry<Long, InputRowInfo> entry : rowInfos.entrySet()) {
                jsonWriter.startAttribute(String.valueOf(entry.getKey()))
                        .startMap()
                        .writeAttribute("rowId", entry.getValue().getRowId())
                        .writeAttribute("inputId", entry.getValue().getInputId())
                        .closeMap();
            }
            jsonWriter.closeMap().finishResponse();
            FxJavascriptUtils.beginJavascript(writer);
            writer.write(MessageFormat.format(
                            "  document.getElementById(''{0}'')." + JS_OBJECT
                            + " = new flexive.input.FxMultiLanguageValueInput(''{0}'', ''{1}'', {2}, ''{3}'', ''{4}'');\n"
                            + "  document.getElementById(''{3}'').onchange();\n",
                    inputClientId, inputClientId + AbstractFxValueInputRenderer.LANG_CONTAINER,
                    jsonWriter.toString(),
                    languageSelectId,
                    defaultLanguageId
            ));
            FxJavascriptUtils.endJavascript(writer);
        }

        @Override
        public Object saveState(FacesContext context) {
            final Object[] state = new Object[3];
            state[0] = super.saveState(context);
            state[1] = rowInfos;
            state[2] = defaultLanguageId;
            return state;
        }

        @SuppressWarnings({"unchecked"})
        @Override
        public void restoreState(FacesContext context, Object stateValue) {
            final Object[] state = (Object[]) stateValue;
            super.restoreState(context, state[0]);
            rowInfos = (Map<Long, InputRowInfo>) state[1];
            defaultLanguageId = (Long) state[2];
        }
    }

    /**
     * Renders a radio button to choose the default language of a multilanguage component.
     */
    public static class DefaultLanguageRadioWriter extends DeferredInputWriter {
        private long languageId;
        private String radioName;
        private String containerId;
        private String languageCode;

        public long getLanguageId() {
            return languageId;
        }

        public void setLanguageId(long languageId) {
            this.languageId = languageId;
        }

        public String getLanguageCode() {
            return languageCode;
        }

        public void setLanguageCode(String languageCode) {
            this.languageCode = languageCode;
        }

        public String getRadioName() {
            return radioName;
        }

        public void setRadioName(String radioName) {
            this.radioName = radioName;
        }

        public String getContainerId() {
            return containerId;
        }

        public void setContainerId(String containerId) {
            this.containerId = containerId;
        }

        @Override
        public void encodeBegin(FacesContext facesContext) throws IOException {
            final ResponseWriter writer = facesContext.getResponseWriter();

            writer.startElement("div", null);
            writer.writeAttribute("id", containerId + "_language", null);
            writer.writeAttribute("class", AbstractFxValueInputRenderer.CSS_LANG_ICON, null);
            writer.writeText(languageCode, null);
            writer.endElement("div");

            writer.startElement("input", null);
            writer.writeAttribute("type", "checkbox", null);
            writer.writeAttribute("name", radioName, null);
            writer.writeAttribute("value", languageId, null);
            writer.writeAttribute("class", "fxValueDefaultLanguageRadio", null);

            // set default language to the user's desired input language (for empty values only) - FX-729
            final boolean useDefaultInputLanguage = getInputValue().isEmpty()
                    && languageId == UserConfigurationBean.getUserInputLanguageId();

            if ((languageId == getInputValue().getDefaultLanguage() && !getInputValue().isEmpty())
                    || useDefaultInputLanguage) {
                writer.writeAttribute("checked", "true", null);
            }
            writer.writeAttribute("onclick", "document.getElementById('" + inputClientId
                    + "')." + JS_OBJECT + ".onDefaultLanguageChanged(this, " + languageId + ")", null);
            writer.endElement("input");
        }

        @Override
        public Object saveState(FacesContext context) {
            final Object[] state = new Object[5];
            state[0] = super.saveState(context);
            state[1] = languageId;
            state[2] = radioName;
            state[3] = containerId;
            state[4] = languageCode;
            return state;
        }

        @Override
        public void restoreState(FacesContext context, Object stateValue) {
            final Object[] state = (Object[]) stateValue;
            super.restoreState(context, state[0]);
            languageId = (Long) state[1];
            radioName = (String) state[2];
            containerId = (String) state[3];
            languageCode = (String) state[4];
        }
    }

    /**
     * Renders a text area for plain-text or HTML input values.
     */
    public static class TextAreaWriter extends DeferredInputWriter {
        private long languageId;
        private int rows = -1;
        private boolean useHTMLEditor = false;

        public long getLanguageId() {
            return languageId;
        }

        public void setLanguageId(long languageId) {
            this.languageId = languageId;
        }

        public void setRows(int rows) {
            this.rows = rows;
        }

        public int getRows() {
            return rows;
        }

        public void setUseHTMLEditor(boolean useHTMLEditor) {
            this.useHTMLEditor = useHTMLEditor;
        }

        @Override
        public void encodeBegin(FacesContext facesContext) throws IOException {
            final ResponseWriter writer = facesContext.getResponseWriter();
            final FxValue value = getInputValue();
            if (getInputComponent().isForceLineInput()) {
                renderBasicTextInput(getInputComponent(), this, newYuiAutocompleteWriter(), value, inputClientId, languageId);
                return;
            }
            // make textareas resizable? - IE <= 7.0 not supported, because it cannot scale textareas to 100% height
            final boolean makeResizeable = !FxJsfUtils.isOlderBrowserThan(FxRequest.Browser.IE, 8.0);

            final String wrapperElementId = inputClientId + "_wrap";
            writer.startElement("div", null);
            writer.writeAttribute("class",
                    (useHTMLEditor ? CSS_TEXTAREA_HTML_OUTER : CSS_TEXTAREA_OUTER)
                            + (makeResizeable ? " " + CSS_RESIZEABLE : ""),
                    null
            );
            writer.writeAttribute("id", wrapperElementId, null);
            writer.startElement("textarea", null);
            writer.writeAttribute("id", inputClientId, null);
            writeHtmlAttributes(getInputComponent(), writer);
            if (useHTMLEditor) {
                // render tinyMCE editor
                writer.writeAttribute("name", inputClientId, null);
                writer.writeAttribute("class", CSS_VALUE_INPUT_FIELD + " " + CSS_TEXTAREA_HTML + singleLanguageStyle(languageId), null);
                writer.writeText(getTextValue(value, languageId), null);
                writer.endElement("textarea");
                writer.endElement("div");
                beginJavascript(writer);
                writer.write("flexive.input.initHtmlEditor(false);\n");
                writer.write("tinyMCE.execCommand('mceAddControl', false, '" + inputClientId + "');\n");
                endJavascript(writer);
            } else {
                // render standard text area
                writer.writeAttribute("name", inputClientId, null);
                writer.writeAttribute("class", CSS_VALUE_INPUT_FIELD + " " + AbstractFxValueInputRenderer.CSS_TEXTAREA + singleLanguageStyle(languageId), null);
                if (rows > 0)
                    writer.writeAttribute("rows", String.valueOf(rows), null);
                writer.writeText(getTextValue(value, languageId), null);
                writer.endElement("textarea");
                writer.endElement("div");
                if (makeResizeable) {
                    FxJavascriptUtils.makeResizable(writer, wrapperElementId);
                }

            }
        }

        @Override
        public Object saveState(FacesContext context) {
            final Object[] state = new Object[2];
            state[0] = super.saveState(context);
            state[1] = languageId;
            return state;
        }

        @Override
        public void restoreState(FacesContext context, Object stateValue) {
            final Object[] state = (Object[]) stateValue;
            super.restoreState(context, state[0]);
            languageId = (Long) state[1];
        }

        protected YuiAutocompleteWriter newYuiAutocompleteWriter() {
            return new YuiAutocompleteWriter();
        }
    }

    public static class YuiAutocompleteWriter extends DeferredInputWriter {
        private String autocompleteHandler;

        @Override
        public void encodeBegin(FacesContext facesContext) throws IOException {
            final ResponseWriter out = facesContext.getResponseWriter();
            // write autocomplete container
            final String containerId = inputClientId + "_ac";
            out.write("<div id=\"" + containerId + "\" class=\"fxValueInputAutocomplete\"> </div>");

            // initialize autocomplete
            beginJavascript(out);
            writeYahooRequires(out, "autocomplete");
            onYahooLoaded(out,
                    "function() {\n"
                            + "    var handler = eval('(' + \"" + StringUtils.replace(autocompleteHandler, "\"", "\\\"") + "\" + ')');\n"
                            + "    var ds = handler.getDataSource();\n"
                            + "    var ac = new YAHOO.widget.AutoComplete('" + inputClientId + "', '" + containerId + "', ds);\n"
                            + "    ac.formatResult = handler.formatResult;\n"
                            + "    ac.forceSelection = false;\n"
                            + "}"
            );
            endJavascript(out);
        }

        public String getAutocompleteHandler() {
            return autocompleteHandler;
        }

        public void setAutocompleteHandler(String autocompleteHandler) {
            this.autocompleteHandler = autocompleteHandler;
        }

        @Override
        public Object saveState(FacesContext context) {
            final Object[] state = new Object[2];
            state[0] = super.saveState(context);
            state[1] = autocompleteHandler;
            return state;
        }

        @Override
        public void restoreState(FacesContext context, Object stateValue) {
            final Object[] state = (Object[]) stateValue;
            super.restoreState(context, state[0]);
            autocompleteHandler = (String) state[1];
        }
    }

    public static class YuiDateInputWriter extends DeferredInputWriter {
        private String buttonId;
        private Date date;

        public String getButtonId() {
            return buttonId;
        }

        public void setButtonId(String buttonId) {
            this.buttonId = buttonId;
        }

        public Date getDate() {
            return date != null ? (Date) date.clone() : null;
        }

        public void setDate(Date date) {
            this.date = date != null ? (Date) date.clone() : null;
        }

        @Override
        public void encodeBegin(FacesContext facesContext) throws IOException {
            final ResponseWriter out = facesContext.getResponseWriter();
            final String containerId = "cal_" + inputClientId;
            out.write("<div id=\"" + containerId + "\" class=\"popupCalendar\"> </div>\n");
            beginJavascript(out);
            writeYahooRequires(out, "calendar");
            onYahooLoaded(out,
                    "function() {\n"
                            + "    var button = document.getElementById('" + buttonId + "');\n"
                            + "    var container = document.getElementById('" + containerId + "');\n"
                            + "    var input = document.getElementById('" + inputClientId + "');\n"
                            + "    var date = " + (date != null ? "'" + new SimpleDateFormat("M/d/yyyy").format(date) + "'" : "''") + ";\n"
                            + "    var pdate = " + (date != null ? "'" + new SimpleDateFormat("M/yyyy").format(date) + "'" : "''") + ";\n"
                            + "    var navConfig={"+MessageBean.getInstance().getResource("FxValueInput.datepicker.navigatorConfig")+"};\n"
                            + "    var cal = new YAHOO.widget.Calendar('" + containerId + "', '" + containerId + "', \n"
                            + "                  { navigator: navConfig, close: true, title: '"
                            + MessageBean.getInstance().getResource("FxValueInput.datepicker.title")
                            + "'" + (date != null ? ", selected: date, pagedate: pdate" : "")  + "});\n"
                            + "    cal.cfg.setProperty(\"WEEKDAYS_SHORT\", "+MessageBean.getInstance().getResource("FxValueInput.datepicker.weekdaysShort")+");\n"
                            + "    cal.cfg.setProperty(\"MONTHS_SHORT\", "+MessageBean.getInstance().getResource("FxValueInput.datepicker.monthsShort")+");\n"
                            + "    cal.cfg.setProperty(\"MONTHS_LONG\", "+MessageBean.getInstance().getResource("FxValueInput.datepicker.monthsLong")+");\n"
                            + "    cal.selectEvent.subscribe(function(type, args, obj) {\n"
                            + "             var date = args[0][0];\n"
                            // YYYY/MM/DD
                            + "             input.value = flexive.util.zeroPad(date[0], 4) + '-' "
                            + " + flexive.util.zeroPad(date[1], 2) + '-' + flexive.util.zeroPad(date[2], 2);\n"
                            + "             cal.hide();\n"
                            + "         }, cal, true);\n"
                            + "    cal.render();\n"
                            + "    var Dom = YAHOO.util.Dom;\n"
                            + "    YAHOO.util.Event.on('" + buttonId + "', 'click', \n"
                            + "          function() { cal.show(); Dom.setXY(container, Dom.getXY(button)); }, cal, true);\n"
                            + "}"
            );
            endJavascript(out);
        }

        @Override
        public Object saveState(FacesContext context) {
            final Object[] state = new Object[3];
            state[0] = super.saveState(context);
            state[1] = buttonId;
            state[2] = date;
            return state;
        }

        @Override
        public void restoreState(FacesContext context, Object stateValue) {
            final Object[] state = (Object[]) stateValue;
            super.restoreState(context, state[0]);
            buttonId = (String) state[1];
            date = (Date) state[2];
        }
    }
}
