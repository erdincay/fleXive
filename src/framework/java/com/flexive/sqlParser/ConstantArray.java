/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
package com.flexive.sqlParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Array of constants
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class ConstantArray extends Constant {

    private Constant[] array;

    /**
     * Converts a constant array to a string representation.
     *
     * @param arr the array
     * @return the string value
     */
    private static String arrayToString(Constant arr[]) {
        if (arr==null || arr.length==0) {
            return "(null)";
        }
        StringBuffer sb = new StringBuffer(1024);
        sb.append("(");
        int pos = 0;
        for (Constant c:arr) {
            if ((pos++)>0) {
                sb.append(",");
            }
            sb.append(c.getValue());
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Constructor
     *
     * @param arr the array
     */
    public ConstantArray(Constant[] arr) {
        super(arrayToString(arr));
        this.array = arr;
    }

    /**
     * Adds constant value to the end of the list.
     *
     * @param con the constant to add
     * @return returns the ConstantArray itself
     */
    public ConstantArray add(Constant con) {
        if (array==null) array=new Constant[0];
        ArrayList<Constant> result = new ArrayList<Constant>(array.length+1);
        result.add(con);
        for (Constant tmp:array) {
            result.add(tmp);
        }
        array = result.toArray(new Constant[result.size()]);
        return this;
    }

    /**
     * Returns a string representation of the array list that can be used in sql.
     * <p />
     * Example result: '(1,2,3,4,5)'
     * @return a string representation of the array list that can be used in sql.
     */
    @Override
    public String getValue() {
        return arrayToString(this.array);
    }

    /**
     * Returns a string representation of the array list.
     *
     * @return a string representation of the array list
     */
    @Override
    public String toString() {
        return arrayToString(this.array);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings({"unchecked"})
    public Iterator<Constant> iterator() {
        return Arrays.asList(array).iterator();
    }

    /**
     * Returns the number of elements in the array
     * @return the number of elements in the array
     */
    public int size() {
        return array==null?0:array.length;
    }

}
