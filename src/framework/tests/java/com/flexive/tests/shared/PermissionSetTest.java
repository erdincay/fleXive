package com.flexive.tests.shared;

import org.testng.annotations.Test;
import com.flexive.shared.security.PermissionSet;

/**
 * Tests for the {@link com.flexive.shared.security.PermissionSet} class.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class PermissionSetTest {

    @Test(groups = "shared")
    public void basicPermissionSet() {
        final PermissionSet set = new PermissionSet(true, false, false, true, true);
        assert set.isMayEdit();
        assert !set.isMayRelate();
        assert !set.isMayDelete();
        assert set.isMayExport();
        assert set.isMayCreate();
    }

    @Test(groups = "shared")
    public void genericPermissionSet() {
        for (int i = 0; i < 1 << 5; i++) {
            final boolean edit = (i & 1) > 0;
            final boolean relate = (i & 2) > 0;
            final boolean delete = (i & 4) > 0;
            final boolean export = (i & 8) > 0;
            final boolean create = (i & 16) > 0;
            final PermissionSet set = new PermissionSet(edit, relate, delete, export, create);
            assert edit == set.isMayEdit();
            assert relate == set.isMayRelate();
            assert delete == set.isMayDelete();
            assert export == set.isMayExport();
            assert create == set.isMayCreate();
        }
    }

}
