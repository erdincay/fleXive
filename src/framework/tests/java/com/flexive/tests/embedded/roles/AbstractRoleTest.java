package com.flexive.tests.embedded.roles;

import com.flexive.tests.embedded.TestUser;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.shared.exceptions.FxLoginFailedException;
import com.flexive.shared.exceptions.FxAccountInUseException;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.FxContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;

/**
 * Role test base class.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class AbstractRoleTest {
    protected TestUser user;

    public void init(TestUser user) {
        this.user = user;
    }

    @BeforeMethod(groups = {"security", "roles"})
    public void beforeTestMethod() throws FxLoginFailedException, FxAccountInUseException {
        login(user);
    }

    @AfterMethod(groups = {"security", "roles"})
    public void afterTestMethod() throws Exception {
        logout();
    }

    protected void assertSuccess(Role role, long mandatorId) {
        final String msg = accessAllowed(role, mandatorId);
        if (msg == null) {
            assert false : "User should not be allowed to succeed on call without role " + role;
        }
    }

    protected void assertNoAccess(FxNoAccessException nae, Role role, long mandatorId) {
        final String msg = accessAllowed(role, mandatorId);
        if (msg != null) {
            assert false : msg + " (" + nae.getMessage() + ")";
        }
    }

    private String accessAllowed(Role role, long mandatorId) {
        final UserTicket ticket = FxContext.get().getTicket();
        if (ticket.isGlobalSupervisor()) {
            return "Global supervisor should be allowed to call everything";
        }
        if (ticket.isInRole(role) && (mandatorId == -1 || mandatorId == ticket.getMandatorId())) {
            return "User has role " + role.name() + " and mandator[id=" + mandatorId + "] matches,"
                    + " but still not allowed to call method.";
        }
        if (ticket.isMandatorSupervisor() && mandatorId == ticket.getMandatorId()) {
            return "The mandator supervisor should be able to perform role " + role.name()
                    + " for all objects of his own mandator (id=" + ticket.getMandatorId() + ").";
        }
        return null;
    }
}
