/*
 [The "MIT licence"]
 Copyright (c) 2014 Kyle Lee
 All rights reserved.
*/

parser grammar LessParser;

options { tokenVocab=LessLexer; }

stylesheet
  : statement*
  ;

statement
  : importDeclaration
  | ruleset
  | extendDeclaration ';'
  | variableDeclaration ';'
  | variableName LPAREN ignoreInsideParens RPAREN ';'
  | mixinDefinition
  | mixinReference ';'
  | CHARSET expression ';'
  | NAMESPACE expression ';' 
  ;

extendDeclaration
  : AND COLON Identifier LPAREN ignoreInsideParens RPAREN
  ;
  
variableName
  : AT variableName
  | AT Identifier
  | atIdentifier Identifier?
  ;

commandStatement
  : expression+
  ;

mathCharacter
  : TIMES | PLUS | DIV | MINUS | PERC | EQ | EQEQ
  ;

atIdentifier
  : WEBKET_KEYFRAMES|MOZ_KEYFRAMES|SUPPORTS|PAGE|KEYFRAMES|FONTFACE|DOCUMENT|NAMESPACE|
  		CHARSET|IMPORT|MEDIA|ARGUMENTS|REST|VIEWPORT
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
  | expression mathCharacter expression
  | identifier (DOT identifier)? PERC
  | PLUS expression
  | MINUS expression
  | ARGUMENTS
  ;

function
  : Identifier LPAREN values? RPAREN
  ;

conditions
  : condition ((Identifier|COMMA) condition)*
  ;

condition
  : Identifier? LPAREN ignoreInsideParens RPAREN
  ;

conditionStatement
  : commandStatement ( EQ | LT | GT | GTEQ | LTEQ ) commandStatement
  | commandStatement
  ;

variableDeclaration
  : variableName COLON (values | block)
  ;

//Imports
importDeclaration
	: '@import' (LPAREN ignoreInsideParens RPAREN)? referenceUrl mediaTypes? (COMMA referenceUrl mediaTypes?)* ';'
	;

referenceUrl
    : StringLiteral
    | UrlStart Url UrlEnd
    ;

mediaTypes
  : (Identifier (COMMA Identifier)*)
  ;

//Rules
ruleset
   : selectors block
  ;

block
  : BlockStart (property ';' | statement | mixinReference ';')* (property|mixinReference)? BlockEnd
  ;

mixinDefinition
  : selectors LPAREN ignoreInsideParens RPAREN mixinGuard? block
  ;

mixinGuard
  : Identifier conditions
  ;

mixinDefinitionParam
  : variableName
  | variableDeclaration
  ;

mixinReference
  : selector (LPAREN ignoreInsideParens RPAREN)? IMPORTANT?
  ;

selectors
	: directive | selector (COMMA selector)*
	;

directive
	: (DOCUMENT | SUPPORTS | MEDIA | PAGE | FONTFACE | KEYFRAMES | WEBKET_KEYFRAMES | MOZ_KEYFRAMES | VIEWPORT) 
		(COLON|Identifier|COMMA|LPAREN ignoreInsideParens RPAREN|url)*  
	;
	
selector
	: COLON? element (selectorPrefix? element)*
	;

attrib
  : '[' ignoreInsideBrackets ']'
  ;

pseudo
  : (COLON|COLONCOLON) Identifier (LPAREN ignoreInsideParens RPAREN)?
  ;

element
	: ((identifier | HASH identifier | DOT identifier | PERC identifier | identifier PERC | '&' | '*' | '>' | '+' | '~') attrib* pseudo*) | (attrib+ pseudo*)  
	;

selectorPrefix
  : (GT | PLUS | TIL)
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
	: (TIMES|DOT|PLUS)? identifier (PLUS|PLUS UNDERSCORE)? COLON values IMPORTANT?
	;

values
  : commandStatement (COMMA commandStatement)*
  ;

url
  : UrlStart Url UrlEnd
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
  