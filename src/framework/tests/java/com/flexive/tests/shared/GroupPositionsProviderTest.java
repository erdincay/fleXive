package com.flexive.tests.shared;

import com.flexive.core.storage.GroupPositionsProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
@Test(groups = "shared")
public class GroupPositionsProviderTest {

    @Test
    public void builderTest() {
        final GroupPositionsProvider.Builder builder = GroupPositionsProvider.builder();
        assertEquals(builder.build(), "");
        try {
            builder.addPos(new int[] { 1 }, 1);
            fail("Adding positions without an assignment should not work");
        } catch (IllegalStateException e) {
            // pass
        }
        builder.startAssignment(21);
        builder.addPos(new int[] { 1, 1 }, 13);

        final String part1 = "21:/=13";
        assertEquals(builder.build(), part1);

        builder.addPos(new int[] { 5, 3, 1, 3 }, 1);

        final String part2 = part1 + ",5/3//3=1";
        assertEquals(builder.build(), part2);

        builder.startAssignment(99);
        builder.addPos(new int[] { 1 }, 1);

        final String part3 = part2 + ";99:=1";
        assertEquals(builder.build(), part3);
    }
}
