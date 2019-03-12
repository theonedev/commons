grammar CodeAssistTest9;

query
    : WS* criteria WS* EOF
    ;

criteria
    : Quoted WS+ Is WS+ Quoted
    ;

Is
	: 'is'
	;

Quoted
    : '"' (ESCAPE|~["\\])+? '"'
    ;
	
WS
    : ' '
    ;

fragment
ESCAPE
    : '\\'["\\]
    ;
