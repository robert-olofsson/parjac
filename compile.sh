javac \
 -classpath libs/asm-5.0.3.jar \
 -d bin \
 -sourcepath src \
 $(find src/ -name \*.java)
