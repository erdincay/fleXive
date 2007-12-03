/**
 * Initialization script for the tutorial01 application.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */

import com.flexive.shared.scripting.groovy.*
import com.flexive.shared.value.*
import com.flexive.shared.structure.*

new GroovyTypeBuilder().document01(
        description: new FxString(true, "Tutorial document 01"),
        usePermissions: false)
{
    // assign the root caption under /caption
    caption(assignment: "ROOT/CAPTION")

    // store the mandatory binary under /file
    file(dataType: FxDataType.Binary,
         multiplicity: FxMultiplicity.MULT_1_1,
         description: new FxString(true, "File"))
}
