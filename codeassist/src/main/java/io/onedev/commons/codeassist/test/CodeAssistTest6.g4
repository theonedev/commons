grammar CodeAssistTest6;

rule1: Keyword1;

rule2: 'ab' WS 'cd';

rule3: (Keyword2|Keyword3) WS+;

WS: ' ';

Keyword1: 'ab' WS+ 'cd';

Keyword2: 'is';

Keyword3: 'is' WS+ 'not';

