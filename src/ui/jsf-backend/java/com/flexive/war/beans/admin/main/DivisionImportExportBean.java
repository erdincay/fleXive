/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
package com.flexive.war.beans.admin.main;


import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;

import java.io.Serializable;

/**
 * Division import/export
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
public class DivisionImportExportBean implements Serializable {
    private static final long serialVersionUID = 6097763705884318936L;

    private String source;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String importDivision() {
        System.out.println("importing " + source);
        try {
            EJBLookup.getDivisionConfigurationEngine().importDivision(source);
            new FxFacesMsgInfo("DivisionImportExport.import.success").addToContext();
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        }
        return "import";
    }

    public String exportDivision() {
        System.out.println("exporting " + source);
        try {
            EJBLookup.getDivisionConfigurationEngine().exportDivision(source);
            new FxFacesMsgInfo("DivisionImportExport.export.success").addToContext();
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        }
        return "export";
    }
}
