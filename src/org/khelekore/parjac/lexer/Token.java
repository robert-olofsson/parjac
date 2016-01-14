package org.khelekore.parjac.lexer;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum Token {
    SUB ("\u001a"),  // only allowed at end of input and should be ignored

    // Whitespace, note that line terminators are also whitespace
    WHITESPACE ("whitespace"), // ' ', '\t', '\f'

    // LineTerminator:
    LF ("line feed"),
    CR ("carrige return"),
    CRLF ("carrige return, line feed"),

    // Separators
    LEFT_PARENTHESIS ("("),
    RIGHT_PARENTHESIS (")"),
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
    AND ("&"),
    OR ("|"),
    XOR ("^"),
    REMAINDER ("%"),
    LEFT_SHIFT ("<<"),
    RIGHT_SHIFT (">>"),
    RIGHT_SHIFT_UNSIGNED (">>>"),
    PLUS_EQUAL ("+="),
    MINUS_EQUAL ("-="),
    MULTIPLY_EQUAL ("*="),
    DIVIDE_EQUAL ("/="),
    BIT_AND_EQUAL ("&="),
    BIT_OR_EQUAL ("|="),
    BIT_XOR_EQUAL ("^="),
    REMAINDER_EQUAL ("%="),
    LEFT_SHIFT_EQUAL ("<<="),
    RIGHT_SHIFT_EQUAL (">>="),
    RIGHT_SHIFT_UNSIGNED_EQUAL (">>>="),

    // comments
    MULTILINE_COMMENT ("/* ... */"),
    ONELINE_COMMENT ("// ..."),

    IDENTIFIER ("identifier"),

    // Literal
    INT_LITERAL ("int_literal"),
    LONG_LITERAL ("long_literal"),
    FLOAT_LITERAL ("float_literal"),
    DOUBLE_LITERAL ("double_literal"),
    CHARACTER_LITERAL ("character_literal"),
    STRING_LITERAL ("string_literal"),
    NULL ("null"),
    TRUE ("true"),
    FALSE ("false"),

    // Signal lexer error
    ERROR ("error"),

    // Signal end of input
    END_OF_INPUT ("<end_of_input>");

    private final String description;

    private Token (String description) {
	this.description = description;
    }

    private static final EnumSet<Token> whitespaces =
    EnumSet.of (WHITESPACE, LF, CR, CRLF, ONELINE_COMMENT, MULTILINE_COMMENT);

    private static final EnumSet<Token> operators =
    EnumSet.of (EQUAL, GT, LT, NOT, TILDE, QUESTIONMARK, COLON, ARROW, DOUBLE_EQUAL,
		GE, LE, NOT_EQUAL, LOGICAL_AND, LOGICAL_OR, INCREMENT, DECREMENT,
		PLUS, MINUS, MULTIPLY, DIVIDE, AND, OR, XOR,
		REMAINDER, LEFT_SHIFT, RIGHT_SHIFT, RIGHT_SHIFT_UNSIGNED,
		PLUS_EQUAL, MINUS_EQUAL, MULTIPLY_EQUAL, DIVIDE_EQUAL,
		BIT_AND_EQUAL, BIT_OR_EQUAL, BIT_XOR_EQUAL, REMAINDER_EQUAL,
		LEFT_SHIFT_EQUAL, RIGHT_SHIFT_EQUAL, RIGHT_SHIFT_UNSIGNED_EQUAL,
		INSTANCEOF);

    private static final EnumSet<Token> logicalOperators =
    EnumSet.of (GT, LT, DOUBLE_EQUAL, GE, LE, NOT_EQUAL, LOGICAL_AND, LOGICAL_OR, INSTANCEOF);

    /** Operators that automatically widen both sides to be the widest type used */
    private static final EnumSet<Token> autoWideOperators =
    EnumSet.of (AND, XOR, OR, PLUS, MINUS, MULTIPLY, DIVIDE, REMAINDER);

    private static final EnumSet<Token> bitOperators =
    EnumSet.of (AND, XOR, OR, LEFT_SHIFT, RIGHT_SHIFT, RIGHT_SHIFT_UNSIGNED);

    private static final EnumSet<Token> assignmentOperators =
    EnumSet.of (EQUAL, MULTIPLY_EQUAL, DIVIDE_EQUAL, REMAINDER_EQUAL, PLUS_EQUAL, MINUS_EQUAL,
		LEFT_SHIFT_EQUAL, RIGHT_SHIFT_EQUAL, RIGHT_SHIFT_UNSIGNED_EQUAL,
		BIT_AND_EQUAL, BIT_XOR_EQUAL, BIT_OR_EQUAL);

    private static final EnumSet<Token> keywords =
    EnumSet.of (ABSTRACT, ASSERT, BOOLEAN, BREAK, BYTE, CASE, CATCH, CHAR, CLASS, CONST,
		CONTINUE, DEFAULT, DO, DOUBLE, ELSE, ENUM, EXTENDS, FINAL, FINALLY, FLOAT,
		FOR, GOTO, IF, IMPLEMENTS, IMPORT, INSTANCEOF, INT, INTERFACE, LONG,
		NATIVE, NEW, PACKAGE, PRIVATE, PROTECTED, PUBLIC, RETURN, SHORT, STATIC,
		STRICTFP, SUPER, SWITCH, SYNCHRONIZED, THIS, THROW, THROWS, TRANSIENT,
		TRY, VOID, VOLATILE, WHILE);

    private static final EnumSet<Token> literals =
    EnumSet.of (INT_LITERAL, LONG_LITERAL, FLOAT_LITERAL, DOUBLE_LITERAL,
		CHARACTER_LITERAL, STRING_LITERAL, NULL, TRUE, FALSE);

    private static final EnumSet<Token> separator =
    EnumSet.of (LEFT_PARENTHESIS, RIGHT_PARENTHESIS, LEFT_CURLY, RIGHT_CURLY,
		LEFT_BRACKET, RIGHT_BRACKET, SEMICOLON, COMMA, DOT, ELLIPSIS,
		AT, DOUBLE_COLON);

    private static final EnumSet<Token> primitives =
    EnumSet.of (BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE, BOOLEAN);

    private static final EnumSet<Token> modifiers =
    EnumSet.of (PUBLIC, PROTECTED, PRIVATE, ABSTRACT, STATIC, FINAL, STRICTFP,
		TRANSIENT, VOLATILE, SYNCHRONIZED, NATIVE, DEFAULT);

    private static final Map<String, Token> nameToToken = new HashMap<> ();
    static {
	for (Token t : keywords)
	    nameToToken.put (t.description, t);
	nameToToken.put (NULL.description, NULL);
	nameToToken.put (TRUE.description, TRUE);
	nameToToken.put (FALSE.description, FALSE);
    }

    private static final EnumSet<Token> valueTokens =
    EnumSet.of (INT_LITERAL, LONG_LITERAL, FLOAT_LITERAL, DOUBLE_LITERAL,
		CHARACTER_LITERAL, STRING_LITERAL, TRUE, FALSE, IDENTIFIER, NULL);

    @Override public String toString () {
	return description;
    }

    /** Check if an identifier is actually a keyword or the null, true or false litera. */
    public static Token getFromIdentifier (String id) {
	return nameToToken.get (id);
    }

    private static final Map<String, Token> descToToken = new HashMap<> ();
    static {
	for (Token t : values ())
	    descToToken.put (t.description, t);
    }

    public static Token getFromDescription (String desc) {
	return descToToken.get (desc);
    }

    public boolean isWhitespace () {
	return whitespaces.contains (this);
    }

    public boolean isIdentifier () {
	return this == IDENTIFIER;
    }

    public boolean isKeyword () {
	return keywords.contains (this);
    }

    public boolean isLiteral () {
	return literals.contains (this);
    }

    public boolean isSeparator () {
	return separator.contains (this);
    }

    public boolean isOperator () {
	return operators.contains (this);
    }

    public boolean isLogicalOperator () {
	return logicalOperators.contains (this);
    }

    public boolean isAutoWidenOperator () {
	return autoWideOperators.contains (this);
    }

    public boolean isBitOrShiftOperator () {
	return bitOperators.contains (this);
    }

    public boolean isPrimitive () {
	return primitives.contains (this);
    }

    public boolean isModifier () {
	return modifiers.contains (this);
    }

    public boolean isAssignmentOperator () {
	return assignmentOperators.contains (this);
    }

    public boolean hasValue () {
	return valueTokens.contains (this);
    }
}
