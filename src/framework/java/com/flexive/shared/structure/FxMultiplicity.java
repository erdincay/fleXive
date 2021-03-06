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
package com.flexive.shared.structure;

import com.flexive.shared.exceptions.FxInvalidParameterException;

import java.io.Serializable;
import java.util.Random;

/**
 * Multiplicity of groups and properties
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxMultiplicity implements Serializable {
    private static final long serialVersionUID = -7989666071224292389L;

    /**
     * Unlimited occurances
     */
    public static final int N = Integer.MAX_VALUE;

    /**
     * No more than 20 elements to be created in random creations
     */
    public static final int RANDOM_MAX = 20;

    public final static FxMultiplicity MULT_0_1 = new FxMultiplicity(0, 1);
    public final static FxMultiplicity MULT_1_1 = new FxMultiplicity(1, 1);
    public final static FxMultiplicity MULT_0_N = new FxMultiplicity(0, N);
    public final static FxMultiplicity MULT_1_N = new FxMultiplicity(1, N);

    /**
     * minimum multiplicity
     */
    private final int min;

    /**
     * maximum multiplicity, Integer.MAX_VALUE if unlimited
     */
    private final int max;

    /**
     * Symbol used to display unlimited
     */
    private static final String SYMBOL_UNLIMITED = "N";


    /**
     * Constructor
     *
     * @param min minimum multiplicity
     * @param max maximum multiplicity, Integer.MAX_VALUE if unlimited
     */
    public FxMultiplicity(int min, int max) {
        if (min < 0)
            throw new FxInvalidParameterException("min", "ex.structure.multiplicity.minimum.invalid", min, max).asRuntimeException();
        if (max <1)
            throw new FxInvalidParameterException("max", "ex.structure.multiplicity.maximum.invalid", max, min).asRuntimeException();
        if (min >max)
             throw new FxInvalidParameterException("min", "ex.structure.multiplicity.minimum.invalid", min, max).asRuntimeException();
        
        this.min = min;
        this.max = max;
    }

    /**
     * Copy Constructor
     *
     * @param m     an FxMultiplicity object
     */
    public FxMultiplicity(FxMultiplicity m) {
        this.min = m.min;
        this.max = m.max;
    }

    /**
     * Is an occurance optional?
     *
     * @return if an occurance is optional
     */
    public boolean isOptional() {
        return min == 0;
    }

    /**
     * Is at least one occurance required?
     *
     * @return if at least one occurance is required
     */
    public boolean isRequired() {
        return min > 0;
    }

    /**
     * May occur unlimited?
     *
     * @return unlimited?
     */
    public boolean isUnlimited() {
        return max == Integer.MAX_VALUE;
    }

    /**
     * More than one possible?
     *
     * @return if more than one are possible
     */
    public boolean isMultiple() {
        return max > 1;
    }

    /**
     * Get the minimum multiplicity
     *
     * @return minimum multiplicity
     */
    public int getMin() {
        return min;
    }

    /**
     * Get the maximum multiplicity
     *
     * @return maximum multiplicity
     */
    public int getMax() {
        return max;
    }

    /**
     * String representation, uses SYMBOL_UNLIMITED for unlimited max occurances
     *
     * @return String representation, uses SYMBOL_UNLIMITED for unlimited max occurances
     */
    @Override
    public String toString() {
        return min + ".." + (isUnlimited() ? SYMBOL_UNLIMITED : max);
    }

    /**
     * Get an FxMultiplicity from a String representation
     *
     * @param s string representation
     * @return FxMultiplicity
     */
    public static FxMultiplicity fromString(String s) {
        String[] range = s.split("\\.\\.");
        return FxMultiplicity.of(getStringToInt(range[0], false), getStringToInt(range[1], true));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FxMultiplicity) {
            FxMultiplicity o = (FxMultiplicity) obj;
            return o.getMin() == this.getMin() && o.getMax() == this.getMax();
        } else if (obj instanceof String) {
            String o = (String) obj;
            if (o.startsWith("[")) {
                return o.equals("[" + this.toString() + "]");
            } else
                return o.equals(this.toString());
        } else
            return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.getMin() + this.getMax();
    }

    /**
     * Get the number of elements to create for this multiplicity if creating random test data
     *
     * @param rnd             Random
     * @param maxMultiplicity the maximum (random) multiplicity
     * @return number of elements to create for this multiplicity if creating random test data
     */
    public int getRandomRange(Random rnd, int maxMultiplicity) {
        int count;
        if (isUnlimited())
            count = maxMultiplicity;
        else
            count = getMax() - getMin() + 1;
        if (count <= 0)
            count = 1;
        count = rnd.nextInt(count);
        if (count <= 0)
            count = 1;
        if (count < getMin())
            count = getMin();
        if (count > getMax())
            count = getMax();
// uncomment the following lines to return at least 2 elements if possible       
//        if( count == 1 && getMax() > 1 )
//            count = 2;
        return count;
    }

    /**
     * Check if the given index is valid for this multiplicity
     *
     * @param index the index to check
     * @return valid
     */
    public boolean isValid(int index) {
        return index >= getMin() && index <= getMax();
    }

    /**
     * Check if the given index is valid for this multiplicity (only the upperbounds)
     *
     * @param index the index to check
     * @return valid
     * @since 3.1.4
     */
    public boolean isValidMax(int index) {
        return index >= 1 && index <= getMax();
    }

     /**
     * Converts a String that represents a Number or "N" to int
     *
     * @param m representing a Number or "N"
     * @return the number as int or Integer.MAX_VALUE
     */
    public static int getStringToInt(String m) {
         return getStringToInt(m, true);
     }

     /**
     * Converts a String that represents a Number or "N" to int
     *
     * @param m representing a Number or "N"
     * @param canBeN can ghe given symbol be "N"
     * @return the number as int or Integer.MAX_VALUE
     * @since 3.1.4
     */
    public static int getStringToInt(String m, boolean canBeN) {
        int mul;
        if (SYMBOL_UNLIMITED.equals(m.toUpperCase())) {
            if (canBeN)
                mul= N;
            else {
                throw new FxInvalidParameterException("minMultiplicity", "ex.structure.multiplicity.minimum.notN").asRuntimeException();
            }
        }
        else {
            mul = Integer.parseInt(m);
        }
        return mul;
    }

     /**
     * Converts a int into a String holding its number or SYMBOL_UNLIMITED
     *
     * @param m representing a Number
     * @return the number as String
     */
    public static String getIntToString(int m) {
        String mul;
        if (m == FxMultiplicity.N)
            mul = FxMultiplicity.SYMBOL_UNLIMITED;
        else
            mul =String.valueOf(m);
        return mul;
    }

    /**
     * Return a multiplicity instance for the given min/max parameters.
     *
     * @param min    minimum multiplicity
     * @param max    maximum multiplicity, Integer.MAX_VALUE if unlimited
     * @return  the multiplicity instance
     * @since 3.2.1
     */
    public static FxMultiplicity of(int min, int max) {
        switch (min) {
            case 0:
                switch (max) {
                    case 1:
                        return MULT_0_1;
                    case N:
                        return MULT_0_N;
                    default:
                        return new FxMultiplicity(min, max);
                }
            case 1:
                switch (max) {
                    case 1:
                        return MULT_1_1;
                    case N:
                        return MULT_1_N;
                    default:
                        return new FxMultiplicity(min, max);
                }
        }
        return new FxMultiplicity(min, max);
    }
}
