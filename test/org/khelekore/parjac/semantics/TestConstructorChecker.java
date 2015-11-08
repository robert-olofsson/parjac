package org.khelekore.parjac.semantics;

import java.io.IOException;
import java.util.List;

import org.khelekore.parjac.tree.ClassBody;
import org.khelekore.parjac.tree.ConstructorDeclaration;
import org.khelekore.parjac.tree.NormalClassDeclaration;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.TreeNode;
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

    protected void handleSyntaxTree (SyntaxTree tree) {
	ConstructorChecker cc = new ConstructorChecker (tree, diagnostics);
	cc.run ();
    }
}