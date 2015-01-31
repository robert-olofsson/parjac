package org.khelekore.parjac.tree;

import java.nio.file.Path;

public class SyntaxTree {
    private final Path origin;
    private final CompilationUnit cu;

    public SyntaxTree (Path origin, CompilationUnit cu) {
	this.origin = origin;
	this.cu = cu;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{path: " + origin + ", cu: " + cu + "}";
    }

    public Path getOrigin () {
	return origin;
    }

    public CompilationUnit getCompilationUnit () {
	return cu;
    }
}
