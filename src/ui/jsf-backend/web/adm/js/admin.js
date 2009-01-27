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
    loadContentPage(url + queryString);
}

// Load a given URL (relative to the application base path) into the content frame
// @param url - the URL to be loaded, e.g. "adm/content/query.jsf"
function loadContentPage(url) {
    getContentFrame().location.href = getBase() + url;
}

// evaluates command when the 'enter' key was pressen
function onReturnEval(event, expression) {
    var keycode;
    if (event && event.keyCode) {
        keycode = event.keyCode;
    } else if (window.event) {
        keycode = window.event.keyCode;
    } else if (event) {
        keycode = event.which;
    } else {
        alertDialog("onReturnEval: no event object found.");
        return true;
    }
    if (keycode != 13) {
        return true;
    }
    eval(expression);
    return false;   // don't "execute" the enter key
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


// A simple clipboard for content objects
var ContentClipboard = function() {
    this.ids = [];
};

ContentClipboard.prototype = {
    // set the clipboard content to the given object ID array
    set: function(contentIds) {
        this.ids = [];
        // copy IDs to our own array to prevent aliasing and inter-frame issues
        for (var i = 0; i < contentIds.length; i++) {
            this.ids.push(contentIds[i]);
        }
    },

    // get the current clipboard contents as object ID array
    get: function() {
        return this.ids != null ? this.ids : [];
    },

    // clear the clipboard
    clear: function() {
        this.ids = [];
    },

    // returns true for empty clipboards
    isEmpty: function() {
        return this.get().length == 0;
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
function ajaxButtonRequest() {
    initialiseAjaxComponent();
    regAjaxComponentInToolbar();
}

/**
 * Helper function for Ajax requests, initialises Yahoo components
 * after completion of a request.
 */
function initialiseAjaxComponent() {
    flexive.yui.processOnYahoo();
}

/**
 * This function retrieves both commandButtons from the content iframe of the rendered page as well as all toolbar buttons.
 * It then applies logical rules to rerender the toolbar buttons according to the current state of the ajax request.
 * 
 */
function regAjaxComponentInToolbar() {
    var regEx1;
    var tbContentButtonCount = 0; // counts the toolbar buttons which are the same as in the content
    var countTbButtons = 0; // general var for counting buttins in the toolbar
    var countButtons = 0;
    var toolbarButtonIds = new Array();
    var contentButtonIds = new Array();
    var _toolbarHelp = new Array(); // variables f. holding commandButton attributes
    var _toolbarClick = new Array();
    var _toolbarImages = new Array();
    var _toolbarHelpRest = new Array(); // variables f. holding toolbar button (attributes)
    var _toolbarClickRest = new Array(); // which do not exist in the content frame
    var _toolbarImagesRest = new Array();
    var _toolBar = window.parent.document.getElementById('fxToolbar'); // window.parent.document must be called!
    var _iframe = window.parent.document.getElementById('contentFrame');
    var childNodeCounter_1 = 1;
    var childNodeCounter_2 = 3;
    if (document.all) { // IE ONLY!
        childNodeCounter_1 = 0;
        childNodeCounter_2 = 1;
    }

    // retrieve the buttons from the content (residing in the iframe)
    if (_iframe != null) {
        var content = _iframe.contentWindow.document;
        var elements = content.getElementsByTagName('div'); // retrieve all divs
        regEx1 = /commandButton_.*/;
        for (var i = 0; i < elements.length; i++) {
            var el = elements[i].id;
            if (el != null && el.match(regEx1)) {
                if (elements[i].childNodes[childNodeCounter_1] != null) { // is null on ajax generated buttons // doesn't work in ie!
                    contentButtonIds[countButtons] = el.substring(el.indexOf('_') + 1, el.length);
                    var help = elements[i].childNodes[childNodeCounter_1].childNodes[childNodeCounter_2].childNodes[childNodeCounter_1].innerHTML;
                    _toolbarHelp[countButtons] = help.replace(/[\t\r\n]*/g, '').replace(/^\s*/, '').replace(/\s*$/, '');
                    _toolbarClick[countButtons] = '_caller.triggerCommandElement_' + contentButtonIds[countButtons] + '()';
                    var imgsrc = elements[i].childNodes[childNodeCounter_1].childNodes[childNodeCounter_1].childNodes[childNodeCounter_1].src;
                    _toolbarImages[countButtons] = imgsrc.substring(imgsrc.indexOf('/flexive'), imgsrc.length);
                    countButtons++;
                } else {
                    countButtons++; // FF: increase counter for AJAX buttons
                }
            }
        }
    }

    // retrieve the toolbar buttons
    if (_toolBar != null) {
        var _images = _toolBar.getElementsByTagName('img');
        regEx1 = /.*_toolbarIcon/;
        if (_images.length > 0) {
            for (var i = 0; i < _images.length; i++) {
                var img = _images[i].id;
                if (img != null) {
                    if (img == "") {
                        if (_images[i].src.indexOf('separator.png') != -1) { // unnecessary?
                            toolbarButtonIds[countTbButtons] = img;
                            _toolbarHelpRest[countTbButtons] = _images[i].alt;
                            _toolbarClickRest[countTbButtons] = '';
                            _toolbarImagesRest[countTbButtons] = _images[i].src;
                            countTbButtons++;
                        }
                    } else {
                        var index = linearSearch(img.substring(0, img.indexOf('_')), contentButtonIds, false);
                        if (img.match(regEx1) && index != -1) {
                            tbContentButtonCount++;
                        } else {
                            toolbarButtonIds[countTbButtons] = img.substring(0, img.indexOf('_'));
                            _toolbarHelpRest[countTbButtons] = _images[i].alt;
                            _toolbarClickRest[countTbButtons] = '_caller.triggerCommandElement_' + img.substring(0, img.indexOf('_')) + '()';
                            _toolbarImagesRest[countTbButtons] = _images[i].src;
                            countTbButtons++;
                        }
                    }
                }
            }
        }
    }

    // fills the parent. variables with the information from the rendered page (incl. ajax requests).
    var fillToolbar = function() { // reassign all variables in main.js
        parent.toolbarImages = _toolbarImages.concat(parent.toolbarImages);
        parent.toolbarIds = contentButtonIds.concat(parent.toolbarIds);
        parent.toolbarClick = _toolbarClick.concat(parent.toolbarClick);
        parent.toolbarHelp = _toolbarHelp.concat(parent.toolbarHelp);
        if (toolbarButtonIds.length > 0) {
            parent.toolbarImages = parent.toolbarImages.concat(_toolbarImagesRest);
            parent.toolbarIds = parent.toolbarIds.concat(toolbarButtonIds);
            parent.toolbarClick = parent.toolbarClick.concat(_toolbarClickRest);
            parent.toolbarHelp = parent.toolbarHelp.concat(_toolbarHelpRest);
        }
    };

    // removes ajax buttons from the toolbar
    var removeFromToolbar = function() {
        if (parent.ajaxButtonIds.length > 0) {
            for (var i = 0; i < parent.ajaxButtonIds.length; i++) {
                var index = linearSearch(parent.ajaxButtonIds[i], toolbarButtonIds, false);
                if (index != -1) {
                    toolbarButtonIds.splice(index, 1);
                    _toolbarImagesRest.splice(index, 1);
                    _toolbarClickRest.splice(index, 1);
                    _toolbarHelpRest.splice(index, 1);
                }
            }
        }
    };

    // IE will have all ajax buttons in the DOM tree, FF won't, hence some entries need to be removed.
    var removeFromContentForIE = function(inputArray) {
        for (var i = 0; i < inputArray.length; i++) {
            var index = linearSearch(inputArray[i], contentButtonIds, false);
            if (index != -1) {
                removeFromContent(index);
            }
        }
    };

    // similar to the previous function, also IE only.
    var removeFromContent = function(index) {
        contentButtonIds.splice(index, 1);
        _toolbarImages.splice(index, 1);
        _toolbarClick.splice(index, 1);
        _toolbarHelp.splice(index, 1);
    };

    // action1: rerender a vanilla toolbar, as given by the initial state of the page
    var toolbarRenderAction_1 = function() {
        parent.clearToolbar();
        removeFromToolbar();
        fillToolbar();
        parent.renderToolbar();
        parent.ajaxButtonIds.length = 0;// clear the ajaxButtonIds
    };

    // action2: clear the toolbar, rerender after retrieving the corresponing buttons
    var toolbarRenderAction_2 = function() {
        parent.clearToolbar();
        fillToolbar();
        parent.renderToolbar();
    };

    // action3: same as action2, also stores all ajax buttons in a separate variable in main.js
    var toolbarRenderAction_3 = function() {
        parent.ajaxButtonIds = parent.toolbarIds.slice(0);
        fillToolbar();
        parent.renderToolbar();
    };

    // actual rendering logic
    if (parent.ajaxButtonIds.length > 0) {
        if (parent.toolbarIds.length != parent.ajaxButtonIds.length) { // commandbutton's location="content"
            toolbarRenderAction_1();
        } else if (parent.ajaxButtonIds.length == parent.toolbarIds.length) { // we are probably dealing with an "empty" submission
            if (document.all) {
                var contentOnlyButtonIndex = linearSearch('_LOCCONTENT', contentButtonIds, true);
                if (contentOnlyButtonIndex < 0) {
                    toolbarRenderAction_2();
                } else {
                    while (contentOnlyButtonIndex >= 0) {
                        removeFromContent(contentOnlyButtonIndex);
                        contentOnlyButtonIndex = linearSearch('_LOCCONTENT', contentButtonIds, true);
                    }
                }
            }
            toolbarRenderAction_2();
        }
    } else { // commandButton's location="toolbar" / "content"
        if (parent.toolbarIds.length != (contentButtonIds.length + toolbarButtonIds.length)) {
            if (document.all) { // IE ajax call, need to remove ajaxbutton from contentButtonIds
                var contentOnlyButtonIndex = linearSearch('_LOCCONTENT', contentButtonIds, true);
                if (contentOnlyButtonIndex < 0) {
                    removeFromContentForIE(parent.toolbarIds);
                    toolbarRenderAction_3();
                } else {
                    while (contentOnlyButtonIndex >= 0) {
                        removeFromContent(contentOnlyButtonIndex);
                        contentOnlyButtonIndex = linearSearch('_LOCCONTENT', contentButtonIds, true);
                    }
                    if (parent.toolbarIds.length != (contentButtonIds.length + toolbarButtonIds.length)) {
                        removeFromContentForIE(parent.toolbarIds);
                        toolbarRenderAction_3();
                    }
                }
            } else {
                toolbarRenderAction_3();
            }
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