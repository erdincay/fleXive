#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.structure.FxType;
import java.util.Map;
import javax.ejb.EJB;

/**
 * JSF bean accessing an EJB.
 */
public class ExampleBean {
    @EJB EJBExampleBean exampleEJB;
    private Map<FxType, Integer> instanceCounts;

    public Map<FxType, Integer> getInstanceCounts() throws FxApplicationException {
        if (instanceCounts == null) {
            instanceCounts = exampleEJB.getInstanceCounts();
        }
        return instanceCounts;
    }
}
