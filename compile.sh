javac \
 -classpath libs/asm-5.0.3.jar \
 -d bin \
 -sourcepath src \
 -Xdiags:verbose \
 $(find src/ -name \*.java)
