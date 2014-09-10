package com.flexive.tests.embedded.persistence;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.XPathElement;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxData;
import com.flexive.shared.content.FxGroupData;
import com.flexive.shared.exceptions.FxAccountInUseException;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLoginFailedException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.tests.embedded.FxTestUtils;
import com.flexive.tests.embedded.TestUsers;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;

/**
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class FxDataTest {

    @Test(groups = {"ejb", "fxdata"})
    public void moveGroupChildrenTest() throws FxApplicationException, FxLoginFailedException, FxAccountInUseException, FxLogoutFailedException {
        FxTestUtils.login(TestUsers.REGULAR);
        try {
            final FxContent co = EJBLookup.getContentEngine().initialize("ARTICLE");
            final FxGroupData group = co.getRootGroup();
            final FxData a = group.getChildren().get(0);
            final FxData b = group.getChildren().get(1);
            final FxData c = group.getChildren().get(2);
            final FxData d = group.getChildren().get(3);
            assertPositions(group, a, 0, b, 1, c, 2, d, 3);

            group.setChildPosition(b, 0);
            assertPositions(group, a, 1, b, 0, c, 2, d, 3);

            group.setChildPosition(a, 3);
            assertPositions(group, a, 3, b, 0, c, 1, d, 2);

            final int count = group.getChildren().size();
            group.setChildPosition(a, count - 1);
            assertPositions(group, a, count - 1, b, 0, c, 1, d, 2);

            group.setChildPosition(b, count / 2);
            assertPositions(group, a, count - 1, b, count / 2, c, 0, d, 1);
        } finally {
            FxTestUtils.logout();
        }
    }

    @Test(groups = {"ejb", "fxdata"})
    public void replaceChildTest() throws FxLoginFailedException, FxAccountInUseException, FxLogoutFailedException, FxApplicationException {
        FxTestUtils.login(TestUsers.REGULAR);
        try {
            final FxContent co = EJBLookup.getContentEngine().initialize("ARTICLE");

            final FxGroupData copy = new FxGroupData(co.getRootGroup(), null);
            FxData titleCopy = null;
            for (FxData data : copy.getChildren()) {
                if ("TITLE".equals(data.getAlias())) {
                    titleCopy = data;
                    break;
                }
            }
            assertNotNull(titleCopy);
            assertSame(titleCopy.getParent(), copy);

            co.getRootGroup().replaceChild(XPathElement.toElement("TITLE[1]", "TITLE"), titleCopy);

            assertSame(co.getData("/TITLE").get(0).getParent(), co.getRootGroup());
        } finally {
            FxTestUtils.logout();
        }
    }

    private void assertPositions(FxGroupData group, FxData value1, int pos1, FxData value2, int pos2, FxData value3, int pos3, FxData value4, int pos4) {
        checkPosition(group, value1, pos1, 1);
        checkPosition(group, value2, pos2, 2);
        checkPosition(group, value3, pos3, 3);
        checkPosition(group, value4, pos4, 4);
    }

    private void checkPosition(FxGroupData group, FxData value, int pos, int valNum) {
        assertSame(group.getChildren().get(pos), value, "Expected position " + pos + " for value #" + valNum + ", got: " + group.getChildren().indexOf(value));
    }

}
