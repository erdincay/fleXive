package com.flexive.shared.security;

import com.flexive.shared.FxLanguage;

import java.util.Calendar;
import java.util.Date;

/**
 * An editable account wrapper implementation. Use this class for creating new accounts
 * or editing existing ones.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class AccountEdit extends Account {
    private static final long serialVersionUID = 7602517754874344449L;

    public AccountEdit() {
        //Init with a start date of today
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        setValidFrom(cal.getTime());
    }

    public AccountEdit(Account account) {
        super(account);
    }

    public AccountEdit setId(long id) {
        this.id = id;
        return this;
    }

    public AccountEdit setDefaultNodeId(long defaultNodeId) {
        this.defaultNodeId = defaultNodeId;
        return this;
    }

    public AccountEdit setUpdateToken(String updateToken) {
        this.updateToken = updateToken;
        return this;
    }

    public AccountEdit setLifeCycleInfo(LifeCycleInfo lifeCycleInfo) {
        this.lifeCycleInfo = lifeCycleInfo;
        return this;
    }

    public AccountEdit setName(String sName) {
        this.name = sName;
        if (this.loginName == null) {
            this.loginName = sName;
        }
        return this;
    }

    public AccountEdit setLoginName(String sLoginName) {
        this.loginName = sLoginName;
        if (this.name == null) {
            this.name = sLoginName;
        }
        return this;
    }

    public AccountEdit setMandatorId(long iMandator) {
        this.mandatorId = iMandator;
        return this;
    }

    public AccountEdit setEmail(String sEmail) {
        this.email = sEmail;
        return this;
    }

    public AccountEdit setLanguage(FxLanguage language) {
        this.language = language;
        return this;
    }

    public AccountEdit setActive(boolean bActive) {
        this.active = bActive;
        return this;
    }

    public AccountEdit setValidated(boolean bValidated) {
        this.validated = bValidated;
        return this;
    }

    public AccountEdit setValidFrom(Date dValidFrom) {
        this.validFrom = (Date) dValidFrom.clone();
        return this;
    }

    public AccountEdit setValidTo(Date dValidTo) {
        this.validTo = (Date) dValidTo.clone();
        return this;
    }

    public AccountEdit setDefaultNode(long lDefaultNode) {
        this.defaultNodeId = lDefaultNode;
        return this;
    }

    public AccountEdit setDescription(String sDescription) {
        this.description = sDescription;
        return this;
    }

    public AccountEdit setContactDataId(long lContactDataId) {
        this.contactDataId = lContactDataId;
        return this;
    }

    public AccountEdit setAllowMultiLogin(boolean bAllowMultiLogin) {
        this.allowMultiLogin = bAllowMultiLogin;
        return this;
    }

    public AccountEdit setRestToken(String token) {
        this.restToken = token;
        return this;
    }

    public AccountEdit setRestTokenExpires(long timestamp) {
        this.restTokenExpires = timestamp;
        return this;
    }
}
