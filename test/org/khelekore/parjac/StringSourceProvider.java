package org.khelekore.parjac;

import java.nio.CharBuffer;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

public class StringSourceProvider implements SourceProvider {
    private final Path path;
    private final String source;

    public StringSourceProvider (Path path, String source) {
	this.path = path;
	this.source = source;
    }

    public void setup (CompilerDiagnosticCollector diagnostics) {
	// empty
    }

    public Collection<Path> getSourcePaths () {
	return Collections.singleton (path);
    }

    public CharBuffer getInput (Path path) {
	return CharBuffer.wrap (source.toCharArray ());
    }
}