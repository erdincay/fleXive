package com.flexive.tests.embedded.jsf.component
import com.flexive.shared.*
import com.flexive.shared.scripting.groovy.*
import org.testng.annotations.*
import org.testng.Assert

/**
 * Some basic tests for the FxContentView tag. Creates a sample content instance and then
 * uses the lookup methods supplied by the FxContentView's mapped variable to read it.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */

class FxContentViewTest {
    @BeforeClass
    def beforeClass() {
        def builder = new GroovyContentBuilder()
    }
}