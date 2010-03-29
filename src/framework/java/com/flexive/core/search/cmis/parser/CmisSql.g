grammar CmisSql;

options {
	output=AST;
	ASTLabelType=CommonTree;
}

// AST tokens
tokens {
	STATEMENT;	// root node
	CREF;		// column reference
	CALIAS;		// column alias ("as ...")
	TREF;		// table reference
	TALIASDEF;	// table alias definition
	FUNCTION;
	ANYREF;		// '*'
	SORTSPEC;
	COMPOP;		// comparison condition
	NUMLIT;		// numeric literal
	CHARLIT;	// character literal
	IDENTIFIER;	// identifier
}

@header {
package com.flexive.core.search.cmis.parser;

import com.flexive.core.search.cmis.parser.CmisSqlLexer;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

}

@lexer::header {
package com.flexive.core.search.cmis.parser;
}

@lexer::members {
	@Override
	public void reportError(RecognitionException e) {
		throw new RuntimeException(e);
	}
}

@members {
	private final List<String> errorMessages = new ArrayList<String>();
	
	public static final class ParseResult {
		private final CmisSqlParser parser;
		private final statement_return returnValue;
		
		public ParseResult(CmisSqlParser parser, statement_return returnValue) {
			this.parser = parser;
			this.returnValue = returnValue;
		}
		
		public CmisSqlParser getParser() {
			return parser;
		}
		
		public statement_return getReturnValue() {
			return returnValue;
		}
	}
	
	public static ParseResult parse(String query) throws RecognitionException {
		final CmisSqlLexer lexer = new CmisSqlLexer(new ANTLRStringStream(query));
	        final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
	        final CmisSqlParser parser = new CmisSqlParser(tokenStream);
	        return new ParseResult(parser, parser.statement());
	}
	
	
	@Override
	public void emitErrorMessage(String msg) {
		errorMessages.add(msg);
	}
	
	public boolean hasErrors() {
		return !errorMessages.isEmpty();
	}
	
	public List<String> getErrorMessages() {
		return errorMessages;
	}
	/*
	@Override
	public void mismatch(IntStream input, int ttype, BitSet follow) throws RecognitionException {
		throw new MismatchedTokenException(ttype, input);
	}
	*/
	@Override
	public Object recoverFromMismatchedSet(IntStream input, RecognitionException e, BitSet follow) throws RecognitionException {
		throw e;
	}
	
}

@rulecatch {
}

statement
	:	SELECT selectList
		fromClause
		whereClause?
		orderByClause?	-> ^(STATEMENT selectList fromClause? whereClause? orderByClause?)
	;

selectList
	:	'*'					-> ^(SELECT ANYREF)
	| 	selectSublist (',' selectSublist)*	-> ^(SELECT selectSublist+);

selectSublist
	:	valueExpression ( AS columnName )?	-> ^(valueExpression ^(CALIAS columnName)?)
	|	qualifier '.*'				-> ^(ANYREF ^(TREF qualifier));
// TODO: multiValueColumnReference cannot be detected in this stage	
//	| 	multiValuedColumnReference;
	
valueExpression
	:	columnReference
	|	stringValueFun
	| 	numericValueFun;
	
columnReference
	:	(qualifier '.')? columnName 		-> ^(CREF columnName ^(TREF qualifier)?);
	
multiValuedColumnReference
	:	(qualifier '.')? multiValuedColumnName	-> ^(CREF ANY multiValuedColumnName ^(TREF qualifier)?);
	
stringValueFun
	:	(fun=UPPER | fun=LOWER) 
		'(' columnReference ')'			-> ^(FUNCTION $fun columnReference);
	
numericValueFun
	:	SCORE '(' ')'				-> ^(FUNCTION SCORE);

qualifier
	:	tableName;
// TODO: cannot choose between identifier without context
//	:	(tableName | correlationName);
	
fromClause
	:	FROM tableReference			-> ^(FROM tableReference);
	
tableReference
	:	tableId
	|	joinedTable;
	
joinedTable
	:	'(' joinedTable ')'			-> joinedTable
	|	tableId joinType? JOIN 
		tableId joinSpecification?		-> ^(JOIN tableId joinType? tableId joinSpecification?);
	
tableId	:	tableName (AS correlationName)?		-> ^(TREF tableName ^(TALIASDEF correlationName)?);

joinType
	:	INNER | LEFT | OUTER;

joinSpecification
	:	ON '(' c1=columnReference 
		'=' c2=columnReference ')'		-> ^(ON $c1 $c2);

whereClause
	:	WHERE searchCondition			-> ^(WHERE searchCondition);
	
searchCondition
	:	booleanTerm (OR^ booleanTerm)*;
	
booleanTerm
	:	booleanFactor (AND^ booleanFactor)*;	
		
booleanFactor
	:	NOT? booleanTest;

booleanTest
	:	predicate				-> predicate
	|	'(' searchCondition ')'			-> searchCondition;
	
predicate
	:	comparisonPredicate
	|	inPredicate
	|	likePredicate
	|	nullPredicate
	|	quantifiedComparisonPredicate
	|	quantifiedInPredicate
	|	textSearchPredicate
	|	folderPredicate;
	
comparisonPredicate
	:	valueExpression compOp literal		-> ^(COMPOP ^(compOp valueExpression literal));
	
compOp	:	'=' | '<>' | '<' | '>' | '<=' | '>=';

literal	:	signedNumericLiteral
	|	characterStringLiteral;
	
inPredicate
	:	columnReference 
		NOT? IN '(' inValueList ')'		-> ^(IN NOT? columnReference inValueList);
		
inValueList
	:	literal (',' literal)*			-> literal+;

likePredicate
	:	columnReference 
		NOT? LIKE characterStringLiteral	-> ^(LIKE NOT? columnReference characterStringLiteral);
	
nullPredicate
	:	columnReference IS NOT? NULL		-> ^(NULL NOT? columnReference);
// TODO: choose between column and multivalue column reference?	
//	:	(columnReference | multiValuedColumnReference) IS NOT? NULL;

quantifiedComparisonPredicate
	:	literal compOp 
		ANY multiValuedColumnReference		-> ^(COMPOP ^(compOp multiValuedColumnReference literal));
	
quantifiedInPredicate
	:	ANY multiValuedColumnReference 
		NOT? IN '(' inValueList ')'		-> ^(IN NOT? multiValuedColumnReference inValueList);
	
textSearchPredicate
	:	CONTAINS '(' (qualifier ',')? 
		textSearchExpression ')'		-> ^(CONTAINS textSearchExpression qualifier?);
		
folderPredicate
	:	(fun=IN_FOLDER | fun=IN_TREE) 
		'(' (qualifier ',')? folderId ')'		-> ^($fun folderId ^(TREF qualifier)?);
	
orderByClause
	:	ORDER BY sortSpecification 
		(',' sortSpecification)*		-> ^(ORDER sortSpecification+);
	
sortSpecification
	:	columnName (dir=ASC | dir=DESC)?	-> ^(SORTSPEC columnName $dir?);

correlationName
	:	identifier;
	
tableName
	:	identifier;
	
columnName
	:	identifier;
	
multiValuedColumnName
	:	identifier;
	
folderId:	characterStringLiteral;

textSearchExpression
	:	characterStringLiteral;
	
// TODO: implement SQL-92 rules
identifier
	:	IDENT 		-> ^(IDENTIFIER IDENT)
	| QUOTEDIDENT		-> ^(IDENTIFIER QUOTEDIDENT);
	
signedNumericLiteral
	:	INTVAL		-> ^(NUMLIT INTVAL);
	
characterStringLiteral
	:	QUOTEDSTRING	-> ^(CHARLIT QUOTEDSTRING);
	

// LEXER

// hint for case insensitive keywords taken from http://www.antlr.org/wiki/pages/viewpage.action?pageId=1782
SELECT	:	S E L E C T;
FROM	:	F R O M;
WHERE	:	W H E R E;
ORDER	:	O R D E R;
BY	:	B Y;
AS	:	A S;
UPPER	:	U P P E R;
LOWER	:	L O W E R;
SCORE	:	S C O R E;
JOIN	:	J O I N;
INNER	:	I N N E R;
LEFT	:	L E F T;
OUTER	:	O U T E R;
ON	:	O N;
OR	:	O R;
AND	:	A N D;
NOT	:	N O T;
IN	:	I N;
LIKE	:	L I K E;
IS	:	I S;
NULL	:	N U L L;
ANY	:	A N Y;
CONTAINS:	C O N T A I N S;
IN_FOLDER
	:	I N '_' F O L D E R;
IN_TREE	:	I N '_' T R E E;
ASC	:	A S C;
DESC	:	D E S C;

// case sensitive keywords:
/*SELECT	:	'SELECT';
FROM	:	'FROM';
WHERE	:	'WHERE';
ORDER	:	'ORDER';
BY	:	'BY';
AS	:	'AS';
UPPER	:	'UPPER';
LOWER	:	'LOWER';
SCORE	:	'SCORE';
JOIN	:	'JOIN';
INNER	:	'INNER';
LEFT	:	'LEFT';
OUTER	:	'OUTER';
ON	:	'ON';
OR	:	'OR';
AND	:	'AND';
NOT	:	'NOT';
IN	:	'IN';
LIKE	:	'LIKE';
IS	:	'IS';
NULL	:	'NULL';
ANY	:	'ANY';
CONTAINS:	'CONTAINS';
IN_FOLDER
	:	'IN_FOLDER';
IN_TREE	:	'IN_TREE';
ASC	:	'ASC';
DESC	:	'DESC';
*/
INTVAL	:	('0'..'9')+;
IDENT	:	('a'..'z' | 'A'..'Z') ('a'..'z'|'A'..'Z'|'0'..'9'|'_'|':')*;
QUOTEDSTRING
	:	'\'' ( ~ '\'')* '\'';
QUOTEDIDENT
	:	'"' ( ~ '"')* '"';

WS	:	(' '|'\t'|'\n'|'\r')+ { skip(); };

fragment A:('a'|'A');
fragment B:('b'|'B');
fragment C:('c'|'C');
fragment D:('d'|'D');
fragment E:('e'|'E');
fragment F:('f'|'F');
fragment G:('g'|'G');
fragment H:('h'|'H');
fragment I:('i'|'I');
fragment J:('j'|'J');
fragment K:('k'|'K');
fragment L:('l'|'L');
fragment M:('m'|'M');
fragment N:('n'|'N');
fragment O:('o'|'O');
fragment P:('p'|'P');
fragment Q:('q'|'Q');
fragment R:('r'|'R');
fragment S:('s'|'S');
fragment T:('t'|'T');
fragment U:('u'|'U');
fragment V:('v'|'V');
fragment W:('w'|'W');
fragment X:('x'|'X');
fragment Y:('y'|'Y');
fragment Z:('z'|'Z');
