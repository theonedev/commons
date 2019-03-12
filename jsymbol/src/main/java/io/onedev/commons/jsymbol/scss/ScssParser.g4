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

parser grammar ScssParser;

options { tokenVocab=ScssLexer; }

stylesheet
	: statement*
	;

statement
  : importDeclaration
  | extendDeclaration
  | nested
  | ruleset
  | mixinDeclaration
  | functionDeclaration
  | variableDeclaration
  | includeDeclaration
  | ifDeclaration
  | forDeclaration
  | whileDeclaration
  | eachDeclaration
  | functionReturn
  | ERROR expression ';'?
  | WARN expression ';'?
  | DEBUG expression ';'?
  | CHARSET expression ';'?
  | NAMESPACE expression ';'? 
  | AT_CONTENT ';'?
  ;



//Params to mixins, includes, etc
params
  : param (COMMA param)* Ellipsis?
  ;

param
  : variableName paramOptionalValue?
  ;

variableName
  : DOLLAR Identifier
  ;

paramOptionalValue
  : COLON expression+
  ;


//MIXINS
mixinDeclaration
  : '@mixin' Identifier (LPAREN params? RPAREN)? block
  ;

//Includes
includeDeclaration
  : INCLUDE Identifier (';' | (LPAREN ignoreInsideParens RPAREN ';'?)? block?)
  ;

//FUNCTIONS
functionDeclaration
  : '@function' Identifier LPAREN params? RPAREN BlockStart functionBody? BlockEnd
  ;
  
extendDeclaration
  : '@extend' selector OPTIONAL? ';'
  ;

functionBody
  : functionStatement*
  ;

functionReturn
  : '@return' commandStatement ';'
  ;

functionStatement
  : commandStatement ';' | statement
  ;


commandStatement
  : expression+
  ;

mathCharacter
  : TIMES | PLUS | DIV | MINUS | PERC | EQ | EQEQ
  ;

atIdentifier
  : SUPPORTS|PAGE|KEYFRAMES|WEBKET_KEYFRAMES|MOZ_KEYFRAMES|FONTFACE|DOCUMENT|NAMESPACE|CHARSET|MIXIN|
  		FUNCTION|AT_ELSE|AT_IF|AT_FOR|AT_WHILE|AT_EACH|INCLUDE|IMPORT|RETURN|EXTEND|WARN|DEBUG|ERROR|
  		MEDIA|AT_ROOT|AT_CONTENT|VIEWPORT
  ;
  
expression
  : '#' identifier
  | '.' identifier
  | ':' identifier
  | '$' identifier
  | '@' identifier
  | atIdentifier identifier?  
  | BACKSLASH identifier
  | identifier
  | AND
  | LPAREN ignoreInsideParens RPAREN
  | StringLiteral
  | url
  | variableName
  | functionCall
  | expression mathCharacter expression
  | identifier (DOT identifier)? PERC
  | PLUS expression
  | MINUS expression
  ;




//If statement
ifDeclaration
  : AT_IF conditions block elseIfStatement* elseStatement?
  ;

elseIfStatement
  : AT_ELSE Identifier conditions block
  ;

elseStatement
  : AT_ELSE block
  ;

conditions
  : condition (COMBINE_COMPARE conditions)?
  | Identifier
  | AND
  ;

condition
  : commandStatement (( '==' | LT | GT | '!=' | GTEQ | LTEQ) conditions)?
  | '(' ignoreInsideParens ')'
  ;

variableDeclaration
  : variableName COLON values DEFAULT? GLOBAL? ';'
  ;


//for
forDeclaration
  : AT_FOR variableName Identifier expression Identifier expression block
  ;

//while
whileDeclaration
  : AT_WHILE conditions block
  ;

//EACH
eachDeclaration
  : AT_EACH variableName (COMMA variableName)* Identifier eachValueList+ block
  ;

eachValueList
  :  Identifier 
  |	 COMMA
  |  variableName
  |  identifierListOrMap
  ;

identifierListOrMap
  : LPAREN ignoreInsideParens RPAREN
  ;

identifierValue
  : identifier (COLON values)?
  ;


//Imports
importDeclaration
	: '@import' referenceUrl mediaTypes? (COMMA referenceUrl mediaTypes?)* ';'
	;

referenceUrl
    : StringLiteral
    | UrlStart Url UrlEnd
    ;


mediaTypes
  : (Identifier (COMMA Identifier)*)
  ;




//Nested (stylesheets, etc)
nested
 	: '@' nest selectors BlockStart stylesheet BlockEnd
	;

nest
	: (Identifier | '&') Identifier* pseudo*
	;


ignoreInsideParens
	:	(~('('|')') | '(' ignoreInsideParens ')')*
	;

ignoreInsideBrackets
	:	(~('['|']') | '[' ignoreInsideParens ']')*
	;
	
ignoreInsideBraces
	:	(~('{'|'}') | '{' ignoreInsideBraces '}')*
	;
	
//Rules
ruleset
 	: selectors COLON? block
	;

block
  : BlockStart (property ';' | statement)* property? BlockEnd
  ;

selectors
	: directive | selector (COMMA selector)*
	;

directive
	: (DOCUMENT | SUPPORTS | MEDIA | PAGE | FONTFACE | KEYFRAMES | MOZ_KEYFRAMES | WEBKET_KEYFRAMES | VIEWPORT) 
		(COLON|Identifier|COMMA|LPAREN ignoreInsideParens RPAREN|url)*  
	;
	
selector
	: COLON? element (selectorPrefix? element)*
	;

selectorPrefix
  : (GT | PLUS | TIL)
  ;

element
	: ((identifier | HASH identifier | DOT identifier | PERC identifier | identifier PERC | '&' | '*' |  '>' | '+' | '~' | AT_ROOT) attrib* pseudo*) | (attrib+ pseudo*) | pseudo+  
	;

pseudo
	: (COLON|COLONCOLON) Identifier
	| (COLON|COLONCOLON) functionCall
	;

attrib
	: '[' ignoreInsideBrackets ']'
	;

attribRelate
	: '='
	| '~='
	| '|='
	;

identifier
  : Identifier
  ;

property
	: (TIMES|DOT|PLUS)? identifier COLON values IMPORTANT?
	;

values
	: commandStatement (COMMA commandStatement)*
	;

url
  : UrlStart Url UrlEnd
  ;


functionCall
	: Identifier LPAREN ignoreInsideParens RPAREN
	;
