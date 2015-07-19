package org.khelekore.parjac;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.khelekore.parjac.CompilerDiagnosticCollector;
import org.khelekore.parjac.NoSourceDiagnostics;

/** Batch compiler command line arguments */
public class CompilationArguments {
    private final List<Path> srcDirs;
    private final BytecodeWriter classWriter;
    private final Charset encoding;
    private final List<Path> classPathEntries;
    private final boolean reportTime;
    private final boolean debug;

    public CompilationArguments (List<Path> srcDirs, BytecodeWriter classWriter,
				 Charset encoding, List<Path> classPathEntries,
				 boolean reportTime, boolean debug) {
	this.srcDirs = new ArrayList<> (srcDirs);
	this.classWriter = classWriter;
	this.encoding = encoding;
	this.classPathEntries = classPathEntries;
	this.reportTime = reportTime;
	this.debug = debug;
    }

    public List<Path> getSrcDirs () {
	return Collections.unmodifiableList (srcDirs);
    }

    public BytecodeWriter getClassWriter () {
	return classWriter;
    }

    public Charset getEncoding () {
	return encoding;
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
    	if (srcDirs == null)
	    diagnostics.report (new NoSourceDiagnostics ("Source dirs may not be null"));
	else if (srcDirs.isEmpty ())
	    diagnostics.report (new NoSourceDiagnostics ("Source dirs may not be empty"));
	if (classWriter == null)
	    diagnostics.report (new NoSourceDiagnostics ("BytecodeWriter may not be null"));
    }
}