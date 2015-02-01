mkdir -p javadoc
javadoc -windowtitle parjac -doctitle parjac \
	-use -version -author -encoding utf-8 \
	-link http://docs.oracle.com/javase/8/docs/api/ \
	-classpath libs/\* -sourcepath src/ -d javadoc/ \
	-subpackages org
