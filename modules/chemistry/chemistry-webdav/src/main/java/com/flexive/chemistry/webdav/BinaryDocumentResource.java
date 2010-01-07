/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
package com.flexive.chemistry.webdav;

import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.ReplaceableResource;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import org.apache.chemistry.CMISException;
import org.apache.chemistry.Document;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class BinaryDocumentResource extends DocumentResource implements ReplaceableResource {
    private static final Log LOG = LogFactory.getLog(BinaryDocumentResource.class);

    public BinaryDocumentResource(ChemistryResourceFactory resourceFactory, String path, Document object) {
        super(resourceFactory, path, object);
    }

    /**
     * {@inheritDoc}
     */
    public String getContentType(String accepts) {
        try {
            return getObject().getContentStream().getMimeType();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Long getContentLength() {
        try {
            return getObject().getContentStream().getLength();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending binary content (range=" + range + ",params=" + params + ", contentType=" + contentType+ ")");
        }
        final InputStream in = getObject().getContentStream().getStream();
        try {
            final byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            in.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void replaceContent(InputStream in, Long length) {
        try {
            getObject().setContentStream(new UploadContentStream(in, getName(), null, length));
            getObject().save();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (CMISException e) {
            throw CMISExceptionWrapper.wrap(e);
        }
    }
}
