package org.khelekore.parjac.tree;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SyntaxTree {
    private final Path origin;
    private final TreeNode node;

    public SyntaxTree (Path origin, TreeNode node) {
	this.origin = origin;
	this.node = node;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{path: " + origin + ", tree: " + node + "}";
    }

    public Path getOrigin () {
	return origin;
    }

    public TreeNode getRoot () {
	return node;
    }

    public Path getRelativeClassName () {
	Path srcrelative = getSourceRelativePath ();
	String filename = srcrelative.getFileName ().toString ();
	String classname = filename.replaceAll ("(?i).java$", ".class");
	return Paths.get (srcrelative.getParent ().toString (), classname);
    }

    public String getFQN () {
	Path srcrelative = getSourceRelativePath ();
	String filename = srcrelative.getFileName ().toString ();
	String classname = filename.replaceAll ("(?i).java$", "");
	Path cn = Paths.get (srcrelative.getParent ().toString (), classname);
	return cn.toString ();
    }

    private Path getSourceRelativePath () {
	return origin.subpath (1, origin.getNameCount ());
    }
}
