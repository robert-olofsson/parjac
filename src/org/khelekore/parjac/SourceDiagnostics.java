package org.khelekore.parjac;

import java.nio.file.Path;
import java.util.Locale;
import javax.tools.Diagnostic;

public class SourceDiagnostics implements Diagnostic<Path> {
    private final Path path;
    private final long startPos, endPos, line, column;
    private final String format;
    private final Object[] args;

    public SourceDiagnostics (Path path, long startPos, long endPos, long line, long column,
			      String format, Object... args) {
	this.path = path;
	this.startPos = startPos;
	this.endPos = endPos;
	this.line = line;
	this.column = column;
	this.format = format;
	this.args = args;
    }

    public Diagnostic.Kind getKind () {
	return Diagnostic.Kind.ERROR;
    }

    public Path getSource () {
	return path;
    }

    public long getPosition () {
	return startPos;
    }

    public long getStartPosition () {
	return startPos;
    }

    public long getEndPosition () {
	return endPos;
    }

    public long getLineNumber () {
	return line;
    }

    public long getColumnNumber () {
	return column;
    }

    public String getCode () {
	return null;
    }

    public String getMessage (Locale locale) {
	String msg = String.format (format, args);
	return String.format ("%s:%d:%d: %s", path.toString (), line, column, msg);
    }
}