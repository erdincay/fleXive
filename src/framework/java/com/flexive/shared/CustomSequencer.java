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
package com.flexive.shared;

import java.io.Serializable;

/**
 * A user defined sequencer
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class CustomSequencer implements Serializable {
    
    private static final long serialVersionUID = 4821229397499448319L;

    private String name;
    private boolean allowRollover;
    private long currentNumber;

    /**
     * Constructor
     *
     * @param name          unique name
     * @param allowRollover rollover when Long.MAX_VALUE is reached?
     * @param currentNumber the current number of the sequence (last delivered)
     */
    public CustomSequencer(String name, boolean allowRollover, long currentNumber) {
        this.name = name;
        this.allowRollover = allowRollover;
        this.currentNumber = currentNumber;
    }

    /**
     * Get the unique name of this sequencer
     *
     * @return unique name of this sequencer
     */
    public String getName() {
        return name;
    }

    /**
     * Does this sequencer support rollover?
     * If the current number gets close to Long.MAX_VALUE it will be reset to zero if rollover is allowed, else
     * an exception will be thrown
     *
     * @return rollover allowed
     */
    public boolean isAllowRollover() {
        return allowRollover;
    }

    /**
     * Get the current number of this sequencer, however no guarantee can be made that the next number requested
     * will be this current number + 1
     *
     * @return current number of this sequencer
     */
    public long getCurrentNumber() {
        return currentNumber;
    }
}
