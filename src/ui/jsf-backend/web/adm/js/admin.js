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
        alert("onReturnEval: no event object found.");
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
    return dojo.lang.map(pks, function(pk) { return pk.substring(0, pk.indexOf(".")) });
}

function setImage(elementId, src) {
    document.getElementById(elementId).src = src;
}