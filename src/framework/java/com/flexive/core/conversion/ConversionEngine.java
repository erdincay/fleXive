/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.core.conversion;

import com.thoughtworks.xstream.XStream;
import com.flexive.shared.value.FxValue;
import com.flexive.core.conversion.FxValueConverter;
import com.flexive.shared.content.FxContent;

/**
 * Conversion Engine - responsible for XML Import/Export
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
public class ConversionEngine {

    /**
     * Get a XStream instance with all registered converters and aliases
     *
     * @return XStream instance
     */
    public static XStream getXStream() {
        XStream xs = new XStream();
        xs.aliasType("val", FxValue.class);
        xs.aliasType("co", FxContent.class);
        xs.registerConverter(new FxValueConverter());
        xs.registerConverter(new FxContentConverter());
        xs.registerConverter(new LifeCycleInfoConverter());
        return xs;
    }
}
