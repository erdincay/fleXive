/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
 *  license from the author are found in LICENSE.txt distributed with
 *  these libraries.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  For further information about UCS - unique computing solutions gmbh,
 *  please see the company website: http://www.ucs.at
 *
 *  For further information about [fleXive](R), please see the
 *  project website: http://www.flexive.org
 *
 *
 *  This copyright notice MUST APPEAR in all copies of the file!
 ***************************************************************/
package com.flexive.tests.embedded;

import com.flexive.shared.CustomSequencer;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.interfaces.SequencerEngine;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import org.apache.commons.lang.RandomStringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Sequencer tests
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "structure"})
public class SequencerTest {

    private SequencerEngine id;

    @BeforeClass
    public void beforeClass() throws Exception {
        id = EJBLookup.getSequencerEngine();
        login(TestUsers.SUPERVISOR);
    }

    @AfterClass
    public void afterClass() throws FxLogoutFailedException {
        logout();
    }

    @Test
    public void customSequencer() throws Exception {
        List<CustomSequencer> startList = id.getCustomSequencers();
        String seq1 = "A" + RandomStringUtils.randomAlphanumeric(16).toUpperCase();
        String seq2 = "B" + RandomStringUtils.randomAlphanumeric(16).toUpperCase();
        String seq3 = "C" + RandomStringUtils.randomAlphanumeric(16).toUpperCase();
        id.createSequencer(seq1, true, 0);
        id.createSequencer(seq2, true, SequencerEngine.MAX_ID);
        id.createSequencer(seq3, false, SequencerEngine.MAX_ID);
        assert id.sequencerExists(seq1) : "Expected sequencer " + seq1 + " to exist!";
        assert id.sequencerExists(seq2) : "Expected sequencer " + seq2 + " to exist!";
        assert id.sequencerExists(seq3) : "Expected sequencer " + seq3 + " to exist!";
        long i1 = id.getId(seq1);
        long i2 = id.getId(seq1);
        assert i2 > i1 : "Expected a higher id after 2nd getId()!";
        i1 = id.getId(seq2); //call should cause the sequencer to roll over
        assert i1 == 0 : "Expected: " + 0 + ", got: " + i1;

        try {
            id.getId(seq3);
            assert false : "Expected an exception since seq3 should be exhausted!";
        } catch (FxApplicationException e) {
            //expected
        }

        List<CustomSequencer> g2 = id.getCustomSequencers();
        g2.removeAll(startList);
        assert g2.size() == 3 : "Expected a size of 3, but got " + g2.size();
        assert g2.get(0).getName().equals(seq1) : "Expected " + seq1 + " got " + g2.get(0).getName();
        assert g2.get(1).getName().equals(seq2) : "Expected " + seq2 + " got " + g2.get(1).getName();
        assert g2.get(2).getName().equals(seq3) : "Expected " + seq3 + " got " + g2.get(2).getName();
        assert g2.get(0).isAllowRollover();
        assert g2.get(1).isAllowRollover();
        assert !g2.get(2).isAllowRollover();
        id.removeSequencer(seq1);
        id.removeSequencer(seq2);
        id.removeSequencer(seq3);
        assert id.getCustomSequencers().size() == startList.size();
    }
}
