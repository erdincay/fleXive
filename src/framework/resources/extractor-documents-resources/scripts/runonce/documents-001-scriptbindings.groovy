/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
import com.flexive.shared.scripting.FxScriptEvent
import com.flexive.shared.*
import com.flexive.shared.scripting.*
import com.flexive.shared.structure.FxType

def se = EJBLookup.getScriptingEngine()

// remove possibly existing scripts from main distribution
se.scriptInfos.each { FxScriptInfo info ->
  if (info.name == "BinaryProcessor_Documents.gy" || info.name == "DocumentMetaParser.gy") {
    se.remove(info.id)
  }
}

// assign preview generation
se.createScriptFromDropLibrary("flexive-extractor-documents", FxScriptEvent.BinaryPreviewProcess,
    "BinaryProcessor_Documents.gy",
    "BinaryProcessor_Documents.gy",
    "Binary processing for documents")

// attach document extractor to document type    
long scriptId = se.createScriptFromDropLibrary("flexive-extractor-documents", FxScriptEvent.BeforeContentCreate,
    "DocumentMetaParser.gy", "DocumentMetaParser.gy", "Script filling document properties").id;
se.createTypeScriptMapping(scriptId, CacheAdmin.environment.getType(FxType.DOCUMENT).id, true, true)
