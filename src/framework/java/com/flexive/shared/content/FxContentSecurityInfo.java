/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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

import org.apache.commons.lang.ArrayUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Security related information about a content (primary key)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxContentSecurityInfo implements Serializable {

    private static final long serialVersionUID = -5140606563012031397L;

    /**
     * Primary key this info relates to
     */
    private FxPK pk;

    /**
     * user id of the owner of the content
     */
    private long ownerId;

    /**
     * Id of preview image, only relevant for security if &gt; 0
     */
    private long previewId;

    /**
     * Id of the FxType used
     */
    private long typeId;

    /**
     * binary encoded type permissions
     */
    private byte permissions;

    /**
     * ACL assigned to the type
     */
    private int typeACL;

    /**
     * ACL of the used step
     */
    private int stepACL;

    /**
     * ACL of the content itself
     */
    private int contentACL;

    /**
     * Property ACL of the preview, only relevant for security if <code>previewId</code> &gt; 0
     */
    private int previewACL;

    /**
     * All used and relevant property ACL's. will be empty if property permissions are disabled for
     * the type
     */
    private List<Long> usedPropertyACL;

    /**
     * Constructor
     *
     * @param pk              the primary key this info relates to
     * @param ownerId         owner of the content
     * @param previewId       Id of preview image, only relevant for security if &gt; 0
     * @param typeId          id of the used type
     * @param typePermissions byte encoded type permission handling
     * @param typeACL         ACL of the type
     * @param stepACL         ACL of the step
     * @param contentACL      ACL of the content instance
     * @param previewACL      Property ACL of the preview, only relevant for security if <code>previewId</code> &gt; 0
     * @param usedPropertyACL relevant property ACL's
     */
    public FxContentSecurityInfo(FxPK pk, long ownerId, long previewId, long typeId, byte typePermissions, int typeACL, int stepACL, int contentACL,
                                 int previewACL,
                                 long[] usedPropertyACL) {
        this.pk = pk;
        this.ownerId = ownerId;
        this.previewId = previewId;
        this.typeId = typeId;
        this.permissions = typePermissions;
        this.typeACL = typeACL;
        this.stepACL = stepACL;
        this.contentACL = contentACL;
        this.previewACL = previewACL;
        this.usedPropertyACL = Collections.unmodifiableList(Arrays.asList(ArrayUtils.toObject(usedPropertyACL)));
    }

    /**
     * Get the primary key of the content instance this info relates to
     *
     * @return primary key of the content instance this info relates to
     */
    public FxPK getPk() {
        return pk;
    }

    /**
     * Get the owner of the content
     *
     * @return owner of the content
     */
    public long getOwnerId() {
        return ownerId;
    }

    /**
     * Getter for the used type id
     *
     * @return used type id
     */
    public long getTypeId() {
        return typeId;
    }

    /**
     * Get the ACL of the type
     *
     * @return ACL of the type
     */
    public int getTypeACL() {
        return typeACL;
    }

    /**
     * Get the ACL of the step
     *
     * @return ACL of the step
     */
    public int getStepACL() {
        return stepACL;
    }

    /**
     * Get the ACL of the content instance
     *
     * @return ACL of the content instance
     */
    public int getContentACL() {
        return contentACL;
    }

    /**
     * Get all used and relevant property ACL's. will be empty if property permissions are disabled for
     * the type
     *
     * @return relevant property ACL's
     */
    public List<Long> getUsedPropertyACL() {
        return usedPropertyACL;
    }

    /**
     * Use permissions at all?
     *
     * @return if permissions are used at all
     */
    public boolean usePermissions() {
        return permissions != 0;
    }

    public long getPreviewId() {
        return previewId;
    }

    public int getPreviewACL() {
        return previewACL;
    }

    /**
     * Use content instance permissions?
     *
     * @return if content instance permissions are used
     */
    public boolean useInstancePermissions() {
        return (permissions & FxPermissionUtils.PERM_MASK_INSTANCE) == FxPermissionUtils.PERM_MASK_INSTANCE;
    }

    /**
     * Use property permissions?
     *
     * @return if property permissions are used
     */
    public boolean usePropertyPermissions() {
        return (permissions & FxPermissionUtils.PERM_MASK_PROPERTY) == FxPermissionUtils.PERM_MASK_PROPERTY;
    }

    /**
     * Use step permissions?
     *
     * @return if step permissions are used
     */
    public boolean useStepPermissions() {
        return (permissions & FxPermissionUtils.PERM_MASK_STEP) == FxPermissionUtils.PERM_MASK_STEP;
    }

    /**
     * Use type permissions?
     *
     * @return if type permissions are used
     */
    public boolean useTypePermissions() {
        return (permissions & FxPermissionUtils.PERM_MASK_TYPE) == FxPermissionUtils.PERM_MASK_TYPE;
    }
}
