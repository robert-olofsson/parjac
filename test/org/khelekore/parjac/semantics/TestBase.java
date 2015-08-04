package org.khelekore.parjac.semantics;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.parser.TestParseHelper;
import org.khelekore.parjac.tree.SyntaxTree;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

public class TestBase {
    protected Grammar g;
    protected CompilerDiagnosticCollector diagnostics;
    protected ClassResourceHolder crh;
    protected ClassInformationProvider cip;

    @BeforeClass
    public void beforeClass () throws IOException {
	g = TestParseHelper.getJavaGrammarFromFile ("CompilationUnit", false);
	crh = new ClassResourceHolder (Collections.<Path>emptyList (),
				       // Can not use instance field
				       new CompilerDiagnosticCollector ());
	crh.scanClassPath ();
    }

    @BeforeMethod
    public void createDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
	CompiledTypesHolder cth = new CompiledTypesHolder ();
	cip = new ClassInformationProvider (crh, cth);
    }

    protected void parseAndSetClasses (String... sourceCodes) {
	List<Source> ls =
	    Arrays.asList (sourceCodes).stream ().
	    map (s -> new Source (null, s)).
	    collect (Collectors.toList ());
	parseAndSetClasses (ls);
    }

    protected void parseAndSetClass (String sourcePath, String sourceCode) {
	parseAndSetClasses (Collections.singletonList (new Source (sourcePath, sourceCode)));
    }

    protected void parseAndSetClasses (List<Source> sourceCodes) {
	List<SyntaxTree> trees = new ArrayList<> ();
	for (Source s : sourceCodes) {
	    SyntaxTree st = TestParseHelper.earleyParseBuildTree (g, s.sourceCode, s.sourcePath, diagnostics);
	    assert st != null : "Failed to parse:"  + s.sourceCode + ": " + getDiagnostics ();
	    cip.addTypes (st);
	    trees.add (st);
	}
	ClassSetter.fillInClasses (cip, trees, diagnostics);
	trees.forEach (this::handleSyntaxTree);
    }

    /** Does nothing, override if any special handling is required after parsing and class setting */
    protected void handleSyntaxTree (SyntaxTree tree) {
    }

    protected void assertNoErrors () {
	assert !diagnostics.hasError () : "Got errors: " + getDiagnostics ();
    }

    protected String getDiagnostics () {
	Locale loc = Locale.getDefault ();
	return diagnostics.getDiagnostics ().
	    map (c -> c.getMessage (loc)).
	    collect (Collectors.joining ("\n"));
    }

    public static class Source {
	public final String sourcePath;
	public final String sourceCode;

	public Source (String sourcePath, String sourceCode) {
	    this.sourcePath = sourcePath;
	    this.sourceCode = sourceCode;
	}
    }
}