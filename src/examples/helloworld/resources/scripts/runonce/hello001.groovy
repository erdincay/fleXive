/**
 * Initialization script for the hello world application.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */

import com.flexive.shared.scripting.groovy.*
import com.flexive.shared.value.*
import com.flexive.shared.structure.*

new GroovyTypeBuilder().blogEntry(description: new FxString(true, "Blog Entry"), usePermissions: false) {

    caption(assignment: "ROOT/CAPTION")
    entryTitle(multiplicity: FxMultiplicity.MULT_1_1, description: new FxString(true, "Title"))
    entryText(multiplicity: FxMultiplicity.MULT_1_1, description: new FxString(true, "Text"),
         multiline: true)

}
