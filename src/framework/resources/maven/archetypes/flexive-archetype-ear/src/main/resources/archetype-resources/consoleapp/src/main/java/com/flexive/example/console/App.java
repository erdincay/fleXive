package com.flexive.example.console;

import com.flexive.example.shared.interfaces.EJBExample;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.structure.FxType;

import java.util.Map;

/**
 * Experimental [fleXive] console application.
 *
 * <p>
 * Execute with:<br/>
 * {@code  mvn install exec:java -Dexec.mainClass=com.flexive.example.console.App -Dopenejb.base=../openejb/}
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class App {
    public static void main(String[] args) throws FxApplicationException {
        FxContext.initializeSystem(1, "consoleapp");

        System.out.println("Hello [fleXive] World - calling example EJB, listing available content:");
        for (Map.Entry<FxType, Integer> entry
                : EJBLookup.getEngine(EJBExample.class).getInstanceCounts().entrySet()) {
            System.out.println(entry.getKey().getLabel() + ": " + entry.getValue());
        }

        System.exit(0);
    }
}
