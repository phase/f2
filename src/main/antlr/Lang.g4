grammar Lang;

// Parsing

module: externalDeclaration* EOF;

externalDeclaration
    : functionDeclaration
    | functionDefinition
    | structDeclaration
    ;

type: ID; // TODO Generics

structDeclaration
    : 'struct' ID '{' field (',' field)* '}'
    ;

field: ID ':' type;

functionDeclaration
    : ID '::' type ('->' type)* PERMISSION*
    ;

functionDefinition
    : ID ID* '=' (statement ',')* expression '.'
    ;

statement
    : fieldSetter
    | variableAssignment
    | returnStatement
    ;

variableAssignment
    : 'let' ID '=' expression
    ;

returnStatement
    : 'return' expression
    ;

fieldSetter
    : ID '.' ID '=' expression
    ;

expression
    : functionCall
    | fieldGetter
    | allocateStruct
    | ID
    ;

functionCall
    : ID '(' arguments ')'
    ;

fieldGetter
    : ID '.' ID
    ;

allocateStruct
    : type '{' arguments '}'
    ;

arguments
    : expression (',' expression)*
    ;

// Lexing

HEX: '0' ('x'|'X') HEXDIGIT+ [Ll]?;

INT: DIGIT+ [Ll]?;

fragment HEXDIGIT: ('0'..'9'|'a'..'f'|'A'..'F');

FLOAT
    : DIGIT+ '.' DIGIT* EXP? [dq]?
    | DIGIT+ EXP? [dq]?
    | '.' DIGIT+ EXP? [dq]?
    ;

fragment DIGIT: '0'..'9';
fragment EXP: ('E' | 'e') ('+' | '-')? INT;

STRING
    : '"' ( ESC | ~[\\"] )*? '"'
    | '\'' ( ESC | ~[\\'] )*? '\''
    ;

fragment ESC
    : '\\' [abtnfrv"'\\]
    | UNICODE_ESCAPE
    | HEX_ESCAPE
    | OCTAL_ESCAPE
    ;
fragment UNICODE_ESCAPE
    : '\\' 'u' HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT
    | '\\' 'u' '{' HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT '}'
    ;
fragment OCTAL_ESCAPE
    : '\\' [0-3] [0-7] [0-7]
    | '\\' [0-7] [0-7]
    | '\\' [0-7]
    ;
fragment HEX_ESCAPE
    : '\\' HEXDIGIT HEXDIGIT?
    ;

PERMISSION: '+' LETTER+;
ID: LETTER (LETTER|DIGIT|'_')*;
fragment LETTER: [a-zA-Z];

WS: [ \r\n\t\u000C]+ -> skip;
