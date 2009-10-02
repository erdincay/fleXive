// Global helper functions available in all admin pages

// DOJO config
var djConfig = {
    isDebug: false,
    baseRelativePath: "pub/js/dojo/",
    preventBackButtonFix: true,
    parseWidgets: false,
    enableMozDomContentLoaded: true
};

function dojoInit() {
    if (djConfig.isDebug) {
        dojo.require("dojo.debug.console");
    }
    //dojo.setModulePrefix("flexive.widget", "../flexive/widget");
    //dojo.widget.manager.registerWidgetPackage("flexive.widget");
    //dojo.registerModulePath("flexive.widget", "../flexive/widget");
    //dojo.require("flexive.manifest");
}

// Return the content frame
function getContentFrame() {
    if (frames["contentFrame"]) {
        return frames["contentFrame"];
    } else {
        return parent.frames["contentFrame"];
    }
}

// Invoke an action in the content frame.
// @param url - the JSF page to be loaded, e.g. "adm/content/contentEditor.jsf"
// @param action - the action to be executed, e.g. "newInstance"
// @param parameters - the parameters to be passed in the query string, e.g. {id: 25, name: "test"}
function invokeContentAction(url, action, parameters) {
    var queryString = "?action=" + action;
    for (var key in parameters) {
        queryString += "&" + key + "=" + encodeURIComponent(parameters[key]);
    }
    setBusyCursor();
    loadContentPage(url + queryString);
}

// Load a given URL (relative to the application base path) into the content frame
// @param url - the URL to be loaded, e.g. "adm/content/query.jsf"
function loadContentPage(url) {
    getContentFrame().location.href = getBase() + url;
}

// writes the current document's size (width x height) in the given form variables
function updateClientSize(varClientSizeX, varClientSizeY) {
    var clientSizeX = getWindowWidth();
    var clientSizeY = getWindowHeight();
    varClientSizeX.value = clientSizeX;
    varClientSizeY.value = clientSizeY;
}

// return the URI for the preview of the given primary key
function getThumbnailURI(pk, previewSize) {
    return getBase() + "thumbnail/pk" + pk + "/s" + (previewSize != null ? previewSize : "2");
}

/** Find the first object containing an attribute of the given name, starting
 at the source element of the given event. */
function findParentWithAttribute(event, attributeName) {
    if (event == null) {
        event = window.event;
    }
    if (event == null) {
        return;
    }
    var cur;
    if (document.all) {
        try {
            cur = event.srcElement;
        } catch (e) {
            // on some events, IE7 throws an exception here
        }
    }
    if (cur == null) {
        cur = event.target;
    }
    while (cur != null && cur.tagName != "BODY" && cur.getAttribute(attributeName) == null)
        cur = cur.parentNode;
    return cur;
}

// Scales the given image to fit in the given box. Both dimensions must be specified.
function scaleImage(img, boxWidth, boxHeight) {
    if (img == null) {
        return;
    }
    var scaleX = boxWidth / img.width;
    var scaleY = boxHeight / img.height;
    var scale = Math.min(scaleX, scaleY);
    img.width = img.width * scale;
}


// returns the menu item with the given id and class
function findMenuItem(menu, id, cls) {
    var items = menu.getChildrenOfType(cls != null ? cls : dojo.widget.MenuItem2);
    for (var i = 0; i < items.length; i++) {
        var item = items[i];
        if (item.widgetId.indexOf(":" + id) == item.widgetId.length - id.length - 1) {
            return item;
        }
    }
    return null;
}


// update the tree node disabled actions list to reflect the disabled status of the given tree action
function setActionDisabled(treeNode, name, disabled) {
    var newDisabled = [];
    var attachedDisabled = false;
    //dojo.debug("Disabling action " + name);
    //dojo.debug("Old disabled items: " + treeNode.actionsDisabled);
    for (var i = 0; i < treeNode.actionsDisabled.length; i++) {
        if (name.toUpperCase() != treeNode.actionsDisabled[i]) {
            newDisabled.push(treeNode.actionsDisabled[i]);
        } else if (disabled) {
            newDisabled.push(treeNode.actionsDisabled[i]);
            attachedDisabled = true;
        }
    }
    if (disabled && !attachedDisabled) {
        newDisabled.push(name.toUpperCase());
    }
    //dojo.debug("-->: " + newDisabled);
    treeNode.actionsDisabled = newDisabled;
}

// extract the reference IDs from the given list of PKs (e.g. ["10.1", "22.5"] --> [10, 22])
function extractPkIds(pks) {
    return dojo.lang.map(pks, function(pk) {
        return pk.substring(0, pk.indexOf("."))
    });
}

function setImage(elementId, src) {
    document.getElementById(elementId).src = src;
}


function initializeTreeReporter(widgetIds, _clickHandler) {
    for (var i = 0; i < widgetIds.length; i++) {
        var widget = dojo.widget.manager.getWidgetById(widgetIds[i]);
        var selectHandler = new TreeHandler({clickHandler: _clickHandler});
        selectHandler.subscribe(widget);
    }
}

/** Flexive (DOJO) Tree Handler base class */

var TreeHandler = function(params) {
    this.clickHandler = null;
    for (var param in params) {
        this[param] = params[param];
    }
};

TreeHandler.prototype = {
    /**
     * Attach this tree handler to the given (tree) widget.
     */
    subscribe: function(widget) {
        if (!widget.eventNames) {
            return;
        }
        dojo.event.topic.subscribe(widget.eventNames["select"], this, "onSelect");
        dojo.event.topic.subscribe(widget.eventNames["dblselect"], this, "onDblSelect");
    },

    /**
     * Standard select handler - called when a node is selected/clicked.
     */
    onSelect: function(messages) {
        for (var i in messages) {
            var node = messages[i];
            var link = node["link"];
            if (this.clickHandler) {
                this.clickHandler(node, false);
            } else if (link) {
                if (link.indexOf("javascript:") == 0) {
                    eval(link.substr("javascript:".length));
                } else {
                    // open link in other frame
                    parent.contentFrameObj.src = link;
                }
            } else if (node.children.length > 0) {
                // default action: toggle node
                if (node.isExpanded) {
                    node.tree.fxController.collapse(node);
                } else {
                    node.tree.fxController.expand(node);
                }
            }
        }
    },

    onDblSelect: function(messages) {
        for (var i in messages) {
            var node = messages[i];
            if (this.clickHandler) {
                this.clickHandler(node, true);
            }
        }
    }
};


/**
 * DHTML replacement for Javascript's confirm() dialog
 * @param message       the message to be displayed
 * @param onConfirmed   the function to be executed when the user confirmed the message
 * @param onCancel      the function to be executed when the user did not confirm (optional)
 */
function confirmDialog(message, onConfirmed, onCancel) {
    var wnd = window;
    while (wnd != null && wnd.parent != wnd && !wnd._confirmDialog) {
        wnd = wnd.parent;
    }
    if (wnd != null) {
        wnd._confirmDialog(message, onConfirmed, onCancel);
    }
}

/**
 * DHTML replacement for Javascript's prompt() dialog
 *
 * @param message       the message to be displayed
 * @param defaultValue  the default value (optional)
 * @param onSuccess     the function to be executed when the user entered a valid value. The input value is passed in the first parameter.
 */
function promptDialog(message, defaultValue, onSuccess) {
    var wnd = window;
    while (wnd != null && wnd.parent != wnd && !wnd._promptDialog) {
        wnd = wnd.parent;
    }
    if (wnd != null) {
        wnd._promptDialog(message, defaultValue, onSuccess);
    }
}

/**
 * DHTML replacement for Javascript's alertDialog() dialog
 * Don't invoke directly, use alertDialog(...) from admin.js.
 *
 * @param message       the message to be displayed
 * @param onConfirmed   the function to be executed when the user confirmed the message
 * @param onCancel      the function to be executed when the user did not confirm (optional)
 */
function alertDialog(message) {
    var wnd = window;
    while (wnd != null && wnd.parent != wnd && !wnd._alertDialog) {
        wnd = wnd.parent;
    }
    if (wnd != null) {
        wnd._alertDialog(message);
    }
}

/**
 * This function should be called from any ajax supporting jsf tags (e.g. "<a4j:support oncomplete="ajaxButtonRequest()" ..</a4j>
 * if commandbuttons are used in combination with ajax requests.
 */
function ajaxButtonRequest(isFF) {
    initialiseAjaxComponent();
    regAjaxComponentInToolbar(false, isFF);
}

/**
 * Call this for the BE application ajax requests in the content editor component
 */
function beCeAjaxButtonRequest(reloadContentTree) {
    regAjaxComponentInToolbar(true, true);
    flexive.yui.load();
    if(reloadContentTree) {
        ceReloadTree();
    }
}

function ceReloadTree() {
    window.parent.getContentNavFrame().reloadContentTree();
}

/**
 * Helper function for Ajax requests, initialises Yahoo components
 * after completion of a request.
 */
function initialiseAjaxComponent() {
    flexive.yui.processOnYahoo();
}

/**
 * This function provides the rendering logic for ajax-enabled command buttons in the toolbar
 * The actual logic will only be executed if ajaxButtons are found in parent.ajaxRegisteredIds (main.js).
 * [i.e. the corresponding adm:commandButtons's "ajax" attribute was set to "true"]
 *
 * @param contentEditor if used for the contentEditor (simple click update only) (if this is set to true, "isFF" will be ignored
 * @param isFF boolean switch if FF is used - helps with latest richfaces update (v3.3.1)
 */
function regAjaxComponentInToolbar(contentEditor, isFF) {
    // retrieve the content frame (kept in sep. var for possible future changes)
    var contentFrame = window.parent.document.getElementById('contentFrame');
    var content = contentFrame.contentWindow.document;
    var cmdButtonId = 'commandButton_';
    var cmdElementId = 'commandElement_';
    // vars holding toolbar elements
    var _toolBar = window.parent.document.getElementById('fxToolbar'); // window.parent.document must be called!
    var toolbarButtonIds = [];
    var _toolbarHelp = [];
    var _toolbarClick = [];
    var _toolbarImages = [];
    var countTbButtons = 0;
    var ffImgIds = [];
    var ffImgOnclick = [];

    /**
     * ContentEditor only:
     * This function merely updates the onclick events in the toolbar from the given onclicks in the commandbuttons
     * This is for FF only (necessary since the last richfaces update)
     */
    function updateToolbarClickFuncsOnly() {
        if (_toolBar != null) {
            var _images = _toolBar.getElementsByTagName('img');
            if (_images.length > 0) {
                for (var i = 0; i < _images.length; i++) {
                    var currentImgId = _images[i].id;
                    // only check "regular" buttons
                    if (currentImgId != "" && _images[i].src.indexOf('separator.png' < 1)) {
                        window.parent.document.getElementById(currentImgId).onclick = new Function(firefoxOnClick(currentImgId));
                    }
                }
            }
        }
    }

    /**
     * Retrieves the onclick action of a toolbar img as a string value
     * @param imgId the current toolbar img id
     */
    var firefoxOnClick = function(imgId) {
        var commandEleName = imgId.substring(0, imgId.lastIndexOf('_'));
        var formEleName = 'frm:' + commandEleName;
        if (content.getElementById(formEleName)) { // ajax button was removed
            if (content.getElementById(formEleName).onclick) {
                var onClickAction = content.getElementById(formEleName).onclick.toString();

                var afterSplit = [];
                afterSplit = onClickAction.split('{\n'); // [1].split('\n}')[0];
                afterSplit.shift(); // remove first ele and reconcat the array
                if (afterSplit.length > 1)
                    onClickAction = afterSplit.join(" {\n ");
                else
                    onClickAction = afterSplit.join(" ");
                onClickAction = onClickAction.substring(0, onClickAction.lastIndexOf("\n}"));

                var idx = linearSearch(commandEleName, parent.toolbarButtonParamId, false);
                if (idx != -1) {
                    if (parent.toolbarButtonParamLockScreen[idx])
                        onClickAction = "parent.lockScreen();" + onClickAction;
                    if (parent.toolbarButtonParamConfirm[idx] != null)
                        onClickAction = "confirmDialog('" + parent.toolbarButtonParamConfirm[idx] + "', function() { " + onClickAction + " });";
                }
                return onClickAction;
            }
        }
        return '';
    };

    // retrieve the current toolbar buttons from the DOM (leaving out ajaxButtons) tree
    function getToolbarButtons() {
        if (_toolBar != null) {
            var _images = _toolBar.getElementsByTagName('img');
            if (_images.length > 0) {
                for (var i = 0; i < _images.length; i++) {
                    var img = _images[i].id;
                    if (img == "" && _images[i].src.indexOf('separator.png' >= 0)) { // separator
                        toolbarButtonIds[countTbButtons] = '';
                        _toolbarHelp[countTbButtons] = ''; // _images[i].alt;
                        _toolbarClick[countTbButtons] = '';
                        _toolbarImages[countTbButtons] = _images[i].src;
                        countTbButtons++;
                    } else { // "regular" button
                        toolbarButtonIds[countTbButtons] = img.substring(0, img.indexOf('_')); //img;
                        _toolbarHelp[countTbButtons] = _images[i].alt;
                        if(isFF) { // FF ONLY
                            _toolbarClick[countTbButtons] = "";
                            ffImgIds[countTbButtons] = img;
                            ffImgOnclick[countTbButtons] = firefoxOnClick(img);
                        } else {
                            _toolbarClick[countTbButtons] = '_caller.triggerCommandElement_' + img.substring(0, img.indexOf('_')) + '()';
                        }
                        _toolbarImages[countTbButtons] = _images[i].src;
                        countTbButtons++;
                    }
                }
            }
        }
    }

    /**
     * Check which of the registered ajax buttons are present in the DOM tree and add or remove them
     * from the toolbar
     */
    function renderAjaxButtonsToToolbar() {
        var rerender = false;
        var reorder = true;
        // if parent.toolbarIds.length != toolbarButtonIds.length ==> ajaxButtons were rendered, add them to the local vars
        if (!compareToolbarWithParent()) {
            rerender = addAjaxButtonsFromParent();

            if (!rerender) {
                // special case: some ajax buttons are removed, others are rerendered but stay on the page
                rerender = removeAjaxButtonsFromToolbar();
                if (!rerender) {// special case: refresh w/o a rerender = ajax request w/o changes
                    copyToolbarButtonsToParent(false);
                    rerender = true;
                    reorder = false;
                }
            }

            // if a toolbarPosition was given for the ajax buttons, reorder the relevant arrays
            if (reorder && rerender && parent.ajaxRegisteredIdPositions.length > 0
                    && (parent.ajaxRegisteredIdPositions.length == parent.ajaxRegisteredIds.length)) {
                var smallestNonRenderedPos = -1; // var to keep track of buttons which are not rendered
                for (var i = 0; i < parent.ajaxRegisteredIds.length; i++) {
                    if (linearSearch(parent.ajaxRegisteredIds[i], parent.ajaxRegisteredIdsToolbarOnly, false) >= 0) {
                        if (linearSearch(parent.ajaxRegisteredIds[i], parent.toolbarIds, true) >= 0)
                            rerender = reorderToolbarButtons(i, smallestNonRenderedPos);
                        else
                            smallestNonRenderedPos = i;
                    } else { // reorder only if the commandbutton was rendered
                        if (content.getElementById(cmdButtonId + parent.ajaxRegisteredIds[i]) != null)
                            rerender = reorderToolbarButtons(i, smallestNonRenderedPos);
                        else
                            smallestNonRenderedPos = i;
                    }
                }
            }
        } else { // case2: button(s) need(s) to be removed
            rerender = removeAjaxButtonsFromToolbar();
        }
        return rerender;
    }

    // compares the locally generated toolbar with the parent toolbar
    // disregarding separators
    var compareToolbarWithParent = function() {
        if (parent.toolbarIds.length == toolbarButtonIds.length)
            return true;
        else {
            var localIds = toolbarButtonIds.slice(0);
            var parentIds = parent.toolbarIds.slice(0);
            var idx = -1;
            while (true) { // local ids
                idx = linearSearch('', localIds, false);
                if (idx < 0)
                    break;
                localIds.splice(idx, 1);
            }
            while (true) { // parent ids
                idx = linearSearch('', parentIds, false);
                if (idx < 0)
                    break;
                parentIds.splice(idx, 1);
            }
            return localIds.length == parentIds.length;
        }
    };

    // copies the data to the parent arrays
    var copyToolbarButtonsToParent = function(copyOnly) {
        var localToolbarStyle = [];
        // check if any of the newly assigned buttons has a different toolbarStyle ("disabled" buttons)
        if (!copyOnly && parent.toolbarStyle.length > 0) {
            for (var i = 0; i < parent.toolbarIds.length; i++) {
                if (parent.toolbarStyle[i] != null) { // get the id from the local toolbarButton array
                    var idx = linearSearch(parent.toolbarIds[i], toolbarButtonIds, false);
                    if (idx >= 0)
                        localToolbarStyle[idx] = parent.toolbarStyle[i];
                }
            }
        }

        parent.clearToolbar(); // clear the toolbar first
        if (localToolbarStyle.length > 0)
            parent.toolbarStyle = localToolbarStyle.slice(0); // copy to the parent
        parent.toolbarIds = toolbarButtonIds.slice(0);
        parent.toolbarImages = _toolbarImages.slice(0);
        parent.toolbarClick = _toolbarClick.slice(0);
        parent.toolbarHelp = _toolbarHelp.slice(0);
    };

    /**
     * Copies the data from the rendered page (incl. ajax requests). IFF
     * the ajaxbuttons are not in the toolbar
     */
    var addAjaxButtonsFromParent = function() {
        var rerender = false;
        var positionsToCopy = [];
        var j = 0;

        for (var i = 0; i < parent.toolbarIds.length; i++) {
            if (linearSearch(parent.toolbarIds[i], toolbarButtonIds, true) < 0) {
                positionsToCopy[j] = i;
                j++;
            }
        }

        if (positionsToCopy.length > 0) {
            for (var i = 0; i < positionsToCopy.length; i++) {
                var last = toolbarButtonIds.length;
                toolbarButtonIds[last] = parent.toolbarIds[positionsToCopy[i]];
                _toolbarImages[last] = parent.toolbarImages[positionsToCopy[i]];
                if(isFF) {
                    _toolbarClick[last] = "";
                    ffImgIds[ffImgIds.length] = toolbarButtonIds[last] + '_toolbarIcon';
                    ffImgOnclick[ffImgOnclick.length] = firefoxOnClick(ffImgIds[ffImgIds.length - 1]);
                } else {
                    _toolbarClick[last] = parent.toolbarClick[positionsToCopy[i]];
                }
                _toolbarHelp[last] = parent.toolbarHelp[positionsToCopy[i]];
            }
            rerender = true;
        }
        return rerender;
    };

    var removeAjaxButtonsFromToolbar = function() {
        var rerender = false;
        // check which ajax buttons were disabled
        for (var i = 0; i < parent.ajaxRegisteredIds.length; i++) {
            //                var removeId = -1;
            if (linearSearch(parent.ajaxRegisteredIds[i], parent.ajaxRegisteredIdsToolbarOnly, false) >= 0) {
                if (content.getElementById(cmdElementId + parent.ajaxRegisteredIds[i]) == null) {
                    if (removeElementFromToolbarButtonIds(i) >= 0)
                        rerender = true;
                }
            } else {
                if (content.getElementById(cmdButtonId + parent.ajaxRegisteredIds[i]) == null) {
                    if (removeElementFromToolbarButtonIds(i) >= 0)
                        rerender = true;
                }
            }
        }
        return rerender;
    };

    // Helper function to remove a button from the (local) toolbar arrays
    var removeElementFromToolbarButtonIds = function(element) {
        var removeId = linearSearch(parent.ajaxRegisteredIds[element], toolbarButtonIds, false);
        if (removeId >= 0) {
            toolbarButtonIds.splice(removeId, 1);
            _toolbarImages.splice(removeId, 1);
            _toolbarClick.splice(removeId, 1);
            _toolbarHelp.splice(removeId, 1);
        }
        return removeId;
    };

    // refactorisation of reordering algorithm
    var reorderToolbarButtons = function(currentElement, smallestNonRenderedPos) {
        var rerender = false;
        var oldpos = -1;
        if (parent.ajaxRegisteredIds.length == parent.ajaxRegisteredIdPositions.length) { // safety fallback
            oldpos = linearSearch(parent.ajaxRegisteredIds[currentElement], toolbarButtonIds, false);
            if (oldpos >= 0) {
                // check the smallest non-rendered position
                if (smallestNonRenderedPos != -1 && smallestNonRenderedPos < currentElement)
                    currentElement = smallestNonRenderedPos;

                reorder(oldpos, parent.ajaxRegisteredIdPositions[currentElement]);
                rerender = true;
            }
        }
        return rerender;
    };

    // helper function for "reorderToolbarButtons"
    var reorder = function(oldpos, newpos) {
        var tmpId = toolbarButtonIds.splice(oldpos, 1);
        var tmpImg = _toolbarImages.splice(oldpos, 1);
        var tmpClick = _toolbarClick.splice(oldpos, 1);
        var tmpHelp = _toolbarHelp.splice(oldpos, 1);

        toolbarButtonIds.splice(newpos, 0, tmpId[0]);
        _toolbarImages.splice(newpos, 0, tmpImg[0]);
        _toolbarClick.splice(newpos, 0, tmpClick[0]);
        _toolbarHelp.splice(newpos, 0, tmpHelp[0]);
    };

    // render new clicks for FF
    function replaceFFOnclicks() {
        if(ffImgIds.length > 0 && ffImgOnclick.length > 0) {
            for(var i = 0; i < ffImgIds.length; i++) {
                if(ffImgIds[i]) { // skip undefined array items
                    window.parent.document.getElementById(ffImgIds[i]).onclick = new Function(ffImgOnclick[i]);
                }
            }
        }
    }

    // rendering logic
    if (contentEditor) {
        updateToolbarClickFuncsOnly();
    } else if (parent.ajaxRegisteredIds.length > 0) {
        getToolbarButtons();
        if (renderAjaxButtonsToToolbar()) {
            copyToolbarButtonsToParent(false);
            parent.renderToolbar();
            if(isFF) {
                replaceFFOnclicks();
            }
        } else if(isFF && ffImgIds.length > 0 && ffImgOnclick.length > 0) {
            replaceFFOnclicks();
        }
    }
}

/**
 * Linear search of the targetArray for searchItem returns the index of the first occurrence
 * of the searchItem or -1 if not found
 * @param searchItem
 * @param targetArray
 * @param regEx boolean flag to make a regEx search for the searchItem
 * @return resultIndex array of result indeces
 */
function linearSearch(searchItem, targetArray, regEx) {
    if (searchItem != null && targetArray != null) {
        for (var i = 0; i < targetArray.length; i++) {
            if (regEx) {
                if (targetArray[i].match(searchItem)) {
                    return i;
                }
            } else {
                if (targetArray[i] == searchItem) {
                    return i;
                }
            }
        }
    }
    return - 1;
}

/**
 * EDITAREA: Activates the EditArea js plugin
 * @param id the form's id
 * @param init boolean value whether the EditArea should be initialised
 */
function activateEditArea(id, init, userLang, syntax, displayToggle, toggleOnFunction, toggleOffFunction) {
    if (init) {
        if (userLang == null)
            userLang = "en";
        if (syntax == null)
            syntax = "js";
        if (syntax == "groovy" || syntax == "gy")
            syntax = "js";
        if(displayToggle == null) {
            displayToggle = "onload";
            eaToggleOnAdmin();
        } else {
            eaToggleState = displayToggle;
        }
        if(toggleOnFunction == null)
            toggleOnFunction = "eaToggleOnAdmin";
        if(toggleOffFunction == null)
            toggleOffFunction = "eaToggleOffAdmin";

        parent.contentFrameObj.contentWindow.editAreaLoader.init({
            id : id,
            syntax: syntax,
            start_highlight: true,
            language: userLang,
            font_size: 8,
            allow_resize: "both",
            change_callback: "editAreaChange",
            plugins: "executeScript",
            display: displayToggle,
            EA_toggle_on_callback: toggleOnFunction,
            EA_toggle_off_callback: toggleOffFunction
        });
    }
}

/**
 * EDITAREA: Helper function for the EditArea onchange callback, sets the value
 * of the textarea html element
 * @param id the form id
 */
function editAreaChange(id) {
    var contentFrame = parent.contentFrameObj.contentWindow.document;
    if (contentFrame.getElementById(id) != null) {
        contentFrame.getElementById(id).value = parent.contentFrameObj.contentWindow.editAreaLoader.getValue(id);
    }
}

// EDITAREA: hold the current toggle state of the editArea
var eaToggleState = "";

/**
 * EDITAREA: the toggle on callback function
 */
function eaToggleOnAdmin() {
    eaToggleState = "onload";
}

/**
 * EDITAREA: the toggle off callback function
 */
function eaToggleOffAdmin() {
    eaToggleState = "later";
}

/**
 * Copies the value of the input source element to input destination element.
 *
 * @param formname name of the form
 * @param inputSource id of the input source
 * @param inputDest id of the input destination
 */
function transferInputValue(formname, inputSource, inputDest) {
    var value = document.getElementById(formname + ":" + inputSource).value;
    document.getElementById(formname + ":" + inputDest).value = value;
}


function setBusyCursor() {
    document.body.style.cursor = "progress";
}


function setDefaultCursor() {
    document.body.style.cursor = "default";
}

/**
 * Flashes the target element's background color to indicate that something happened to the element.
 * This requires the "animation" module of YUI to be loaded.
 *
 * @param elementId         the target element ID
 * @param flashColor        the highlight color
 * @param backgroundColor   the (approximate) background color of the element or the element's background
 * @return                  the animation (useful for chaining)
 */
function flash(elementId, flashColor, backgroundColor) {
    //    if (document.all) {
    //        return null;     // IE seems to play animations a bit too choppy for my taste...
    //    }
    var anim = new YAHOO.util.ColorAnim(elementId, { backgroundColor: { from: backgroundColor, to: flashColor } }, 0.3, YAHOO.util.Easing.easeInStrong);
    var anim2 = new YAHOO.util.ColorAnim(elementId, { backgroundColor: { to: backgroundColor } }, 1.5, YAHOO.util.Easing.easeOutStrong);
    anim.onComplete.subscribe(function() {
        anim2.animate();
    });
    // remove element background after animation
    anim2.onComplete.subscribe(function() {
        document.getElementById(elementId).style.backgroundColor = "";
    });
    anim.animate();
    return anim2;
}

/**
 * This function is used in conjunction with the ajaxCommandButton and enables
 * the clickable area over the whole commandButton div
 *
 * @param id the id of the commandElement div containing the link
 * @param onClickAction an additional onClickAction passed to the ajaxCommandButton
 */
function getOnClick(id, onClickAction, lockscreen) {
    var contentFrame = window.parent.document.getElementById('contentFrame');
    var callerWindow = contentFrame.contentWindow;
    var linkElement = getFirstLinkElement(callerWindow.document, id);
    if (onClickAction == 'null')
        onClickAction = "";
    if (lockscreen)
        onClickAction = onClickAction + "parent.lockScreen();";
    onClickAction = onClickAction + "document.getElementById('" + linkElement.getAttribute("id") + "').onclick();";
    return eval(onClickAction);
}

// adm:sortableTable page change / row change utilities - YUI ***********************
var currentDTPage = 1;
var rowsPerDTPage = 10;

var DTpageChange = function(event) {
    rowsPerDTPage = event.newState.rowsPerPage;
    currentDTPage = event.newValue;
};
// **********************************************************************************
