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
package com.flexive.sqlParser;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.exceptions.FxExceptionMessage;


public class SqlParserException extends Exception {

    Object[] params;
    private final FxLanguage errorMessageLang;

    public SqlParserException(TokenMgrError pe, String query) {
        super("ex.sqlSearch.tokenMgrException");
        params=new Object[]{pe.getErrorLine(),pe.getErrorColumn(),pe.getCurCharEscaped(),pe.getErrorAfter(),query};
        this.errorMessageLang = CacheAdmin.getEnvironment().getLanguage(FxLanguage.ENGLISH);
    }

    public SqlParserException(ParseException pe, String query) {
        super("ex.sqlSearch.parserException");
        this.errorMessageLang = CacheAdmin.getEnvironment().getLanguage(FxLanguage.ENGLISH);
        String expected = "";
        String encountered = "";

        // Expected
        int[][] expectedTokenSequences = pe.expectedTokenSequences;
        int maxSize = 0;
        for (int[] expectedTokenSequence : expectedTokenSequences) {
            if (maxSize < expectedTokenSequence.length) {
                maxSize = expectedTokenSequence.length;
            }
            for (int anExpectedTokenSequence : expectedTokenSequence) {
                expected += pe.tokenImage[anExpectedTokenSequence] + " ";
            }
            if (expectedTokenSequence[expectedTokenSequence.length - 1] != 0) {
                expected += "...";
            }
        }

        // Encountered
        Token tok = pe.currentToken.next;
        for (int i = 0; i < maxSize; i++) {
          if (i != 0) encountered += " ";
          if (tok.kind == 0) {
            encountered += pe.tokenImage[0];
            break;
          }
          encountered += pe.add_escapes(tok.image);
          tok = tok.next;
        }
        int line = pe.currentToken.next.beginLine;
        int column = pe.currentToken.next.beginColumn;
        params=new Object[]{line,column,encountered,expected,pe.getMessage(),query};
    }


    public SqlParserException(String message,Throwable cause,Object... values) {
        super(message,cause);
        this.errorMessageLang = CacheAdmin.getEnvironment().getLanguage(FxLanguage.ENGLISH);
        this.params = values;
    }


    public SqlParserException(String message,Object... values) {
        super(message);
        this.errorMessageLang = CacheAdmin.getEnvironment().getLanguage(FxLanguage.ENGLISH);
        this.params = values;
    }


    public Object[] getValues() {
        return params==null?new Object[0]:params;
    }

    @Override
    public String getMessage() {
        return new FxExceptionMessage(super.getMessage(), getValues()).getLocalizedMessage(errorMessageLang);
    }
}
