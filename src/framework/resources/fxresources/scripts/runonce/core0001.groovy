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
import com.flexive.shared.CacheAdmin
import com.flexive.shared.EJBLookup
import com.flexive.shared.configuration.SystemParameters
import com.flexive.shared.security.*
import com.flexive.shared.structure.*
import com.flexive.shared.value.FxString
/**
* Core System initialization script, has to be run before anything else!
*
* @author Markus Plesser, Unique Computing Solutions (UCS)
*/

println "Setting up [fleXive] ..."


//create the /CAPTION property and register it in the global configuration
FxPropertyEdit caption = FxPropertyEdit.createNew("CAPTION", new FxString("Caption"),
        new FxString("Caption"), FxMultiplicity.MULT_0_1,
        CacheAdmin.environment.getACL(ACLCategory.STRUCTURE.defaultId), FxDataType.String1024).setMultiLang(true)
long captionId = EJBLookup.assignmentEngine.createProperty(caption, "/")
long captionAssignment = CacheAdmin.environment.getAssignment("ROOT/CAPTION").id
println "Created /CAPTION with id $captionId, assignment $captionAssignment"
EJBLookup.configurationEngine.put(SystemParameters.TREE_CAPTION_PROPERTY, captionId);
EJBLookup.configurationEngine.put(SystemParameters.TREE_CAPTION_ROOTASSIGNMENT, captionAssignment);

//create the /FQN property and register it in the global configuration
FxPropertyEdit fqn = FxPropertyEdit.createNew("FQN", new FxString("FQN"),
        new FxString("Fully qualified name"), FxMultiplicity.MULT_0_1,
        CacheAdmin.environment.getACL(ACLCategory.STRUCTURE.defaultId), FxDataType.String1024).setMultiLang(false)
long fqnId = EJBLookup.assignmentEngine.createProperty(fqn, "/")
long fqnAssignment = CacheAdmin.environment.getAssignment("ROOT/FQN").id
println "Created /FQN with id $fqnId, assignment $fqnAssignment"
EJBLookup.configurationEngine.put(SystemParameters.TREE_FQN_PROPERTY, fqnId);
EJBLookup.configurationEngine.put(SystemParameters.TREE_FQN_ROOTASSIGNMENT, fqnAssignment);

//create mandator groups
EJBLookup.userGroupEngine.rebuildMandatorGroups()