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
package com.flexive.shared.content;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.ACLAssignment;
import com.flexive.shared.security.UserGroup;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.value.FxNoAccess;

import java.util.ArrayList;
import java.util.List;

/**
 * Permission Utilities
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxPermissionUtils {

    public static final byte PERM_MASK_INSTANCE = 0x01;
    public static final byte PERM_MASK_PROPERTY = 0x02;
    public static final byte PERM_MASK_STEP = 0x04;
    public static final byte PERM_MASK_TYPE = 0x08;

    /**
     * Permission check for (new) contents
     *
     * @param ticket calling users ticket
     * @param permission permission to check
     * @param type used type
     * @param stepACL step ACL
     * @param contentACL content ACL
     * @param throwException should exception be thrown
     * @return access granted
     * @throws FxNoAccessException if not accessible for calling user
     */
    public static boolean checkPermission(UserTicket ticket, ACL.Permission permission, FxType type, long stepACL,
                                          long contentACL, boolean throwException) throws FxNoAccessException {
        if (ticket.isGlobalSupervisor() || !type.usePermissions() || FxContext.get().getRunAsSystem())
            return true;
        boolean typeAllowed = !type.useTypePermissions();
        boolean stepAllowed = !type.useStepPermissions();
        boolean contentAllowed = !type.useInstancePermissions();
        for (ACLAssignment assignment : ticket.getACLAssignments()) {
            if (!typeAllowed && assignment.getAclId() == type.getACL().getId())
                typeAllowed = assignment.getPermission(permission);
            if (!stepAllowed && assignment.getAclId() == stepACL)
                stepAllowed = assignment.getPermission(permission);
            if (!contentAllowed && assignment.getAclId() == contentACL)
                contentAllowed = assignment.getPermission(permission);
        }
        if (throwException && !(typeAllowed && stepAllowed && contentAllowed)) {
            List<String> lacking = new ArrayList<String>(3);
            if (!typeAllowed)
                addACLName(lacking, ticket.getLanguage(), type.getACL().getId());
            if (!stepAllowed)
                addACLName(lacking, ticket.getLanguage(), stepACL);
            if (!contentAllowed)
                addACLName(lacking, ticket.getLanguage(), contentACL);
            String[] params = new String[lacking.size() + 1];
            params[0] = "ex.acl.name." + permission.toString().toLowerCase();
            for (int i = 0; i < lacking.size(); i++)
                params[i + 1] = lacking.get(i);
            throw new FxNoAccessException("ex.acl.noAccess.extended." + lacking.size(), (Object[]) params);
        }
        return typeAllowed && stepAllowed && contentAllowed;
    }

    /**
     * Permission check for existing contents
     *
     * @param ticket calling users ticket
     * @param permission permission to check
     * @param si security info of the content to check
     * @param throwException should exception be thrown
     * @return access granted
     * @throws FxNoAccessException if access denied and exception should be thrown
     */
    public static boolean checkPermission(UserTicket ticket, ACL.Permission permission, FxContentSecurityInfo si, boolean throwException) throws FxNoAccessException {
        if (!si.usePermissions() || ticket.isGlobalSupervisor() || FxContext.get().getRunAsSystem())
            return true;
        boolean typeAllowed = !si.useTypePermissions();
        boolean stepAllowed = !si.useStepPermissions();
        boolean contentAllowed = !si.useInstancePermissions();
        boolean propertyAllowed = true;

        List<String> lacking = (throwException ? new ArrayList<String>(3) : null);

        for (ACLAssignment assignment : ticket.getACLAssignments()) {
            if (!typeAllowed && assignment.getAclId() == si.getTypeACL())
                typeAllowed = assignment.getPermission(permission);
            if (!stepAllowed && assignment.getAclId() == si.getStepACL())
                stepAllowed = assignment.getPermission(permission);
            if (!contentAllowed && assignment.getAclId() == si.getContentACL())
                contentAllowed = assignment.getPermission(permission);
            if (permission == ACL.Permission.DELETE) {
                //property permissions are only checked for delete operations since no
                //exception should be thrown when ie loading as properties are wrapped in
                //FxNoAccess values or set to read only
                if (si.usePermissions() && si.getUsedPropertyACL().size() > 0 && assignment.getACLCategory() == ACL.Category.STRUCTURE) {
                    for (long propertyACL : si.getUsedPropertyACL())
                        if (propertyACL == assignment.getAclId())
                            if (!assignment.getPermission(permission)) {
                                propertyAllowed = false;
                                addACLName(lacking, ticket.getLanguage(), propertyACL);
                            }
                }
            }
        }
        if (throwException && !(typeAllowed && stepAllowed && contentAllowed && propertyAllowed)) {
            if (!typeAllowed)
                addACLName(lacking, ticket.getLanguage(), si.getTypeACL());
            if (!stepAllowed)
                addACLName(lacking, ticket.getLanguage(), si.getStepACL());
            if (!contentAllowed)
                addACLName(lacking, ticket.getLanguage(), si.getContentACL());
            String[] params = new String[lacking.size() + 1];
            params[0] = "ex.acl.name." + permission.toString().toLowerCase();
            for (int i = 0; i < lacking.size(); i++)
                params[i + 1] = lacking.get(i);
            throw new FxNoAccessException("ex.acl.noAccess.extended." + lacking.size(), (Object[]) params);
        }
        return typeAllowed && stepAllowed && contentAllowed && propertyAllowed;
    }

    /**
     * Get the translation for an ACL name in the requested language and add it to the given list
     *
     * @param list list to add the name
     * @param lang language
     * @param acl  id of the acl
     */
    private static void addACLName(List<String> list, FxLanguage lang, long acl) {
        String name;
        try {
            name = CacheAdmin.getEnvironment().getACL(acl).getLabel().getTranslation(lang);
        } catch (Exception e) {
            name = "#" + acl;
        }
        if (!list.contains(name))
            list.add(name);
    }

    /**
     * Process a contents property and wrap FxValue's in FxNoAccess or set them to readonly where appropriate
     *
     * @param ticket       calling users ticket
     * @param securityInfo needed security information
     * @param content      the content to process
     * @param type         the content's FxType
     * @param env          environment
     * @throws FxNotFoundException         on errors
     * @throws FxInvalidParameterException on errors
     * @throws FxNoAccessException         on errors
     */
    public static void processPropertyPermissions(UserTicket ticket, FxContentSecurityInfo securityInfo, FxContent content, FxType type, FxEnvironment env) throws FxNotFoundException, FxInvalidParameterException, FxNoAccessException {
        if (!type.usePropertyPermissions() || ticket.isGlobalSupervisor() || FxContext.get().getRunAsSystem())
            return; //invalid call, nothing to process ...
        List<String> xpaths = content.getAllPropertyXPaths();
        FxPropertyData pdata;
        List<Long> noAccess = new ArrayList<Long>(5);
        List<Long> readOnly = new ArrayList<Long>(5);
        for (long aclId : securityInfo.getUsedPropertyACL()) {
            if (!ticket.mayReadACL(aclId))
                noAccess.add(aclId);
            else if (!ticket.mayEditACL(aclId))
                readOnly.add(aclId);
        }
        for (String xpath : xpaths) {
            pdata = content.getPropertyData(xpath);
            if (pdata.getValue() instanceof FxNoAccess)
                continue;
            ACL propACL = ((FxPropertyAssignment) env.getAssignment(pdata.getAssignmentId())).getACL();
            if (noAccess.contains(propACL.getId())) {
                //may not read => wrap in a FxNoAccess value
                pdata.setValue(new FxNoAccess(ticket, pdata.getValue()));
            } else if (readOnly.contains(propACL.getId())) {
                //may not edit => set readonly
                pdata.getValue().setReadOnly();
            }
        }
    }

    /**
     * Unwrap all FxNoAccess values to their original values.
     * Must be called as supervisor to work ...
     *
     * @param content the FxContent to process
     * @throws FxNotFoundException         on errors
     * @throws FxInvalidParameterException on errors
     * @throws FxNoAccessException         on errors
     */
    public static void unwrapNoAccessValues(FxContent content) throws FxNotFoundException, FxInvalidParameterException, FxNoAccessException {
        List<String> xpaths = content.getAllPropertyXPaths();
        FxPropertyData pdata;
        for (String xpath : xpaths) {
            pdata = content.getPropertyData(xpath);
            if (pdata.getValue() instanceof FxNoAccess)
                pdata.setValue(((FxNoAccess) pdata.getValue()).getWrappedValue());
        }
    }

    /**
     * Encode permissions for use in FxType
     *
     * @param useInstancePermissions instance
     * @param usePropertyPermissions property
     * @param useStepPermissions     (workflow)step
     * @param useTypePermissions     type
     * @return encoded permissions
     */
    public static byte encodeTypePermissions(boolean useInstancePermissions, boolean usePropertyPermissions, boolean useStepPermissions,
                                             boolean useTypePermissions) {
        byte perm = 0;
        if (useInstancePermissions)
            perm = PERM_MASK_INSTANCE;
        if (usePropertyPermissions)
            perm |= PERM_MASK_PROPERTY;
        if (useStepPermissions)
            perm |= PERM_MASK_STEP;
        if (useTypePermissions)
            perm |= PERM_MASK_TYPE;
        return perm;
    }

    /**
     * Get a human readable form of bit coded permissions
     *
     * @param bitCodedPermissions permissions
     * @return human readable form
     */
    public static String toString(byte bitCodedPermissions) {
        StringBuilder res = new StringBuilder(30);
        if( (bitCodedPermissions & PERM_MASK_TYPE) == PERM_MASK_TYPE)
            res.append(",Type");
        if( (bitCodedPermissions & PERM_MASK_STEP) == PERM_MASK_STEP)
            res.append(",Step");
        if( (bitCodedPermissions & PERM_MASK_PROPERTY) == PERM_MASK_PROPERTY)
            res.append(",Property");
        if( (bitCodedPermissions & PERM_MASK_INSTANCE) == PERM_MASK_INSTANCE)
            res.append(",Instance");
        if( res.length() > 0 )
            res.deleteCharAt(0);
        return res.toString();
    }

    /**
     * Get a users permission for a given instance ACL
     *
     * @param acl instance ACL
     * @param type used type
     * @param stepACL step ACL
     * @param createdBy owner
     * @param mandator mandator
     * @return array of permissions in the order edit, relate, delete, export and create
     * @throws com.flexive.shared.exceptions.FxNoAccessException if no read access if permitted
     */
    public static boolean[] getPermissions(long acl, FxType type, long stepACL, long createdBy, long mandator) throws FxNoAccessException {
        boolean[] perms = new boolean[5];
        UserTicket ticket = FxContext.get().getTicket();
        final boolean _system = FxContext.get().getRunAsSystem() || ticket.isGlobalSupervisor();
        checkPermission(ticket, ACL.Permission.READ, type, stepACL, acl, true);
        if (_system || ticket.isMandatorSupervisor() && mandator == ticket.getMandatorId() ||
                !type.usePermissions() || ticket.isInGroup((int) UserGroup.GROUP_OWNER) && createdBy == ticket.getUserId()) {
            perms[0] = perms[1] = perms[2] = perms[3] = perms[4] = true;
        } else {
            //throw exception if read is forbidden
            checkPermission(ticket, ACL.Permission.READ, type, stepACL, acl, true);
            perms[0] = checkPermission(ticket, ACL.Permission.EDIT, type, stepACL, acl, false);
            perms[1] = checkPermission(ticket, ACL.Permission.RELATE, type, stepACL, acl, false);
            perms[2] = checkPermission(ticket, ACL.Permission.DELETE, type, stepACL, acl, false);
            perms[3] = checkPermission(ticket, ACL.Permission.EXPORT, type, stepACL, acl, false);
            perms[4] = checkPermission(ticket, ACL.Permission.CREATE, type, stepACL, acl, false);
        }
        return perms;
    }
}
