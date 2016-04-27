package org.khelekore.parjac;

import java.nio.file.Path;
import java.util.Locale;
import javax.tools.Diagnostic;
import org.khelekore.parjac.lexer.ParsePosition;

public class SourceDiagnostics implements Diagnostic<Path> {
    private final Diagnostic.Kind kind;
    private final Path path;
    private final ParsePosition parsePosition;
    private final String format;
    private final Object[] args;

    private SourceDiagnostics (Diagnostic.Kind kind, Path path, ParsePosition  parsePosition,
			       String format, Object... args) {
	this.kind = kind;
	this.path = path;
	this.parsePosition = parsePosition;
	this.format = format;
	this.args = args;
    }

    public static  SourceDiagnostics error (Path path, ParsePosition  parsePosition,
					    String format, Object... args) {
	return new SourceDiagnostics (Diagnostic.Kind.ERROR, path, parsePosition, format, args);
    }

    public static SourceDiagnostics warning (Path path, ParsePosition  parsePosition,
					     String format, Object... args) {
	return new SourceDiagnostics (Diagnostic.Kind.WARNING, path, parsePosition, format, args);
    }

    public Diagnostic.Kind getKind () {
	return kind;
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
	return parsePosition != null ? parsePosition.getLineNumber () : -1;
    }

    public long getColumnNumber () {
	return parsePosition != null ? parsePosition.getTokenColumn () : -1;
    }

    public String getCode () {
	return null;
    }

    public String getMessage (Locale locale) {
	String msg = String.format (format, args);
	return String.format ("%s:%d:%d: %s: %s", path.toString (),
			      getLineNumber (), getColumnNumber (), kind, msg);
    }
}