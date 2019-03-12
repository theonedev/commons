/*
 [The "MIT License"]
 Copyright (c) 2014 Kyle Lee
 All rights reserved.
*/

lexer grammar LessLexer;

Identifier
	:	(('_' | 'a'..'z'| 'A'..'Z' | '\u0100'..'\ufffe' | '0'..'9' | INTERPOLATION)
		('_' | '-' | 'a'..'z'| 'A'..'Z' | '\u0100'..'\ufffe' | '0'..'9' | INTERPOLATION)*
	|	'-' ('_' | 'a'..'z'| 'A'..'Z' | '\u0100'..'\ufffe' | '0'..'9' | INTERPOLATION)
		('_' | '-' | 'a'..'z'| 'A'..'Z' | '\u0100'..'\ufffe' | '0'..'9' | INTERPOLATION)*)
	;


fragment INTERPOLATION
	: '@{' .*? '}'
	;

Ellipsis: '...';

//Separators
LPAREN          : '(';
RPAREN          : ')';
BlockStart      : '{';
BlockEnd        : '}';
LBRACK          : '[';
RBRACK          : ']';
GT              : '>';
TIL             : '~';

LT              : '<';
COLON           : ':';
SEMI            : ';';
COMMA           : ',';
DOT             : '.';
DOLLAR          : '$';
AT              : '@';
AND 		    : '&';
HASH            : '#';
COLONCOLON      : '::';
PLUS            : '+';
TIMES           : '*';
DIV             : '/';
MINUS           : '-';
PERC            : '%';
UNDERSCORE		: '_';
MOUT			: '^';
BACKSLASH		: '\\';

EQEQ            : '==';
GTEQ            : '>=';
LTEQ            : '<=';
NOTEQ           : '!=';
EQ              : '=';
PIPE_EQ         : '|=';
TILD_EQ         : '~=';

UrlStart
  : ('url'|'url-prefix'|'domain') LPAREN -> pushMode(URL_STARTED)
  ;

WEBKET_KEYFRAMES :	'@-webkit-keyframes';
MOZ_KEYFRAMES	 :	'@-moz-keyframes';
SUPPORTS		: '@supports';
VIEWPORT		: '@-ms-viewport';
PAGE			: '@page';
KEYFRAMES		: '@keyframes';
FONTFACE		: '@font-face';
DOCUMENT		: '@document';
NAMESPACE		: '@namespace';
CHARSET			: '@charset';
IMPORT          : '@import';

MEDIA           : '@media';
ARGUMENTS       : '@arguments';
REST            : '@rest';

IMPORTANT       : '!important';


fragment STRING
  :  '"' (~('"'|'\n'|'\r'))* '"'
  |  '\'' (~('\''|'\n'|'\r'))* '\''
  ;

// string literals
StringLiteral
  :  '~'? STRING
  ;


// Whitespace -- ignored
WS
  : (' '|'\t'|'\n'|'\r'|'\r\n')+ -> skip
  ;

// Single-line comments
SL_COMMENT
  :  '//' (~('\n'|'\r'))* -> skip
  ;


// multiple-line comments
COMMENT
  :  '/*' .*? '*/' -> skip
  ;

mode URL_STARTED;
UrlEnd                 : RPAREN -> popMode;
Url                    :  STRING | (~(')' | '\n' | '\r' | ';'))+;
