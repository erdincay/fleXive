/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2012
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
package com.flexive.faces.beans;

import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.value.FxBoolean;
import com.flexive.shared.value.FxString;

import java.io.Serializable;

/**
 * NewsletterInfo
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev: 1295 $
 */
@SuppressWarnings("UnusedDeclaration")
public class NewsletterSubscriber implements Serializable {
    FxPK pk;
    FxString salutation, name, surname, email, code;
    FxBoolean plainText, htmlText;

    public NewsletterSubscriber(FxPK pk, FxString salutation, FxString name, FxString surname, FxString email, FxBoolean plainText, FxBoolean htmlText, FxString code) {
        this.pk = pk;
        this.salutation = salutation;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.plainText = plainText;
        this.htmlText = htmlText;
        this.code = code;
    }

    public NewsletterSubscriber(FxContent subscriber) {
        this.pk = subscriber.getPk();
        this.salutation = subscriber.containsValue("/SALUTATION") ? (FxString)subscriber.getValue("/SALUTATION") : new FxString(false, "").setEmpty();
        this.name = subscriber.containsValue("/NAME") ? (FxString)subscriber.getValue("/NAME") : new FxString(false, "").setEmpty();
        this.surname = (FxString)subscriber.getValue("/SURNAME");
        this.email = (FxString)subscriber.getValue("/EMAIL");
        this.code = (FxString)subscriber.getValue("/CODE");
        this.plainText = (FxBoolean)subscriber.getValue("/SENDPLAIN");
        this.htmlText = (FxBoolean)subscriber.getValue("/SENDHTML");
    }

    public FxPK getPk() {
        return pk;
    }

    public FxString getSalutation() {
        return salutation;
    }

    public FxString getName() {
        return name;
    }

    public FxString getSurname() {
        return surname;
    }

    public FxString getEmail() {
        return email;
    }

    public FxBoolean getPlainText() {
        return plainText;
    }

    public FxBoolean getHtmlText() {
        return htmlText;
    }

    public FxString getCode() {
        return code;
    }

    public String getCancelCode(FxPK newsletterPK) {
       return "C" + code.getBestTranslation() + String.valueOf(pk.getId())+"_"+String.valueOf(newsletterPK.getId());
    }
}
