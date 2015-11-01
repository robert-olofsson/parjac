java \
 -Xmx4g \
 -cp bin/:resources/:libs/asm-5.0.4.jar:libs/asm-tree-5.0.4.jar \
 org.khelekore.parjac.batch.Main "$@"

#gradle run -Pargs="$*"
