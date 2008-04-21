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
package com.flexive.shared.media;

import java.awt.color.ICC_Profile;

/**
 * Metadata for images
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public abstract class FxImageMetadata extends FxMetadata {

    /**
     * Get the image width
     *
     * @return image width
     */
    public abstract int getWidth();

    /**
     * Get the image height
     *
     * @return image height
     */
    public abstract int getHeight();

    /**
     * Get the format (eg "JPEG")
     *
     * @return format
     */
    public abstract String getFormat();

    /**
     * Get a more descriptive name of the format (eg "JPEG (Joint Photographic Experts Group) Format")
     *
     * @return long format
     */
    public abstract String getFormatDescription();

    /**
     * Get the compression algorithm used (eg "JPEG")
     *
     * @return compression algorithm used
     */
    public abstract String getCompressionAlgorithm();

    /**
     * Get the X-Resolution in DPI
     *
     * @return X-Resolution in DPI
     */
    public abstract double getXResolution();

    /**
     * Get the Y-Resolution in DPI
     *
     * @return Y-Resolution in DPI
     */
    public abstract double getYResolution();

    /**
     * Get the used color type (eg "RGB")
     *
     * @return used color type
     */
    public abstract String getColorType();

    /**
     * Does the image use its own color palette?
     *
     * @return use own color palette?
     */
    public abstract boolean usePalette();

    /**
     * Get the bits per pixel used (eg "24")
     *
     * @return bits per pixel used
     */
    public abstract int getBitsPerPixel();

    /**
     * Is this image progressive scan?
     *
     * @return progressive scan?
     */
    public abstract boolean isProgressive();

    /**
     * Does the image use a transparent background?
     *
     * @return transparency?
     */
    public abstract boolean isTransparent();

    /**
     * Is an ICC profile attached to this image?
     *
     * @return ICC profile attached?
     */
    public abstract boolean hasICC_Profile();

    /**
     * Get the ICC profile if one is attached, else <code>null</code>
     *
     * @return ICC profile if attached
     */
    public abstract ICC_Profile getICC_Profile();
}
