package com.flexive.war.beans.admin.test;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.scripting.FxScriptBinding;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.beans.FxContentViewBean;

import javax.faces.context.FacesContext;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Miscellaneous tests for the fx:content component
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class FxContentViewTestBean {
    private List<String> testPropertyNames;

    public FxContentViewTestBean() {
        // ensure that the SearchTest type exists
        try {
            EJBLookup.getScriptingEngine().runScript("SearchTestType.gy", new FxScriptBinding());
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        }
    }

    public String getTypeName() {
        return "SearchTest";
    }

    public List<String> getTestPropertyNames() {
        if (testPropertyNames == null) {
            testPropertyNames = new ArrayList<String>();
            for (FxPropertyAssignment assignment : getType().getAssignedProperties()) {
                final String name = assignment.getProperty().getName().toLowerCase();
                if (name.endsWith("searchprop")
                        || name.endsWith("searchpropml")) {
                    testPropertyNames.add(name);
                }
            }
        }
        return testPropertyNames;
    }

    public FxType getType() {
        return CacheAdmin.getEnvironment().getType(getTypeName());
    }

    public String getSavedXML() {
        final Map<String, Object> requestMap = FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
        final FxContent savedContent = (FxContent) requestMap.get(FxContentViewBean.REQUEST_CONTENT);
        if (savedContent != null) {
            try {
                return EJBLookup.getContentEngine().exportContent(savedContent);
            } catch (FxApplicationException e) {
                throw e.asRuntimeException();
            }
        }
        return null;
    }
}
