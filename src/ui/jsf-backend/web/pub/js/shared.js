/**
 * Stops the propagation of the given event.
 */
function ignoreEvent(e) {
    if (!e) return;
    if (e == null) return;
    try {
        e.cancelBubble = true;
        return;
    } catch(e) {
        /* ignore, bubble is IE specific */
    }
    e.stopPropagation();
}


//======================================================================================================================
// Returns the first <a ... > element within the specified parent element.
//
// The lookup is only performed in the first child hirarchy.
// @doc: the document the element is in
// @elementName: the name of the parent element
// @returns: the first <a ... > element found within the parent element, or null
//           if the parent element does not exist or doesnt have a link element.
//======================================================================================================================
function getFirstLinkElement(doc,elementName) {
    try {
        var parent = doc.getElementById(elementName);
        if (parent==null) {
            alertDialog("No parent with id=" + elementName + " found");
            return null;
        }
        var childs = parent.childNodes;
        if (childs==null) {
            alertDialog("No children for parent-id=" + elementName + " found");
            return null;
        }
        for (var i=0;i<childs.length;i++) {
            if (childs[i].tagName=="A" || childs[i].tagName=="a") {
                return childs[i];
            }
        }
        return null;
    } catch (e) {
        alert("getFirstLinkElement("+elementName+") failed: "+e);
        return null;
    }
}
//======================================================================================================================
// Returns the first <tagName ... > element within the specified parent element.
//
// The lookup is only performed in the first child hirarchy.
// @doc: the document the element is in
// @elementName: the name of the parent element
// @returns: the first <a ... > element found within the parent element, or null
//           if the parent element does not exist or doesnt have a link element.
//======================================================================================================================
function getFirstMatchingElement(parent,tagName) {
    try {
        var childs = parent.childNodes;
        if (childs==null) {
            return null;
        }
        for (var i=0;i<childs.length;i++) {
            if (childs[i].tagName==tagName) {
                return childs[i];
            } else {
                var ele = getFirstMatchingElement(childs[i],tagName);
                if (ele!=null) {
                    return ele;
                }
            }
        }
        return null;
    } catch (e) {
        alertDialog("getFirstMatchingElement("+tagName+") failed: "+e);
        return null;
    }
}

function getWindowWidth() {
    var myWidth = 0;
    if( typeof( window.innerWidth ) == 'number' ) {
        //Non-IE
        myWidth = window.innerWidth;
    } else if( document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight ) ) {
        //IE 6+ in 'standards compliance mode'
        myWidth = document.documentElement.clientWidth;
    } else if( document.body && ( document.body.clientWidth || document.body.clientHeight ) ) {
        //IE 4 compatible
        myWidth = document.body.clientWidth;
    }
    return myWidth;
}

function getWindowHeight() {
    var myHeight = 0;
    if( typeof( window.innerWidth ) == 'number' ) {
        //Non-IE
        myHeight = window.innerHeight;
    } else if( document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight ) ) {
        //IE 6+ in 'standards compliant mode'
        myHeight = document.documentElement.clientHeight;
    } else if( document.body && ( document.body.clientWidth || document.body.clientHeight ) ) {
        //IE 4 compatible
        myHeight = document.body.clientHeight;
    }
    return myHeight;
}


function trimPx(value) {
    if ((""+value).charAt(value.length-1)=="x") value = value.substring(0, value.length-2);
    return parseInt(value);
}

function getScrollWidth() {
    var w = window.pageXOffset ||
            document.body.scrollLeft ||
            document.documentElement.scrollLeft;
    return w ? w : 0;
}

function getScrollHeight() {
    var h = window.pageYOffset ||
            document.body.scrollTop ||
            document.documentElement.scrollTop;
    return h ? h : 0;
}

function escapeQuotes(string) {
    return flexive.util.escapeQuotes(string);
}

/**
 * copy cliptext to the OS's clipboard - works in IE only!
 *
 * @param cliptext text to copy to the clipboard
 * @param errorMessage error message to display if copying failed (usually a how to enable the clipboard in mozilla/firefox)
 * @param successMessage message to display in an alert box if copy operation was successful (if present)
 */
function copy2clipboard(cliptext, errorMessage, successMessage) {

    if (window.clipboardData) {
        window.clipboardData.setData("Text", ""+cliptext);
    }
    if( successMessage ) alertDialog(successMessage);
    return false;
}

function getPreElementContent(elementName) {
    if( window.netscape ) {
        if( document.getElementById(elementName).innerText )
            return document.getElementById(elementName).innerText;
        return document.getElementById(elementName).textContent;
    } else
        return document.getElementById(elementName).innerText;
}

// evaluates command when the 'enter' key was pressed
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

/* Returns the value of the selected option of the given select element,
   or null if no option is selected. */
function getSelectedOptionValue(selectElement) {
    var option = getSelectedOption(selectElement);
    return option != null ? option.value : null;
}

function getSelectedOption(selectElement) {
    if (selectElement == null || selectElement.selectedIndex < 0 || selectElement.options == null ||
        selectElement.selectedIndex >= selectElement.options.length)
        return null;
    else
        return selectElement.options[selectElement.selectedIndex];
}
