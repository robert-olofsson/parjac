package org.khelekore.parjac.tree;

import java.util.List;

public interface FlaggedType extends TreeNode {
    /** Get the access modifier flags */
    int getFlags ();

    /** Get the annotations on this tree node */
    List<TreeNode> getAnnotations ();
}