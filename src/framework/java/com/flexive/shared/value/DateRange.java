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
package com.flexive.shared.value;

import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.exceptions.FxInvalidParameterException;

import java.io.Serializable;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;

/**
 * A class to describe an immutable date range
 * <p/>
 * parts of the code are taken from JFreeChart (http://www.jfree.org/jfreechart/index.html) which is released
 * under an LGPL license. Code is taken from org.jfree.data.Range (http://www.koders.com/java/fidBBD43A5DBC05830ABCF1267D7B17BF90BD2B6521.aspx)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class DateRange implements Serializable {
    private static final long serialVersionUID = -2528729942501241287L;

    private Date lower;
    private Date upper;


    public DateRange(Date lower, Date upper) {
        if (lower == null || upper == null) {
            throw new FxInvalidParameterException("lower, upper", "ex.daterange.empty").asRuntimeException();
        }
        if (upper.getTime() < lower.getTime()) {
            throw new FxInvalidParameterException("lower", "ex.daterange.order", lower, upper).asRuntimeException();
        }
        this.lower = (Date) lower.clone();
        this.upper = (Date) upper.clone();
    }

    public DateRange(String dateRange) {
        final SimpleDateFormat sdf = FxFormatUtils.getDateTimeFormat();
        final ParsePosition pos = new ParsePosition(0);
        this.lower = sdf.parse(dateRange, pos);
        pos.setIndex(pos.getIndex() + 2);   // skip the " - "
        this.upper = sdf.parse(dateRange, pos);
        if (this.lower == null || this.upper == null) {
            throw new FxInvalidParameterException("dateRange", "ex.daterange.format", dateRange).asRuntimeException();
        }
    }

    public Date getLower() {
        return (Date) lower.clone();
    }

    public Date getUpper() {
        return (Date) upper.clone();
    }

    public long getDuration() {
        return upper.getTime() - lower.getTime();
    }

    /**
     * Does <code>range</code> intersect with this DateRange?
     *
     * @param range the DateRange to test for intersection
     * @return if the ranges intersect
     */
    public boolean intersects(DateRange range) {
        if (range.lower.getTime() <= this.lower.getTime()) {
            return (range.upper.getTime() > this.lower.getTime());
        } else {
            return (range.lower.getTime() < this.upper.getTime() && range.upper.getTime() >= range.lower.getTime());
        }
    }

    /**
     * Returns the central value for the range.
     *
     * @return The central value.
     */
    public Date getCentralValue() {
        return new Date((long) ((double) this.lower.getTime() / 2.0 + (double) this.upper.getTime() / 2.0));
    }

    /**
     * Returns <code>true</code> if the range contains the specified value and <code>false</code>
     * otherwise.
     *
     * @param value the value to check
     * @return <code>true</code> if the range contains the specified value.
     */
    public boolean contains(final Date value) {
        return (value.getTime() >= this.lower.getTime() && value.getTime() <= this.upper.getTime());
    }

    /**
     * Returns the value within the range that is closest to the specified value.
     *
     * @param value the value.
     * @return The constrained value.
     */
    public Date constrain(final Date value) {
        Date result = value;
        if (!contains(value)) {
            if (value.getTime() > this.upper.getTime()) {
                result = new Date(this.upper.getTime());
            } else if (value.getTime() < this.lower.getTime()) {
                result = new Date(this.lower.getTime());
            }
        }
        return result;
    }

    /**
     * Creates a new range by combining two existing ranges.
     * <p/>
     * Note that:
     * <ul>
     * <li>either range can be <code>null</code>, in which case the other range is returned;</li>
     * <li>if both ranges are <code>null</code> the return value is <code>null</code>.</li>
     * </ul>
     *
     * @param range1 the first range (<code>null</code> permitted).
     * @param range2 the second range (<code>null</code> permitted).
     * @return A new range (possibly <code>null</code>).
     */
    public static DateRange combine(final DateRange range1, final DateRange range2) {
        if (range1 == null) {
            return range2;
        } else {
            if (range2 == null) {
                return range1;
            } else {
                final Date l = new Date(Math.min(range1.lower.getTime(), range2.lower.getTime()));
                final Date u = new Date(Math.max(range1.upper.getTime(), range2.upper.getTime()));
                return new DateRange(l, u);
            }
        }
    }

    /**
     * Creates a new range by adding margins to an existing range.
     *
     * @param range       the range (<code>null</code> not permitted).
     * @param lowerMargin the lower margin (expressed as a percentage of the range length).
     * @param upperMargin the upper margin (expressed as a percentage of the range length).
     * @return The expanded range.
     */
    public static DateRange expand(DateRange range, double lowerMargin, double upperMargin) {
        if (range == null) {
            throw new IllegalArgumentException("Null 'range' argument.");
        }
        double length = range.getDuration();
        double lower = length * lowerMargin;
        double upper = length * upperMargin;
        return new DateRange(new Date(range.lower.getTime() - (long) lower), new Date(range.upper.getTime() + (long) upper));
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     *         argument; <code>false</code> otherwise.
     * @see #hashCode()
     * @see java.util.Hashtable
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DateRange)) return false;
        DateRange o = (DateRange) obj;
        return o.lower.getTime() == this.lower.getTime() && o.upper.getTime() == this.upper.getTime();
    }


    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hashtables such as those provided by
     * <code>java.util.Hashtable</code>.
     *
     * @return a hash code value for this object.
     * @see Object#equals(Object)
     * @see java.util.Hashtable
     */
    @Override
    public int hashCode() {
        return lower.hashCode() + 31 * upper.hashCode();
    }

    @Override
    public String toString() {
        final SimpleDateFormat sdf = FxFormatUtils.getDateTimeFormat();
        return sdf.format(lower) + " - " + sdf.format(upper);
    }

}
