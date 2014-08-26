package org.khelekore.parjac.batch;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class CompilationArguments {
    private final List<Path> srcDirs;
    private final Path outputDir;

    public CompilationArguments (List<Path> srcDirs, Path outputDir) {
	if (srcDirs == null)
	    throw new NullPointerException ("srcDirs may not be null");
	if (srcDirs.isEmpty ())
	    throw new IllegalArgumentException ("srcDirs may not be empty");
	if (outputDir == null)
	    throw new NullPointerException ("outputDir may not be null");
	this.srcDirs = new ArrayList<> (srcDirs);
	this.outputDir = outputDir;
    }

    public List<Path> getSrcDirs () {
	return Collections.unmodifiableList (srcDirs);
    }

    public Path getOutputDir () {
	return outputDir;
    }
}