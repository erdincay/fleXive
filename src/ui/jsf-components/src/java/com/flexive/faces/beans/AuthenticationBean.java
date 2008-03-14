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
package com.flexive.faces.beans;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.shared.FxContext;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.security.UserTicket;
import com.flexive.war.FxRequest;

import java.io.Serializable;

public class AuthenticationBean implements Serializable {
    private static final long serialVersionUID = -8245131162897398694L;

    private String username;
    private String password;
    private boolean takeover;


    public UserTicket getTicket() {
        return FxContext.get().getTicket();
    }

    public boolean getIsLoggedIn() {
        return FxContext.get().getTicket().isGuest();
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isTakeover() {
        return takeover;
    }

    public void setTakeover(boolean takeover) {
        this.takeover = takeover;
    }

    public String login() {
        try {
            FxRequest request = FxJsfUtils.getRequest();
            request.login(username, password, takeover);
            request.getUserTicket();
            if( CacheAdmin.isNewInstallation() )
                return "loginSuccessInit";
            return "loginSuccess";
        } catch (Exception exc) {
            new FxFacesMsgErr(exc).addToContext();
        }
        return null;
    }

    public String logout() {
        try {
            FxRequest request = FxJsfUtils.getRequest();
            request.logout();
            FxJsfUtils.getSession().invalidate();
        } catch (Exception exc) {
            new FxFacesMsgErr(exc).addToContext();
        }
        return "login";
    }
}
