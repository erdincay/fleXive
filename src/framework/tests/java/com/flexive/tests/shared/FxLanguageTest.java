package com.flexive.tests.shared;

import com.flexive.shared.FxLanguage;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
@Test(groups = "shared")
public class FxLanguageTest {

    @Test
    public void languageEquals() {
        assertEquals(new FxLanguage("en"), new FxLanguage("en"));
        assertEquals(new FxLanguage(1, "en", null, true), new FxLanguage(1, "en", null, true));
        assertFalse((new FxLanguage(-1, "en", null, true)).equals(new FxLanguage(1, "en", null, true)));
    }
}
