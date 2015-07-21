package org.khelekore.parjac;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class MemoryBytecodeWriter implements BytecodeWriter {
    private Map<Path, byte[]> bytecode = new HashMap<> ();

    public void createDirectory (Path path) {
	// ignore
    }

    public void write (Path path, byte[] data) {
	bytecode.put (path, data);
    }

    public byte[] getBytecode (Path path) {
	return bytecode.get (path);
    }
}