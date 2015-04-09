package org.khelekore.parjac;

import java.nio.file.Path;
import java.util.Locale;
import javax.tools.Diagnostic;
import org.khelekore.parjac.lexer.ParsePosition;

public class SourceDiagnostics implements Diagnostic<Path> {
    private final Path path;
    private final ParsePosition parsePosition;
    private final String format;
    private final Object[] args;

    public SourceDiagnostics (Path path, ParsePosition  parsePosition,
			      String format, Object... args) {
	this.path = path;
	this.parsePosition = parsePosition;
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
	return parsePosition.getTokenStartPos ();
    }

    public long getStartPosition () {
	return parsePosition.getTokenStartPos ();
    }

    public long getEndPosition () {
	return parsePosition.getTokenEndPos ();
    }

    public long getLineNumber () {
	return parsePosition.getLineNumber ();
    }

    public long getColumnNumber () {
	return parsePosition.getTokenColumn ();
    }

    public String getCode () {
	return null;
    }

    public String getMessage (Locale locale) {
	String msg = String.format (format, args);
	return String.format ("%s:%d:%d: %s", path.toString (),
			      getLineNumber (), getColumnNumber (), msg);
    }
}