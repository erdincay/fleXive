//======================================================================================================================
// Registers a command element (eg. commandButton, commandIcon, ...)
//
// @callerWindow: the calling window
// @id: the unique id of the command element
//======================================================================================================================
function registerCommandElement(callerWindow,id,confirmTxt,lockscreen) {
    try {
        var doc = callerWindow.document;
        // Retrieve the command link
        var linkElement = getFirstLinkElement(callerWindow.document,"commandElement_"+id);
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
        if (confirmTxt && confirmTxt!=null && confirmTxt!='') {
            onClickAction="if (confirm('"+confirmTxt+"')) {"+onClickAction+"}";
        }
        // Store the action within the caller
        eval("callerWindow.commandElementAction_"+id+"=\""+onClickAction+"\"");
    } catch (e) {
        alert("Unable to register the command element '"+id+"': "+e);
    }
}


//======================================================================================================================
// Returns the javascript of a command element, which can then be called by using eval(__).
//
// The command element has to be registered in order for this function to work.
// @callerWindow: the calling window
// @id: the unique id of the command element
//======================================================================================================================
function getCommandElementScript(callerWindow,id) {
    try {
        return eval("callerWindow.commandElementAction_"+id);
    } catch (e) {
        alert("Unable to trigger the command element '"+id+"': "+e);
        return null;
    }
}