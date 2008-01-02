package com.flexive.tests.shared;

import org.testng.annotations.Test;
import com.flexive.shared.value.DateRange;

import java.util.Date;

/**
 * Tests for the {@link com.flexive.shared.value.DateRange} data type.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class DateRangeTest {
    /**
     * Converts a daterange to a string, then converts it back to a daterange.
     */
    @Test(groups = "shared")
    public void dateRangeStringRep() {
        final DateRange range = new DateRange(new Date(0), new Date());
        assert new DateRange(range.toString()).equals(range)
                : "Range constructed from '" + range + "' not equal to original object.";
    }
}
