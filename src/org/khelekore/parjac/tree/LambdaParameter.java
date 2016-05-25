package org.khelekore.parjac.tree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac.lexer.ParsePosition;

/** Class to hold inferred lambda parameters.
 *  Not part of the grammar as such.
 */
public class LambdaParameter implements  FlaggedType {
    private final Identifier id;

    public LambdaParameter (Identifier id) {
	this.id = id;
    }

    public String getId () {
	return id.get ();
    }

    @Override public int getFlags () {
	return 0;
    }

    @Override public List<TreeNode> getAnnotations () {
	return Collections.emptyList ();
    }

    @Override public ParsePosition getParsePosition () {
	return id.getParsePosition ();
    }

}