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
package com.flexive.war.beans.admin.main;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.security.Role;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.commons.lang.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Formatter;

/**
 * A simple groovy console for testing (war-layer) groovy scripts.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class GroovyConsoleBean {
    private String code;
    private long executionTime;
    private boolean ejb;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    public boolean isEjb() {
        return ejb;
    }

    public void setEjb(boolean ejb) {
        this.ejb = ejb;
    }

    public Object getResult() {
        if (StringUtils.isBlank(code)) {
            return "";
        }
        long start = System.currentTimeMillis();
        try {
            if (!ejb) {
                System.out.println("Running on war layer");
                if( !FxContext.get().getTicket().isInRole(Role.ScriptExecution))
                    return "No permission to execute scripts!";
                GroovyShell shell = new GroovyShell();
                Script script = shell.parse(code);
                return script.run();
            } else {
                System.out.println("Running server side");
                return EJBLookup.getScriptingEngine().runScript("console.gy", null, code).getResult();
            }
        } catch (Exception e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            return new Formatter().format("Exception caught: %s%n%s", e.getMessage(), writer.getBuffer());
        } finally {
            executionTime = System.currentTimeMillis() - start;
        }
    }
}
