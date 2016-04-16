package org.khelekore.parjac.semantics;

import java.util.Arrays;

import org.objectweb.asm.Type;

public class MethodInformation {
    private final String classname;
    private final int access;
    private final String name;
    private final String desc;
    private final Type[] arguments;
    private final String signature;
    private final String[] exceptions;

    public MethodInformation (String classname, int access, String name,
			      String desc, String signature,
			      String[] exceptions) {
	this.classname = classname;
	this.access = access;
	this.name = name;
	this.desc = desc;
	arguments = Type.getArgumentTypes (desc);
	this.signature = signature;
	this.exceptions = exceptions;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" + access + " " + name + " " +
	    Arrays.toString (arguments) + " " + signature + " " +
	    Arrays.toString (exceptions) + "}";
    }

    public String getClassname () {
	return classname;
    }

    public int getAccess () {
	return access;
    }

    public String getName () {
	return name;
    }

    public String getDesc () {
	return desc;
    }

    public Type[] getArguments () {
	return arguments;
    }

    public int getNumberOfArguments () {
	return arguments.length;
    }

    public String getSignature () {
	return signature;
    }

    public String[] getExceptions () {
	return exceptions;
    }

    public boolean isVarArgs () {
	return FlagsHelper.isVarArgs (access);
    }
}
