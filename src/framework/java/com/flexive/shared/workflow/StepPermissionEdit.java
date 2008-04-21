/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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
package com.flexive.shared.workflow;

import java.io.Serializable;

/**
 * Editable step permission object.
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class StepPermissionEdit extends StepPermission implements Serializable {
    private static final long serialVersionUID = -478210780025520823L;

    /**
     * Copy constructor.
     * @param stepPermission    the source step permission
     */
    public StepPermissionEdit(StepPermission stepPermission) {
        super(stepPermission.getStepId(), stepPermission.getStepDefId(), stepPermission.getWorkflowId(),
                stepPermission.getMayRead(), stepPermission.getMayEdit(), stepPermission.getMayRelate(),
                stepPermission.getMayDelete(), stepPermission.getMayExport(), stepPermission.getMayCreate());
    }
    
    /**
     * Default constructor.
     */
    public StepPermissionEdit() {
        
    }
    
    public void setMayRead(boolean value) {
        this.mayRead = value;
    }
    
    public void setMayDelete(boolean value) {
        this.mayDelete = value;
    }
    
    public void setMayEdit(boolean value) {
        this.mayEdit = value;
    }
    
    public void setMayExport(boolean value) {
        this.mayExport = value;
    }
    
    public void setMayRelate(boolean value) {
        this.mayRelate = value;
    }
    
    public void setMayCreate(boolean value) {
        this.mayCreate = value;
    }

}
