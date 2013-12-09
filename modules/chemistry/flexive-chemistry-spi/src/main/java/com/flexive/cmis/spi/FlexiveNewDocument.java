/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
package com.flexive.cmis.spi;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;

/**
 * A new document for a given content type. A document can be created inside an folder, or "unfiled".
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FlexiveNewDocument extends FlexiveDocument {
    private FxContent content;
    private final FlexiveFolder folder;

    private FxPK savedPK;   // cache information before saving the content to avoid unnecessary instance loads
    private String savedName;

    /**
     * Create a new flexive document.
     *
     * @param context   the connection context
     * @param content   the (unsaved) content instance
     * @param folder    the parent folder, or null for creating an unfiled document
     */
    FlexiveNewDocument(FlexiveConnection.Context context, FxContent content, FlexiveFolder folder) {
        super(context, content);
        if (!content.getPk().isNew()) {
            throw new IllegalArgumentException("Content is already saved (PK=" + content.getPk().toString() + ")");
        }
        this.content = content;
        this.folder = folder;
        useFolderPermissions(folder);
    }

    /**
     * Create a new flexive document.
     *
     * @param context   the connection context
     * @param typeName  the desired content type name
     * @param folder    the parent folder, or null for creating an unfiled document
     */
    FlexiveNewDocument(FlexiveConnection.Context context, String typeName, FlexiveFolder folder) {
        super(context, newContent(typeName));
        this.content = super.getContent();
        this.folder = folder;
        useFolderPermissions(folder);
    }

    protected void useFolderPermissions(FlexiveFolder folder) {
        if (this.folder != null) {
            this.content.setAclIds(folder.getNode().getACLIds());
        }
    }

    @Override
    public FxContent getContent() {
        if (content == null && savedPK != null) {
            try {
                content = EJBLookup.getContentEngine().load(savedPK);
                savedName = null;   // reset cached name
            } catch (FxApplicationException e) {
                throw e.asRuntimeException();
            }
        }
        return content;
    }

    @Override
    protected FxPK getPK() {
        return getContent().getPk();
    }

    @Override
    public String getId() {
        if (savedPK != null) {
            return savedPK.toString();
        } else {
            return content.getPk().isNew() ? null : content.getPk().toString();
        }
    }

    @Override
    public String getName() {
        return savedName != null ? savedName : super.getName();
    }

    @Override
    public void save() {
        if (!getContent().getPk().isNew()) {
            // delegate to default behaviour since we don't need to replace the content instance
            super.save();
            return;
        }
        try {
            savedName = getName();
            savedPK = EJBLookup.getContentEngine().save(getContent());
            content = null;
            if (folder != null) {
                folder.add(this);
                folder.processAdd();
            }
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }

    @Override
    protected long getFxTypeId() {
        return content == null ? super.getFxTypeId() : content.getTypeId();
    }

    private static FxContent newContent(String typeName) {
        try {
            return EJBLookup.getContentEngine().initialize(SPIUtils.getFxTypeName(typeName));
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }

}
