package org.khelekore.parjac.batch;

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
    private final Path outputDir;
    private final Charset encoding;
    private final boolean debug;

    public CompilationArguments (List<Path> srcDirs, Path outputDir,
				 Charset encoding, boolean debug) {
	this.srcDirs = new ArrayList<> (srcDirs);
	this.outputDir = outputDir;
	this.encoding = encoding;
	this.debug = debug;
    }

    public List<Path> getSrcDirs () {
	return Collections.unmodifiableList (srcDirs);
    }

    public Path getOutputDir () {
	return outputDir;
    }

    public Charset getEncoding () {
	return encoding;
    }

    public boolean getDebug () {
	return debug;
    }

    public void validate (CompilerDiagnosticCollector diagnostics) {
    	if (srcDirs == null)
	    diagnostics.report (new NoSourceDiagnostics ("Source dirs may not be null"));
	else if (srcDirs.isEmpty ())
	    diagnostics.report (new NoSourceDiagnostics ("Source dirs may not be empty"));
	if (outputDir == null)
	    diagnostics.report (new NoSourceDiagnostics ("Output dir may not be null"));
    }
}