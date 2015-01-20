package org.khelekore.parjac.parser;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.khelekore.parjac.grammar.Grammar;
import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.grammar.RuleCollection;
import org.khelekore.parjac.grammar.RulePart;
import org.khelekore.parjac.lexer.Lexer;
import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.tree.BooleanLiteral;
import org.khelekore.parjac.tree.CharLiteral;
import org.khelekore.parjac.tree.ClassBody;
import org.khelekore.parjac.tree.CompilationUnit;
import org.khelekore.parjac.tree.DottedName;
import org.khelekore.parjac.tree.DoubleLiteral;
import org.khelekore.parjac.tree.EnumBody;
import org.khelekore.parjac.tree.EnumBodyDeclarations;
import org.khelekore.parjac.tree.EnumConstant;
import org.khelekore.parjac.tree.EnumConstantList;
import org.khelekore.parjac.tree.EnumDeclaration;
import org.khelekore.parjac.tree.FieldDeclaration;
import org.khelekore.parjac.tree.FloatLiteral;
import org.khelekore.parjac.tree.Identifier;
import org.khelekore.parjac.tree.IntLiteral;
import org.khelekore.parjac.tree.LongLiteral;
import org.khelekore.parjac.tree.MarkerAnnotation;
import org.khelekore.parjac.tree.ModifierTokenType;
import org.khelekore.parjac.tree.NormalClassDeclaration;
import org.khelekore.parjac.tree.NullLiteral;
import org.khelekore.parjac.tree.OperatorTokenType;
import org.khelekore.parjac.tree.PackageDeclaration;
import org.khelekore.parjac.tree.PrimitiveTokenType;
import org.khelekore.parjac.tree.PrimitiveType;
import org.khelekore.parjac.tree.ShiftOp;
import org.khelekore.parjac.tree.SingleStaticImportDeclaration;
import org.khelekore.parjac.tree.SingleTypeImportDeclaration;
import org.khelekore.parjac.tree.StaticImportOnDemandDeclaration;
import org.khelekore.parjac.tree.StringLiteral;
import org.khelekore.parjac.tree.TreeNode;
import org.khelekore.parjac.tree.TypeImportOnDemandDeclaration;
import org.khelekore.parjac.tree.VariableDeclarator;
import org.khelekore.parjac.tree.VariableDeclaratorId;
import org.khelekore.parjac.tree.VariableDeclaratorList;
import org.khelekore.parjac.tree.ZOMEntry;

public class JavaTreeBuilder {

    // Indexed by rule id
    private List<Builder> builders;

    public JavaTreeBuilder (Grammar g) {
	builders = new ArrayList<> (g.getNumberOfRules ());
	for (int i = 0; i < g.getNumberOfRules (); i++)
	    builders.add (null);

	// Productions from §3 (Lexical Structure)

	// Productions from §4 (Types, Values, and Variables)
	register (g, "PrimitiveType", constructored (PrimitiveType::new));
	// register (g, "ClassType", constructored (ClassType::new));
	// register (g, "SimpleClassType", constructored (SimpleClassType::new));
	// register (g, "ArrayType", constructored (ArrayType::new));
	// register (g, "Dims", constructored (Dims::new));
	// register (g, "TypeParameter", constructored (TypeParameter::new));
	// register (g, "TypeBound", constructored (TypeBound::new));
	// register (g, "AdditionalBound", constructored (AdditionalBound::new));
	// register (g, "TypeArguments", constructored (TypeArguments::new));
	// register (g, "TypeArgumentList", constructored (TypeArgumentList::new));
	// register (g, "Wildcard", constructored (Wildcard::new));
	// register (g, "WildcardBounds", constructored (WildcardBounds::new));

	// Productions from §6 Names
	register (g, "DottedName", constructored (DottedName::create));

	// Productions from §7 (Packages)
	register (g, "CompilationUnit", constructored (CompilationUnit::new));
	register (g, "PackageDeclaration", constructored (PackageDeclaration::new));
	register (g, "SingleTypeImportDeclaration", constructored (SingleTypeImportDeclaration::new));
	register (g, "TypeImportOnDemandDeclaration", constructored (TypeImportOnDemandDeclaration::new));
	register (g, "SingleStaticImportDeclaration", constructored (SingleStaticImportDeclaration::new));
	register (g, "StaticImportOnDemandDeclaration", constructored (StaticImportOnDemandDeclaration::new));

	// Productions from §8 (Classes)
	register (g, "NormalClassDeclaration", constructored (NormalClassDeclaration::new));
	register (g, "ClassBody", constructored (ClassBody::new));
	register (g, "FieldDeclaration", constructored (FieldDeclaration::new));
	register (g, "VariableDeclaratorList", constructored (VariableDeclaratorList::new));
	register (g, "VariableDeclarator", constructored (VariableDeclarator::new));
	register (g, "VariableDeclaratorId", constructored (VariableDeclaratorId::new));
	register (g, "EnumDeclaration", constructored (EnumDeclaration::new));
	register (g, "EnumBody", constructored (EnumBody::new));
	register (g, "EnumConstantList", constructored (EnumConstantList::new));
	register (g, "EnumConstant", constructored (EnumConstant::new));
	register (g, "EnumBodyDeclarations", constructored (EnumBodyDeclarations::new));

	// Productions from §9 (Interfaces)
	register (g, "MarkerAnnotation", constructored (MarkerAnnotation::new));

	// Productions from §10 (Arrays)
	// regiser (g, "VariableInitializerList", constructored (VariableInitializerList::new));

	// Productions from §14 (Blocks and Statements)

	// Productions from §15 (Expressions)
	register (g, "ShiftOp", constructored (ShiftOp::create));
    }

    private void register (Grammar g, String rulename, Builder b) {
	RuleCollection rc = g.getRules (rulename);
	for (Rule r : rc.getRules ())
	    builders.set (r.getId (), b);
    }

    /** Constructor taking one argument */
    public interface ConstructorOne<T> {
	TreeNode create (T arg);
    }

    /** Constructor taking two argument */
    public interface ConstructorTwo<T, U> {
	TreeNode create (T t, U u);
    }

    /** Given a rule and a deque of parts pop some parts and push the newly built part. */
    private interface Builder {
	void build (Rule r, Deque<TreeNode> parts);
    }

    private static Builder constructored (ConstructorOne<Deque<TreeNode>> cb) {
	return new Builder () {
	    public void build (Rule r, Deque<TreeNode> parts) {
		parts.push (cb.create (parts));
	    }
	};
    }

    private static Builder constructored (ConstructorTwo<Rule, Deque<TreeNode>> cb) {
	return new Builder () {
	    public void build (Rule r, Deque<TreeNode> parts) {
		parts.push (cb.create (r, parts));
	    }
	};
    }

    public TreeNode getTokenValue (Lexer lexer, Token token) {
	if (token.isOperator ())
	    return new OperatorTokenType (token);
	else if (token.isPrimitive ())
	    return new PrimitiveTokenType (token);
	else if (token.isModifier ())
	    return new ModifierTokenType (token);
	else if (!token.hasValue ())
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
	case NULL:
	    return NullLiteral.NULL;
	default:
	    throw new IllegalStateException ("Do not know how to get value from:" + token);
	}
    }

    public void build (State start, Deque<TreeNode> parts) {
	Rule rule = start.getRule ();
	Builder b = builders.get (rule.getId ());
	if (b != null) {
	    b.build (rule, parts);
	} else if (rule.getName ().startsWith ("ZOM_")) {
	    buildZOM (rule, parts);
	}
    }

    private void buildZOM (Rule r, Deque<TreeNode> parts) {
	ZOMEntry z;
	String name = r.getName ();
	if (r.size () > 1 && r.getRulePart (0).getId () == name) {
	    z = (ZOMEntry)parts.pop ();
	} else {
	    z = new ZOMEntry ();
	}
	// We should not have ',' or similar in the parts so no need to pop those
	// TODO: this will break for some "ZOM_34 -> [ZOM_34, [, ]]"
	z.add (parts.pop ());
	parts.push (z);
    }
}