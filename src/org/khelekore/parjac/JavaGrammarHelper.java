package org.khelekore.parjac;

import java.io.IOException;

import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.grammar.GrammarReader;
import org.khelekore.parjac.lexer.Token;

public class JavaGrammarHelper {
    public static Grammar getValidatedJavaGrammar () throws IOException {
	GrammarReader gr = new GrammarReader ();
	gr.read (JavaGrammarHelper.class.getResource ("/java_8.pj"));
	Grammar g = gr.getGrammar ();
	g.addRule ("Goal", "CompilationUnit", Token.END_OF_INPUT);
	g.addClearableRule ("ClassBodyDeclaration");
	g.validateRules ();
	return g;
    }
}