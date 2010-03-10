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
package com.flexive.shared;

import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.io.Serializable;

import com.flexive.shared.content.FxPK;

/**
 * Metadata about a content instance, not attached to the content itself but to an external
 * content reference (e.g. a briefcase item).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 * @param <T> the key type
 */
public class FxReferenceMetaData<T extends Serializable> implements Serializable, Map<String, String> {
    private static final long serialVersionUID = -3470847276817832766L;
    private static final char SEPARATOR = ';';

    private final Map<String, String> attributes;
    private final T reference;

    public FxReferenceMetaData() {
        this(null);
    }

    public FxReferenceMetaData(T reference) {
        this.reference = reference;
        attributes = new LinkedHashMap<String, String>();
    }

    private FxReferenceMetaData(T key, Map<String, String> attributes) {
        this.reference = key;
        this.attributes = attributes;
    }

    public static FxReferenceMetaData createNew() {
        return new FxReferenceMetaData();
    }

    public static <T extends Serializable> FxReferenceMetaData<T> createNew(T key) {
        return new FxReferenceMetaData<T>(key);
    }

    /**
     * Return a plain string serialization of the metadata attributes. The result can be parsed by
     * {@link com.flexive.shared.FxReferenceMetaData#fromSerializedForm(java.io.Serializable, String)}.
     *
     * @return  a plain string serialization of the metadata attributes.
     */
    public String getSerializedForm() {
        final StringBuilder out = new StringBuilder();
        for (Entry<String, String> entry : attributes.entrySet()) {
            out.append(escapeSeparatorSeq(entry.getKey()))
                    .append('=')
                    .append(escapeSeparatorSeq(entry.getValue()))
                    .append(SEPARATOR);
        }
        return out.toString();
    }

    /**
     * Merges this metadata instance with another. Existing
     * values get overwritten. To remove a value, specify its key with a null or empty string.
     *
     * @param other the metadata object to be applied to this one
     */
    public void merge(FxReferenceMetaData<?> other) {
        for (Entry<String, String> entry : other.entrySet()) {
            if (entry.getValue() == null || StringUtils.isBlank(entry.getValue())) {
                attributes.remove(entry.getKey());
            } else {
                attributes.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public T getReference() {
        return reference;
    }

    public FxReferenceMetaData put(String key, Number value) {
        attributes.put(key, String.valueOf(value));
        return this;
    }

    public String get(String key) {
        return attributes.get(key);
    }

    public String get(String key, String defaultValue) {
        return attributes.containsKey(key) ? attributes.get(key) : defaultValue;
    }

    public String getString(String key, String defaultValue) {
        return get(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return getNumber(key, defaultValue).intValue();
    }

    public long getLong(String key, long defaultValue) {
        return getNumber(key, defaultValue).longValue();
    }

    public double getDouble(String key, double defaultValue) {
        return attributes.containsKey(key) ? Double.valueOf(attributes.get(key)) : defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return attributes.containsKey(key) ? Boolean.valueOf(attributes.get(key)) : defaultValue;
    }

    private Number getNumber(String key, Number defaultValue) {
        return attributes.containsKey(key) ? Long.valueOf(attributes.get(key)) : defaultValue;
    }

    private String escapeSeparatorSeq(String value) {
        return value.replace(String.valueOf(SEPARATOR), String.valueOf(SEPARATOR) + SEPARATOR)
                .replace("=", "==");
    }

    @Override
    public String toString() {
        return attributes.toString();
    }

    // ------------- Map delegates ------------------

    @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
    @Override
    public boolean equals(Object o) {
        return attributes.equals(o);
    }

    @Override
    public int hashCode() {
        return attributes.hashCode();
    }

    public int size() {
        return attributes.size();
    }

    public boolean isEmpty() {
        return attributes.isEmpty();
    }

    public String put(String key, String value) {
        return attributes.put(key, value);
    }

    public void putAll(Map<? extends String, ? extends String> m) {
        attributes.putAll(m);
    }

    public String get(Object key) {
        return attributes.get(key);
    }

    public boolean containsKey(Object key) {
        return attributes.containsKey(key);
    }

    public String remove(Object key) {
        return attributes.remove(key);
    }

    public void clear() {
        attributes.clear();
    }

    public boolean containsValue(Object value) {
        return attributes.containsValue(value);
    }

    public Set<String> keySet() {
        return attributes.keySet();
    }

    public Collection<String> values() {
        return attributes.values();
    }

    public Set<Map.Entry<String, String>> entrySet() {
        return attributes.entrySet();
    }

    /**
     * Create a new instance from metadata serialized by calling {@link FxReferenceMetaData#getSerializedForm()}.
     *
     * @param metadata  the serialized metadata
     * @return          a new metadata instance
     */
    public static <T extends Serializable> FxReferenceMetaData<T> fromSerializedForm(T key, String metadata) {
        if (StringUtils.isBlank(metadata)) {
            return new FxReferenceMetaData<T>(key);
        }
        final StringBuilder buf = new StringBuilder();
        String currentKey = null;
        final Map<String, String> attributes = new HashMap<String, String>();

        for (int i = 0; i < metadata.length(); i++) {
            final char ch = metadata.charAt(i);
            if (ch == SEPARATOR) {
                if (i == metadata.length() - 1 || metadata.charAt(i + 1) != SEPARATOR) {
                    // separator, not escaped - finish attribute
                    attributes.put(currentKey, buf.toString());
                    currentKey = null;
                    buf.setLength(0);
                } else {
                    // separator, escaped - add once
                    buf.append(SEPARATOR);
                    i++;
                }
            } else if (ch == '=') {
                if (metadata.charAt(i + 1) != '=') {
                    // "=", not escaped - store key
                    currentKey = buf.toString();
                    buf.setLength(0);
                } else {
                    // "=", escaped
                    buf.append('=');
                    i++;
                }
            } else {
                buf.append(ch);
            }
        }

        return new FxReferenceMetaData<T>(key, attributes);
    }

    /**
     * Returns the item that matches the given reference, or {@code null} if no such item was found.
     *
     * @param items     the metadata items to be queried
     * @param reference the metadata reference
     * @param <T>       the reference type
     * @return          the item that matches the given reference, or {@code null} if no such item was found.
     */
    public static <T extends Serializable> FxReferenceMetaData<T> find(List<FxReferenceMetaData<T>> items, T reference) {
        if (reference == null) {
            return null;
        }
        for (FxReferenceMetaData<T> item : items) {
            if (reference.equals(item.getReference())) {
                return item;
            }
        }
        return null;
    }

    /**
     * Returns the item that matches the given reference, or {@code null} if no such item was found.
     * Different version of the same content are treated as equal, i.e. the version information will not be
     * used in the test.
     *
     * @param items     the metadata items to be queried
     * @param reference the metadata reference
     * @param <T>       the reference type
     * @return          the item that matches the given reference, or {@code null} if no such item was found.
     */
    public static <T extends FxPK> FxReferenceMetaData<T> findByContent(List<FxReferenceMetaData<T>> items, T reference) {
        if (reference == null) {
            return null;
        }
        for (FxReferenceMetaData<T> item : items) {
            if (item.getReference() != null && reference.getId() == item.getReference().getId()) {
                return item;
            }
        }
        return null;
    }
}
