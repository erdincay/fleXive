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

import java.util.ArrayList;

/**
 * Brace
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class Brace implements BraceElement{

    private ArrayList<BraceElement> conditions;
    private Brace parent;
    private String type;
    private FxStatement stmt;
    private int id;


    protected Brace(FxStatement stmt) {
        this.conditions = new ArrayList<BraceElement>(25);
        this.parent=null;
        this.stmt=stmt;
        this.id=stmt.getNewBraceElementId();
    }

    public int getId() {
        return this.id;
    }

    private void checkTbl(Value val) throws SqlParserException {
        if (!(val instanceof Property)) {
            return;
        }
        Property pr = (Property)val;
        if (stmt.getTableByAlias(pr.getTableAlias())==null) {
            throw new SqlParserException("ex.sqlSearch.unknownTable",pr.getTableAlias());
        }
    }

    /**
     * Adds a element (condition or sub-brace) to the this brace.
     *
     * @param ele the element to add
     * @throws SqlParserException if the condition is invalid
     */
    protected void addElement(BraceElement ele) throws SqlParserException {
        if (ele instanceof Condition) {
            Condition co = (Condition)ele;
            checkTbl(co.getLValueInfo());
            checkTbl(co.getRValueInfo());
        }
        if (ele instanceof Brace) {
            ((Brace)ele).parent=this;
        }
        conditions.add(ele);
    }

    protected void addElements(BraceElement eles[]) throws SqlParserException {
        for (BraceElement ele:eles) {
            addElement(ele);
        }
    }

    public int getLevel() {
        int level = 0;
        Brace p = this;
        while (p.parent!=null) {
            p = p.parent;
            level++;
        }
        return level;
    }

    public int getSize() {
        return this.conditions.size();
    }

    public Brace getParent() {
        return this.parent;
    }

    /**
     * Returns the statement this brace belongs to.
     *
     * @return the statement this brace belongs to
     */
    public FxStatement getStatement() {
        return stmt;
    }


    protected BraceElement removeLastElement() {
        BraceElement be = this.conditions.remove(this.conditions.size()-1);
        if (be instanceof Brace){
            ((Brace)be).parent=null;
        }
        return be;
    }

    protected boolean removeElement(BraceElement ele) {
        boolean removed = this.conditions.remove(ele);
        if (removed && ele instanceof Brace) {
            ((Brace)ele).parent=null;
        }
        return removed;
    }

    protected BraceElement[] removeAllElements() {
        BraceElement result[] = conditions.toArray(new BraceElement[conditions.size()]);
        conditions.clear();
        return result;
    }


    /**
     * Returns the type of the table.
     *
     * @return the type of the table
     */
    public String getType() {
        return this.type;
    }

    public boolean isOr() {
        return (this.type!=null && this.type.equalsIgnoreCase("or"));
    }

    public boolean isAnd() {
        return (this.type!=null && this.type.equalsIgnoreCase("and"));
    }

    public boolean containsAnd() {
        return containsType("and");
    }

    public boolean containsOr() {
        return containsType("or");
    }

    private boolean containsType(String type) {
        if (type.equalsIgnoreCase(this.type)) {
            return true;
        }
        // check all nested braces
        for (BraceElement element : getElements()) {
            if (element instanceof Brace) {
                if (((Brace) element).containsType(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void setType(String value) {
        this.type=value;
    }

    /**
     * Returns the amount of conditons and sub-braces in this brace.
     *
     * @return the amount of conditons and sub-braces in this brace
     */
    public int size() {
        return conditions.size();
    }
    
    public BraceElement[] getElements() {
        return conditions.toArray(new BraceElement[conditions.size()]);
    }

    public BraceElement getElementAt(int pos) {
        return conditions.get(pos);
    }


    public String toString() {
        return "Brace[type="+type+";"+super.toString()+"]";
    }

    protected boolean processAndOr(String andOr,boolean insideforcedBrace) throws SqlParserException {
            if (type!=null && !type.equals(andOr)) {
				if (insideforcedBrace) {
					insideforcedBrace=false;
					stmt.endSubBrace();
				} else {
                    if (andOr.equalsIgnoreCase("and")) {
                        insideforcedBrace=true;
                        BraceElement ele = removeLastElement();
                        System.out.println("#remAdd:"+ele);
                        Brace newBrace=stmt.startSubBrace();
                        newBrace.addElement(ele);
                        newBrace.setType(andOr);
                    } else {
                        BraceElement ele[] = removeAllElements();
                        Brace newBrace = new Brace(stmt);
                        newBrace.addElements(ele);
                        newBrace.type=this.type;
                        this.addElement(newBrace);
                        this.type=andOr;
                    }
                }
			}
            setType(andOr);
        return insideforcedBrace;
    }
}
