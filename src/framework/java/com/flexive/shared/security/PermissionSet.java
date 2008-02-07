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
package com.flexive.shared.security;

import static com.flexive.shared.security.ACL.Permission;

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
        int set = 1 << Permission.READ.ordinal();
        set |= (mayEdit ? 1 : 0) << Permission.EDIT.ordinal();
        set |= (mayRelate ? 1 : 0) << Permission.RELATE.ordinal();
        set |= (mayDelete ? 1 : 0) << Permission.DELETE.ordinal();
        set |= (mayExport ? 1 : 0) << Permission.EXPORT.ordinal();
        set |= (mayCreate ? 1 : 0) << Permission.CREATE.ordinal();
        this.permissions = set;
    }

    public boolean isPermitted(ACL.Permission permission) {
        return (permissions & (1 << permission.ordinal())) > 0;
    }

    public boolean isMayRead() {
        return isPermitted(Permission.READ);
    }

    public boolean isMayEdit() {
        return isPermitted(Permission.EDIT);
    }

    public boolean isMayRelate() {
        return isPermitted(Permission.RELATE);
    }

    public boolean isMayDelete() {
        return isPermitted(Permission.DELETE);
    }

    public boolean isMayExport() {
        return isPermitted(Permission.EXPORT);
    }

    public boolean isMayCreate() {
        return isPermitted(Permission.CREATE);
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
        for (Permission permission: Permission.values()) {
            if (isPermitted(permission)) {
                out.append(permission.name()).append(',');
            }
        }
        out.deleteCharAt(out.length() - 1); // at least 'read' is always rendered
        out.append("]");
        return out.toString();
    }
}
