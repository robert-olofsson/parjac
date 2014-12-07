package org.khelekore.parjac.grammar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.parser.LRParser;

public class GrammarReader {
    private final LRParser lr;
    private String currentRule;

    public GrammarReader (boolean debug) {
	lr = new LRParser (debug);
    }

    public LRParser getParser () {
	return lr;
    }

    public void read (URL u) throws IOException {
	try (InputStream is = u.openStream ();
	     InputStreamReader isr = new InputStreamReader (is, "UTF-8");
	     BufferedReader br = new BufferedReader (isr)) {
		String line = null;
		while ((line = br.readLine ()) != null) {
		    parseLine (line);
		}
	    }
    }

    // TODO: rewrite it in parjac :-)
    private void parseLine (String line) {
	line = line.trim ();
	if (line.isEmpty ())
	    return;
	if (line.startsWith ("#"))
	    return;
	if (line.endsWith (":")) {
	    currentRule = line.substring (0, line.length () - 1);
	} else {
	    StringTokenizer st = new StringTokenizer (line, "'?*() ", true);
	    lr.addRule (currentRule, parseRecursive (st));
	}
    }

    private Object parseRecursive (StringTokenizer st) {
	List<Object> currentSequence = new ArrayList<> ();
	Object lastParsed = null;
	while (st.hasMoreElements ()) {
	    String s = st.nextToken ();
	    switch (s) {
	    case " ": // split on blanks
		currentSequence.add (lastParsed);
		break;
	    case "?":
		lastParsed = lr.zeroOrOne (lastParsed);
		break;
	    case "*":
		lastParsed = lr.zeroOrMore (lastParsed);
		break;
	    case "(":
		lastParsed = parseRecursive (st);
		break;
	    case ")": // current group finished
		return finish (currentSequence, lastParsed);
	    case "'":
		lastParsed = Token.getFromDescription (st.nextToken ("'"));
		st.nextToken ("'?*() "); // '
		break;
	    default:
		lastParsed = s; // rule name
		break;
	    }
	}
	return finish (currentSequence, lastParsed);
    }

    private Object finish (List<Object> currentSequence, Object lastParsed) {
	currentSequence.add (lastParsed);
	return lr.sequence (currentSequence.toArray ());
    }

    public static void main (String[] args) throws IOException {
	for (String filename : args) {
	    GrammarReader gr = new GrammarReader (true);
	    gr.read (Paths.get (filename).toFile ().toURI ().toURL ());
	    LRParser lr = gr.getParser ();
	    lr.addRule ("Goal", "CompilationUnit");
	    lr.build ();
	}
    }
}