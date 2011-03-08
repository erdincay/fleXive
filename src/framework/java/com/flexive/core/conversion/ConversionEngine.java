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
package com.flexive.core.conversion;

import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxGroupData;
import com.flexive.shared.content.FxPropertyData;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxConversionException;
import com.flexive.shared.structure.FxGroupAssignment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.value.FxValue;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.ACLAssignment;
import com.flexive.shared.structure.FxEnvironment;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * Conversion Engine - responsible for XML Import/Export
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ConversionEngine {

    public final static String KEY_VALUE = "value";
    public final static String KEY_CONTENT = "content";
    public static final String KEY_PROPERTY = "property";
    public static final String KEY_GROUP = "group";
    public static final String KEY_LCI = "lci";
    public final static String KEY_TYPE = "type";
    public static final String KEY_PROPERTY_AS = "propertyAssignment";
    public static final String KEY_GROUP_AS = "groupAssignment";
    public static final String KEY_ACL = "acl";
    public static final String KEY_ACLASSIGNMENT = "aclAssignment";
    public static final String SYS_LANG = "--";

    private static XStream xStream;

    /**
     * Get a XStream instance with all registered converters and aliases
     *
     * @return XStream instance
     */
    public static synchronized XStream getXStream() {
        if (xStream == null) {
            xStream = new XStream();
            xStream.aliasType(KEY_VALUE, FxValue.class);
            xStream.aliasType(KEY_CONTENT, FxContent.class);
            xStream.aliasType(KEY_PROPERTY, FxPropertyData.class);
            xStream.aliasType(KEY_GROUP, FxGroupData.class);
            xStream.aliasType(KEY_TYPE, FxType.class);
            xStream.aliasType(KEY_GROUP_AS, FxGroupAssignment.class);
            xStream.aliasType(KEY_PROPERTY_AS, FxPropertyAssignment.class);
            xStream.aliasType(KEY_ACL, ACL.class);
            xStream.aliasType(KEY_ACLASSIGNMENT, ACLAssignment.class);
            xStream.registerConverter(new FxValueConverter());
            xStream.registerConverter(new FxPropertyDataConverter());
            xStream.registerConverter(new FxGroupDataConverter());
            xStream.registerConverter(new FxContentConverter());
            xStream.registerConverter(new LifeCycleInfoConverter());
            xStream.registerConverter(new FxTypeConverter());
            xStream.registerConverter(new FxPropertyAssignmentConverter());
            xStream.registerConverter(new FxGroupAssignmentConverter());
            xStream.registerConverter(new ACLConverter());
            xStream.registerConverter(new ACLAssignmentConverter());
        }
        return xStream;
    }

    /**
     * Get language code
     *
     * @param environment   the environment
     * @param code language code as long
     * @return 2-digit ISO language code
     * @throws FxApplicationException on errors
     */
    static String getLang(FxEnvironment environment, long code) throws FxApplicationException {
        return code == 0 ? SYS_LANG : environment.getLanguage(code).getIso2digit();
    }

    /**
     * Get language code
     *
     * @param environment FxEnvironment
     * @param code 2-digit ISO language code
     * @return language code as long
     * @throws FxApplicationException on errors
     */
    static long getLang(FxEnvironment environment, String code) throws FxApplicationException {
        return SYS_LANG.equals(code) ? 0 : environment.getLanguage(code).getId();
    }

    /**
     * Get an FxValue from the reader
     *
     * @param nodeName expected node name
     * @param caller   caller object
     * @param reader   the reader
     * @param ctx      context
     * @return FxValue
     */
    static FxValue getFxValue(String nodeName, Object caller, HierarchicalStreamReader reader, UnmarshallingContext ctx) {
        //default values may not be set, and node may not exist ->return null
        if ("defaultValue".equals(nodeName)) {
            //if no child node exists anymore return null
            if (!reader.hasMoreChildren())
                return null;
            else {
                //check if the next child node is the "defaultValue" node
                reader.moveDown();
                if (!reader.getNodeName().equals(nodeName)) {
                    reader.moveUp();
                    return null;
                }
                //node found-> read value
                else {
                    FxValue value = (FxValue) ctx.convertAnother(caller, FxValue.class);
                    reader.moveUp();
                    return value;
                }
            }
        }
        if (!reader.hasMoreChildren())
            //noinspection ThrowableInstanceNeverThrown
            throw new FxConversionException("ex.conversion.missingNode", nodeName).asRuntimeException();
        reader.moveDown();
        if (!reader.getNodeName().equals(nodeName))
            //noinspection ThrowableInstanceNeverThrown
            throw new FxConversionException("ex.conversion.wrongNode", nodeName, reader.getNodeName()).asRuntimeException();
        FxValue value = (FxValue) ctx.convertAnother(caller, FxValue.class);
        reader.moveUp();
        return value;
    }
}
