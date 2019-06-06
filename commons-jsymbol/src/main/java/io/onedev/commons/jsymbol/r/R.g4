/*
 [The "BSD licence"]
 Copyright (c) 2013 Terence Parr
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/**
derived from http://svn.r-project.org/R/trunk/src/main/gram.y
http://cran.r-project.org/doc/manuals/R-lang.html#Parser

I'm no R genius but this seems to work.

Requires RFilter.g4 to strip away NL that are really whitespace,
not end-of-command. See TestR.java

Usage:

$ antlr4 R.g4 RFilter.g4
$ javac *.java
$ java TestR sample.R
... prints parse tree ...
*/
grammar R;

prog:   exprlist;

expr
	:   unaryExpr
    |   expr ('::'|':::') NL* expr
    |   expr ('$'|'@') NL* expr
    |   <assoc=right> expr '^' NL* expr
    |   ('-'|'+') NL* expr
    |   expr ':' NL* expr
    |   expr USER_OP NL* expr // anything wrappedin %: '%' .* '%'
    |   expr ('*'|'/') NL* expr
    |   expr ('+'|'-') NL* expr
    |   expr ('>'|'>='|'<'|'<='|'=='|'!=') NL* expr
    |   '!' NL* expr
    |   expr ('&'|'&&') NL* expr
    |   expr ('|'|'||') NL* expr
    |   '~' NL* expr
    |   expr '~' NL* expr
    |   expr assignment=('<-'|'<<-'|'='|':='|'->'|'->>') NL* expr
    |   FUNCTION NL* '(' ignoreInsideParens ')' NL* expr // define function
    |   '{' exprlist '}' // compound statement
    |   ifExpr
    |   ifElseExpr
    |   forExpr
    |   whileExpr
    |   repeatExpr
    |   '?' NL* expr // get help on expr, usually string or ID
    |   'next'
    |   'break'
    ;

unaryExpr
	: ID
    | STRING
    | HEX
    | INT
    | FLOAT
    | COMPLEX
    | 'NULL'
    | 'NA'
    | 'Inf'
    | 'NaN'
    | 'TRUE'
    | 'FALSE'
    | parensExpr
    | unaryExpr '[' '[' ignoreInsideBrackets ']' ']'
    | unaryExpr '[' ignoreInsideBrackets ']'
    | unaryExpr '(' ignoreInsideParens ')'              // call function
	;    

ifExpr
	: 'if' NL* '(' ignoreInsideParens ')' NL* expr
	;
	
ifElseExpr
	: 'if' NL* '(' ignoreInsideParens ')' NL* expr NL* 'else' NL* expr
	;	
	
forExpr
	: 'for' NL* '(' ignoreInsideParens ')' NL* expr
	;	
	
whileExpr
	: 'while' NL* '(' ignoreInsideParens ')' NL* expr
	;
		
repeatExpr
	: 'repeat' NL* expr
	;
			
parensExpr
	: '(' NL* expr NL* ')'
	;
	
exprlist
    :   exprSeparator* (expr (exprSeparator+ expr)* exprSeparator*)?
    ;
    
exprSeparator
	: ';' | NL
	;    

FUNCTION: 'function';

HEX :   '0' ('x'|'X') HEXDIGIT+ [Ll]? ;

INT :   DIGIT+ [Ll]? ;

fragment
HEXDIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;

FLOAT:  DIGIT+ '.' DIGIT* EXP? [Ll]?
    |   DIGIT+ EXP? [Ll]?
    |   '.' DIGIT+ EXP? [Ll]?
    ;
fragment
DIGIT:  '0'..'9' ; 
fragment
EXP :   ('E' | 'e') ('+' | '-')? INT ;

COMPLEX
    :   INT 'i'
    |   FLOAT 'i'
    ;

STRING
    :   '"' ( ESC | ~[\\"] )*? '"'
    |   '\'' ( ESC | ~[\\'] )*? '\''
    |   '`' ( ESC | ~[\\'] )*? '`'
    ;

fragment
ESC :   '\\' [abtnfrv"'\\]
    |   UNICODE_ESCAPE
    |   HEX_ESCAPE
    |   OCTAL_ESCAPE
    ;

fragment
UNICODE_ESCAPE
    :   '\\' 'u' HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT
    |   '\\' 'u' '{' HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT '}'
    ;

fragment
OCTAL_ESCAPE
    :   '\\' [0-3] [0-7] [0-7]
    |   '\\' [0-7] [0-7]
    |   '\\' [0-7]
    ;

fragment
HEX_ESCAPE
    :   '\\' HEXDIGIT HEXDIGIT?
    ;

ID  :   '.' (LETTER|'_'|'.') (LETTER|DIGIT|'_'|'.')*
    |   LETTER (LETTER|DIGIT|'_'|'.')*
    ;
    
fragment LETTER  : [a-zA-Z] ;

USER_OP :   '%' .*? '%' ;

COMMA: ',' ;

PERIOD: '.';

COMMENT: '#' (~('\n'|'\r'))* -> skip;

// Match both UNIX and Windows newlines
NL      :   '\r'? '\n' ;

WS      :   [ \t\u000C]+ -> skip ;

ignoreInsideParens
	:	(~('('|')') | '(' ignoreInsideParens ')')*
	;

ignoreInsideBrackets
	:	(~('['|']') | '[' ignoreInsideParens ']')*
	;
	
ignoreInsideBraces
	:	(~('{'|'}') | '{' ignoreInsideBraces '}')*
	;
