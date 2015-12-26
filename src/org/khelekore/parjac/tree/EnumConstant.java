package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac.grammar.Rule;
import org.khelekore.parjac.lexer.ParsePosition;
import org.khelekore.parjac.lexer.Token;
import org.objectweb.asm.Opcodes;

public class EnumConstant extends PositionNode implements FlaggedType {
    private String type; // fqn of enum declaration
    private final List<TreeNode> annotations;
    private final String id;
    private final ArgumentList arguments;
    private final ClassBody body;

    public EnumConstant (Rule r, Deque<TreeNode> parts, ParsePosition ppos) {
	super (ppos);
	int pos = 0;
	TreeNode tn = parts.pop ();
	if (r.getRulePart (pos).getId () != Token.IDENTIFIER) {
	    annotations = ((ZOMEntry)tn).get ();
	    tn = parts.pop ();
	    pos++;
	} else {
	    annotations = null;
	}
	id = ((Identifier)tn).get ();
	pos++;
	if (r.size () > pos && r.getRulePart (pos).getId () == Token.LEFT_PARENTHESIS) {
	    pos++; // '('
	    if (r.getRulePart (pos).getId () != Token.RIGHT_PARENTHESIS) {
		arguments = (ArgumentList)parts.pop ();
		pos++;
	    } else {
		arguments = null;
	    }
	    pos++; // ')'
	} else {
	    arguments = null;
	}
	body = r.size () > pos ? (ClassBody)parts.pop () : null;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{annotations: " + annotations +
	    ", id: " + id + ", arguments: " + arguments + ", body: " + body + "}";
    }

    @Override public int getFlags () {
	return Opcodes.ACC_PUBLIC;
    }

    @Override public List<TreeNode> getAnnotations () {
	return annotations;
    }

    public void setType (String fqn) {
	this.type = fqn;
    }

    public String getType () {
	return type;
    }

    public String getId () {
	return id;
    }

    public void visit (TreeVisitor visitor) {
	if (arguments != null)
	    arguments.visit (visitor);
	if (visitor.visit (this)) {
	    if (body != null)
		body.visit (visitor);
	}
    }

    public Collection<? extends TreeNode> getChildNodes () {
	if (arguments == null && body == null)
	    return Collections.emptyList ();
	List<TreeNode> ls = new ArrayList<> ();
	if (arguments != null)
	    ls.add (arguments);
	if (body != null)
	    ls.add (body);
	return ls;
    }

    @Override public ExpressionType getExpressionType () {
	return new ExpressionType (type);
    }
}