package org.khelekore.parjac.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.khelekore.parjac.lexer.Token;

import static org.objectweb.asm.Opcodes.*;

public class ModifierHelper {
    public static List<Annotation> getAnnotations (List<TreeNode> modifiers) {
	List<Annotation> ret = Collections.emptyList ();
	if (modifiers != null) {
	    for (TreeNode tn : modifiers) {
		if (!(tn instanceof ModifierTokenType)) {
		    if (ret.isEmpty ())
			ret = new ArrayList<> ();
		    ret.add ((Annotation)tn);
		}
	    }
	}
	return ret;
    }

    public static int getModifiers (List<TreeNode> modifiers) {
	int ret = 0;
	if (modifiers != null) {
	    for (TreeNode tn : modifiers) {
		if (tn instanceof ModifierTokenType) {
		    ret |= getModifier (((ModifierTokenType)tn).get ());
		}
	    }
	}
	return ret;
    }

    private static int getModifier (Token t) {
	switch (t) {
	case PUBLIC:
	    return ACC_PUBLIC;
	case PROTECTED:
	    return ACC_PROTECTED;
	case PRIVATE:
	    return ACC_PRIVATE;
	case ABSTRACT:
	    return ACC_ABSTRACT;
	case STATIC:
	    return ACC_STATIC;
	case FINAL:
	    return ACC_FINAL;
	case STRICTFP:
	    return ACC_STRICT;
	case TRANSIENT:
	    return ACC_TRANSIENT;
	case VOLATILE:
	    return ACC_VOLATILE;
	case SYNCHRONIZED:
	    return ACC_SYNCHRONIZED;
	case NATIVE:
	    return ACC_NATIVE;
	case DEFAULT:
	    // hmm?
	default:
	    throw new IllegalStateException ("Got unexpected token: " + t);
	}
    }
}