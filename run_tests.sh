javac \
 -classpath libs/testng-6.8.jar:bin/:libs/asm-5.0.3.jar \
 -d bin \
 -sourcepath src \
 -Xdiags:verbose \
 $(find src/ -name \*.java) $(find test -name \*.java) \
&& \
java \
 -ea \
 -classpath bin/:resources/:libs/testng-6.8.jar:libs/asm-5.0.3.jar \
 org.testng.TestNG test/testng.xml
