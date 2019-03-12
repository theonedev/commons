grammar CodeAssistTest7;

query
    : criteria1 'order-by' EOF
    | criteria2 EOF
    ;
    
query2
	: Created
	| CreatedBy
	| CreatedByMe
	; 
	
query3
	: 'issues' WS+ CreatedByMe
	;	   
    
criteria1
	: criteria
	;

criteria2
	: criteria
	;

criteria
	: 'created-by-me'
    | criteria 'and' criteria	
    | criteria 'or' criteria
    ;

Created
	: 'created'
	;    
	
CreatedBy
	: 'created' WS+ 'by'
	;    	
	
CreatedByMe
	: 'created' WS+ 'by' WS+ 'me'
	;	
	
WS
	: ' '
	;	