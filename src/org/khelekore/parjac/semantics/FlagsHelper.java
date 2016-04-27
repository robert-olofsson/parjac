package org.khelekore.parjac.semantics;

import static org.objectweb.asm.Opcodes.*;

public class FlagsHelper {
    public static boolean isPublic (int accessFlags) {
	return (accessFlags & ACC_PUBLIC) == ACC_PUBLIC;
    }

    public static boolean isProtected (int accessFlags) {
	return (accessFlags & ACC_PROTECTED) == ACC_PROTECTED;
    }

    public static boolean isPrivate (int accessFlags) {
	return (accessFlags & ACC_PRIVATE) == ACC_PRIVATE;
    }

    public static boolean isFinal (int f) {
	return (f & ACC_FINAL) == ACC_FINAL;
    }

    public static boolean isVolatile (int f) {
	return (f & ACC_VOLATILE) == ACC_VOLATILE;
    }

    public static boolean isNative (int f) {
	return (f & ACC_NATIVE) == ACC_NATIVE;
    }

    public static boolean isStrictFp (int f) {
	return (f & ACC_STRICT) == ACC_STRICT;
    }

    public static boolean isAbstract (int f) {
	return (f & ACC_ABSTRACT) == ACC_ABSTRACT;
    }

    public static boolean isStatic (int f) {
	return (f & ACC_STATIC) == ACC_STATIC;
    }

    public static boolean isSynchronized (int f) {
	return (f & ACC_SYNCHRONIZED) == ACC_SYNCHRONIZED;
    }

    public static boolean isInterface (int f) {
	return (f & ACC_INTERFACE) == ACC_INTERFACE;
    }

    public static boolean isVarArgs (int f) {
	return (f & ACC_VARARGS) == ACC_VARARGS;
    }

    public static boolean isSynthetic (int f) {
	return (f & ACC_SYNTHETIC) == ACC_SYNTHETIC;
    }
}