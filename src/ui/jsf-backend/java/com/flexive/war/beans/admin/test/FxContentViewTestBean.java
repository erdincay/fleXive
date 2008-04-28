package com.flexive.war.beans.admin.test;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.search.FxResultSet;
import static com.flexive.shared.EJBLookup.getContentEngine;
import static com.flexive.shared.EJBLookup.getScriptingEngine;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxRuntimeException;
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
    private FxPK searchTestPK;

    public FxContentViewTestBean() {
        try {
            CacheAdmin.getEnvironment().getType("SearchTest");
        } catch (FxRuntimeException e) {
            // ensure that the SearchTest type exists
            try {
                getScriptingEngine().runScript("SearchTestType.gy", new FxScriptBinding());
                for (int i = 0; i < 5; i++) {
                    final FxContent co = getContentEngine().initialize("SearchTest");
                    co.randomize(1);
                    getContentEngine().save(co);
                }
            } catch (FxApplicationException e2) {
                new FxFacesMsgErr(e2).addToContext();
            }
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
                return getContentEngine().exportContent(savedContent);
            } catch (FxApplicationException e) {
                throw e.asRuntimeException();
            }
        }
        return null;
    }

    public SqlQueryBuilder getSearchTestBuilder() {
        return new SqlQueryBuilder().select("@pk").type("SearchTest");
    }

    public FxPK getSearchTestPK() {
        if (searchTestPK == null) {
            try {
                final FxResultSet result = new SqlQueryBuilder().select("@pk").type("SearchTest").getResult();
                searchTestPK = result.<FxPK>collectColumn(1).get(0);
            } catch (FxApplicationException e) {
                throw e.asRuntimeException();
            }
        }
        return searchTestPK;
    }
}
