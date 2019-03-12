grammar CodeAssistTest8;

query
    : criteria+ 
    ;
    
column
	: (tableName=id '.')? columnName=id
	;
	    
id
	: Identifier
	;	    

criteria
	: Identifier 'is' Value
	;
	
Value: '{{' (ESCAPE|~[{}\\])+? '}}';

Identifier
	: [a-zA-Z0-9]+
	;    

fragment
ESCAPE: '\\'[{}\\];

WS
   : ' '+ -> skip
   ;


    