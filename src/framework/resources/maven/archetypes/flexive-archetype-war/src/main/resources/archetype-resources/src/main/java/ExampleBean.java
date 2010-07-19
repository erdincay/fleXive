#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.structure.FxType;
import java.util.Map;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import javax.inject.Inject;
import org.apache.commons.lang.StringUtils;

/**
 * JSF bean accessing an EJB.
 */
@Named
@RequestScoped
public class ExampleBean {

    @Inject EJBExampleBean exampleEJB;
    private Map<FxType, Integer> instanceCounts;
    private String value;

    public Map<FxType, Integer> getInstanceCounts() throws FxApplicationException {
        if (instanceCounts == null) {
            instanceCounts = exampleEJB.getInstanceCounts();
        }
        return instanceCounts;
    }

    public void reverse() {
        if (value != null) {
            value = StringUtils.reverse(value);
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
