var activeMenu = null;
var activationTime = -1;

function closeMenu(e,force) {
    ignoreEvent(e);
    var delta = ((new Date().getTime())-activationTime);
    if (activeMenu!=null) {
        if (force || delta>200) {
            activeMenu.style.display='none';
            activeMenu=null;
            activationTime=-1;
        }
    }
}

function showMenu(e,caller,xpath) {
    ignoreEvent(e);
    closeMenu(e,true);
    var eleId = 'Menu_'+xpath;
    try {
        activeMenu = document.getElementById(eleId);
    } catch(e) {
        activeMenu = null;
    }
    if (activeMenu==null) {
        alertDialog("Menu is missing for id:"+eleId);
        return;
    }

    activeMenu.style.top=caller.offsetTop+"px";
    activeMenu.style.left=caller.offsetLeft+"px";
    activeMenu.style.display='inline';
    activationTime = new Date().getTime();
}

function prepareAdd(listBox) {
    var ele = document.getElementById("frm:"+listBox);
    var value = "";
    for (var i = 0; i < ele.length; i++) {
        if (ele.options[i].selected) {
            value+=((value=="")?"":",")+ele.options[i].value;
        }
    }
    document.getElementById("frm:elements").value=value;
    return true;
}

var dirtyFileInputs = false;
function fileInputChanged() {
    dirtyFileInputs = true;
}

function saveHtmlEditors() {
    var instances = [];
    for (var instanceName in tinyMCE.instances) {
        if (tinyMCE.isInstance(tinyMCE.instances[instanceName])) {
            instances.push(tinyMCE.instances[instanceName]);
        }
    }
    if (instances.length > 0) {
        tinyMCE.triggerSave();
        for (var i = 0; i < instances.length; i++) {
            tinyMCE.execCommand("mceRemoveControl", false, instances[i].editorId);
        }
    }
}

function preSubmit() {
    saveHtmlEditors();
}

function preA4jAction(xpath,action) {
    preSubmit();
    if (dirtyFileInputs) {
        // If a file input was changed we need to submit the whole form, since a4j XhtmlHttpRequests
        // are not able to process binaries.
        document.getElementById("frm:editorActionName").value=action;
        document.getElementById("frm:editorActionXpath").value=xpath;
        document.forms["frm"].submit();
        return false;
    } else {
        // If no file input was changed we can use the a4j XhtmlHttpRequest for submiting the
        // action and data.
        document.getElementById("frm:editorActionName").value='';
        document.getElementById("frm:editorActionXpath").value='';
        return true;
    }
}