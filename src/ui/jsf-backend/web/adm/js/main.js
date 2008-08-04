/**
 * Javascript functions included by the main page.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at) 
 */
var SCREEN_LOCKED = false;
var activeTabId = 0;
var contentFrameObj;
var leftArea;
var dragArea;
var rightArea;
var pageName;
var pageTime;
var info;
var slbg;
var slbgLine;
var mainTbl;
var header;
var inDrag = false;
var lock;
var dragAreaBottomLinie;
var lockFrame;
var searchOpts;
var contentHeader;
var fxToolbar;
var tabTitels = [];
var tabActive = [];
var tabId = [];
var tabUri = [];
var _caller;
var _tabResponseId;
var _toolbarResponseId;
var messages = {};

window.onresize = windowResize;

var contentClipboard = new ContentClipboard();

function getContentClipboard() {
    return contentClipboard;
}

function stopPropagation(ev) {
    try {
        ev.stopPropagation();
        ev.preventDefault();
    } catch(e) {
        // nothing
    }
}

function beginDrag(ev) {
    stopPropagation(ev);
    inDrag=true;
    getNavFrameObj().style.display='none';
    contentFrameObj.style.display='none';
    slbg.style.display='block';
    slbgLine.style.display='inline';
}

function endDrag(ev) {
    stopPropagation(ev);
    inDrag=false;
    getNavFrameObj().style.display='inline';
    contentFrameObj.style.display='inline';
    slbg.style.display='none';
    slbgLine.style.display='none';
    getNavFrameWnd().windowResize();
}



function doDrag(event) {
    if (!inDrag) return;
    resize(event.clientX);
}

function init() {
    var loadingDiv = document.getElementById('loading');
    if( loadingDiv )
        loadingDiv.style.height=getWindowHeight()+"px";
    leftArea = document.getElementById('leftArea');
    dragArea = document.getElementById('dragArea');
    rightArea = document.getElementById('rightArea');

    dragAreaBottomLinie = document.getElementById('dragAreaBottomLinie');
    contentFrameObj = document.getElementById('contentFrame');
    pageName = document.getElementById('pageName');
    pageTime = document.getElementById('pageTime');
    info = document.getElementById('info');
    header = document.getElementById('header');
    slbg = document.getElementById('slbg');
    slbgLine = document.getElementById('slbgLine');
    lock = document.getElementById("lock");
    mainTbl = document.getElementById("mainTbl");
    contentHeader = document.getElementById("contentHeader");
    fxToolbar =document.getElementById("fxToolbar");
    lockFrame = document.getElementById("lockFrame");
    windowResize();
    renderTabs();

    gotoNavMenu(0);
    if( loadingDiv )
        loadingDiv.style.display='none';
}

// Opens the given tab menu in the main navigation
// Returns true if the navigation page is already available, false if it still loads
function gotoNavMenu(id) {
    // hide current menu
    var loaded = true;
    getNavFrameObj().style.display = "none";
    activeTabId = parseInt(id);
    if (getNavFrameObj().getAttribute("loaded") == "false") {
        // implement lazy loading of navigation tabs 
        getNavFrameWnd().document.location.href = getBase() + "/" + getNavFrameObj().getAttribute("navigationSrc");
        getNavFrameObj().setAttribute("loaded", true)
        loaded = false;
    } else {
        if (getNavFrameWnd().onShow) {
            // call custom onShow handler
            getNavFrameWnd().onShow();
        }
    }
    // show new menu
    getNavFrameObj().style.display = "block";
    windowResize();
    return loaded;
}

function getNavFrameObj() {
    return document.getElementById('treeNavFrame_' + activeTabId);
}

function getNavFrameWnd() {
    return frames["treeNavFrame_" + activeTabId];
}

function getContentNavFrame() {
    return frames["treeNavFrame_0"];
}

function getStructureNavFrame() {
    return frames["treeNavFrame_1"];
}

function getNavFrameTop() {
    return frames['navFrameTop'];
}

function resize(leftAreaWidth) {
    if (leftAreaWidth<160) {
        leftAreaWidth=160;
    }
    var windowWidth = trimPx(document.body.clientWidth);
    var newRightAreaWidth = (windowWidth)-leftAreaWidth;
    if (newRightAreaWidth<0) {
        newRightAreaWidth=0;
    }
    slbgLine.style.left=leftAreaWidth+"px";
    slbgLine.style.height=getWindowHeight()+"px";
    leftArea.style.width=leftAreaWidth+"px";
    //dragAreaBottomLinie.style.left=leftAreaWidth+"px";
}

function windowResize() {
    var newHeight = getWindowHeight();
    //navFrame.style.height=(newHeight-30-(getMenuItemCount()*21))+"px";
    var footerHeight = 28/*60*/;
    getNavFrameObj().style.height=(newHeight-30)+"px";
    contentFrameObj.style.height=(newHeight-30-footerHeight)+"px";
    //dragAreaBottomLinie.style.top=(newHeight-34)+"px";
    info.style.left=(getWindowWidth()-50)+"px";
    if (getNavFrameWnd().windowResize) {
        getNavFrameWnd().windowResize();
    }
}


function lockScreen() {
    if (SCREEN_LOCKED) {
        return true;
    }
    SCREEN_LOCKED = true;

    var windowHeight = getScrollHeight()+getWindowHeight();
    var windowWidth = getScrollWidth()+getWindowWidth();

    // Position the lockScreen message, and take the scrollarea into account
    lockFrame.style.top=(100+getScrollHeight())+"px";
    lockFrame.style.left=(((windowWidth-402)/2)+getScrollWidth())+"px";

    lock.style.height=windowHeight+"px";
    lock.style.width=windowWidth+"px";
    lock.style.display="inline";
    return false;
}

function unlockScreen() {
    if (!SCREEN_LOCKED) {
        return;
    }
    SCREEN_LOCKED = false;
    lock.style.display="none";
    greyOutInputs(getNavFrameWnd().document,false);
    greyOutInputs(frames['contentFrame'].document,false);
}

function greyOutInputs(_document,state) {
    var idx;
    var i;
    try {
        // Display fix for IE (see lockScreen)
        // set all drop down lists to background color white
        var color = state?'#eeeeee':'#ffffff';
        for (idx = 0; idx < _document.forms.length; idx++) {
            for (i = 0; i < _document.forms[idx].elements.length; i++) {
                var ele = _document.forms[idx].elements[i];
                var type = ele.type;
                if (type == 'select-one') {
                    ele.style.backgroundColor = color;
                }
            }
        }
    } catch (e) {
        /* ignore */
    }
}



function refreshTaskFrame() {
    getNavFrameWnd().location.reload();
}

function clearTabs() {
    tabTitels=[];
    tabActive = [];
    tabId=[];
    tabUri= [];
}


function addDynTab(responseId,caller,id,title,active) {

    // First get the document of the caller
    var _document;
    try {
        _caller = caller;
        _document = caller.document;
        if (!_document || _document==null) {
            return;
        }
    } catch (e) {
        // document not defined, just return
        return;
    }

    // Clear the tabs of an old request/response if needed
    if (_tabResponseId!=responseId) {
        clearTabs();
        _tabResponseId=responseId;
    }

    // Add the tab
    try {
        // Either add the tab, or replace it if it exists
        var pos = -1;
        for (var i=0;i<tabTitels.length;i++) {
            if (tabTitels[i]==title) {
                pos = i;
                break;
            }
        }
        if (pos==-1) {
            pos = tabTitels.length;
        }
        tabTitels[pos]=title;
        tabActive[pos]=active;
        tabId[pos]=id;
        tabUri[pos]="toggleDynTab("+pos+",'"+id+"')";
    } catch (e) {
        alertDialog("failed to add the tab '"+title+"':"+e);
    }
}

function toggleDynTab(pos,divId) {
    for (var i=0;i<tabActive.length;i++) {
        tabActive[i]=(i==pos);
    }
    renderTabs();
}

function addTab(responseId,caller,id,title,url,active) {

    // First get the document of the caller
    var _document;
    try {
        _caller = caller;
        _document = caller.document;
        if (!_document || _document==null) {
            return;
        }
    } catch (e) {
        // document not defined, just return
        return;
    }

    // Clear the tabs of an old request/response if needed
    if (_tabResponseId!=responseId) {
        clearTabs();
        _tabResponseId=responseId;
    }

    // Add the tab
    try {
        var ele = null;
        var jsfOnClick = "";
        var idx;
        for (idx=0;idx<_document.forms.length;idx++) {
            var formName = _document.forms[idx].id;
            var eleName = formName+":"+id;
            ele = _document.getElementById(eleName);
            if (ele && ele!=null) {
                break;
            }
        }
        if (!ele || ele==null) {
            //alertDialog("Unable to obtain reference to tab '"+id+"'");
        } else {
            jsfOnClick = getJsfOnClick(ele);
            eval("_caller.tabClickScript"+id+"=\""+jsfOnClick+"\"");
            jsfOnClick = "_caller.tabClickFct"+id+"()";
            //alertDialog(jsfOnClick);
            //_document.alertDialog('a');
        }

        // Either add the tab, or replace it if it exists
        var pos = -1;
        for (var i=0;i<tabTitels.length;i++) {
            if (tabTitels[i]==title) {
                pos = i;
                break;
            }
        }
        if (pos==-1) {
            pos = tabTitels.length;
        }
        tabTitels[pos]=title;
        tabActive[pos]=active;
        tabId[pos]=id;
        tabUri[pos]=jsfOnClick;
    } catch (e) {
        alertDialog("Failed to add tab '"+title+"':"+e);
    }
}




//======================================================================================================================
// Extracts the onclick javascript event from the given element.
//
// @ele: the element to process
// @returns: the onclick javascript event defined by the element
//======================================================================================================================
function getJsfOnClick(ele) {
    return "document.getElementById('" + ele.getAttribute("id") + "').onclick();";
/*    var onclickevent = ""+ele.onclick;
    var lines = onclickevent.split("\n");
    var callme = "";
    if (navigator.userAgent.toLowerCase().indexOf('msie')>0) {
        callme = lines[2];
        callme=callme.replace("return false;","");
    } else {
        for (var i=1;i<(lines.length-2);i++) {
            callme=callme+lines[i];
        }
    }
    callme = callme.replace(/"/g,"'");
    return callme;*/
}

var toolbarImages = [];
var toolbarHelp   = [];
var toolbarClick  = [];
var toolbarIds    = [];

function clearToolbar() {
    toolbarImages=[];
    toolbarHelp=[];
    toolbarClick=[];
    toolbarIds=[];
}

function getCallerElement(caller,id) {
    var _document;
    // First get the document of the caller
    try {
        _document = caller.document;
        if (!_document || _document==null) {
            return null;
        }
    } catch (e) {
        // document not defined, just return
        return null;
    }
    // Now try to find the element in the forms
    try {
        var ele = null;
        for (var idx=0;idx<_document.forms.length;idx++) {
            var formName = _document.forms[idx].id;
            var eleName = formName+":"+id;
            ele = _document.getElementById(eleName);
            if (ele && ele!=null) {
                break;
            }
        }
        if (!ele || ele==null) {
            return null;
        } else {
            return ele;
        }
    } catch (e) {
        alertDialog('getCallerElement(..,'+id+') failed: '+e);
        return null;
    }
}




//======================================================================================================================
// Adds a item to the toolbar
//
// @responseId: the unique response id
// @caller: the calling document
// @id: the id of the item to add
// @url: the url to cal (not used at the moment)
// @icon: the icon to use in the toolbar
//======================================================================================================================
function addToolbarItem(responseId,caller,id,helpTxt,url,iconUrl) {
    try {
        _caller = caller;

        // Clear the tabs of an old request/response if needed
        if (_toolbarResponseId!=responseId) {
            clearToolbar();
            _toolbarResponseId=responseId;
        }

        // Add the new element
        var pos = toolbarImages.length;
        toolbarImages[pos]=iconUrl;
        toolbarHelp[pos]=helpTxt;
        toolbarClick[pos]="_caller.triggerCommandElement_"+id+"()";
        toolbarIds[pos]=id;
    } catch (e) {
        alertDialog("Unable to trigger the command element '"+id+"' from the toolbar: "+e);
    }
}

//Add a toolbar separator
//a separator can never be the first item (responseId wont match which is a desired sideffect!)
function addToolbarSeparator() {
    var pos = toolbarImages.length;
    toolbarImages[pos] = getBase()+'/adm/images/toolbar/separator.png';
    toolbarHelp[pos] = '';
    toolbarClick[pos] = '';
    toolbarIds[pos] = '';
}


function setActionClickScript(caller,id) {
    try {
        var ele = getCallerElement(caller,id);
        if (!ele || ele==null) {
            return;
        }
        eval("caller.actionClickScript"+id+"=\""+getJsfOnClick(ele)+"\"");
    } catch (e) {
        alertDialog("Failed to set the action click script for element with id='"+id+"': "+e);
    }
}

function renderToolbar() {
    if (!fxToolbar || fxToolbar==null) {
        return;
    }
    var html = "";
    var pos;
    var clazz = "";
    for (pos = 0; pos < toolbarImages.length; pos++) {
        if( pos > 0 && toolbarIds[pos] == '' && toolbarIds[pos-1] == '' )
            continue; //prevent double separators
        clazz = toolbarIds[pos] == '' ? "fxToolbarSeparator" : "fxToolbarIcon";
        html += "<img "+
                (toolbarIds[pos] != '' ? "id=\"" + toolbarIds[pos] + "_toolbarIcon\" " : "")+
                "src=\"" + toolbarImages[pos] +
                "\" class=\"" + clazz + "\" " +
                (toolbarClick[pos] != '' ? "onClick=\"" + toolbarClick[pos] + "\" ":"")+
                "alt=\"" + toolbarHelp[pos]
                + "\" title=\"" + toolbarHelp[pos] + "\"/>";
    }
    html+="<span id='toolbarMessage'> </span>";
    fxToolbar.innerHTML=html;
}

function renderTabs() {
    if (!contentHeader || contentHeader==null) {
        return;
    }
    var tabCount = tabTitels.length;
    var height = (tabCount==0)?"19":"18";
    var html =
            '<table cellpadding="0" cellspacing="0" id="contentHeaderTabTbl">\n'+
            ' <tr>\n';

    for (var pos=0;pos<tabCount;pos++) {
        var tabDiv = _caller.document.getElementById(tabId[pos]+"_content");
        if (tabDiv && tabDiv!=null) {
            tabDiv.style.display=tabActive[pos]?"inline":"none";
        }
        var act = tabActive[pos]?"Active":"";
        var prefix = (pos==0)?"First":"";
        var onClick = (tabActive[pos] || tabUri[pos]==null || tabUri.length=='')?"":" onClick=\""+tabUri[pos]+"\" ";
        html+=
        '  <td><img src="adm/images/layout/tabs/TabLinks'+prefix+act+'.gif" alt="tabImg" /></td>\n'+
        '  <td '+onClick+' class="tabText'+act+'">'+tabTitels[pos]+'</td>\n'+
        '  <td><img src="adm/images/layout/tabs/TabRechts'+act+'.gif" alt="tabImg" /></td>\n';
    }

    html+=
    '  <td width="100%" align="right">\n'+
    '    <div id="renderTime" style="height:'+height+'px;border-bottom:1px solid #bdbdbd">&nbsp;</div>\n'+
    '  </td>\n'+
    ' </tr>\n'+
    '</table>\n';
    contentHeader.innerHTML=html;
}



function pageLoaded(responseId,renderTime,bTreeWasModified) {
    document.getElementById("info").innerHTML= renderTime+"ms";
    renderTabs();
    renderToolbar();
    renderErrors();
    unlockScreen();
    if (bTreeWasModified && getNavFrameWnd().reloadContentTree) {
        getNavFrameWnd().reloadContentTree();
    }
}

function beginPage() {
    clearToolbar();
    clearTabs();
    clearclientIdsWithError();
}

var clientIdsWithError = [];


function clearclientIdsWithError() {
    clientIdsWithError = [];
}

function addClientIdWithError(caller,clientId,detailTxt) {
    if (!clientId || clientId=='') return;
    _caller = caller;
    var i;
    var clientIdInput = clientId + '_input_';
    for( i=0; i<clientIdsWithError.length; i++ ) {
        if( clientIdsWithError[i][0] == clientId || clientIdsWithError[i][0] == clientIdInput ) {
            clientIdsWithError[i][1] = detailTxt;
            return;
        }
    }
    clientIdsWithError[clientIdsWithError.length]=[clientId,detailTxt];
}

function renderErrors() {
    var i;
    var elementId;
    var errorObj;
    var errorTxt;
    var fctSet;
    var ele;
    var fctUnset = function() {document.getElementById("toolbarMessage").innerHTML= "";};
    for (i=0;i<clientIdsWithError.length;i++) {
        try {
            errorObj = clientIdsWithError[i];
            elementId = errorObj[0];
            errorTxt = errorObj[1];
            ele = findElementById(elementId+"_input_");
            if( ele == null ) 
                ele = findElementById(elementId);
            if (ele !=null) {
                // Write back the whole element id, might have been incomplete before
                errorObj[0] = ele.id;
                // Set background color, since border will not work for IE drop down lists
                ele.style.backgroundColor ="#f8e4e4";
                ele.onmouseover=function() {showError(this)};
                ele.onfocus=function() {showError(this);};
                ele.onblur=fctUnset;
                ele.onmouseout=fctUnset;
                ele = null;
            }
        } catch (e) {
            alertDialog("main.js/renderErrors():"+e+", element="+elementId+"; caller="+_caller);
        }
    }
}

function showError(element) {
    var i;
    for (i=0;i<clientIdsWithError.length;i++) {
        var errorObj = clientIdsWithError[i];
        var id = errorObj[0];
        if (id==element.id) {
            document.getElementById("toolbarMessage").innerHTML= "<font color='red'>"+errorObj[1]+"<font>";
            return;
        }
    }
}

function findElementById(elementId) {
    try {
        // Cut away 'null:' in case the form is not defined but still included in the elementId
        try {
            if (elementId.split(":")[0]=='null') {
                elementId=elementId.split(":")[1];
            }
        } catch (e) { /*ignore*/ }

        var result = _caller.document.getElementById(elementId);
        if (result!=null) {
            return result;
        }
        // We didnt find the variable, lets try to find it within the form
        var i;
        var _document = _caller.document;
        elementId = elementId.toLowerCase();
        for (var idx = 0; idx < _document.forms.length; idx++) {
            var _form = _document.forms[idx];
            for (i = 0; i < _form.elements.length; i++) {
                var ele = _form.elements[i];
                var curId = ele.id.toLowerCase();
                var curIdSplit = curId.split(":")[1];
                if (curId==elementId || curIdSplit==elementId) {
                    return ele;
                }
            }
        }
        return null;
    } catch (e) {
        alertDialog("main.js/findElementById():"+e+", element="+elementId+"; caller="+_caller);
        return null;
    }
}

// Reload the briefcase view, if available
function reloadBriefcases() {
    if (getNavFrameWnd().briefcasePanel) {
        getNavFrameWnd().reload();
    }
}

/**
 * DHTML replacement for Javascript's confirm() dialog
 * Don't invoke directly, use confirmDialog(...) from admin.js.
 *
 * @param message       the message to be displayed
 * @param onConfirmed   the function to be executed when the user confirmed the message
 * @param onCancel      the function to be executed when the user did not confirm (optional)
 */
function _confirmDialog(message, onConfirmed, onCancel) {
    var dialog = new YAHOO.widget.SimpleDialog("dlg", {
	    width: Math.min(message.length + 5, 40) + "em",
	    fixedcenter:true,
	    modal:true,
	    visible:false,
	    draggable:true,
        constraintoviewport: true,
        buttons:  [   { text: messages["Global.dialog.confirm.yes"],
	                    handler: function() {
                            dialog.disableButtons();
                            onConfirmed();
                            dialog.hide();
                            dialog.destroy();
                        },
                        isDefault:true },
                      { text: messages["Global.dialog.confirm.no"],
                        handler:function() {
                            dialog.disableButtons();
                            if (onCancel) onCancel();
                            dialog.hide();
                            dialog.destroy();
                        } }
                  ]
    });
    dialog.disableButtons = function() {
        this.getButtons()[0].disabled = true;
        this.getButtons()[1].disabled = true;
    }
    dialog.setHeader(messages["Global.dialog.confirm.title"]);
	dialog.setBody(message);
	dialog.cfg.setProperty("icon",YAHOO.widget.SimpleDialog.ICON_WARN);
    dialog.render(document.body);
    dialog.show();
}

/**
 * DHTML replacement for Javascript's alertDialog() dialog
 * Don't invoke directly, use alertDialog(...) from admin.js.
 *
 * @param message       the message to be displayed
 * @param onConfirmed   the function to be executed when the user confirmed the message
 * @param onCancel      the function to be executed when the user did not confirm (optional)
 */
function _alertDialog(message) {
    var dialog = new YAHOO.widget.SimpleDialog("dlg", {
	    width: Math.min(message.length + 5, 40) + "em",
	    fixedcenter:true,
	    modal:true,
	    visible:false,
	    draggable:true,
        constraintoviewport: true,
        buttons:  [   { text: messages["Global.dialog.alert.ok"],
	                    handler: function() { dialog.hide(); dialog.destroy(); },
                        isDefault:true } ]
    });
	dialog.setHeader(messages["Global.dialog.alert.title"]);
	dialog.setBody(message);
	dialog.cfg.setProperty("icon",YAHOO.widget.SimpleDialog.ICON_INFO);
    dialog.render(document.body);
    dialog.show();
}

/**
 * DHTML replacement for Javascript's prompt() dialog
 * Don't invoke directly, use promptDialog(...) from admin.js.
 *
 * @param message       the message to be displayed
 * @param defaultValue  the default value (optional)
 * @param onSuccess     the function to be executed when the user entered a valid value. The input value is passed in the first parameter.
 */
function _promptDialog(message, defaultValue, onSuccess) {
    var e = document.getElementById("promptDialog");
    document.getElementById("promptLabel").innerHTML = message;
    document.getElementById("promptInput").value = defaultValue != null ? defaultValue : "";

    if (e.dialog == null) {
        e.dialog = new YAHOO.widget.Dialog("promptDialog", {
            width: "40em",
            fixedcenter:true,
            modal:true,
            visible:false,
            draggable:true,
            postmethod: "none",
            constraintoviewport: true,
            buttons:  [   { text: messages["Global.dialog.prompt.submit"],
                            handler: function() { e.onSuccess(document.getElementById("promptInput").value); e.dialog.hide(); },
                            isDefault:true },
                          { text: messages["Global.dialog.prompt.cancel"],
                            handler:function() { e.dialog.hide(); } }
                      ]
        });
        document.getElementById("promptDialog").style.display = "block";
        e.dialog.render();
    }
    e.onSuccess = onSuccess;
    e.dialog.show();
}