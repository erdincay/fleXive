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

/**
 * Miscellaneous classes and functions used by flexive JSF components.
 */


var flexive = new function() {
    /**
     * Enumeration of valid thumbnail preview sizes - see BinaryDescriptor#PreviewSizes
     */
    this.PreviewSizes = {
        PREVIEW1: { id: 1, size: 42 },
        PREVIEW2: { id: 2, size: 85 },
        PREVIEW3: { id: 3, size: 232 },
        ORIGINAL: { id: 0, size: -1}
    };

    /** Absolute application base URL (set by fx:includes) */
    this.baseUrl = null;
    /** Weblets resource provider root URL. */
    this.componentsWebletUrl = null;
}

// miscellaneous utility functions
flexive.util = new function() {
    /**
     * Return the absolute URL for the thumbnail of the given PK.
     *
     * @param pk    the object PK
     * @param size  the thumbnail size (one of FxComponents.PreviewSizes)
     * @param includeTimestamp  if true, the current timestamp will be included (disables caching)
     */
    this.getThumbnailURL = function(/* String */ pk, /* flexive.PreviewSizes */ size, /* boolean */ includeTimestamp) {
        return flexive.baseUrl + "thumbnail/pk" + pk
                + (size != null ? "/s" + size.id : "")
                + (includeTimestamp ? "/ts" + new Date().getTime() : "");
    };

    /**
     * Returns the preview size with the given ID.
     *
     * @param id    the preview size ID (0-3)
     */
    this.getPreviewSize = function(/* int */ id) {
        for (var ps in flexive.PreviewSizes) {
            if (flexive.PreviewSizes[ps].id == id) {
                return flexive.PreviewSizes[ps];
            }
        }
        return null;
    };

    /**
     * Parses the given PK and returns a PK object.
     *
     * @param pk     a primary key (e.g. "125.1")
     * @returns      the parsed PK, e.g. { id: 125, version: 1 }
     */
    this.parsePk = function(/* String */ pk) {
        if (pk == null || pk.indexOf(".") == -1) {
            throw "Not a valid PK: " + pk;
        }
        return { id: parseInt(pk.substr(0, pk.indexOf("."))),
                 version: parseInt(pk.substr(pk.indexOf(".") + 1)), 
                 toString: function() { return this.id + "." + this.version }
        };
    }

    /**
     * Extracts the object IDs of the given PK array.
     *
     * @param pks   the PKs
     * @return  the IDs of the given PKs
     */
    this.getPkIds = function(/* Array[PK] */ pks) {
        var result = []
        for (var i = 0; i < pks.length; i++) {
            result[i] = pks[i].id;
        }
        return result;
    }

    this.JSON_RPC_CLIENT = null;
    this.getJsonRpc = function() {
        if (this.JSON_RPC_CLIENT == null) {
            this.JSON_RPC_CLIENT = new JSONRpcClient(flexive.baseUrl + "/adm/JSON-RPC");
        }
        return this.JSON_RPC_CLIENT;
    }

    /**
     * Adds "zero padding" to a number, e.g.: flexive.util.zeroPad(51, 4) --> "0051"
     *
     * @param number    the number to be formatted
     * @param count     the desired input width
     */
    this.zeroPad = function(number, count) {
        var result = "" + number;
        while (result.length < count) {
            result = "0" + result;
        }
        return result;
    }

}

// Yahoo UI (YUI) helper methods and classes
flexive.yui = new function() {
    /** A list of all required components in the current page. Evaluated at the end of the page to initialize Yahoo.*/
    this.requiredComponents = [];
    /** A list of callback functions to be called when YUI has been fully loaded */
    this.onYahooLoadedFunctions = [];

    /**
     * Loads the required Yahoo components of the current page.
     *
     * @param loaderPath    the path to yuiloader-min.js
     */
    this.load = function() {
        if (this.requiredComponents.length > 0) {
            // load Yahoo UI only if at least one component is required
            var loader = new YAHOO.util.YUILoader({
                        base: flexive.componentsWebletUrl + "js/yui/",
                        require: this.requiredComponents,
                        loadOptional: true,
                        onSuccess: function() {
                            flexive.yui.processOnYahoo();
                        }
                });
            loader.insert();
        }
    }

    /**
     * Adds the given function to flexive.yui.onYahooLoaded.
     *
     * @param fn    the function to be called when Yahoo has been loaded
     */
    this.onYahooLoaded = function(fn) {
        this.onYahooLoadedFunctions.push(fn);
    }

    /**
     * Adds the given Yahoo component to the required components of this page.
     *
     * @param component the Yahoo component (e.g. "button")
     */
    this.require = function(component) {
        var found = false;
        for (var i = 0; i < this.requiredComponents.length; i++) {
            if (this.requiredComponents[i] == component) {
                found = true;
                break;
            }
        }
        if (!found) {
            this.requiredComponents.push(component);
        }
    }

    /**
     * Call setup methods of flexive components after yahoo has been initialized.
     */
    this.processOnYahoo = function() {
        this.requiredComponents = [];
        for (var i = 0; i < this.onYahooLoadedFunctions.length; i++) {
            this.onYahooLoadedFunctions[i]();
        }
        this.onYahooLoadedFunctions = [];
    }

    /**
     * Updates a property of a menu item.
     *
     * @param id    the menu item ID
     * @param property  the property (e.g. "disabled")
     * @param value the new value
     */
    this.setMenuItem = function(/* String */ id, /* String */ property, value) {
        var item = YAHOO.widget.MenuManager.getMenuItem(id);
        if (item == null) {
            alert("Menu item not found: " + id);
            return;
        }
        item.cfg.setProperty(property, value);
    }
}

flexive.yui.datatable = new function() {
    /**
     * Return the datatable wrapper for the correct view (list or thumbnails).
     *
     * @param result    the search result object as returned by fxSearchResultBean.jsonResult
     */
    this.getViewWrapper = function(result) {
        return result.viewType == "THUMBNAILS"
                ? new flexive.yui.datatable.ThumbnailView(result)
                : new flexive.yui.datatable.ListView(result);
    }

    /**
     * <p>Return the primary key of the result table row/column of the given element (e.g. an event target).
     * Supports both list and thumbnail view.</p>
     *
     * <p>Note that the primary key is only available if it was selected in the FxSQL query (@pk).</p>
     *
     * @param dataTable the datatable variable (set with the 'var' attribute of fx:resultTable)
     * @param element   the table element (i.e. an element nested in a table cell/row)
     * @return  a PK object, e.g. {id: 21, version: 10}
     */
    this.getPk = function(/* YAHOO.widget.DataTable */ dataTable, /* Element */ element) {
        return flexive.util.parsePk(this.getRecordValue(dataTable, element, "pk"));
    }

    /**
     * <p>Return the permissions object of the result table row/column of the given element (e.g. an event target).
     * Supports both list and thumbnail view.</p>
     *
     * <p>Note that the permissions are only available if they were selected in the FxSQL query (@permissions).</p>
     *
     * @param dataTable the datatable variable (set with the 'var' attribute of fx:resultTable)
     * @param element   the table element (i.e. an element nested in a table cell/row)
     * @return  <p>a permissions object with the following boolean properties: <ul>
     * <li>read</li>
     * <li>edit</li>
     * <li>relate</li>
     * <li>delete</li>
     * <li>export</li>
     * <li>create</li>
     * </ul>
     * A property is set if the current user has the appropriate rights to perform the action.</p>
     */
    this.getPermissions = function(/* YAHOO.widget.DataTable */ dataTable, /* Element */ element) {
        var p = this.getRecordValue(dataTable, element, "permissions");
        return {
            "read": (p & 1)  > 0,
            "create": (p & 2) > 0,
            "delete": (p & 4) > 0,
            "edit": (p & 8) > 0,
            "export": (p & 16) > 0,
            "relate": (p & 32) > 0
        };
    }

    /**
     * Extracts a property from the data record of the row/column indicated by the given element
     * (e.g. the target element of a "click" or "contextmenu" event).
     *
     * @param dataTable the Yahoo datatable widget
     * @param element   the source element (must be inside the table)
     * @param property  the property value to be looked up
     * @return  the property value, or null if no data record was found
     */
    this.getRecordValue = function(/* YAHOO.widget.DataTable */ dataTable, /* Element */ element, /* String */ property) {
        var elRow = dataTable.getTrEl(element);
        var record = dataTable.getRecord(elRow);
        if (record == null) {
            YAHOO.log("No primary key found for element " + element, "warn");
            return null;
        }
        var data = record.getData();
        if (data[property] && !YAHOO.lang.isArray(data[property])) {
            // only one property for this column, return it
            return data[property];
        } else {
            // one property per column, get column index and return PK
            var elCol = dataTable.getTdEl(element);
            var col = dataTable.getColumn(elCol);
            return data[property][col.getKeyIndex()];
        }
    }

    /**
     * <p>Returns the currently selected PKs of the given datatable.</p>
     *
     * <p><i>
     * Implementation note: selections over page boundaries are only supported in list view.
     * This appears to be a limitation of the YUI datatable widget, and may work in future versions.
     * </i></p>
     *
     * @param dataTable the datatable instance.
     * @return  a PK object, e.g. {id: 21, version: 10}
     */
    this.getSelectedPks = function(/* YAHOO.widget.DataTable */ dataTable) {
        var selectedPks = [];
        var tdEls = dataTable.getSelectedTdEls();
        if (tdEls.length > 0) {
            // column selection mode - works only on current page anyway
            for (var i = 0; i < tdEls.length; i++) {
                selectedPks.push(flexive.yui.datatable.getPk(dataTable, tdEls[i]));
            }
        } else {
            // get all selected rows (may include rows outside the current page)
            var rows = resultTable.getSelectedRows();
            for (var i = 0; i < rows.length; i++) {
                selectedPks.push(flexive.util.parsePk(dataTable.getRecord(rows[i]).getData()["pk"]));
            }
        }
        return selectedPks;
    }
}

/**
 * List result view - returns the linear result list of the search result
 * @param result    the search result object as returned by fxSearchResultBean.jsonResult
 */
flexive.yui.datatable.ListView = function(result) {
    this.result = result;
    this.rowsPerPage = 25;
}

flexive.yui.datatable.ListView.prototype = {
    getColumns: function() {
        return this.result.columns;
    },

    getResponseSchema: function() {
        return this.result.responseSchema;
    },

    getRows: function() {
        return this.result.rows;
    }
}

/**
 * Thumbnail view - projects the linear result list to a thumbnail grid
 * @param result    the search result object as returned by fxSearchResultBean.jsonResult
 */
flexive.yui.datatable.ThumbnailView = function(result) {
    this.result = result;
    this.previewSize = flexive.PreviewSizes.PREVIEW2;
    this.gridColumns = Math.max(1, Math.round(YAHOO.util.Dom.getViewportWidth() / (this.previewSize.size * 1.3)));
    this.rowsPerPage = 5;
}

flexive.yui.datatable.ThumbnailView.prototype = {
    // return the columns of the thumbnail grid
    getColumns: function() {
        var columns = [];
        for (var i = 0; i < this.gridColumns; i++) {
            columns.push({ key: "c" + i, label: "" })
        }
        return columns;
    },

    getRows: function() {
        // transpose the linear result rows according to the grid size
        var grid = [];
        var currentRow = { "pk" : [], "permissions": [] };    // the columns of the current row
        var currentColumn = 0;
        for (var i = 0; i < this.result.rowCount; i++) {
            var resultRow = this.result.rows[i];
            var data = "";
            if (resultRow.pk != null) {
                data = "<img src=\"" + flexive.util.getThumbnailURL(resultRow.pk, this.previewSize, true) + "\"/>";
            }
            /*if (resultRow["c1"] != null) {
                data += "<br/>" + resultRow["c1"];
            }*/
            if (currentColumn >= this.gridColumns) {
                // grid row completed
                grid.push(currentRow);
                currentRow = { "pk" : [], "permissions": [] };
                currentColumn = 0;
            }
            // store column
            currentRow["c" + currentColumn] = data;
            currentRow.pk.push(resultRow.pk);
            currentRow.permissions.push(resultRow.permissions);
            currentColumn++;
        }
        if (currentColumn > 0) {
            grid.push(currentRow);
        }
        return grid;
    },

    getResponseSchema: function() {
        var fields = ["pk", "permissions"];
        for (var i = 0; i < this.gridColumns; i++) {
            fields.push("c" + i);
        }
        return { "fields": fields };
    }
}

flexive.yui.AutoCompleteHandler = function(queryFn) {
    if (queryFn != null) {
        this.query = queryFn;
    }
}

flexive.yui.AutoCompleteHandler.prototype = {
    /**
     * <p>Return a datasource for the autocomplete handler.</p>
     * <p>Usually <code>YAHOO.widget.DS_JSArray</code> for data that can be sent to the client
     * or <code>YAHOO.widget.DS_JSFunction</code> for a JSON/RPC/Java wrapper.</p>
     *
     * @return The YAHOO.widget.DataSource to be used for the autocomplete. The default implementation
     * returns a JSFunction datasource that uses <code>this.query()</code> to determine the
     * valid choices.
     */
    getDataSource: function() {
        return new YAHOO.widget.DS_JSFunction(this.query);
    },

    /**
     * Implement the query logic here.
     *
     * @param query the query from the autocomplete field
     * @return the autocomplete choices (format determined by the data source)
     */
    query: function(query) {
        return [["Java 1.5", "Java"], ["Groovy 1.5", "Groovy"], ["Scala 2.7", "Scala"], ["JRuby 1.0", "JRuby"], ["JavaScript 2", "Javascript"]];
    },

    /**
     * Formats an item returned by query().
     *
     * @param item  the item to be formatted
     * @param query the current user query
     * @return  the formatted markup for the item
     */
    formatResult: function(/* Object */ item, /* String */ query) {
        return item[1];
    }
}

flexive.input = new function() {
    this.fxValueInputList = [];   // a global list of all registered FxValueInput elements on the current page

    /**
     * Updates the current language of all multilanguage inputs on the current page.
     *
     * @param languageId    the language ID
     */
    this.setLanguageOnAllInputs = function(languageId) {
        var newList = [];
        var oldList = flexive.input.fxValueInputList;
        for (var i = 0; i < oldList.length; i++) {
            if (oldList[i].isValid()) {
                oldList[i].showLanguage(languageId);
                newList.push(oldList[i]);
            }
        }
        flexive.input.fxValueInputList = newList;     // store new list without defunct inputs
        flexive.input._fixHtmlEditorsIE();
    }

    // trigger tinyMCE repaint on IE
    this._fixHtmlEditorsIE = function() {
        if (typeof(window.tinyMCE) != 'undefined' && tinyMCE.isIE) {
            for (var instanceName in tinyMCE.instances) {
                if (tinyMCE.isInstance(tinyMCE.instances[instanceName])) {
                    tinyMCE.execInstanceCommand(instanceName, "mceCleanup");
                }
            }
        }
    }

    // initialize the TinyMCE HTML editor
    this.initHtmlEditor = function(autoPopulate) {
        try {
            tinyMCE.baseURL = flexive.componentsWebletUrl + "js/tiny_mce";
            tinyMCE.srcMode = ""; // "_src" to enable original sourcefiles
            tinyMCE.init({
                    mode: autoPopulate ? "specific_textareas" : "exact",
                    dialog_type: "modal",
                    editor_selector: "fxValueTextAreaHtml",
                    theme: "advanced",
                    plugins: "paste,fullscreen,inlinepopups",
                    language: tinyMCE.guiLanguage ? tinyMCE.guiLanguage : "en",
                    entity_encoding: "raw",     // don't replace special characters (like umlaut) with HTML entities

                    // general layout options
                    theme_advanced_layout_manager: "SimpleLayout",
                    theme_advanced_toolbar_location : "top",
                    theme_advanced_buttons1: "pastetext,pasteword,separator,undo,redo"
                        + ",separator,link,unlink,separator,bullist,justifyleft,justifycenter,justifyright,justifyfull,separator,code",
                    theme_advanced_buttons2: "bold,italic,underline,strikethrough,sub,sup,separator,forecolor,fontsizeselect,removeformat",
                    theme_advanced_buttons3: "",
                    theme_advanced_toolbar_align: "left",
                    theme_advanced_resizing : true,
                    theme_advanced_statusbar_location: "bottom",

                    // fix alert when switching between multiple editors
                    focus_alert: false,
                    strict_loading_mode: true   /* Fixes Firefox HTML encoding problem with multiple editors */
                    /*width: "100%"*/
            });
        } catch (e) {
            alert("initHtml exception: " + e);
            // HTML editor component not configured
        }
    }

    // TODO: implement for usage outside the backend
    this.openReferenceQueryPopup = function(xpath, updateInputId, formName) {
        var win = window.open(flexive.baseUrl + "jsf-components/browseReferencesPopup.jsf?xPath=" + xpath + "&inputName="
                + updateInputId + "&formName=" + formName,
                "searchReferences", "scrollbars=yes,width=800,height=600,toolbar=no,menubar=no,location=no");
        win.focus();
    }

}

/**
 * JS object for fx:fxValueInput components.
 *
 * @param id            the fx:fxValueInput input ID
 * @param baseRowId     the row prefix, a valid row ID is constructed by appending the language ID
 * @param rowInfos      a map of type <code>{ languageId: { rowId: String, inputId: String } }</code>
 * @param languageSelectId  the input ID of the language select listbox
 * @param defaultLanguageId the default language (may be -1 if no default language is selected)
 */
flexive.input.FxMultiLanguageValueInput = function(id, baseRowId, rowInfos, languageSelectId, defaultLanguageId) {
    this.id = id;
    this.baseRowId = baseRowId;
    this.rowInfos = rowInfos;
    this.languageSelectId = languageSelectId;
    this.defaultLanguageId = defaultLanguageId;
    flexive.input.fxValueInputList.push(this);
    this._attachInputListeners();
};

flexive.input.FxMultiLanguageValueInput.prototype = {
    onLanguageChanged: function(languageSelect) {
        this.showLanguage(languageSelect.options[languageSelect.selectedIndex].value);
        flexive.input._fixHtmlEditorsIE();
    },

    showRow: function(showRowId) {
        for (var i in this.rowInfos) {
            var rowId = this.rowInfos[i].rowId;
            var enabled = showRowId == null || rowId == showRowId;
            document.getElementById(rowId).style.display = enabled ? "block" : "none";
            // all languages / show language icon for each row
            document.getElementById(rowId + "_language").style.display = (enabled && showRowId == null) ? "inline" : "none";
        }
    },

    showLanguage: function(languageId) {
        // show row for this language
        this.showRow(languageId >= 0 ? this.baseRowId + languageId : null);
        // update language select
        var options = document.getElementById(this.languageSelectId).options
        for (var i = 0; i < options.length; i++) {
            options[i].selected = (options[i].value == languageId);
        }
    },

    onDefaultLanguageChanged: function(inputCheckbox, languageId) {
        if (inputCheckbox.checked) {
            // uncheck other languages since only one language may be the default
            var languageCheckboxes = document.getElementsByName(inputCheckbox.name);
            for (var i = 0; i < languageCheckboxes.length; i++) {
                if (languageCheckboxes[i] != inputCheckbox) {
                    languageCheckboxes[i].checked = false;
                }
            }
            this.defaultLanguageId = languageId;
        }
    },

    /**
     * Returns true if the input elements assigned to this object are still present.
     */
    isValid: function() {
        return document.getElementById(this.languageSelectId) != null;
    },

    /**
     * Attaches an input listener to the given row IDs
     */
    _attachInputListeners: function() {
        // set value from current default language in all empty inputs
        for (var i in this.rowInfos) {
            var rowInfo = this.rowInfos[i];
            var input = document.getElementById(rowInfo.inputId);
            if (input != null && input.type == "text" && this.defaultLanguageId > 0) {
                input.value = document.getElementById(this.rowInfos[this.defaultLanguageId].inputId).value;
                input.setAttribute("defaultLanguageSet", true);
            } else if (input != null) {
                input.removeAttribute("defaultLanguageSet");
            }
        }
    }
}

flexive.dojo = new function() {
    /** Creates a DOJO menu from a JSON object. Based on Dojo example code. */
    this.makeMenu = function(id, menuClass, itemClass, items, isTop, contextMenuTarget) {
        var options = {
            templateCssPath: dojo.uri.dojoUri("../../css/dojoCustom/Menu2.css"),
            contextMenuForWindow: isTop,
            toggle: "fade"
        };
        if (id != null) {
            options.widgetId = id;
        }
        if (contextMenuTarget != null) {
            options.targetNodeIds = [contextMenuTarget];
        }
        var menu2 = dojo.widget.createWidget(menuClass, options);
        dojo.lang.forEach(items, function(itemJson){
            // if submenu is specified, create the submenu and then make submenuId point to it
            if( itemJson.submenu){
                var submenu = flexive.dojo.makeMenu(null, menuClass, itemClass, itemJson.submenu, false);
                itemJson.submenuId = submenu.widgetId;
            }
            var item = dojo.widget.createWidget(itemJson.createAsType == null ? itemClass : itemJson.createAsType,  itemJson);
            menu2.addChild(item);
        });
        return menu2;
    }
}
