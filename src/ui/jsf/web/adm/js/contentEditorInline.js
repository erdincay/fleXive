var theEditor       = null;
var editorHolder    = null;
var theControlPanel = null;
var cmsElementsOn   = true;
var fxPK            = null;
var fxNode           = null;
var fxContentType   = null;
var fxContext       = null;
var fxNodeId        = null;
var fxIsIE          = false;

/**
 * Closes any open CMS menu.
 */
function closeCmsMenu(event) {
    fxPK = null;
    fxContentType = null;
    if (event!=null) {
        ignoreEvent(event);
    }
    var menu = document.getElementById("flexiveCmsMenu");
    menu.style.display = 'none';
    return false;
}

function highlight(caller,state) {
    caller.style.backgroundColor=state?'#eeeeee':'#F9FCF1';
}

function closeEditor() {
    editorHolder.style.display='none';
    closeCmsMenu(null);
    // Enable scrollbars again
    document.getElementsByTagName('body')[0].style.overflow='auto';
}

/**
 * Opens a CMS Menu for the given pk.

 * @param caller the caller (the image that the user clicked on)
 * @param event the on click event
 * @param pk the primary key
 * @param node the node to work on (past, new, ..). If null/empty the current page is used
 * @param typeName the content type to use for operations like 'new'
 */
function showCmsMenu(caller, event, pk, typeName,node) {
    ignoreEvent(event);
    fxPK = pk.replace(/^\s*|\s*$/g,'');
    fxNode = (node==null || node=='')?".":node.replace(/^\s*|\s*$/g,'');
    fxContentType = typeName.replace(/^\s*|\s*$/g,'');
    var menu = document.getElementById("flexiveCmsMenu");
    menu.style.top = getYpos(caller) + "px";
    menu.style.left = getXpos(caller) + "px";
    menu.style.display = 'inline';

    var inline = fxIsIE?"inline":"table-row";

    if (pk=='') {
        document.getElementById('flexiveCmsMenu_edit').style.display='none';
        document.getElementById('flexiveCmsMenu_newBefore').style.display='none';
        document.getElementById('flexiveCmsMenu_newAfter').style.display='none';
        document.getElementById('flexiveCmsMenu_cut').style.display='none';
        document.getElementById('flexiveCmsMenu_copy').style.display='none';
        document.getElementById('flexiveCmsMenu_new').style.display=inline;
    } else {
        document.getElementById('flexiveCmsMenu_edit').style.display=inline;
        document.getElementById('flexiveCmsMenu_newBefore').style.display=inline;
        document.getElementById('flexiveCmsMenu_newAfter').style.display=inline;
        document.getElementById('flexiveCmsMenu_cut').style.display=inline;
        document.getElementById('flexiveCmsMenu_copy').style.display=inline;
        document.getElementById('flexiveCmsMenu_new').style.display='none';
    }
    return false;
}

function getXpos(ai) {
    var xPos = ai.offsetLeft;
    var tempEl = ai.offsetParent;
    while (tempEl != null) {
        xPos += tempEl.offsetLeft;
        tempEl = tempEl.offsetParent;
    }
    return xPos;
}

function getYpos(ai) {
    var yPos = ai.offsetTop;
    var tempEl = ai.offsetParent;
    while (tempEl != null) {
        yPos += tempEl.offsetTop;
        tempEl = tempEl.offsetParent;
    }
    return yPos;
}

/**
 * Returns a handle to the editor object.
 */
function getEditor(visible) {

    document.getElementById("flexiveCmsMenu").style.display='none';
    if (theEditor!=null) return theEditor;

    var theBody = document.getElementsByTagName('body')[0];
    editorHolder = document.createElement('div');
    editorHolder.style.verticalAlign = 'middle';
    editorHolder.style.textAlign = 'center';
    editorHolder.setAttribute("id", "flexiveInlineEditorHolder");
    editorHolder.style.marginTop='0';
    editorHolder.style.position = 'absolute';
    editorHolder.style.top = '0';
    editorHolder.style.left = '0';
    editorHolder.style.paddingTop='30px';
    editorHolder.style.width = '100%';
    editorHolder.style.height = '100%';
    editorHolder.style.backgroundImage="url('"+fxContext+"/adm/images/layout/screenLockBg.gif')";
    editorHolder.style.display = visible?'inline':'none';
    editorHolder.onclick='';

    var theFrame = document.createElement("iframe");
    theFrame.setAttribute("id", "flexiveInlineEditorFrame");
    theFrame.setAttribute("src", "");
    theFrame.setAttribute("scrolling", "on");
    theFrame.setAttribute("frameborder", "0");
    theFrame.style.width = '80%';
    theFrame.style.height = '90%';
    theFrame.style.border='1px solid black';
    editorHolder.appendChild(theFrame);

    theBody.appendChild(editorHolder);
    theEditor = theFrame;
    return theEditor;
}

/**
 * Launches the content editor for the specified primary key.
 *
 * @param pk the primary key
 */
function editInstance() {
    try {
        // Obtain a reference to the editor frame ..
        var editor = getEditor(true);
        // Load the editor into it ...
        editor.src=fxContext+"/adm/content/inlineContentEditor.jsf?action=editInstance&pk="+fxPK+
                   "&page="+fxNodeId;
        editorHolder.style.display='inline';
        // Scroll to top ..
        scrollTo(0,0);
        // and disable any scrollbars.
        document.getElementsByTagName('body')[0].style.overflow='hidden';
    } catch (exc) {
        // Outch .. something went wrong. show it!
        alert("Failed to initialize the editor for instance ["+fxPK+"] : "+exc);
    }
}

/**
 * Launches the editor with the given type.
 *
 * @param typeName the type
 */
function newInstance(typeName,beforeAncor) {
    try {
        // Obtain a reference to the editor frame ..
        var editor = getEditor(true);
        // Load the editor into it ...
        editor.src=fxContext+"/adm/content/inlineContentEditor.jsf?action=newInstance&ancorPk="+fxPK+
                   "&page="+fxNodeId+"&beforeAncor="+beforeAncor+"&node="+fxNode+"&typeName="+fxContentType;
        // Scroll to top ..
        scrollTo(0,0);
        // and disable any scrollbars.
        document.getElementsByTagName('body')[0].style.overflow='hidden';
    } catch (exc) {
        // Outch .. something went wrong. show it!
        alert("Failed to initialize the editor for type ["+typeName+"] : "+exc);
    }
}


function copyInstance() {
    try {
        // Obtain a reference to the editor frame ..
        var editor = getEditor(false);
        // Load the editor into it ...
        editor.src=fxContext+"/adm/content/iceCutCopyPaste.jsf?action=copy&pk="+fxPK+"&page="+fxNodeId+"&node="+fxNode;
        // Scroll to top ..
        scrollTo(0,0);
        // and disable any scrollbars.
        document.getElementsByTagName('body')[0].style.overflow='hidden';
    } catch (exc) {
        // Outch .. something went wrong. show it!
        alert("Failed to copy the instance ["+fxPK+"] : "+exc);
    }
}


function cutInstance() {
    try {
        // Obtain a reference to the editor frame ..
        var editor = getEditor(false);
        // Load the editor into it ...
        editor.src=fxContext+"/adm/content/iceCutCopyPaste.jsf?action=cut&pk="+fxPK+"&page="+fxNodeId+"&node="+fxNode;
        // Scroll to top ..
        scrollTo(0,0);
        // and disable any scrollbars.
        document.getElementsByTagName('body')[0].style.overflow='hidden';
    } catch (exc) {
        // Outch .. something went wrong. show it!
        alert("Failed to copy the instance ["+fxPK+"] : "+exc);
    }
}



function pasteInstance(beforeAncor) {
    try {
        // Obtain a reference to the editor frame (invisible) ..
        var editor = getEditor(false);
        // Call the paste action
        editor.src=fxContext+"/adm/content/iceCutCopyPaste.jsf?action=paste&beforeAncor="+beforeAncor+
                   "&page="+fxNodeId+"&ancorPk="+fxPK+"&node="+fxNode;
    } catch (exc) {
        // Outch .. something went wrong. show it!
        alert("Failed to paste the instance ["+fxPK+"] : "+exc);
    }
}


/**
 * Enables/Disables all CMS panels in the page
 */
function toggleCmsElements() {
    var idx;
    try {
        var theBody = document.getElementsByTagName('body')[0];
        var allDIVs = theBody.getElementsByTagName("div");
        var allIMGs = theBody.getElementsByTagName("img");
        _toggleCmsElements(allDIVs);
        _toggleCmsElements(allIMGs);
        cmsElementsOn = !cmsElementsOn;
    } catch (e) {
        alert(e);
        /* ignore */
    }
}

/**
 * Helper function for toggleCmsElements.
 */
function _toggleCmsElements(elemtents) {
    for (var idx = 0; idx < elemtents.length; idx++) {
        var ele = elemtents[idx];
        if (ele.getAttribute('cmsElement') == 'true') {
            if (cmsElementsOn) {
                ele.style.display = 'none';
            } else {
                if (ele.id != 'flexiveCmsMenu') {
                    ele.style.display = 'inline';
                }
            }
        }
    }
}



function addTab() {

}

function lockScreen() {

}

function addToolbarItem() {

}

function addClientIdWithError(caller,clientId,detailTxt) {

}
