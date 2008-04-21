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
package com.flexive.sqlParser;

/**
 * Condition
 * 
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class Condition implements BraceElement {

    private Comparator comparator;
    private Value sLeft;
    private Value sRight;
    private int id;

    public enum Comparator {
        LIKE("like"),
        NOT_LIKE("not like"),
        EQUAL("="),
        NOT_EQUAL("!="),
        LESS("<"),
        GREATER(">"),
        GREATER_OR_EQUAL(">="),
        LESS_OR_EQUAL("<="),
        NOT_GREATER("!>"),
        NOT_LESS("!<"),
        IS("is"),
        IS_NOT("is not"),
        IN("in"),
        NOT_IN("not in"),
        NEAR(null) /* no SQL equivalent */,
        IS_CHILD_OF(null),
        IS_DIRECT_CHILD_OF(null);

        private String sql;

        Comparator(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    /**
     * Constructor.
     * <p />
     * At least one value has to be an constant or an FxSqlParserException is thrown.
     *  
     * @param stmt the statement
     * @param vleft the left value
     * @param comparator the comparator
     * @param vright the right value
     * @throws SqlParserException if a error occured
     */
    protected Condition(FxStatement stmt,Value vleft, Comparator comparator,Value vright) throws SqlParserException {
        this.comparator = comparator;
        this.sLeft = vleft;
        this.sRight = vright;
        this.id=stmt.getNewBraceElementId();
        if (vleft instanceof Property && vright instanceof Property) {
            String sParam = vleft+String.valueOf(comparator)+vright;
            throw new SqlParserException("ex.sqlSearch.invalidConditionNoConst",sParam);
        }
        if (vleft instanceof Property) {
            if (((Property)vleft).getPropertyName().equals("*") && vleft.hasFunction()) {
                throw new SqlParserException("ex.sqlSearch.parser.fulltextSearchMayNotHaveFunct",
                        vleft.getFunctionsStart()+".."+vleft.getFunctionsEnd());
            }
        }
        if (vright instanceof Property) {
            if (((Property)vright).getPropertyName().equals("*") && vright.hasFunction()) {
                throw new SqlParserException("ex.sqlSearch.parser.fulltextSearchMayNotHaveFunct",
                        vright.getFunctionsStart()+".."+vright.getFunctionsEnd());
            }
        }
    }

    /**
     * Returns the id
     * @return the id
     */
    public int getId() {
        return this.id;
    }

    public BraceElement[] getElements() {
        return new BraceElement[0];
    }

    public String toString() {
        return (sLeft==null?"null":sLeft.getValue()+" ")+
                String.valueOf(comparator)+
                (sRight==null?"":" "+sRight.getValue());
    }

    public Value getLValueInfo() {
        return this.sLeft;
    }

    public Value getRValueInfo() {
        return this.sRight;
    }

    public Comparator getComperator() {
        return this.comparator;
    }

    public String getSqlComperator() {
        return " " + this.comparator.getSql() + " ";
    }


    /**
     * Returns true if the condition is always true.
     *
     * @return true if the condition is always true.
     * @throws SqlParserException if a error occured
     */
    public boolean isAlwaysTrue() throws SqlParserException {
        if (!(sLeft instanceof Constant  && sRight instanceof Constant)) {
            return false;
        }
        if (comparator == Comparator.EQUAL) {
            return  sLeft.equals(sRight);
        }
        throw new SqlParserException("ex.sqlSearch.connotUseComperator", comparator,(sLeft+" "+ comparator +" "+sRight));
    }

    /**
     * Returns true if the condition is always false.
     *
     * @return true if the condition is always false.
     * @throws SqlParserException if a error occured
     */
    public boolean isAlwaysFalse() throws SqlParserException {
        if (!(sLeft instanceof Constant && sRight instanceof Constant)) {
            return false;
        }
        if (comparator == Comparator.EQUAL) {
            return  !sLeft.equals(sRight);
        }
        throw new SqlParserException("ex.sqlSearch.connotUseComperator", comparator,(sLeft+" "+ comparator +" "+sRight));
    }

    public Constant getConstant() {
        if (this.sLeft instanceof Constant) {
            return (Constant)sLeft;
        } else if (this.sRight instanceof Constant) {
            return (Constant)sRight;
        } else {
            return null;
        }
    }

    public Property getProperty() {
        if (this.sLeft instanceof Property) {
            return (Property)sLeft;
        } else if (this.sRight instanceof Property) {
            return (Property)sRight;
        } else {
            return null;
        }
    }


}
