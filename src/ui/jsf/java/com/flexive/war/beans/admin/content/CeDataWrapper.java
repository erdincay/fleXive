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
package com.flexive.war.beans.admin.content;

import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxValue;

import java.util.Hashtable;

class CeDataWrapper extends Hashtable<String, FxValue> {

    private ContentEditorBean parent;

    protected CeDataWrapper(ContentEditorBean parent) {
        super(0);
        this.parent = parent;
    }

    public FxValue put(String xpath, FxValue value) {
        FxValue oldValue = get(xpath);
        try {
            parent.getContent().setValue(xpath, value);
        } catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
        return oldValue;
    }

    public FxValue get(Object xpath) {
        try {
            return parent.getContent().getPropertyData(String.valueOf(xpath)).getValue();
        } catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
            return new FxString("Error for " + xpath + ":" + t.getMessage());
        }
    }

}
