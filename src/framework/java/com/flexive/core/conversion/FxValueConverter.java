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

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxConversionException;
import com.flexive.shared.exceptions.FxStreamException;
import com.flexive.shared.interfaces.LanguageEngine;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxSelectList;
import com.flexive.shared.structure.FxSelectListItem;
import com.flexive.shared.value.*;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * XStream converter for FxValues
 * <p/>
 * XML format for e.g. a FxString:
 * <p/>
 * <code><val t="FxString" ml="1" dl="1">
 * <d l="1">text lang 1</d>
 * <d l="2">text lang 2</d>
 * </val></code>
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxValueConverter implements Converter {
    private String downloadURL = null;

    /**
     * Get the download URL for binaries
     *
     * @return download URL for binaries
     */
    public String getDownloadURL() {
        if (downloadURL != null)
            return downloadURL;
        try {
            downloadURL = EJBLookup.getDivisionConfigurationEngine().get(SystemParameters.EXPORT_DOWNLOAD_URL);
            if (!downloadURL.endsWith("/"))
                downloadURL = downloadURL + "/";
            return downloadURL;
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }

    /**
     * Marshall a FxBinary
     *
     * @param writer writer
     * @param ctx    context
     * @param value  FxBinary
     * @param lang   current language
     * @throws FxStreamException on errors
     */
    private void marshalBinary(HierarchicalStreamWriter writer, MarshallingContext ctx, FxBinary value, long lang) throws FxStreamException {
        BinaryDescriptor desc = value.getTranslation(lang);
        writer.addAttribute("fileName", desc.getName());
        writer.addAttribute("size", String.valueOf(desc.getSize()));
        writer.startNode("content");
        if (desc.isNewBinary()) {
            writer.addAttribute("content", "EMPTY");
        } else if (desc.getSize() > 500 * 1024 && ctx.get("pk") != null) {
            // > 500 KBytes add a download URL
            writer.addAttribute("content", "URL");
            try {
                writer.setValue(getDownloadURL() + "pk" + ctx.get("pk") + "/" +
                        URLEncoder.encode(FxSharedUtils.escapeXPath(value.getXPath()), "UTF-8") + "/" +
                        desc.getName());
            } catch (UnsupportedEncodingException e) {
                //unlikely to happen since UTF-8 should be available
                throw new RuntimeException(e);
            }
        } else {
            //add BASE64 encoded binary data
            writer.addAttribute("content", "Base64");
            ByteArrayOutputStream bos = new ByteArrayOutputStream((int) desc.getSize());
            desc.download(bos);
            try {
                writer.setValue(new String(Base64.encodeBase64(bos.toByteArray()), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                //unlikely to happen since UTF-8 should be available
                throw new RuntimeException(e);
            }
        }
        writer.endNode();
    }

    /**
     * Unmarshal a binary value
     *
     * @param reader XStream reader
     * @param ctx    context
     * @param v      value being processed
     * @return BinaryDescriptor
     * @throws FxStreamException on errors
     */
    private BinaryDescriptor unmarshalBinary(HierarchicalStreamReader reader, UnmarshallingContext ctx, FxValue v) throws FxStreamException {
        String fileName = reader.getAttribute("fileName");
        long size = Long.valueOf(reader.getAttribute("size"));
        BinaryDescriptor binary = null;
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            if ("content".equals(reader.getNodeName())) {
                String content = reader.getAttribute("content");
                if (StringUtils.isEmpty(content) || "EMPTY".equalsIgnoreCase(content)) {
                    binary = new BinaryDescriptor();
                } else if ("Base64".equals(content)) {
                    try {
                        binary = new BinaryDescriptor(fileName, size, new ByteArrayInputStream(Base64.decodeBase64(reader.getValue().getBytes("UTF-8"))));
                    } catch (UnsupportedEncodingException e) {
                        //can not happen really ...
                    }
                } else if ("URL".equals(content)) {
                    try {
                        URL url = new URL(reader.getValue());
                        InputStream in = null;
                        try {
                            URLConnection con = url.openConnection();
                            in = con.getInputStream();
                            binary = new BinaryDescriptor(fileName, size, in);
                        } finally {
                            if (in != null)
                                in.close();
                        }
                    } catch (Exception e) {
                        throw new FxApplicationException("ex.conversion.unmarshal.error", v.getXPath(),
                                e.getMessage() + " (" + reader.getValue() + ")").asRuntimeException();
                    }
                }
            }
            reader.moveUp();
        }
        return binary;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext ctx) {
        FxValue value = (FxValue) o;
        writer.addAttribute("t", value.getClass().getSimpleName());
        writer.addAttribute("ml", String.valueOf(value.isMultiLanguage()));
        final LanguageEngine le = EJBLookup.getLanguageEngine();
        try {
            writer.addAttribute("dl", ConversionEngine.getLang(le, value.getDefaultLanguage()));
            if (!value.isEmpty()) {
                for (long lang : value.getTranslatedLanguages()) {
                    writer.startNode("d");
                    writer.addAttribute("l", ConversionEngine.getLang(le, lang));
                    if (value instanceof FxBinary) {
                        marshalBinary(writer, ctx, (FxBinary) value, lang);
                    } else if (value instanceof FxSelectOne) { //format LISTNAME.ITEMNAME
                        final FxSelectOne v = (FxSelectOne) value;
                        writer.setValue(v.getSelectList().getName() + "." + v.getTranslation(lang).getName());
                    } else if (value instanceof FxSelectMany) { //format LISTNAME.ITEMNAME, LISTNAME.ITEMNAME
                        final FxSelectMany v = (FxSelectMany) value;
                        StringBuilder sb = new StringBuilder(500);
                        for (FxSelectListItem item : v.getTranslation(lang).getSelected())
                            sb.append(item.getList().getName()).append('.').append(item.getName()).append(", ");
                        if (sb.length() > 0)
                            sb.delete(sb.length() - 2, sb.length());
                        writer.setValue(sb.toString());
                    } else
                        writer.setValue(value.getStringValue(value.getTranslation(lang)));
                    writer.endNode();
                }
            } else
                writer.addAttribute("empty", "true");
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext ctx) {
        FxValue v;
        final LanguageEngine le = EJBLookup.getLanguageEngine();
        final String type = reader.getAttribute("t");
        try {
            boolean multiLanguage = Boolean.valueOf(reader.getAttribute("ml"));
            long defaultLanguage = ConversionEngine.getLang(le, reader.getAttribute("dl"));
            v = (FxValue) Class.forName("com.flexive.shared.value." + type).
                    getConstructor(long.class, boolean.class).newInstance(defaultLanguage, multiLanguage);
            v.setXPath(reader.getAttribute("xpath"));
            if (reader.getAttribute("empty") != null && Boolean.valueOf(reader.getAttribute("empty")))
                v.setEmpty();
            while (reader.hasMoreChildren()) {
                reader.moveDown();

                if ("d".equals(reader.getNodeName())) {
                    final long lang = ConversionEngine.getLang(le, reader.getAttribute("l"));
                    Object value;
                    if (v instanceof FxBinary) {
                        value = unmarshalBinary(reader, ctx, v);
                    } else if (v instanceof FxSelectOne) { //format LISTNAME.ITEMNAME
                        String desc = reader.getValue();
                        FxSelectList list = CacheAdmin.getEnvironment().getSelectList(desc.substring(0, desc.indexOf('.')));
                        value = list.getItem(desc.substring(desc.indexOf('.') + 1, desc.length()));
                    } else if (v instanceof FxSelectMany) { //format LISTNAME.ITEMNAME, LISTNAME.ITEMNAME, ...
                        String[] descs = reader.getValue().split(",");
                        final FxEnvironment environment = CacheAdmin.getEnvironment();
                        FxSelectList list = null;
                        SelectMany m = null;
                        for (String desc : descs) {
                            if (list == null) { //has to be the same list ....
                                list = environment.getSelectList(desc.substring(0, desc.indexOf('.')));
                                m = new SelectMany(list);
                            }
                            m.selectItem(list.getItem(desc.substring(desc.indexOf('.') + 1, desc.length())));
                        }
                        value = m;
                    } else
                        value = v.fromString(reader.getValue());
                    v.setTranslation(lang, value);
                }
                reader.moveUp();
            }
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        } catch (Exception e) {
            throw new FxConversionException(e, "ex.conversion.import.value", type, e.getMessage()).asRuntimeException();
        }
        return v;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canConvert(Class aClass) {
        return FxValue.class.isAssignableFrom(aClass);
    }
}
