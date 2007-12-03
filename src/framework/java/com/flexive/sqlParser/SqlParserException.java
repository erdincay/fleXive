/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
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
package com.flexive.sqlParser;


public class SqlParserException extends Exception {

    Object[] params;

    public SqlParserException(TokenMgrError pe) {
        super("ex.sqlSearch.tokenMgrException");
        params=new Object[]{pe.getErrorLine(),pe.getErrorColumn(),pe.getCurCharEscaped(),pe.getErrorAfter()};


    }

    public SqlParserException(ParseException pe) {
        super("ex.sqlSearch.parserException");
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
        params=new Object[]{line,column,encountered,expected};
    }


    public SqlParserException(String message,Throwable cause,Object... values) {
        super(message,cause);
        this.params = values;
    }


    public SqlParserException(String message,Object... values) {
        super(message);
        this.params = values;
    }


    public Object[] getValues() {
        return params==null?new Object[0]:params;
    }

}
