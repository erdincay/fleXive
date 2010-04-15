/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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
package com.flexive.extractor;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxFileUtils;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.interfaces.ScriptingEngine;
import com.flexive.shared.media.FxMediaEngine;
import com.flexive.shared.media.FxMetadata;
import com.flexive.shared.scripting.FxScriptBinding;
import com.flexive.shared.scripting.FxScriptResult;
import com.flexive.shared.stream.FxStreamUtils;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.value.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;

/**
 * DocumentParser
 * A class for parsing content instances of DOCUMENTFILE and its derived types
 * Caller: callbacks from ScriptEvents attached to the respective types
 * DOCUMENTFILE: FxScriptEvent.BeforeContentCreate (run())
 * FxScriptEvent.AfterContentSave (convert()
 * DOCUMENT: t.b. implemented, atm orig. scripts are executed, no callback to this class
 * IMAGE: t.b. implemented, atm orig. scripts are executed, no callback to this class
 *
 * @author Christopher Blasnik (c.blasnik@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class DocumentParser {

    private FxContent content;
    private FxType contentType;
    private FxPK pk;
    private static final Log LOG = LogFactory.getLog(DocumentParser.class);
    // store the most commonly used assignment Xpaths
    private static String MIMEPROP = "/MIMETYPE";
    private static String FILEPROP = "/FILE";

    /**
     * Constructor f. after content save events
     * 
     * @param content the FxContent
     */
    public DocumentParser(FxContent content) {
        this.content = content;
    }

    /**
     * Constructor f. after content save events
     *
     * @param pk the FxPK
     */
    public DocumentParser(FxPK pk) {
        this.pk = pk;
    }

    /**
     * Initialise content / contentType vars
     *
     * @param reloadContent reload the content if necessary
     * @return true if successfull
     */
    private boolean init(boolean reloadContent) {
        if (content == null || reloadContent) {
            try {
                this.content = EJBLookup.getContentEngine().load(pk);
            } catch (FxApplicationException e) {
                LOG.error("Could not load content for pk " + pk + " - conversion aborted " + e.getMessage());
                e.printStackTrace();
            }
        }
        if(contentType == null || reloadContent) {
            this.contentType = CacheAdmin.getEnvironment().getType(content.getTypeId());
        }
        return content != null && contentType != null;
    }

    /**
     * Main metaparser method f. the DOCUMENTFILE type (and its derived types [future change])
     * Run the parser and return the FxContent instance
     *
     * @return the FxContent instance
     */
    public FxContent run() {
        if (init(false)) {
            // DOCUMENTFILE content instance
            if (FxType.DOCUMENTFILE.equals(contentType.getName()))
                return runDocumentFileParser();
                // TODO (f. 3.1.1): callback functions f. DOCUMENT / IMAGE der. types
        }
        return content;
    }

    /**
     * Set the MIMETYPE (and CAPTION) property in for a content instance of DOCUMENTFILE
     *
     * @return the FxContent instance
     */
    private FxContent runDocumentFileParser() {
        Object binValue = content.getPropertyData(FILEPROP).getValue().getDefaultTranslation();
        if (binValue != null && binValue instanceof BinaryDescriptor) {
            BinaryDescriptor desc = (BinaryDescriptor) binValue;
            if (desc.getMimeType() != null) {
                if (!content.containsValue(MIMEPROP))
                    content.setValue(MIMEPROP, new FxString(false, desc.getMimeType()));
                if(!content.containsValue("/CAPTION"))
                    content.setValue("/CAPTION", new FxString(true, desc.getName()));
            }
        }
        return content;
    }

    /**
     * convert Method: to be called for AfterContentCreate / AfterContentSave events (DocumentFile type)
     *
     * Convert a given instance of DocumentFile to the given mimetype
     * Due to the fact that the BinaryPreviewProcessor scripts are called during the initial binary transfer,
     * the extraction process needs to be called again
     *
     * @return the FxContent instance
     */
    public FxPK convert() {
        if (init(false)) {
            if (content.containsXPath("/MIMETYPE") && !StringUtils.isEmpty(content.getValue("/MIMETYPE").toString())) {
                // retrieve the matching FxType and convert
                String mimeType = content.getValue("/MIMETYPE").toString();
                // retrieve a matching FxType and convert the content
                FxType destinationType = CacheAdmin.getEnvironment().getMimeTypeMatch(mimeType);
                if (destinationType.getId() == contentType.getId()) {
                    LOG.debug("No matching type for content pk " + pk + " found, content remains instance of type id " + content.getTypeId());
                    return pk;
                }

                try {
                    EJBLookup.getContentEngine().convertContentType(pk, destinationType.getId(), true, true);
                    // only call DOCUMENT / IMAGE extractors if the destination type is derived from or DOCUMENT/IMAGE itself
                    if (checkTypeOrigin(destinationType)) {
                        extractBinaryMetaData();
                    }

                } catch (FxApplicationException e) {
                    LOG.error("An error occurred during content type conversion" + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return pk;
    }

    /**
     * Check if the given FxType is a child of IMAGE or DOCUMENT (or the resp. type itself)
     * // this method will only exist for as long there are merely extractors for these given types
     *
     * @param type the Fxtype
     * @return true if the
     */
    private boolean checkTypeOrigin(FxType type) {
        return FxType.DOCUMENT.equals(type.getName()) ||
                "IMAGE".equals(type.getName()) ||
                type.getParent() != null && (type.isDerivedFrom(FxType.DOCUMENT) || type.isDerivedFrom("IMAGE"));
    }

    /**
     * Extract the metadata from the file loaded via the BinaryDescriptor
     * Incl. decision path for document and image data extraction
     */
    private void extractBinaryMetaData() {
        // reload content & retrieve BinaryDescriptor
        if (init(true)) {
            BinaryDescriptor desc = (BinaryDescriptor) content.getValue(FILEPROP).getBestTranslation();
            // based on the descriptor let's relaunch extraction of the metadata and reassign the descriptor
            if (FxType.DOCUMENT.equals(contentType.getName()) || contentType.getParent() != null && contentType.isDerivedFrom(FxType.DOCUMENT)) {
                desc = extractDocumentMetaData(desc);
                // update content and save
                updateDocumentContentInstance(desc);

            } else if ("IMAGE".equals(contentType.getName()) || contentType.getParent() != null && contentType.isDerivedFrom("IMAGE")) {
                desc = extractImageMetaData(desc);
                // update content and save
                updateImageContentInstance(desc);
            }
        }
    }

    /**
     * Extract metadata from the document (doc will be loaded from the binary storage)
     *
     * @param desc an instance of the BinaryDescriptor for the file t.b. examined
     * @return the (updated) BinaryDescriptor
     */
    private BinaryDescriptor extractDocumentMetaData(BinaryDescriptor desc) {
        // retrieve the binary
        InputStream inputStream = null;
        try {
            inputStream = FxStreamUtils.getBinaryStream(desc, BinaryDescriptor.PreviewSizes.ORIGINAL);
            final Extractor.DocumentType documentType = getDocumentType(desc.getMimeType());
            final ExtractedData extractedData = Extractor.extractData(inputStream, documentType);

            if (extractedData != null) {
                return new BinaryDescriptor(desc.getHandle(), desc.getName(), desc.getSize(),
                        desc.getMimeType(), extractedData.toXML());
            }
            
        } catch (Exception e) {
            LOG.error("Could not download binary file or create InputStream " + e.getMessage());
            e.printStackTrace();
        } finally {
            if(inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // silent death
                }
            }
        }
        return desc;
    }

    /**
     * Extract metadata from the image file (image will be loaded from the binary storage)
     *
     * @param desc an instance of the BinaryDescriptor for the file t.b. examined
     * @return the (updated) BinaryDescriptor
     */
    private BinaryDescriptor extractImageMetaData(BinaryDescriptor desc) {
        InputStream inputStream = null;
        File imageFile = null;
        try {
            imageFile = File.createTempFile("flexive-upload-", desc.getName());
            // create a File from the inputstream
            inputStream = FxStreamUtils.getBinaryStream(desc, BinaryDescriptor.PreviewSizes.ORIGINAL);
            FxFileUtils.copyStream2File(desc.getSize(), inputStream, imageFile);
            // extract the metaData
            final FxMetadata metaData = FxMediaEngine.identify(desc.getMimeType(), imageFile);

            if (metaData != null) {
                return new BinaryDescriptor(desc.getHandle(), desc.getName(), desc.getSize(),
                        desc.getMimeType(), metaData.toXML());
            }
            
        } catch (Exception e) {
            LOG.error("Could not download binary file or create InputStream " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            // delete tmp file
            if(imageFile != null) {
                imageFile.delete();
            }
        }
        return desc;
    }

    /**
     * Update the content with the extracted meta data
     * // ATM this is a callback to DocumentTypeMetaDataParser.gy (possibly changed in future revisions)
     *
     * @param desc the BinaryDescriptor
     */
    private void updateDocumentContentInstance(BinaryDescriptor desc) {
        if (desc.getMetadata() != null && desc.getMetadata().length() >= 20) {
            // let our metadata parser script do the work...
            FxScriptBinding binding = new FxScriptBinding();
            ScriptingEngine scripting = EJBLookup.getScriptingEngine();
            long scriptId = CacheAdmin.getEnvironment().getScript("DocumentTypeMetaDataParser.gy").getId();
            binding.setVariable("content", content);
            binding.setVariable("metaData", desc.getMetadata());
            try {
                FxScriptResult result = scripting.runScript(scriptId, binding);
                // retrieve updated content
                content = (FxContent)result.getBinding().getVariable("content");
                // save content
                EJBLookup.getContentEngine().save(content);
            } catch (FxApplicationException e) {
                LOG.error("updateDocumentContentInstance error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Update the content with the extracted meta data
     * // ATM this is a callback to ImageTypeMetaDataParser.gy (possibly changed in future revisions)
     *
     * @param desc the BinaryDescriptor
     */
    private void updateImageContentInstance(BinaryDescriptor desc) {
        if (desc.getMetadata() != null && desc.getMetadata().length() >= 20) {
            // let our metadata parser script do the work...
            FxScriptBinding binding = new FxScriptBinding();
            ScriptingEngine scripting = EJBLookup.getScriptingEngine();
            long scriptId = CacheAdmin.getEnvironment().getScript("ImageTypeMetaDataParser.gy").getId();
            binding.setVariable("content", content);
            binding.setVariable("desc", desc);
            try {
                FxScriptResult result = scripting.runScript(scriptId, binding);
                // retrieve updated content
                content = (FxContent) result.getBinding().getVariable("content");
                // save content
                EJBLookup.getContentEngine().save(content);
            } catch (FxApplicationException e) {
                LOG.error("updateDocumentContentInstance error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Conversion to DOCUMENT type: retrieve the actual Extractor.DocumentType
     * (see also relevant BinaryPreviewProcess script)
     *
     * @param mimeType the mime type as a String
     * @return the Extractor.DocumentType or null if not match is found
     */
    private Extractor.DocumentType getDocumentType(String mimeType) {
        if ("application/msword".equals(mimeType))
            return Extractor.DocumentType.Word;
        else if ("application/mspowerpoint".equals(mimeType))
            return Extractor.DocumentType.Powerpoint;
        else if ("application/msexcel".equals(mimeType))
            return Extractor.DocumentType.Excel;
        else if ("application/pdf".equals(mimeType))
            return Extractor.DocumentType.PDF;
        // possible future change: on-the-fly creation of relevant text type?
        else if ("text/html".equals(mimeType))
            return Extractor.DocumentType.HTML;
        else
            return null;
    }
}