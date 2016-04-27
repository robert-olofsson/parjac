package org.khelekore.parjac.semantics;

import java.io.IOException;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.tree.ClassBody;
import org.khelekore.parjac.tree.ConstructorDeclaration;
import org.khelekore.parjac.tree.EnumBody;
import org.khelekore.parjac.tree.EnumDeclaration;
import org.khelekore.parjac.tree.ExpressionType;
import org.khelekore.parjac.tree.FormalParameter;
import org.khelekore.parjac.tree.FormalParameterList;
import org.khelekore.parjac.tree.LastFormalParameter;
import org.khelekore.parjac.tree.MethodDeclaration;
import org.khelekore.parjac.tree.NormalClassDeclaration;
import org.khelekore.parjac.tree.NormalFormalParameterList;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.TreeNode;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestConstructorChecker extends TestBase {

    @Test
    public void testClassNoConstructor () throws IOException {
	parseAndSetClasses ("class A {}");
	assertNoErrors ();
	checkConstructor ("A");
    }

    @Test
    public void testClassWithConstructor () throws IOException {
	parseAndSetClasses ("class A { A () {} }");
	assertNoErrors ();
	checkConstructor ("A");
    }

    @Test
    public void testConstructorWrongName () throws IOException {
	parseAndSetClasses ("class A { B () {} }");
	assert diagnostics.hasError () : "Expected to find errors";
    }

    @Test
    public void testEnumConstructor () throws IOException {
	parseAndSetClasses ("enum E { A, B; private E() {} }");
	assertNoErrors ();
	parseAndSetClasses ("enum E { A, B; E() {} }");
	assertNoErrors ();

	parseAndSetClasses ("enum E { A, B; public E() {} }");
	assert diagnostics.hasError () : "Expected to find errors, enum constructor may not be public";

	diagnostics = new CompilerDiagnosticCollector ();
	parseAndSetClasses ("enum E { A, B; protected E() {} }");
	assert diagnostics.hasError () : "Expected to find errors, enum constructor may not be protected";
    }

    @Test
    public void testEnumValues () throws IOException {
	parseAndSetClasses ("enum E { A, B; }");
	checkForEnumMethod ("E", "values");
	assertNoErrors ();
	parseAndSetClasses ("enum E { A, B; public static void values () {} }");
	assert diagnostics.hasError () : "Expected to find errors, values already declared";
	checkForEnumMethod ("E", "values");
    }

    @Test
    public void testEnumValueOf () throws IOException {
	parseAndSetClasses ("enum E { A, B; }");
	checkForEnumMethod ("E", "valueOf");
	assertNoErrors ();
	parseAndSetClasses ("enum E { A, B; public static E valueOf (String s) {} }");
	assert diagnostics.hasError () : "Expected to find errors, valueOf already declared";
	checkForEnumMethod ("E", "valueOf");
    }

    private void checkConstructor (String cls) {
	NormalClassDeclaration ncd = (NormalClassDeclaration)cip.getType (cls);
	ClassBody cb = ncd.getBody ();
	List<TreeNode> nodes = cb.getDeclarations ();
	assert !nodes.isEmpty () : "No nodes in tree";
	for (TreeNode tn : nodes) {
	    if (tn instanceof ConstructorDeclaration) {
		ConstructorDeclaration cd = (ConstructorDeclaration)tn;
		String id = cd.getId ();
		assert cls.equals (id);
	    }
	}
    }

    private void checkForEnumMethod (String cls, String method) {
	EnumDeclaration ed = (EnumDeclaration)cip.getType (cls);
	EnumBody eb = ed.getBody ();
	List<TreeNode> nodes = eb.getDeclarations ();
	Assert.assertNotNull (nodes, "Nodes");
	Assert.assertFalse (nodes.isEmpty (), "No nodes in tree");
	boolean found = false;
	for (TreeNode tn : nodes) {
	    if (tn instanceof MethodDeclaration) {
		MethodDeclaration md = (MethodDeclaration)tn;
		String id = md.getMethodName ();
		if (id.equals (method)) {
		    found = true;
		    FormalParameterList fpl = md.getParameters ();
		    NormalFormalParameterList nfpl = fpl != null ? fpl.getParameters () : null;
		    if (method.equals ("values")) {
			Assert.assertNull (nfpl, "Expected no-arg method: " + method + ", fpl: " + fpl);
		    } else if (method.equals ("valueOf")) {
			Assert.assertNotNull (nfpl, "Expected argument list: " + method + ", fpl: " + fpl);
			List<FormalParameter> formalParameters = nfpl.getFormalParameters ();
			Assert.assertEquals (formalParameters.size (), 1, "Expected only one parameter");
			FormalParameter fp = formalParameters.get (0);
			ExpressionType et = fp.getExpressionType ();
			Assert.assertEquals (et, ExpressionType.STRING, "Expected String");
			LastFormalParameter lfp = nfpl.getLastFormalParameter ();
			Assert.assertNull (lfp, "Did not expect any vararg");
		    }
		}
	    }
	}
	assert found : "Failed to find method";
    }

    protected void handleSyntaxTree (SyntaxTree tree) {
	AddImplicitMethods cc = new AddImplicitMethods (cip, tree, diagnostics);
	cc.run ();
    }
}