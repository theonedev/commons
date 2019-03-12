/*
 [The "BSD licence"]
 Copyright (c) 2014 Vlad Shlosberg
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

lexer grammar ScssLexer;

COMBINE_COMPARE : '&&' | '||';

Ellipsis          : '...';

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
AND             : '&';
HASH            : '#';
COLONCOLON      : '::';
PLUS            : '+';
TIMES           : '*';
DIV             : '/';
MINUS           : '-';
PERC            : '%';
MOUT			: '^';
BACKSLASH		: '\\';


UrlStart
  : ('url'|'url-prefix'|'domain') LPAREN -> pushMode(URL_STARTED)
  ;



EQEQ            : '==';
NOTEQ           : '!=';
GTEQ			: '>=';
LTEQ			: '<=';



EQ              : '=';
PIPE_EQ         : '|=';
TILD_EQ         : '~=';



SUPPORTS		: '@supports';
PAGE			: '@page';
KEYFRAMES		: '@keyframes';
WEBKET_KEYFRAMES :	'@-webkit-keyframes';
MOZ_KEYFRAMES	 :	'@-moz-keyframes';
VIEWPORT		: '@-ms-viewport';
FONTFACE		: '@font-face';
DOCUMENT		: '@document';
NAMESPACE		: '@namespace';
CHARSET			: '@charset';
MIXIN           : '@mixin';
FUNCTION        : '@function';
AT_ELSE         : '@else';
AT_IF           : '@if';
AT_FOR          : '@for';
AT_WHILE        : '@while';
AT_EACH         : '@each';
INCLUDE         : '@include';
IMPORT          : '@import';
RETURN          : '@return';
EXTEND			: '@extend';
WARN			: '@warn';
DEBUG			: '@debug';
ERROR			: '@error';
MEDIA			: '@media';
AT_ROOT			: '@at-root';
AT_CONTENT		: '@content';

DEFAULT  		: '!default';
IMPORTANT		: '!important';
OPTIONAL		: '!optional';
GLOBAL			: '!global';

Identifier
	:	(('-'|'_' | 'a'..'z'| 'A'..'Z' | '\u0100'..'\ufffe' | '0'..'9' | INTERPOLATION)
		('_' | '-' | 'a'..'z'| 'A'..'Z' | '\u0100'..'\ufffe' | '0'..'9' | INTERPOLATION)*
	|	'-' ('_' | 'a'..'z'| 'A'..'Z' | '\u0100'..'\ufffe' | '0'..'9' | INTERPOLATION)
		('_' | '-' | 'a'..'z'| 'A'..'Z' | '\u0100'..'\ufffe' | '0'..'9' | INTERPOLATION)*)
	;


fragment INTERPOLATION
	: '#{' .*? '}'
	;

fragment STRING
  	:	'"' (~('"'|'\n'|'\r'))* '"'
  	|	'\'' (~('\''|'\n'|'\r'))* '\''
  	;

// string literals
StringLiteral
	:	STRING
	;


// Whitespace -- ignored
WS
  : (' '|'\t'|'\n'|'\r'|'\r\n')+ -> skip
  ;

// Single-line comments
SL_COMMENT
	:	'//' (~('\n'|'\r'))* -> skip
	;


// multiple-line comments
COMMENT
	:	'/*' .*? '*/' -> skip
	;

mode URL_STARTED;
UrlEnd                 : RPAREN -> popMode;
Url                    :	STRING | (~(')' | '\n' | '\r' | ';'))+;
