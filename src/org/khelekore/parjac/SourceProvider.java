package org.khelekore.parjac;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.util.Collection;

public interface SourceProvider {
    /** Setup for use, will be called before other methods */
    void setup (CompilerDiagnosticCollector diagnostics) throws IOException;

    /** Get all the input paths */
    Collection<Path> getSourcePaths ();

    /** Get the source data for a given input path */
    CharBuffer getInput (Path path) throws IOException;
}