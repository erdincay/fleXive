package com.flexive.tests.embedded;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.security.ACL;
import com.flexive.shared.value.FxString;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Basic ACL engine tests.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class ACLEngineTest {
    @BeforeClass
    public void beforeClass() throws Exception {
        login(TestUsers.SUPERVISOR);
    }

    @AfterClass
    public void afterClass() throws FxLogoutFailedException {
        logout();
    }

    @Test(groups = {"ejb", "security"})
    public void createAclTest() throws FxApplicationException {
        final long aclId = EJBLookup.getACLEngine().create("create-acl-test", new FxString(""), TestUsers.getTestMandator(),
                "#000000", "", ACL.Category.INSTANCE);
        final ACL acl = CacheAdmin.getFilteredEnvironment().getACL(aclId);
        assertEquals(acl.getName(), "create-acl-test");
        EJBLookup.getACLEngine().remove(acl.getId());
    }

}
