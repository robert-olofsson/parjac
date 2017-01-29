package org.khelekore.parjac.semantics;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

class AssignmentHolder {
    private Set<FieldInformation<?>> assigned;
    private Deque<Set<FieldInformation<?>>> optionallyAssigned;

    @Override public String toString () {
	return getClass ().getSimpleName () + "{assigned: " + assigned +
	    ", optionallyAssigned: " + optionallyAssigned + "}";
    }

    public void startOptionalAssignment () {
	if (optionallyAssigned != null)
	    throw new IllegalStateException ("Already in optional assignment");
	optionallyAssigned = new ArrayDeque<> ();
	optionallyAssigned.add (new HashSet<> ());
    }

    public void otherOptionalAssignment () {
	if (optionallyAssigned == null)
	    throw new IllegalStateException ("Not in optional assignment");
	optionallyAssigned.addLast (new HashSet<> ());
    }

    /**
     * @param keepDefined if true then make the retained assignements definately assigned
     */
    public Set<FieldInformation<?>> makeDefinate (boolean keepDefined) {
	if (optionallyAssigned == null)
	    throw new IllegalStateException ("Not in optional assignment");
	Set<FieldInformation<?>> branchAssigned = optionallyAssigned.removeFirst ();
	Set<FieldInformation<?>> partiallyAssigned = new HashSet<> (branchAssigned);
	for (Set<FieldInformation<?>> otherBranchAssigned : optionallyAssigned) {
	    branchAssigned.retainAll (otherBranchAssigned);
	    partiallyAssigned.addAll (otherBranchAssigned);
	}
	if (keepDefined) {
	    partiallyAssigned.removeAll (branchAssigned);
	    if (assigned == null)
		assigned = branchAssigned;
	    else
		assigned.addAll (branchAssigned);
	}
	optionallyAssigned = null;
	return partiallyAssigned;
    }

    public void copyAssignments (AssignmentHolder other) {
	if (other.assigned == null)
	    return;
	if (optionallyAssigned == null) {
	    if (assigned == null)
		assigned = other.assigned;
	    else
		assigned.addAll (other.assigned);
	} else {
	    optionallyAssigned.getLast ().addAll (other.assigned);
	}
    }

    public void markAssigned (FieldInformation<?> fi) {
	if (optionallyAssigned == null) {
	    if (assigned == null)
		assigned = new HashSet<> ();
	    assigned.add (fi);
	} else {
	    optionallyAssigned.getLast ().add (fi);
	}
    }

    public boolean isAssigned (FieldInformation<?> fi) {
	if (assigned != null && assigned.contains (fi))
	    return true;
	if (optionallyAssigned != null && !optionallyAssigned.isEmpty ())
	    return optionallyAssigned.getLast ().contains (fi);
	return false;
    }
}