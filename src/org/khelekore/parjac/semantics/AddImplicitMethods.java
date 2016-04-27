package org.khelekore.parjac.semantics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.SourceDiagnostics;
import org.khelekore.parjac.lexer.Token;
import org.khelekore.parjac.tree.AnnotationTypeDeclaration;
import org.khelekore.parjac.tree.ArgumentList;
import org.khelekore.parjac.tree.ArrayType;
import org.khelekore.parjac.tree.Block;
import org.khelekore.parjac.tree.CastExpression;
import org.khelekore.parjac.tree.ClassBody;
import org.khelekore.parjac.tree.ClassType;
import org.khelekore.parjac.tree.ConstructorArguments;
import org.khelekore.parjac.tree.ConstructorBody;
import org.khelekore.parjac.tree.ConstructorDeclaration;
import org.khelekore.parjac.tree.ConstructorDeclarator;
import org.khelekore.parjac.tree.Dims;
import org.khelekore.parjac.tree.EnumBody;
import org.khelekore.parjac.tree.EnumDeclaration;
import org.khelekore.parjac.tree.ExplicitConstructorInvocation;
import org.khelekore.parjac.tree.ExpressionType;
import org.khelekore.parjac.tree.FieldAccess;
import org.khelekore.parjac.tree.FieldDeclaration;
import org.khelekore.parjac.tree.FormalParameter;
import org.khelekore.parjac.tree.FormalParameterList;
import org.khelekore.parjac.tree.Identifier;
import org.khelekore.parjac.tree.MethodBody;
import org.khelekore.parjac.tree.MethodDeclaration;
import org.khelekore.parjac.tree.MethodDeclarator;
import org.khelekore.parjac.tree.MethodHeader;
import org.khelekore.parjac.tree.MethodInvocation;
import org.khelekore.parjac.tree.ModifierTokenType;
import org.khelekore.parjac.tree.NormalClassDeclaration;
import org.khelekore.parjac.tree.NormalFormalParameterList;
import org.khelekore.parjac.tree.NormalInterfaceDeclaration;
import org.khelekore.parjac.tree.OneDim;
import org.khelekore.parjac.tree.PrimaryNoNewArray;
import org.khelekore.parjac.tree.Result;
import org.khelekore.parjac.tree.ReturnStatement;
import org.khelekore.parjac.tree.SyntaxTree;
import org.khelekore.parjac.tree.TreeNode;
import org.khelekore.parjac.tree.TreeVisitor;
import org.khelekore.parjac.tree.UntypedMethodHeader;
import org.khelekore.parjac.tree.UntypedMethodInvocation;
import org.khelekore.parjac.tree.VariableDeclarator;
import org.khelekore.parjac.tree.VariableDeclaratorId;
import org.khelekore.parjac.tree.VariableDeclaratorList;

public class AddImplicitMethods {
    private final ClassInformationProvider cip;
    private final SyntaxTree tree;
    private final CompilerDiagnosticCollector diagnostics;

    public AddImplicitMethods (ClassInformationProvider cip, SyntaxTree tree,
			       CompilerDiagnosticCollector diagnostics) {
	this.cip = cip;
	this.tree = tree;
	this.diagnostics = diagnostics;
    }

    public void run () {
	Finder f = new Finder ();
	tree.getCompilationUnit ().visit (f);
    }

    private class Finder implements TreeVisitor {
	private Deque<ConstructorCollector<?>> ccs = new ArrayDeque<> ();

	@Override public boolean visit (NormalClassDeclaration c) {
	    ccs.addLast (new ClassConstructorCollector (c));
	    return true;
	}

	@Override public boolean visit (EnumDeclaration e) {
	    ccs.addLast (new EnumConstructorCollector (e));
	    return true;
	}

	@Override public boolean visit (NormalInterfaceDeclaration i) {
	    ccs.addLast (new NoAddCollector (i));
	    return true;
	}

	@Override public boolean visit (AnnotationTypeDeclaration a) {
	    ccs.addLast (new NoAddCollector (a));
	    return true;
	}

	@Override public void endType () {
	    ConstructorCollector<?> cc = ccs.removeLast ();
	    if (cc == null)
		return;
	    if (cc.isEmpty ())
		cc.addDefaultConstructor ();
	    cc.addOtherMethods ();
	}

	@Override public boolean anonymousClass (TreeNode from, ClassType ct, ClassBody b) {
	    ccs.addLast (new NoAddCollector (b));
	    return true;
	}

	@Override public void endAnonymousClass (ClassType ct, ClassBody b) {
	    ccs.removeLast ();
	}

	@Override public boolean visit (ConstructorDeclaration c) {
	    ccs.getLast ().add (c);
	    return true;
	}

	@Override public  boolean visit (MethodDeclaration m) {
	    ccs.getLast ().method (m);
	    return true;
	}
    }

    private abstract class ConstructorCollector<T> {
	private final List<ConstructorDeclaration> cds = new ArrayList<> ();

	public void add (ConstructorDeclaration cd) {
	    cds.add (cd);
	    String cdid = cd.getId ();
	    if (!getId ().equals (cdid))
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), cd.getParsePosition (),
							     "Constructor name not matching class name"));
	}

	public boolean isEmpty () {
	    return cds.isEmpty ();
	}

	/** Get the id/class name of the type */
	abstract String getId ();

	abstract void addDefaultConstructor ();

	/** A method was encountered */
	void method (MethodDeclaration md) {}

	void addOtherMethods () {}
    }

    private class ClassConstructorCollector extends ConstructorCollector<NormalClassDeclaration> {
	private final NormalClassDeclaration type;

	public ClassConstructorCollector (NormalClassDeclaration type) {
	    this.type = type;
	}

	public String getId () {
	    return type.getId ();
	}

	public void addDefaultConstructor () {
	    List<TreeNode> modifiers = FlagsHelper.isPublic (type.getFlags ()) ? ConstructorDeclaration.PUBLIC_LIST : null;
	    ClassBody cb = type.getBody ();
	    cb.getDeclarations ().add (getConstructor (modifiers, getId (), Collections.emptyList ()));
	}
    }

    private class EnumConstructorCollector extends ConstructorCollector<EnumDeclaration> {
	private final EnumDeclaration type;
	public EnumConstructorCollector (EnumDeclaration type) {
	    this.type = type;
	}

	@Override public void add (ConstructorDeclaration cd) {
	    int flags = cd.getFlags ();
	    if (FlagsHelper.isPublic (flags)) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), cd.getParsePosition (),
							     "Enum constructor may not be public"));
	    } else if (FlagsHelper.isProtected (flags)) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), cd.getParsePosition (),
							     "Enum constructor may not be protected"));
	    }
	    super.add (cd);
	}

	public String getId () {
	    return type.getId ();
	}

	public void addDefaultConstructor () {
	    EnumBody eb = type.getBody ();
	    List<TreeNode> args = Arrays.asList (new Identifier ("$name", null, ExpressionType.STRING),
						 new Identifier ("$id", null, ExpressionType.INT));
	    eb.getDeclarations ().add (getConstructor (ConstructorDeclaration.PRIVATE_LIST, getId (), args));
	}

	@Override void method (MethodDeclaration md) {
	    String id = md.getMethodName ();
	    FormalParameterList fpl = md.getParameters ();
	    NormalFormalParameterList nfpl = fpl != null ? fpl.getParameters () : null;
	    if (id.equals ("values")) {
		if (nfpl == null || nfpl.getNumberOfParameters () == 0) {
		    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), md.getParsePosition (),
								 "May not add values method to enum"));
		}
	    } else if (id.equals ("valueOf")) {
		if (nfpl != null && nfpl.getNumberOfParameters () == 1) {
		    List<FormalParameter> formalParameters = nfpl.getFormalParameters ();
		    if (formalParameters.size () == 1 &&
			formalParameters.get (0).getExpressionType ().equals (ExpressionType.STRING)) {
			diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), md.getParsePosition (),
								     "May not add valueOf method to enum"));
		    }
		}
	    }
	}

	@Override void addOtherMethods () {
	    addValuesField ();
	    addValues ();
	    addValueOf ();
	}

	private void addValuesField () {
	    List<TreeNode> modifiers = new ArrayList<> ();
	    modifiers.add (new ModifierTokenType (Token.PRIVATE, null));
	    modifiers.add (new ModifierTokenType (Token.STATIC, null));
	    modifiers.add (new ModifierTokenType (Token.FINAL, null));
	    List<VariableDeclarator> ls = new ArrayList<> ();
	    VariableDeclaratorId vdi = new VariableDeclaratorId ("$VALUES");
	    ls.add (new VariableDeclarator (vdi, null, null));
	    VariableDeclaratorList variables = new VariableDeclaratorList (ls, null);
	    FieldDeclaration fd = new FieldDeclaration (modifiers, getEnumArray (), variables,
							null, tree.getOrigin (), diagnostics);
	    EnumBody cb = type.getBody ();
	    cb.getDeclarations ().add (fd);
	    String fqn = cip.getFullName (type);
	    cip.addField (fqn, vdi.getId (), new FieldInformation<FieldDeclaration> ("$VALUES", fd, type));
	}

	private void addValues () {
	    // "return (E[])($VALUES.clone());"
	    TreeNode arrayType = getEnumArray ();
	    MethodHeader mh = getMethodHeader (arrayType, "values", null);
	    ClassType ct = new ClassType (null, null);
	    ct.setFullName (cip.getFullName (type));
	    FieldAccess fa = new FieldAccess (ct, "$VALUES", null);
	    UntypedMethodInvocation umi = new UntypedMethodInvocation ("clone", null);
	    MethodInvocation mi = new MethodInvocation (fa, umi, null);
	    CastExpression ce = new CastExpression (arrayType, mi, null);
	    ReturnStatement rs = new ReturnStatement (ce);
	    List<TreeNode> statements = new ArrayList<> ();
	    statements.add (rs);
	    addMethod (mh, statements);
	}

	private void addValueOf () {
	    // "return (E)Enum.valueOf (E.class, "whatever");"
	    List<FormalParameter> fps = new ArrayList<> ();
	    fps.add (new FormalParameter (getType ("java.lang.String"), new VariableDeclaratorId ("s")));
	    NormalFormalParameterList nfpl = new NormalFormalParameterList (fps, null);
	    FormalParameterList fpl = new FormalParameterList (nfpl);
	    TreeNode enumType = enumClass ();
	    MethodHeader mh = getMethodHeader (enumType, "valueOf", fpl);
	    List<TreeNode> statements = new ArrayList<> ();
	    List<TreeNode> args = new ArrayList<> ();
	    args.add (new PrimaryNoNewArray.ClassPrimary (enumType, null, null));
	    args.add (new Identifier ("s", null, ExpressionType.STRING));
	    ArgumentList al = new ArgumentList (args);
	    UntypedMethodInvocation umi = new UntypedMethodInvocation ("valueOf", al);
	    MethodInvocation mi = new MethodInvocation (getType ("java.lang.Enum"), umi, null);
	    CastExpression ce = new CastExpression (enumType, mi, null);
	    ReturnStatement rs = new ReturnStatement (ce);
	    statements.add (rs);
	    addMethod (mh, statements);
	}

	private MethodHeader getMethodHeader (TreeNode retType, String methodName, FormalParameterList params) {
	    Result r = new Result.TypeResult (retType, null);
	    MethodDeclarator declarator = new MethodDeclarator (methodName, params, null);
	    UntypedMethodHeader umh = new UntypedMethodHeader (r, declarator, null);
	    return new MethodHeader (null, null, umh);
	}

	private MethodDeclaration getMethodDeclaration (MethodHeader header, MethodBody body) {
	    List<TreeNode> modifiers = new ArrayList<> ();
	    modifiers.add (new ModifierTokenType (Token.PUBLIC, null));
	    modifiers.add (new ModifierTokenType (Token.STATIC, null));
	    return new MethodDeclaration (modifiers, header, body);
	}

	private void addMethod (MethodHeader mh, List<TreeNode> statements) {
	    Block block = new Block (statements);
	    MethodBody body = new MethodBody.BlockBody (block, null);
	    MethodDeclaration md = getMethodDeclaration (mh, body);
	    EnumBody cb = type.getBody ();
	    cb.getDeclarations ().add (md);

	    String fqn = cip.getFullName (type);
	    // TODO need to extract this to a method
	    cip.addMethod (fqn, md.getMethodName (),
			   new MethodInformation (fqn, md.getFlags (), md.getMethodName (), md.getDescription (), null, new String[0]));
	}

	private TreeNode getEnumArray () {
	    Dims dims = new Dims (new OneDim ());
	    return new ArrayType (enumClass (), dims);
	}

	private TreeNode enumClass () {
	    return getType (cip.getFullName (type));
	}

	private ClassType getType (String type) {
	    ClassType ct = new ClassType (null, null);
	    ct.setFullName (type);
	    return ct;
	}
    }

    private class NoAddCollector extends ConstructorCollector<Void> {
	private final TreeNode type;

	public NoAddCollector (TreeNode type) {
	    this.type = type;
	}

	@Override public void add (ConstructorDeclaration cd) {
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), type.getParsePosition (),
							 "May not add constructor to: " +
							 type.getClass ().getSimpleName ()));

	}

	public String getId () {
	    return null;
	}

	public void addDefaultConstructor () {
	    // TODO: implement
	}
    }

    private ConstructorDeclaration getConstructor (List<TreeNode> modifiers, String id, List<TreeNode> args) {
	ConstructorDeclarator d = new ConstructorDeclarator (null, id, null);
	ConstructorArguments ca = new ConstructorArguments (new ArgumentList (args));
	ExplicitConstructorInvocation eci = new ExplicitConstructorInvocation (null, Token.SUPER, ca);
	ConstructorBody body = new ConstructorBody (eci, null);
	return new ConstructorDeclaration (modifiers, d, null, body);
    }
}
