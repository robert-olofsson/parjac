package org.khelekore.parjac;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;

/** A collector of diagnostics that is thread safe and can say if there
 *  has been any errors reported.
 */
public class CompilerDiagnosticCollector implements DiagnosticListener<Path> {
    private final List<Diagnostic<? extends Path>> list =
	Collections.synchronizedList (new ArrayList<> ());

    // Flag is set in one step and checked when that step has been fully handled
    private volatile boolean hasError;
    private volatile boolean hasWarning;

    public void report (Diagnostic<? extends Path> diagnostic) {
	if (diagnostic.getKind () == Diagnostic.Kind.ERROR)
	    hasError = true;
	if (diagnostic.getKind () == Diagnostic.Kind.WARNING)
	    hasWarning = true;
	list.add (diagnostic);
    }

    public boolean hasError () {
	return hasError;
    }

    public boolean hasWarning () {
	return hasWarning;
    }

    public Stream<Diagnostic<? extends Path>> getDiagnostics () {
	return list.stream ();
    }

    public void clear () {
	synchronized (list) {
	    list.clear ();
	    hasError = false;
	}
    }
}