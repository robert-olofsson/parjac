package org.khelekore.parjac.batch;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Batch compiler command line arguments */
public class CompilationArguments {
    private final List<Path> srcDirs;
    private final Path outputDir;
    private final Charset encoding;
    private final boolean debug;

    public CompilationArguments (List<Path> srcDirs, Path outputDir,
				 Charset encoding, boolean debug) {
	if (srcDirs == null)
	    throw new NullPointerException ("srcDirs may not be null");
	if (srcDirs.isEmpty ())
	    throw new IllegalArgumentException ("srcDirs may not be empty");
	if (outputDir == null)
	    throw new NullPointerException ("outputDir may not be null");
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
}