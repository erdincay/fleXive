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
package com.flexive.sqlParser;

/**
 * Condition
 * 
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class Condition implements BraceElement {

    private COMPERATOR comperator;
    private Value sLeft;
    private Value sRight;
    private int id;

    public enum COMPERATOR {
        LIKE,NOT_LIKE,EQUAL,NOT_EQUAL,LESS,GREATER,
        GREATER_OR_EQUAL,LESS_OR_EQUAL,
        NOT_GREATER,NOT_LESS,NEAR,IS,IS_NOT,IN,NOT_IN,IS_CHILD_OF,IS_DIRECT_CHILD_OF
    }

    /**
     * Constructor.
     * <p />
     * At least one value has to be an constant or an FxSqlParserException is thrown.
     *  
     * @param stmt the statement
     * @param vleft the left value
     * @param comperator the comperator
     * @param vright the right value
     * @throws SqlParserException if a error occured
     */
    protected Condition(FxStatement stmt,Value vleft, COMPERATOR comperator,Value vright) throws SqlParserException {
        this.comperator = comperator;
        this.sLeft = vleft;
        this.sRight = vright;
        this.id=stmt.getNewBraceElementId();
        if (vleft instanceof Property && vright instanceof Property) {
            String sParam = vleft+String.valueOf(comperator)+vright;
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
                String.valueOf(comperator)+
                (sRight==null?"":" "+sRight.getValue());
    }

    public Value getLValueInfo() {
        return this.sLeft;
    }

    public Value getRValueInfo() {
        return this.sRight;
    }

    public COMPERATOR getComperator() {
        return this.comperator;
    }

    public String getSqlComperator() {
        switch(this.comperator) {
            case LIKE:
                return " LIKE ";
            case NOT_LIKE:
                return " NOT LIKE ";
            case EQUAL:
                return " = ";
            case NOT_EQUAL:
                return " != ";
            case GREATER:
                return " > ";
            case LESS:
                return " < ";
            case GREATER_OR_EQUAL:
                return " >= ";
            case LESS_OR_EQUAL:
                return " <= ";
            case NOT_GREATER:
                return " !> ";
            case NOT_LESS:
                return " !< ";
            case IS:
                return " is ";
            case IS_NOT:
                return " is not ";
            case NEAR:
                return null;
            case IN:
                return " IN ";
            case NOT_IN:
                return " NOT IN ";
            default:
                return null;
        }
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
        if (comperator==COMPERATOR.EQUAL) {
            return  sLeft.equals(sRight);
        }
        throw new SqlParserException("ex.sqlSearch.connotUseComperator",comperator,(sLeft+" "+comperator+" "+sRight));
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
        if (comperator==COMPERATOR.EQUAL) {
            return  !sLeft.equals(sRight);
        }
        throw new SqlParserException("ex.sqlSearch.connotUseComperator",comperator,(sLeft+" "+comperator+" "+sRight));
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
