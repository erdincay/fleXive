/**
 * Stopes the propagation of the given event.
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
            alert("No parent with id=" + elementName + " found");
            return null;
        }
        var childs = parent.childNodes;
        if (childs==null) {
            alert("No children for parent-id=" + elementName + " found");
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
        alert("getFirstMatchingElement("+tagName+") failed: "+e);
        return null;
    }
}

function getWindowWidth() {
    var myWidth = 0;
    if( typeof( window.innerWidth ) == 'number' ) {
        //Non-IE
        myWidth = window.innerWidth;
    } else if( document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight ) ) {
        //IE 6+ in 'standards compliant mode'
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