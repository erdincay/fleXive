/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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

import java.util.ArrayList;

/**
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class Value {

    private Object value;
    private ArrayList<String> arFunctions = null;

    
    /**
     * Compares to value objects.
     *
     * @param obj the object
     * @return true if the objects are equal
     */
    public boolean equals(Object obj) {
        if (!this.getClass().equals(obj.getClass())) {
            return false;
        }
        Value comp = (Value)obj;
        return this.getComperatorString().equals(comp.getComperatorString());
    }

    /**
     * Helper fct for equals.
     *
     * @return -
     */
    private String getComperatorString() {
        return value +"."+getFunctionsStart();
    }

    /**
     * Constructor.
     *
     * @param sValue the value
     */
    protected Value(Object sValue) {
        this.value =sValue;
    }

    /**
     * Returns true if this is a NULL value.
     *
     * @return true if this is a NULL value
     */
    public boolean isNull() {
        return this.value==null;
    }

    /**
     * Adds a function to the value info.
     *
     * @param functionName the name of the function
     * @return the value info itself
     */
    public Value addFunction(String functionName) {
        // Remove ending "(" if needed
        if (functionName.endsWith("(")) {
            functionName = functionName.substring(0,functionName.length()-1);
        }

        // Add at beginning
        if (arFunctions==null) {
            arFunctions  = new ArrayList<String>(5);
        }
        arFunctions.add(0,functionName);
        return this;
    }


    /**
     * Returns the value.
     *
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Returns all functions that wrapp the value in the correct order.
     *
     * @return all functions that wrapp the value.
     */
    public String[] getFunctions() {
        if (arFunctions==null) {
            return new String[0];
        }
        return arFunctions.toArray(new String[arFunctions.size()]);
    }

    /**
     * Returns the functions start, eg "upper(max(".
     *
     * @return the functions start
     */
    public String getFunctionsStart() {
        if (arFunctions==null) return "";
        String result = "";
        for (String fct:arFunctions) {
            result+=fct+"(";
        }
        return result;
    }

    /**
     * Returns the functions end, eg "))".
     *
     * @return the functions start
     */
    public String getFunctionsEnd() {
        if (arFunctions==null) return "";
        String result = "";
        //noinspection ForLoopReplaceableByForEach
        for (int i=0;i<arFunctions.size();i++) {
            result+=")";
        }
        return result;
    }

    /**
     * Returns true if the vlaue is wrapped by at least one function.
     *
     * @return true if the vlaue is wrapped by at least one function.
     */
    public boolean hasFunction() {
        return (arFunctions!=null && arFunctions.size()>0);
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object
     */
    public String toString() {
        return this.getFunctionsStart()+this.value +this.getFunctionsEnd();
    }


}
