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
package com.flexive.shared.stream;

import com.flexive.shared.FxContext;
import com.flexive.shared.value.BinaryDescriptor;

import java.io.Serializable;

/**
 * Payload for binary downloads
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class BinaryDownloadPayload implements Serializable {
    private static final long serialVersionUID = -54895840376576824L;

    private long id;
    private int version;
    private int quality;
    private int division;
    private int size;
    private boolean forceImage;

    private boolean serverError;
    private String errorMessage;
    private String mimeType;
    private int datasize;


    public BinaryDownloadPayload(long id, int version, int quality, int size, boolean forceImage) {
        this.id = id;
        this.version = version;
        this.quality = quality;
        this.size = size;
        this.forceImage = forceImage;
        this.division = FxContext.get().getDivisionId();
    }

    public BinaryDownloadPayload(long id, int version, int quality) {
        this(id, version, quality, BinaryDescriptor.PreviewSizes.ORIGINAL.getBlobIndex(), false);
    }

    public BinaryDownloadPayload(boolean serverError, String errorMessage) {
        this.serverError = serverError;
        this.errorMessage = errorMessage;
    }

    public BinaryDownloadPayload(String mimeType, int datasize) {
        this.serverError = false;
        this.mimeType = mimeType;
        this.datasize = datasize;
    }

    public long getId() {
        return id;
    }

    public int getVersion() {
        return version;
    }

    public int getQuality() {
        return quality;
    }

    public int getSize() {
        return size;
    }

    public int getDivision() {
        return division;
    }

    public boolean isServerError() {
        return serverError;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getMimeType() {
        return mimeType;
    }

    public int getDatasize() {
        return datasize;
    }

    public boolean isForceImage() {
        return forceImage;
    }
}
