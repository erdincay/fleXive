/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2009
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

import org.apache.chemistry.ContentStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import com.flexive.shared.value.FxBinary;
import com.flexive.shared.value.BinaryDescriptor;
import com.flexive.shared.stream.FxStreamUtils;
import com.flexive.shared.exceptions.FxStreamException;
import com.flexive.stream.StreamException;

/**
 * A content stream provided by flexive's streaming server.
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FlexiveContentStream implements ContentStream {
    private static final Log LOG = LogFactory.getLog(FlexiveContentStream.class);
    private final FxBinary binary;

    public FlexiveContentStream(FxBinary binary) {
        this.binary = binary;
    }

    public long getLength() {
        return getDescriptor().getSize();
    }

    public String getMimeType() {
        return getDescriptor().getMimeType();
    }

    public InputStream getStream() throws IOException {
        try {
            return FxStreamUtils.getBinaryStream(getDescriptor(), BinaryDescriptor.PreviewSizes.ORIGINAL);
        } catch (FxStreamException e) {
            throw new IOException(e);
        }
    }

    public String getFileName() {
        return getDescriptor().getName();
    }

    private BinaryDescriptor getDescriptor() {
        return binary.getBestTranslation();
    }
}
