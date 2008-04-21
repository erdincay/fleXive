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
package com.flexive.war;

import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxInvalidStateException;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Stack;

/**
 * A simple abstraction for JSON writers, including trivial
 * output format checks.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class JsonWriter {
    /**
     * Current stack of "open" parentheses ([ and {)
     */
    private Stack<Character> openSet = new Stack<Character>();
    /**
     * Tracks number of written elements per level
     */
    private Stack<Integer> numElements = new Stack<Integer>();
    private final Writer out;
    private boolean attributeValueNeeded = false;
    private final DateFormat dateFormat;

    /**
     * not completely JSON-compliant, but makes encoding XHTML much easier
     */
    private boolean singleQuotesForStrings = true;

    public JsonWriter(Writer out) {
        this(out, Locale.getDefault());
    }

    public JsonWriter(Writer out, Locale locale) {
        this.out = out;
        this.dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
    }

    public JsonWriter startArray() throws IOException {
        writeSeparator();
        out.write("[");
        pushElement('[');
        attributeValueNeeded = false;    // array may be value of an attribute
        return this;
    }

    public JsonWriter closeArray() throws IOException {
        checkClosingElement(']');
        out.write("]");
        popElement();
        return this;
    }

    public JsonWriter startMap() throws IOException {
        writeSeparator();
        out.write("{");
        pushElement('{');
        attributeValueNeeded = false;    // map may be value of an attribute
        return this;
    }

    public JsonWriter closeMap() throws IOException {
        checkClosingElement('}');
        out.write("}");
        popElement();
        return this;
    }

    public JsonWriter startAttribute(String name) throws IOException {
        checkInMap();
        writeSeparator();
        out.write("\"" + name + "\":");
        attributeValueNeeded = true;
        return this;
    }

    public JsonWriter writeAttributeValue(Object value, boolean escape) throws IOException {
        if (!attributeValueNeeded) {
            throw new FxInvalidStateException("ex.json.writer.attribute.value").asRuntimeException();
        }
        writeValue(value, escape);
        attributeValueNeeded = false;
        return this;
    }

    public JsonWriter writeAttributeValue(Object value) throws IOException {
        writeAttributeValue(value, true);
        return this;
    }

    public JsonWriter writeAttribute(String name, Object value) throws IOException {
        startAttribute(name);
        writeAttributeValue(value);
        return this;
    }

    public JsonWriter writeAttribute(String name, Object value, boolean escape) throws IOException {
        startAttribute(name);
        writeAttributeValue(value, escape);
        return this;
    }

    public JsonWriter writeLiteral(Object value) throws IOException {
        writeLiteral(value, true);
        return this;
    }

    public JsonWriter writeLiteral(Object value, boolean escapeValue) throws IOException {
        checkInArray();
        writeSeparator();
        writeValue(value, escapeValue);
        return this;
    }

    private void writeValue(Object value, boolean escapeValue) throws IOException {
        if (value instanceof String && escapeValue) {
            writeStringValue((String) value);
        } else if (value instanceof Collection) {
            startArray();
            for (Object item : (Collection<?>) value) {
                writeLiteral(item, escapeValue);
            }
            closeArray();
        } else if (value instanceof Number) {
            out.write(value.toString());
        } else if (value instanceof Date) {
            writeStringValue(dateFormat.format((Date) value));
        } else {
            out.write(String.valueOf(value));
        }
    }

    private void writeStringValue(String value) throws IOException {
        out.write(singleQuotesForStrings
                ? "'" + StringUtils.replace(value, "'", "\\'") + "'"
                : "\"" + StringUtils.replace(value, "\"", "\\\"") + "\"");
    }

    /**
     * Close the response, checking if the response is valid.
     * @return  this
     */
    public JsonWriter finishResponse() {
        if (openSet.size() > 0) {
            throw new FxInvalidStateException("ex.json.writer.state",
                    StringUtils.join(openSet.iterator(), ',')).asRuntimeException();
        }
        return this;
    }

    /**
     * Enable/disable the usage of single quotes (') for string values (default:true).
     * Using single quotes makes encoding of XHTML content much easier, since
     * XHTML attribute values have to use double quotes (").
     *
     * @param singleQuotesForStrings true to enable the usage of single quotes (') for string values
     * @return this
     */
    public JsonWriter setSingleQuotesForStrings(boolean singleQuotesForStrings) {
        this.singleQuotesForStrings = singleQuotesForStrings;
        return this;
    }

    private void writeSeparator() throws IOException {
        if (numElements.size() > 0) {
            if (numElements.lastElement() > 0 && !attributeValueNeeded) {
                out.write(",");
            }
            numElements.set(numElements.size() - 1, numElements.lastElement() + 1);
        }
    }

    private void pushElement(Character element) throws IOException {
        openSet.push(element);
        numElements.push(0);
    }

    private void popElement() {
        openSet.pop();
        numElements.pop();
    }

    private void checkClosingElement(Character element) {
        if (element != ']' && element != '}') {
            throw new FxInvalidParameterException("ELEMENT", "ex.json.writer.openBracketExp").asRuntimeException();
        }
        if (attributeValueNeeded) {
            throw new FxInvalidStateException("ex.json.writer.attribute.close").asRuntimeException();
        }
        if (openSet.size() == 0 || (element == ']' && openSet.lastElement() != '[')
                || (element == '}' && openSet.lastElement() != '{')) {
            throw new FxInvalidStateException("ex.json.writer.expected", element, openSet.lastElement()).asRuntimeException();
        }
    }

    private void checkInMap() {
        if (openSet.size() == 0 || openSet.lastElement() != '{') {
            throw new FxInvalidStateException("ex.json.writer.attribute.map").asRuntimeException();
        }
    }

    private void checkInArray() {
        if (openSet.size() == 0 || openSet.lastElement() != '[') {
            throw new FxInvalidStateException("ex.json.writer.attribute.array").asRuntimeException();
		}
	}

}
