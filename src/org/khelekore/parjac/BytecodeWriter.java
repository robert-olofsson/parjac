package org.khelekore.parjac;

import java.io.IOException;
import java.nio.file.Path;

public interface BytecodeWriter {
    /** Create the output directory needed for some class */
    void createDirectory (Path path) throws IOException;

    /** Write the given bytecode
     * @Param path the relative path
     * @Param data the actual bytecode
     */
    void write (Path path, byte[] data) throws IOException;
}