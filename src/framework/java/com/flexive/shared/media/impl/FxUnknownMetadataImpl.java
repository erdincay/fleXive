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
package com.flexive.shared.media.impl;

import com.flexive.shared.media.FxMediaType;
import com.flexive.shared.media.FxMetadata;

import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Metadata for unknown formats
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxUnknownMetadataImpl extends FxMetadata {

    private final static List<FxMetadataItem> empytmeta = Collections.unmodifiableList(new ArrayList<FxMetadataItem>(0));
    private String mimeType;
    private String filename;

    /**
     * Ctor
     *
     * @param mimeType mimetype
     * @param filename filename or <code>null</code>
     */
    public FxUnknownMetadataImpl(String mimeType, String filename) {
        this.mimeType = mimeType;
        this.filename = filename;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxMediaType getMediaType() {
        return FxMediaType.Unknown;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMimeType() {
        return mimeType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFilename() {
        return filename;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxMetadataItem> getMetadata() {
        return empytmeta;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeXMLTags(XMLStreamWriter writer) {
        //nothing to do
    }
}
