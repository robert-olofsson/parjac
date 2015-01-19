package org.khelekore.parjac.parser;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.Lexer;
import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.tree.BooleanLiteral;
import org.khelekore.parjac.tree.CharLiteral;
import org.khelekore.parjac.tree.CompilationUnit;
import org.khelekore.parjac.tree.DottedName;
import org.khelekore.parjac.tree.DoubleLiteral;
import org.khelekore.parjac.tree.FloatLiteral;
import org.khelekore.parjac.tree.Identifier;
import org.khelekore.parjac.tree.IntLiteral;
import org.khelekore.parjac.tree.LongLiteral;
import org.khelekore.parjac.tree.MarkerAnnotation;
import org.khelekore.parjac.tree.NormalClassDeclaration;
import org.khelekore.parjac.tree.PackageDeclaration;
import org.khelekore.parjac.tree.SingleStaticImportDeclaration;
import org.khelekore.parjac.tree.SingleTypeImportDeclaration;
import org.khelekore.parjac.tree.StaticImportOnDemandDeclaration;
import org.khelekore.parjac.tree.StringLiteral;
import org.khelekore.parjac.tree.TreeNode;
import org.khelekore.parjac.tree.TypeImportOnDemandDeclaration;
import org.khelekore.parjac.tree.ZOMEntry;

public class JavaTreeBuilder {

    private interface Builder {
	void build (Rule r, Deque<TreeNode> parts);
    }

    private static final Map<String, Builder> builders = new HashMap<> ();

    static {
	builders.put ("DottedName", JavaTreeBuilder::buildDottedName);
	builders.put ("PackageDeclaration", constructored (PackageDeclaration::new));
	builders.put ("MarkerAnnotation", constructored (MarkerAnnotation::new));

	builders.put ("SingleTypeImportDeclaration", constructored (SingleTypeImportDeclaration::new));
	builders.put ("TypeImportOnDemandDeclaration", constructored (TypeImportOnDemandDeclaration::new));
	builders.put ("SingleStaticImportDeclaration", constructored (SingleStaticImportDeclaration::new));
	builders.put ("StaticImportOnDemandDeclaration", constructored (StaticImportOnDemandDeclaration::new));

	builders.put ("NormalClassDeclaration", constructored (NormalClassDeclaration::new));

	builders.put ("CompilationUnit", constructored (CompilationUnit::new));
    }

    public interface ConstructorOne<T> {
	TreeNode create (T arg);
    }

    private static Builder constructored (ConstructorOne<Deque<TreeNode>> cb) {
	return new Builder () {
	    public void build (Rule r, Deque<TreeNode> parts) {
		parts.push (cb.create (parts));
	    }
	};
    }

    public interface ConstructorTwo<T, U> {
	TreeNode create (T t, U u);
    }

    private static Builder constructored (ConstructorTwo<Rule, Deque<TreeNode>> cb) {
	return new Builder () {
	    public void build (Rule r, Deque<TreeNode> parts) {
		parts.push (cb.create (r, parts));
	    }
	};
    }

    public TreeNode getTokenValue (Lexer lexer, Token token) {
	if (!token.hasValue ())
	    return null;
	switch (token) {
	case INT_LITERAL:
	    return new IntLiteral (lexer.getIntValue ());
	case LONG_LITERAL:
	    return new LongLiteral (lexer.getLongValue ());
	case FLOAT_LITERAL:
	    return new FloatLiteral (lexer.getFloatValue ());
	case DOUBLE_LITERAL:
	    return new DoubleLiteral (lexer.getDoubleValue ());
	case CHARACTER_LITERAL:
	    return new CharLiteral (lexer.getCharValue ());
	case STRING_LITERAL:
	    return new StringLiteral (lexer.getStringValue ());
	case TRUE:
	    return BooleanLiteral.TRUE_VALUE;
	case FALSE:
	    return BooleanLiteral.FALSE_VALUE;
	case IDENTIFIER:
	    return new Identifier (lexer.getIdentifier ());
	default:
	    throw new IllegalStateException ("Do not know how to get value from:" + token);
	}
    }

    public void build (State start, Deque<TreeNode> parts) {
	Rule rule = start.getRule ();
	Builder b = builders.get (rule.getName ());
	if (b != null) {
	    b.build (rule, parts);
	} else if (rule.getName ().startsWith ("ZOM_")) {
	    buildZOM (rule, parts);
	}
    }

    private static void buildDottedName (Rule r, Deque<TreeNode> parts) {
	DottedName dn;
	if (r.size () > 1)
	    dn = (DottedName)parts.pop ();
	else
	    dn = new DottedName ();

	dn.add (((Identifier)parts.pop ()).get ());
	parts.push (dn);
    }

    private void buildZOM (Rule r, Deque<TreeNode> parts) {
	ZOMEntry z = r.size () == 1 ? new ZOMEntry () : (ZOMEntry)parts.pop ();
	z.add (parts.pop ());
	parts.push (z);
    }
}