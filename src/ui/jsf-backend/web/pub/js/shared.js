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
        alertDialog("getFirstLinkElement("+elementName+") failed: "+e);
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
    return string.replace("'", "\\'").replace('"', '\\"');
}

/**
 * copy cliptext to the OS's clipboard
 *
 * @param cliptext text to copy to the clipboard
 * @param errorMessage error message to display if copying failed (usually a how to enable the clipboard in mozilla/firefox)
 * @param successMessage message to display in an alert box if copy operation was successful (if present)
 */
function copy2clipboard(cliptext, errorMessage, successMessage) {

    if (window.clipboardData) {
        window.clipboardData.setData("Text", ""+cliptext);
    } else if (window.netscape) {
//this code seems to work on FF3 but not FF2 ...        
//        try {
//            netscape.security.PrivilegeManager.enablePrivilege('UniversalXPConnect');
//        } catch(ex) {
//            if( errorMessage ) alertDialog(errorMessage); else alertDialog(ex);
//            return false;
//        }
//        var clip = Components.classes['@mozilla.org/widget/clipboard;1'].createInstance(Components.interfaces.nsIClipboard);
//        if (!clip) {
//            if( errorMessage ) alertDialog(errorMessage);
//            return false;
//        }
//        var trans = Components.classes['@mozilla.org/widget/transferable;1'].createInstance(Components.interfaces.nsITransferable);
//        if (!trans) {
//            if( errorMessage ) alertDialog(errorMessage);
//            return false;
//        }
//        trans.addDataFlavor('text/unicode');
//        var len = new Object();
//        var str = Components.classes["@mozilla.org/supports-string;1"].createInstance(Components.interfaces.nsISupportsString);
//        var copytext = cliptext;
//        str.data = copytext;
//        trans.setTransferData("text/unicode", str, copytext.length * 2);
//        var clipid = Components.interfaces.nsIClipboard;
//        if (!clip) {
//            if( errorMessage ) alertDialog(errorMessage);
//            return false;
//        }
//        clip.setData(trans, null, clipid.kGlobalClipboard);

        //try flash
        var fxClipboard = 'fxClipboard';
        if (!document.getElementById(fxClipboard)) {
            //create the div
            var _div = document.createElement('div');
            _div.id = fxClipboard;
            document.body.appendChild(_div);
        }
        document.getElementById(fxClipboard).innerHTML = '';
        document.getElementById(fxClipboard).innerHTML = '<embed src="'+getBase()+'adm/js/fxClipCopy.swf" FlashVars="cliptext=' +
                                                         encodeURIComponent(cliptext) + '" width="0" height="0" type="application/x-shockwave-flash"></embed>';
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

