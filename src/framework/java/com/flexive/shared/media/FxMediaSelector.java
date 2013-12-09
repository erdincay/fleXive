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
package com.flexive.shared.media;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.value.BinaryDescriptor;
import org.apache.commons.lang.StringUtils;

import java.awt.*;
import java.io.Serializable;

/**
 * A media selector by primary key, xpath etc. of an instance with optional operation requests like scaling or flipping
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
public class FxMediaSelector implements Serializable {
    private static final long serialVersionUID = -968784502929662771L;

    private FxPK pk = null;
    private Boolean useType = null;
    private String xp = null;
    private String lang = null;
    private Boolean langFallback = null;
    private BinaryDescriptor.PreviewSizes size = BinaryDescriptor.PreviewSizes.ORIGINAL;
    private boolean forceImage;
    private int scaleWidth = -1, scaleHeight = -1;
    private int rotationAngle = 0;
    private Boolean flipHorizontal = null, flipVertical = null;
    private Rectangle crop = null;
    private String filename = null; //filename is last parameter (optional) if it contains a dot and does not match another parameter
    private long timestamp = 0; //optional timestamp to apply
    private boolean applyManipulations = false; //are manipulations to be applied?

    /**
     * Empty Ctor
     */
    public FxMediaSelector() {
    }

    /**
     * Ctor
     *
     * @param pk primary key
     */
    public FxMediaSelector(FxPK pk) {
        this.pk = pk;
    }

    /**
     * Setter for the primary key
     *
     * @param pk primary key
     * @return this object, useful for chaining
     */
    public FxMediaSelector setPK(FxPK pk) {
        this.pk = pk;
        return this;
    }

    /**
     * Set the flag to use the assignments default binary image from the type if present
     *
     * @param useType use the assignments default binary image from the type if present
     * @return this object, useful for chaining
     */
    public FxMediaSelector setUseType(Boolean useType) {
        this.useType = useType;
        return this;
    }

    /**
     * Setter for the XPath
     *
     * @param xp xpath
     * @return this object, useful for chaining
     */
    public FxMediaSelector setXPath(String xp) {
        this.xp = xp;
        return this;
    }

    /**
     * Setter for the language
     *
     * @param lang language
     * @return this object, useful for chaining
     */
    public FxMediaSelector setLang(String lang) {
        this.lang = lang;
        return this;
    }

    /**
     * Use the language fallback to the default language if desired language does not exist?
     *
     * @param langFallback this object, useful for chaining
     * @return this object, useful for chaining
     */
    public FxMediaSelector setLangFallback(Boolean langFallback) {
        this.langFallback = langFallback;
        return this;
    }

    /**
     * Set the preview or original size to use
     *
     * @param size preview or original size
     * @return this object, useful for chaining
     */
    public FxMediaSelector setSize(BinaryDescriptor.PreviewSizes size) {
        this.size = size;
        return this;
    }

    /**
     * Set a width to scale to
     *
     * @param scaleWidth width to scale to
     * @return this object, useful for chaining
     */
    public FxMediaSelector setScaleWidth(int scaleWidth) {
        this.scaleWidth = scaleWidth;
        if (scaleWidth != -1)
            applyManipulations = true;
        return this;
    }

    /**
     * Set a height to scale to
     *
     * @param scaleHeight height to scale to
     * @return this object, useful for chaining
     */
    public FxMediaSelector setScaleHeight(int scaleHeight) {
        this.scaleHeight = scaleHeight;
        if (scaleHeight != -1)
            applyManipulations = true;
        return this;
    }

    /**
     * Rotate the image by the given degrees
     *
     * @param angle degrees to rotate
     * @return this object, useful for chaining
     */
    public FxMediaSelector setRotationAngle(int angle) {
        this.rotationAngle = angle % 360;
        if (angle % 360 != 0)
            applyManipulations = true;
        return this;
    }

    /**
     * Flip the image horizontal?
     *
     * @param flipH flip horizontal?
     * @return this object, useful for chaining
     */
    public FxMediaSelector setFlipHorizontal(Boolean flipH) {
        this.flipHorizontal = flipH;
        applyManipulations = true;
        return this;
    }

    /**
     * Flip the image vertical?
     *
     * @param flipV flip vertical?
     * @return this object, useful for chaining
     */
    public FxMediaSelector setFlipVertical(Boolean flipV) {
        this.flipVertical = flipV;
        applyManipulations = true;
        return this;
    }

    /**
     * Crop the image to the given rectangle
     *
     * @param crop rectangle to crop
     * @return this object, useful for chaining
     */
    public FxMediaSelector setCrop(Rectangle crop) {
        this.crop = crop;
        if (crop != null)
            applyManipulations = true;
        return this;
    }

    /**
     * Is a crop operation requested?
     *
     * @return crop operation requested?
     */
    public boolean isCrop() {
        return this.crop != null;
    }

    /**
     * Set the filename to append to a link
     *
     * @param filename filename to append to a link
     * @return this object, useful for chaining
     */
    public FxMediaSelector setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    /**
     * Is a timestamp for a link requested?
     *
     * @return timestamp requested
     */
    public boolean hasTimestamp() {
        return timestamp > 0;
    }

    /**
     * Get the timestamp to append to links
     *
     * @return timestamp to append to links
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Set the timestamp to append to links
     *
     * @param timestamp timestamp to append to links
     * @return this object, useful for chaining
     */
    public FxMediaSelector setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Get the filename to append to links
     *
     * @return filename to append to links
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Append a filename to links?
     *
     * @return append a filename to links
     */
    public boolean hasFilename() {
        return filename != null && StringUtils.isNotBlank(filename);
    }

    /**
     * Use the assignments default binary image from the type if present?
     * @return use the assignments default binary image from the type if present
     */
    public Boolean isUseType() {
        if( useType == null )
            return false;
        return useType;
    }

    /**
     * Get the rotation angle
     *
     * @return rotation angle
     */
    public int getRotationAngle() {
        return rotationAngle;
    }

    /**
     * Flip horizontal?
     *
     * @return flip horizontal?
     */
    public Boolean isFlipHorizontal() {
        return flipHorizontal != null && flipHorizontal;
    }

    /**
     * Flip vertical?
     *
     * @return flip vertical?
     */
    public Boolean isFlipVertical() {
        return flipVertical != null && flipVertical;
    }

    /**
     * Get the crop region if set
     *
     * @return crop region
     */
    public Rectangle getCrop() {
        return crop;
    }

    /**
     * Perform a crop?
     *
     * @return perform a crop?
     */
    public boolean useCrop() {
        return crop != null;
    }

    /**
     * Is a primary key set?
     *
     * @return primary key set
     */
    public boolean hasPK() {
        return pk != null;
    }

    /**
     * Get the XPath of the property containing the image
     *
     * @return XPath of the property containing the image
     */
    public String getXPath() {
        return xp;
    }

    /**
     * Get the primary key
     *
     * @return primary key
     */
    public FxPK getPK() {
        return pk;
    }

    /**
     * Get the requested (pre-scaled) thumbnail size (0..4)
     *
     * @return requested (pre-scaled) thumbnail size (0..4)
     */
    public BinaryDescriptor.PreviewSizes getSize() {
        return size;
    }

    /**
     * Get the iso code of the desired translation for multilingual images
     *
     * @return iso code of the desired translation
     */
    public String getLanguageIso() {
        return lang;
    }

    /**
     * Fallback to the default language if no image present for the requested?
     *
     * @return fallback to the default language?
     */
    public boolean useLangFallback() {
        return langFallback != null && langFallback;
    }

    /**
     * Get the requested language for the image
     *
     * @return requested language
     * @throws com.flexive.shared.exceptions.FxApplicationException
     *          on errors
     */
    public FxLanguage getLanguage() throws FxApplicationException {
        return StringUtils.isNotBlank(lang) ? CacheAdmin.getEnvironment().getLanguage(lang) : null;
    }

    /**
     * Scale to a given height?
     *
     * @return scale to a given height?
     */
    public boolean isScaleHeight() {
        return scaleHeight >= 0;
    }

    /**
     * Get the height to scale to
     *
     * @return height to scale to
     */
    public int getScaleHeight() {
        return scaleHeight;
    }

    /**
     * Scale to a given width?
     *
     * @return scale to a given width?
     */
    public boolean isScaleWidth() {
        return scaleWidth >= 0;
    }

    /**
     * Get the width to scale to
     *
     * @return width to scale to
     */
    public int getScaleWidth() {
        return scaleWidth;
    }

    public boolean isApplyManipulations() {
        return applyManipulations;
    }

    /**
     * If the binary does not represent an image and the "original" preview size was chosen,
     * the biggest thumbnail is returned instead.
     *
     * @return  if the returned binary must be an image
     */
    public boolean isForceImage() {
        return forceImage;
    }

    public void setForceImage(boolean forceImage) {
        this.forceImage = forceImage;
    }
}
