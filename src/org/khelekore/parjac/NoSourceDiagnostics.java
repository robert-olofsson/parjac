package org.khelekore.parjac;

import java.nio.file.Path;
import java.util.Locale;
import javax.tools.Diagnostic;

public class NoSourceDiagnostics implements Diagnostic<Path> {
    private final String format;
    private final Object[] args;

    public NoSourceDiagnostics (String format, Object... args) {
	this.format = format;
	this.args = args;
    }

    public Diagnostic.Kind getKind () {
	return Diagnostic.Kind.ERROR;
    }

    public Path getSource () {
	return null;
    }

    public long getPosition () {
	return Diagnostic.NOPOS;
    }

    public long getStartPosition () {
	return Diagnostic.NOPOS;
    }

    public long getEndPosition () {
	return Diagnostic.NOPOS;
    }

    public long getLineNumber () {
	return Diagnostic.NOPOS;
    }

    public long getColumnNumber () {
	return Diagnostic.NOPOS;
    }

    public String getCode () {
	return null;
    }

    public String getMessage (Locale locale) {
	return String.format (format, args);
    }
}