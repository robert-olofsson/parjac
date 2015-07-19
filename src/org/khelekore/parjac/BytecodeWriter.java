package org.khelekore.parjac;

import java.io.IOException;
import java.nio.file.Path;

public interface BytecodeWriter {
    void createDirectory (Path path) throws IOException;
    void write (Path path, byte[] data) throws IOException;
}