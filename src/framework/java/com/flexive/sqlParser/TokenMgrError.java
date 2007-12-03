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

/**
 * Customized token error class.
 * 
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class TokenMgrError extends Error {

    private boolean EOFSeen;
    private int lexState;
    private int errorLine;
    private int errorColumn;
    private String errorAfter;
    private char curChar;

    public static enum REASON {
        LEXICAL_ERROR,
        STATIC_LEXER_ERROR,
        INVALID_LEXICAL_STATE,
        LOOP_DETECTED
    }

    /*
    * Ordinals for various reasons why an Error of this type can be thrown.
    */

    /**
     * Lexical error occured.
     */
    static final int LEXICAL_ERROR = 0;

    /**
     * An attempt wass made to create a second instance of a static token manager.
     */
    static final int STATIC_LEXER_ERROR = 1;

    /**
     * Tried to change to an invalid lexical state.
     */
    static final int INVALID_LEXICAL_STATE = 2;

    /**
     * Detected (and bailed out of) an infinite loop in the token manager.
     */
    static final int LOOP_DETECTED = 3;

    /**
     * Indicates the reason why the exception is thrown. It will have
     * one of the above 4 values.
     */
    int errorCode;

    /**
     * Replaces unprintable characters by their espaced (or unicode escaped)
     * equivalents in the given string
     * @param str the string to escape
     * @return the escape string
     */
    protected static String addEscapes(String str) {
        StringBuffer retval = new StringBuffer();
        char ch;
        for (int i = 0; i < str.length(); i++) {
            switch (str.charAt(i))
            {
                case 0 :
                    continue;
                case '\b':
                    retval.append("\\b");
                    continue;
                case '\t':
                    retval.append("\\t");
                    continue;
                case '\n':
                    retval.append("\\n");
                    continue;
                case '\f':
                    retval.append("\\f");
                    continue;
                case '\r':
                    retval.append("\\r");
                    continue;
                case '\"':
                    retval.append("\\\"");
                    continue;
                case '\'':
                    retval.append("\\\'");
                    continue;
                case '\\':
                    retval.append("\\\\");
                    continue;
                default:
                    if ((ch = str.charAt(i)) < 0x20 || ch > 0x7e) {
                        String s = "0000" + Integer.toString(ch, 16);
                        retval.append("\\u").append(s.substring(s.length() - 4, s.length()));
                    } else {
                        retval.append(ch);
                    }
            }
        }
        return retval.toString();
    }


    /**
     * Returns a detailed message for the Error when it is thrown by the
     * token manager to indicate a lexical error.
     *
     * @param EOFSeen indicates if EOF caused the lexicl error
     * @param lexState lexical state in which this error occured
     * @param errorLine line number when the error occured
     * @param errorColumn column number when the error occured
     * @param errorAfter prefix that was seen before this error occured
     * @param curChar  the offending character
     * @return the string
     */
    protected static String LexicalError(boolean EOFSeen, int lexState, int errorLine, int errorColumn, String errorAfter, char curChar) {
        return("Lexical error at line " +
                errorLine + ", column " +
                errorColumn + ".  Encountered: " +
                (EOFSeen ? "<EOF> " : ("\"" + addEscapes(String.valueOf(curChar)) + "\"") + " (" + (int)curChar + "), ") +
                "after : \"" + addEscapes(errorAfter) + "\"");
    }


    /**
     * Empty constructor
     */
    public TokenMgrError() {
    }

    /**
     * Constructor.
     *
     * @param message the message
     * @param reason the reason
     */
    public TokenMgrError(String message, int reason) {
        super(message);
        errorCode = reason;
    }

    /**
     * Constructor.
     *
     * @param EOFSeen indicates if EOF caused the lexicl error
     * @param lexState lexical state in which this error occured
     * @param errorLine line number when the error occured
     * @param errorColumn column number when the error occured
     * @param errorAfter prefix that was seen before this error occured
     * @param curChar  the offending character
     * @param reason the reason
     */
    public TokenMgrError(boolean EOFSeen, int lexState, int errorLine, int errorColumn, String errorAfter, char curChar, int reason) {
        this(LexicalError(EOFSeen, lexState, errorLine, errorColumn, errorAfter, curChar), reason);
        this.EOFSeen=EOFSeen;
        this.lexState=lexState;
        this.errorLine=errorLine;
        this.errorColumn=errorColumn;
        this.errorAfter=errorAfter;
        this.curChar=curChar;
        this.errorCode=reason;
    }

    /**
     * indicates if EOF caused the lexicl error
     *
     * @return true or false
     */
    public boolean getEofSeen() {
        return EOFSeen;
    }

    /**
     * lexical state in which this error occured.
     *
     * @return lexical state in which this error occured
     */
    public int getLexState() {
        return lexState;
    }

    /**
     * line number when the error occured
     *
     * @return line number when the error occured
     */
    public int getErrorLine() {
        return errorLine;
    }

    /**
     * column number when the error occured
     *
     * @return column number when the error occured
     */
    public int getErrorColumn() {
        return errorColumn;
    }


    /**
     * prefix that was seen before this error occured
     *
     * @return prefix that was seen before this error occured
     */
    public String getErrorAfter() {
        return errorAfter;
    }

    /**
     * the offending character
     *
     * @return the offending character
     */
    public char getCurChar() {
        return curChar;
    }

    /**
     * The offending character as escaped String.
     * <p />
     * The function replaces unprintable characters by their espaced (or unicode escaped)
     * equivalents in the given string
     *
     * @return the offending character
     */
    public String getCurCharEscaped() {
        return addEscapes(String.valueOf(curChar));
    }


    /**
     * Returns the error reason.
     *
     * @return the error reason
     */
    public REASON getReason() {
        switch(this.errorCode) {
            case LEXICAL_ERROR:
                return REASON.LEXICAL_ERROR;
            case STATIC_LEXER_ERROR:
                return REASON.STATIC_LEXER_ERROR;
            case INVALID_LEXICAL_STATE:
                return REASON.INVALID_LEXICAL_STATE;
            case LOOP_DETECTED:
                return REASON.LOOP_DETECTED;
            default:
                return REASON.LEXICAL_ERROR;
        }
    }
}
