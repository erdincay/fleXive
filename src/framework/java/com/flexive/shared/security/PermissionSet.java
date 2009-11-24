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

import java.io.Serializable;

/**
 * A concrete permission set for a given content instance. By definition, if such a permission object exists
 * at least the read permission for the related object must have been set, so you cannot create
 * this object without read permissions.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public final class PermissionSet implements Serializable {
    private static final long serialVersionUID = 8227621876556289001L;

    private final int permissions;

    public PermissionSet(boolean mayEdit, boolean mayRelate, boolean mayDelete, boolean mayExport, boolean mayCreate) {
        int set = 1 << ACLPermission.READ.ordinal();
        set |= (mayEdit ? 1 : 0) << ACLPermission.EDIT.ordinal();
        set |= (mayRelate ? 1 : 0) << ACLPermission.RELATE.ordinal();
        set |= (mayDelete ? 1 : 0) << ACLPermission.DELETE.ordinal();
        set |= (mayExport ? 1 : 0) << ACLPermission.EXPORT.ordinal();
        set |= (mayCreate ? 1 : 0) << ACLPermission.CREATE.ordinal();
        this.permissions = set;
    }

    private PermissionSet(int permissions) {
        this.permissions = permissions;
    }

    /**
     * Returns this | other (a permission has to be present in one of the sets to be set).
     *
     * @param other the other permission set
     * @return  this | other
     * @since 3.1
     */
    public PermissionSet union(PermissionSet other) {
        return new PermissionSet(permissions | other.permissions);
    }

    /**
     * Returns this & other (a permission has to be present in both of the sets).
     *
     * @param other the other permission set
     * @return  this & other
     * @since 3.1
     */
    public PermissionSet intersect(PermissionSet other) {
        return new PermissionSet(permissions & other.permissions);
    }

    public boolean isPermitted(ACLPermission permission) {
        return (permissions & (1 << permission.ordinal())) > 0;
    }

    public boolean isMayRead() {
        return isPermitted(ACLPermission.READ);
    }

    public boolean isMayEdit() {
        return isPermitted(ACLPermission.EDIT);
    }

    public boolean isMayRelate() {
        return isPermitted(ACLPermission.RELATE);
    }

    public boolean isMayDelete() {
        return isPermitted(ACLPermission.DELETE);
    }

    public boolean isMayExport() {
        return isPermitted(ACLPermission.EXPORT);
    }

    public boolean isMayCreate() {
        return isPermitted(ACLPermission.CREATE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final PermissionSet set = (PermissionSet) o;
        return permissions == set.permissions;

    }

    @Override
    public int hashCode() {
        return permissions;
    }

    @Override
    public String toString() {
        final StringBuilder out = new StringBuilder(50);
        out.append("permissions[");
        for (ACLPermission permission: ACLPermission.values()) {
            if (isPermitted(permission)) {
                out.append(permission.name()).append(',');
            }
        }
        out.deleteCharAt(out.length() - 1); // at least 'read' is always rendered
        out.append("]");
        return out.toString();
    }
}
