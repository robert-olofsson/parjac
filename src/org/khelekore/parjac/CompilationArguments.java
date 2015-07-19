package org.khelekore.parjac;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.NoSourceDiagnostics;

/** Batch compiler command line arguments */
public class CompilationArguments {
    private final SourceProvider sourceProvider;
    private final BytecodeWriter classWriter;
    private final List<Path> classPathEntries;
    private final boolean reportTime;
    private final boolean debug;

    public CompilationArguments (SourceProvider sourceProvider, BytecodeWriter classWriter,
				 List<Path> classPathEntries,
				 boolean reportTime, boolean debug) {
	this.sourceProvider = sourceProvider;
	this.classWriter = classWriter;
	this.classPathEntries = classPathEntries;
	this.reportTime = reportTime;
	this.debug = debug;
    }

    public SourceProvider getSourceProvider () {
	return sourceProvider;
    }

    public BytecodeWriter getClassWriter () {
	return classWriter;
    }

    public List<Path> getClassPathEntries () {
	return classPathEntries;
    }

    public boolean getReportTime () {
	return reportTime;
    }

    public boolean getDebug () {
	return debug;
    }

    public void validate (CompilerDiagnosticCollector diagnostics) {
    	if (sourceProvider == null)
	    diagnostics.report (new NoSourceDiagnostics ("SourceProvider may not be null"));
	if (classWriter == null)
	    diagnostics.report (new NoSourceDiagnostics ("BytecodeWriter may not be null"));
    }
}