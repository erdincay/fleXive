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
package com.flexive.core.search.cmis.parser;

import com.flexive.core.search.cmis.model.Statement;
import com.flexive.core.storage.ContentStorage;
import com.flexive.shared.exceptions.FxCmisSqlParseException;
import static com.flexive.shared.exceptions.FxCmisSqlParseException.ErrorCause;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Methods for working with the CMIS SQL parser. Includes error reporting and convenience methods for parsing
 * CMIS SQL queries.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class CmisSqlUtils {
    private static final Log LOG = LogFactory.getLog(CmisSqlUtils.class);

    private CmisSqlUtils() {
    }

    public static CommonTree parse(String query) throws FxCmisSqlParseException {
        final CmisSqlParser.ParseResult result;
        try {
            result = CmisSqlParser.parse(query);
        } catch (RecognitionException e) {
            throw translateException(query, e);
        }
        // check if we actually consumed all input
        final TokenStream tokens = result.getParser().getTokenStream();
        if (tokens.index() < tokens.size()) {
            // return unmatched input in error message
            final Token token = tokens.get(tokens.index());
            throw new FxCmisSqlParseException(LOG, ErrorCause.UNPARSED_INPUT, getUnparsedInput(query, token.getLine(), token.getCharPositionInLine()));
        }
        if (result.getParser().hasErrors()) {
            throw new FxCmisSqlParseException(LOG, ErrorCause.PARSER_MESSAGES,
                    StringUtils.join(result.getParser().getErrorMessages(), '\n')
            );
        }
        return (CommonTree) result.getReturnValue().getTree();
    }

    public static Statement buildStatement(ContentStorage storage, String query) throws FxCmisSqlParseException {
        return new StatementBuilder(storage, parse(query)).build();
    }

    private static FxCmisSqlParseException translateException(String query, RecognitionException e) {
        // TODO: add more detailed error messages
        final FxCmisSqlParseException parseExc;
        if (e instanceof NoViableAltException) {
            // standard error message when the query syntax is not correct or a keyword is missing
            parseExc = new FxCmisSqlParseException(
                    LOG,
                    ErrorCause.UNPARSED_INPUT,
                    getUnparsedInput(query, e.line, e.charPositionInLine)
            );
        } else {
            parseExc = new FxCmisSqlParseException(
                    LOG,
                    ErrorCause.RECOGNIZER_ERROR,
                    StringUtils.defaultString(e.getMessage(), e.toString()),
                    getUnparsedInput(query, e.line, e.charPositionInLine)
            );
        }
        parseExc.setStackTrace(e.getStackTrace());
        return parseExc;
    }

    private static String getUnparsedInput(String query, int line, int charPositionInLine) {
        if (line <= 0 || charPositionInLine < 0) {
            return "";
        }
        final String[] lines = query.split("\n");
        final StringBuilder out = new StringBuilder();
        // add unparsed part of the first unparsed line
        out.append(lines[line - 1].substring(charPositionInLine));
        // add rest of the query
        for (int i = line; i < lines.length; i++) {
            out.append('\n').append(lines[i]);
        }
        return out.toString();
    }


}
