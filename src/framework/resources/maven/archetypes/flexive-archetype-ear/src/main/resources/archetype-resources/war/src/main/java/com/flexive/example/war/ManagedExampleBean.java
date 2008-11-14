#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package com.flexive.example.${artifactId};

import com.flexive.example.shared.interfaces.EJBExample;
import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.beans.SystemBean;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.structure.FxType;

import java.util.Map;

/**
 * A JSF managed bean providing cached access to an EJB method.
 */
public class ManagedExampleBean {

    private Map<FxType, Integer> instanceCounts;

    public Map<FxType, Integer> getInstanceCounts() throws FxApplicationException {
        if (instanceCounts == null) {
            instanceCounts = EJBLookup.getEngine(EJBExample.class).getInstanceCounts();
        }
        return instanceCounts;
    }

    public String getBuildInfo() {
        // managed bean lookup, asserts that the JSF core components are available in the build path
        return FxJsfUtils.getManagedBean(SystemBean.class).getBuildInfo();
    }
}
