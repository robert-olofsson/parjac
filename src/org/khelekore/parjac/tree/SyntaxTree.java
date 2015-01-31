package org.khelekore.parjac.tree;

import java.nio.file.Path;

public class SyntaxTree {
    private final Path origin;
    private final TreeNode node;

    public SyntaxTree (Path origin, TreeNode node) {
	this.origin = origin;
	this.node = node;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{path: " + origin + ", node: " + node + "}";
    }

    public Path getOrigin () {
	return origin;
    }

    public TreeNode getRoot () {
	return node;
    }

    public CompilationUnit getCompilationUnit () {
	return (CompilationUnit)node;
    }
}
