package com.flexive.shared.exceptions;

import org.apache.commons.logging.Log;

/**
 * Localized exception for CMIS SQL parsing errors. Since we do not want to propagate a dependency
 * on ANTLR's internal exceptions, the code for constructing a properly formatted exception resides
 * in the core CmisSqlUtils class.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class FxCmisSqlParseException extends FxApplicationException {
    private static final long serialVersionUID = 4199081570158005265L;

    public static enum ErrorCause {
        RECOGNIZER_ERROR("ex.cmis.sql.recognizer"),
        UNPARSED_INPUT("ex.cmis.sql.unparsedInput"),
        PARSER_MESSAGES("ex.cmis.sql.parserMessages"),
        AMBIGUOUS_COLUMN_REF("ex.cmis.sql.ambiguousColumnReference"),
        AMBIGUOUS_CONTAINS("ex.cmis.sql.ambiguousContains"),
        JOIN_ON_MV_COLUMN("ex.cmis.sql.join.multivalued"),
        JOIN_ON_MULTILANG_COLUMN("ex.cmis.sql.join.multilang"),
        EXPECTED_MVREF("ex.cmis.sql.expectedMultivaluedRef"),
        FX_EXCEPTION("ex.cmis.sql.exception");

        private final String messageKey;

        ErrorCause(String messageKey) {
            this.messageKey = messageKey;
        }

        public String getMessageKey() {
            return messageKey;
        }
    }

    private final ErrorCause errorCause;

    public FxCmisSqlParseException(Log log, ErrorCause cause, Object... args) {
        super(log, cause.getMessageKey(), args);
        this.errorCause = cause;
    }

    public FxCmisSqlParseException(Log log, FxApplicationException converted) {
        super(log, converted);
        this.errorCause = ErrorCause.FX_EXCEPTION;
    }

    public ErrorCause getErrorCause() {
        return errorCause;
    }
}
