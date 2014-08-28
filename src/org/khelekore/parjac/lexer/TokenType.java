package org.khelekore.parjac.lexer;

public enum TokenType {
    SUB ("\u001a"),  // only allowed at end of input and should be ignored

    // Whitespace, note that line terminators are also whitespace
    SPACE (" "),
    TAB ("\t"),
    FORM_FEED ("\f"),

    // LineTerminator:
    LF ("\n"),
    CR ("\r"),
    CRLF ("\r\n"),

    // Null Literal
    NULL ("null"),

    // Boolean literals
    TRUE ("true"),
    FALSE ("false"),

    // Separators
    LEFT_PARANTHESIS ("("),
    RIGHT_PARANTHESIS (")"),
    LEFT_CURLY ("{"),
    RIGHT_CURLY ("}"),
    LEFT_BRACKET ("["),
    RIGHT_BRACKET ("]"),
    SEMICOLON (";"),
    COMMA (","),
    DOT ("."),
    ELLIPSIS ("..."),
    AT ("@"),
    DOUBLE_COLON ("::"),

    // key words
    ABSTRACT ("abstract"),
    ASSERT ("assert"),
    BOOLEAN ("boolean"),
    BREAK ("break"),
    BYTE ("byte"),
    CASE ("case"),
    CATCH ("catch"),
    CHAR ("char"),
    CLASS ("class"),
    CONST ("const"),            // reserved, but not used
    CONTINUE ("continue"),
    DEFAULT ("default"),
    DO ("do"),
    DOUBLE ("double"),
    ELSE ("else"),
    ENUM ("enum"),
    EXTENDS ("extends"),
    FINAL ("final"),
    FINALLY ("finally"),
    FLOAT ("float"),
    FOR ("for"),
    GOTO ("goto"),              // reserved, but not used
    IF ("if"),
    IMPLEMENTS ("implements"),
    IMPORT ("import"),
    INSTANCEOF ("instanceof"),
    INT ("int"),
    INTERFACE ("interface"),
    LONG ("long"),
    NATIVE ("native"),
    NEW ("new"),
    PACKAGE ("package"),
    PRIVATE ("private"),
    PROTECTED ("protected"),
    PUBLIC ("public"),
    RETURN ("return"),
    SHORT ("short"),
    STATIC ("static"),
    STRICTFP ("strictfp"),
    SUPER ("super"),
    SWITCH ("switch"),
    SYNCHRONIZED ("synchronized"),
    THIS ("this"),
    THROW ("throw"),
    THROWS ("throws"),
    TRANSIENT ("transient"),
    TRY ("try"),
    VOID ("void"),
    VOLATILE ("volatile"),
    WHILE ("while"),

    // operators
    EQUAL ("="),
    GT (">"),
    LT ("<"),
    NOT ("!"),
    TILDE ("~"),
    QUESTIONMARK ("?"),
    COLON (":"),
    ARROW ("->"),
    DOUBLE_EQUAL ("=="),
    GE (">="),
    LE ("<="),
    NOT_EQUAL ("!="),
    LOGICAL_AND ("&&"),
    LOGICAL_OR ("||"),
    INCREMENT ("++"),
    DECREMENT ("--"),
    PLUS ("+"),
    MINUS ("-"),
    MULTIPLY ("*"),
    DIVIDE ("/"),
    BIT_AND ("&"),
    BIT_OR ("|"),
    BIT_XOT ("^"),
    REMAINDER ("%"),
    LEFT_SHIFT ("<<"),
    RIGHT_SHIFT (">>"),
    RIGHT_SHIFT_UNSIGNED (">>>"),
    PLUS_EQUALS ("+="),
    MINUS_EQUALS ("-="),
    MULTIPLY_EQUALS ("*="),
    DIVISION_EQUALS ("/="),
    AND_EQUALS ("&="),
    OR_EQUALS ("|="),
    XOR_EQUALS ("^="),
    REMAINDER_EQUALS ("%="),
    LEFT_SHIFT_EQUALS ("<<="),
    RIGHT_SHIFT_EQUALS (">>="),
    RIGHT_SHIFT_UNSIGNED_EQUALS (">>>="),

    // comments
    TRADITIONAL_COMMENT_START ("/*"),
    JAVADOC_COMMENT_START ("/**"),
    MULTILINE_COMMENT_END ("*/"),
    ONELINE_COMMENT_START ("//"),

    IDENTIFIER ("identifier"),

    INTEGER_LITERAL ("integer literal"),
    FLOATINGPOINT_LITERAL ("floatingpoint literal"),
    CHARACTER_LITERAL ("character literal"),
    STRING_LITERAL ("string literal");

    private final String value;

    private TokenType (String value) {
	this.value = value;
    }

    public boolean isWhitespace () {
	return this == SPACE || this == TAB || this == FORM_FEED ||
	    this == LF || this == CR || this == CRLF;
    }
}
