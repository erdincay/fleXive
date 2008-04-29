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
package com.flexive.shared.security;


import com.flexive.shared.AbstractSelectableObjectWithName;
import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.content.FxPK;

import java.io.Serializable;
import java.util.Date;

/**
 * User account class.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class Account extends AbstractSelectableObjectWithName implements Serializable {
    private static final long serialVersionUID = 5344820690672809623L;

    /**
     * The guest user. Everyone who is not logged in is treated as GUEST
     */
    public static final long USER_GUEST = 1;

    /**
     * The supervisor. This user is in all roles and may operate on all mandators
     */
    public static final long USER_GLOBAL_SUPERVISOR = 2;

    /**
     * Dummy account (id if a null value cannot be applied)
     */
    public static final long NULL_ACCOUNT = 3;

    /**
     * 01.01.3000
     */
    public static final Date VALID_FOREVER = new Date(32503676400000L);


    protected String name = null;
    protected String loginName = null;
    protected long id = -1;
    protected long mandatorId = -1;
    protected String email = null;
    protected FxLanguage language = FxLanguage.DEFAULT;
    protected boolean active = true;
    protected boolean validated = true;
    protected Date validFrom = null;
    protected Date validTo = null;
    protected long defaultNodeId = 0;
    protected String description = null;
    protected long contactDataId = -1;
    protected boolean allowMultiLogin = false;
    protected String updateToken;
    protected LifeCycleInfo lifeCycleInfo = null;

    public Account() {
        /* empty constructor */
    }

    public Account(long id, String name, String loginName, long mandator,
                   String email, FxLanguage language, boolean active,
                   boolean validated, Date validFrom, Date validTo, long defaultNode,
                   String description, long contactDataId, boolean allowMultiLogin,
                   String updateToken, LifeCycleInfo lifeCycleInfo) {
        this.name = name;
        this.loginName = loginName;
        this.id = id;
        this.mandatorId = mandator;
        this.email = email;
        this.language = language;
        this.active = active;
        this.validated = validated;
        this.validFrom = (Date) validFrom.clone();
        this.validTo = (Date) validTo.clone();
        this.defaultNodeId = defaultNode;
        this.description = description;
        this.contactDataId = contactDataId;
        this.allowMultiLogin = allowMultiLogin;
        this.updateToken = updateToken;
        this.lifeCycleInfo = lifeCycleInfo;
    }

    public Account(Account other) {
        this(other.id, other.name, other.loginName, other.mandatorId, other.email, other.language,
                other.active, other.validated, other.validFrom, other.validTo, other.defaultNodeId,
                other.description, other.contactDataId, other.allowMultiLogin, other.updateToken, other.lifeCycleInfo);
    }

    /**
     * Is this a system internal account?
     *
     * @return system internal account
     */
    public boolean isSystemInternalAccount() {
        return id <= NULL_ACCOUNT && id >= 0;
    }

    /**
     * Is this account instance new? (i.e. not saved yet)
     *
     * @return if new
     */
    public boolean isNew() {
        return id < 0;
    }

    /**
     * Returns the email of the user.
     *
     * @return the email of the user
     */
    public String getEmail() {
        return this.email;
    }

    /**
     * Returns the language of the user.
     *
     * @return the language of the user
     */
    public FxLanguage getLanguage() {
        return this.language;
    }

    /**
     * Returns the user name.
     *
     * @return the user name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the unique user id.
     *
     * @return the unique user id.
     */
    public long getId() {
        return this.id;
    }

    /**
     * Returns the unique id of the mandator this user belongs to.
     *
     * @return the unique id of the mandator this user belongs to.
     */
    public long getMandatorId() {
        return this.mandatorId;
    }


    /**
     * Returns the login name of the user.
     *
     * @return the login name of the user.
     */
    public String getLoginName() {
        return loginName;
    }


    /**
     * Return true if the user is active.
     *
     * @return true if the user is active
     */
    public boolean isActive() {
        return this.active;
    }


    /**
     * Returns true if the user is validated.
     *
     * @return true if the user is validated
     */
    public boolean isValidated() {
        return this.validated;
    }

    /**
     * Returns the valid from date of the user.
     * <p/>
     * The valid from/to dates may be used to define a timeperiode
     * in which the user may LOG in.
     *
     * @return the valid from date of the user
     */
    public Date getValidFrom() {
        return validFrom != null ? (Date) this.validFrom.clone() : null;
    }

    /**
     * Returns the valid to date of the user.
     * <p/>
     * The valid from/to dates may be used to define a timeperiode
     * in which the user may LOG in.
     *
     * @return the valid to date of the user
     */
    public Date getValidTo() {
        return validTo != null ? (Date) this.validTo.clone() : null;
    }

    /**
     * Returns the valid from date of the user.
     * <p/>
     * The valid from/to dates may be used to define a timeperiode
     * in which the user may LOG in.
     *
     * @return the valid from date of the user
     */
    public String getValidFromString() {
        return FxFormatUtils.toString(this.validFrom, null);
    }

    /**
     * Returns the valid to date of the user.
     * <p/>
     * The valid from/to dates may be used to define a timeperiode
     * in which the user may LOG in.
     *
     * @return the valid to date of the user
     */
    public String getValidToString() {
        return FxFormatUtils.toString(this.validTo, null);
    }

    /**
     * Gets the description if the user.
     * <p/>
     * The result may be a empty String (but is never null)
     *
     * @return the description if the user
     */
    public String getDescription() {
        return (this.description == null) ? "" : this.description;
    }

    /**
     * Get the primary key of the contact data object holding further informations
     * of the user.
     *
     * @return the id of the contact data object
     */
    public FxPK getContactData() {
        return new FxPK(this.contactDataId);
    }

    /**
     * Get the id of the contact data instance associated to this account
     *
     * @return contact data id
     */
    public long getContactDataId() {
        return contactDataId;
    }

    /**
     * Returns the update token of this account. The update token may be used
     * in external API calls modifying this account to improve security.
     *
     * @return  the update token of this account
     */
    public String getUpdateToken() {
        return updateToken;
    }

    /**
     * Returns the desired default node of the user.
     * May be -1 if no default node is defined.
     *
     * @return the desired default node of the user
     */
    public long getDefaultNode() {
        return this.defaultNodeId;
    }

    /**
     * Is this account allowed to be logged in more than once?
     *
     * @return multi login allowed?
     */
    public boolean isAllowMultiLogin() {
        return this.allowMultiLogin;
    }

    /**
     * Get the lifecycle information
     *
     * @return lifecycle information
     */
    public LifeCycleInfo getLifeCycleInfo() {
        return lifeCycleInfo;
    }

    public AccountEdit asEditable() {
        return new AccountEdit(this);
    }
}
