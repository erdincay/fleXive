/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License version 2.1 or higher as published by the Free Software Foundation.
 *
 *  The GNU Lesser General Public License can be found at
 *  http://www.gnu.org/licenses/lgpl.html.
 *  A copy is found in the textfile LGPL.txt and important notices to the
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CustomSequencer that = (CustomSequencer) o;

        if (allowRollover != that.allowRollover) return false;
        if (currentNumber != that.currentNumber) return false;
        if (!name.equals(that.name)) return false;

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (allowRollover ? 1 : 0);
        result = 31 * result + (int) (currentNumber ^ (currentNumber >>> 32));
        return result;
    }
}
