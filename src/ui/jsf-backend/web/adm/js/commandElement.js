//======================================================================================================================
// Registers a command element (eg. commandButton, commandIcon, ...)
//
// @callerWindow: the calling window
// @id: the unique id of the command element
//======================================================================================================================
function registerCommandElement(callerWindow, id, confirmTxt, lockscreen, disabled) {
    try {
        if (disabled)
            onClickAction = "";
        else {
            // Retrieve the command link
            var linkElement = getFirstLinkElement(callerWindow.document, "commandElement_" + id);
            // Retrieve the onClick action from the command link
            // Add the lock screen function
            //var onClickAction = getJsfOnClick(linkElement);
            //onClickAction = "parent.lockScreen();"+onClickAction;
            // DL: this also works with JSF 1.2 RI
            var onClickAction = "document.getElementById('" + linkElement.getAttribute("id") + "').onclick();";
            if (lockscreen) {
                onClickAction = "parent.lockScreen();" + onClickAction;
            }
            // Add the confirm script part if needed
            if (confirmTxt && confirmTxt != null && confirmTxt != '') {
                onClickAction = "confirmDialog('" + confirmTxt + "', function() { " + onClickAction + " })";
            }
        }
        // Store the action within the caller
        eval("callerWindow.commandElementAction_" + id + "=\"" + onClickAction + "\"");
        if (callerWindow.flexive != null) {
            callerWindow.flexive.yui.onYahooLoaded(
                    function() {
                        if (callerWindow.document.getElementById("commandButton_" + id) != null) {
                            callerWindow.document.getElementById("commandButton_" + id).onclick =
                            function() {
                                callerWindow.eval(onClickAction);
                                return false;
                            };
                        }
                    }
                    );
        }
    } catch (e) {
        alertDialog("Unable to register the command element '" + id + "': " + e);
    }
}


//======================================================================================================================
// Returns the javascript of a command element, which can then be called by using eval(__).
//
// The command element has to be registered in order for this function to work.
// @callerWindow: the calling window
// @id: the unique id of the command element
//======================================================================================================================
function getCommandElementScript(callerWindow, id) {
    try {
        return eval("callerWindow.commandElementAction_" + id);
    } catch (e) {
        alertDialog("Unable to trigger the command element '" + id + "': " + e);
        return null;
    }
}

//======================================================================================================================
// This function copies all relevant ajax-enabled commandbuttons (which also reside in the toolbar) and their
// positions(if given) to the corresponding arrays in main.js.
//======================================================================================================================
function registerAjaxToolbarButtons(ajax, location, id, toolbarPosition) {
    // add the ajax-enabled buttons to the corresp. js array IFF they are registered in the toolbar
    if (ajax && (location == 'both' || location == 'toolbar')) {
        var registeredId = false;
        var registeredIdToolbar = false;
        var regIdsLength = parent.ajaxRegisteredIds.length;
        var toolbarLength = parent.ajaxRegisteredIdsToolbarOnly.length;
        // check if button already in array
        for (var i = 0; i < regIdsLength; i++) {
            if (parent.ajaxRegisteredIds[i] == id)
                registeredId = true;
        }
        // check if toolbar only button already in array
        for (var i = 0; i < toolbarLength; i++) {
            if (parent.ajaxRegisteredIdsToolbarOnly[i] == id)
                registeredIdToolbar = true;
        }
        if (!registeredId) {
            parent.ajaxRegisteredIds[regIdsLength] = id;
        }
        if (!registeredIdToolbar && location == 'toolbar') {
            parent.ajaxRegisteredIdsToolbarOnly[toolbarLength] = id;
        }
        // toolbar position will be arbitrary (previous to a re-render) if not set
        if (toolbarPosition != 'NOTSET') {
            for (var i = 0; i < parent.ajaxRegisteredIds.length; i++) {
                if (parent.ajaxRegisteredIds[i] == id)
                    parent.ajaxRegisteredIdPositions[i] = toolbarPosition;
            }
        }

        if (parent.ajaxRegisteredIdPositions.length > 1)
            sortAjaxPositions();
    }
}

//======================================================================================================================
// A very simple linear one-pass sorting algorithm (which should prove sufficient for the given number of
// commandButtons in the toolbar) to sort the ajaxRegisteredButtons & ids on a (partial) page-rerender.
//======================================================================================================================
function sortAjaxPositions() {
    for (var i = 0; i < parent.ajaxRegisteredIdPositions.length; i++) {
        if (i + 1 < parent.ajaxRegisteredIdPositions.length) {
            if (parent.ajaxRegisteredIdPositions[i] > parent.ajaxRegisteredIdPositions[i + 1]) {
                var tmp = parent.ajaxRegisteredIdPositions[i + 1];
                parent.ajaxRegisteredIdPositions[i + 1] = parent.ajaxRegisteredIdPositions[i];
                parent.ajaxRegisteredIdPositions[i] = tmp;
                tmp = parent.ajaxRegisteredIds[i + 1];
                parent.ajaxRegisteredIds[i + 1] = parent.ajaxRegisteredIds[i];
                parent.ajaxRegisteredIds[i] = tmp;
            }
        }
    }
}