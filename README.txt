Parjac is a java compiler, it is supposed to be fast, it is supposed
to use modern tools and thus be easily hackable.

Some things:
*) Development is done using java 8, no support for earlier versions.
*) Compilation is done on directories, not files (internally it is a
   fileset that can be any number of files).
*) Every step should be internally time logged so that it is easy to
   inspect performance.
*) The lexer is hand written.
*) Tests are done with testng.

Use asm for bytecode handling.
