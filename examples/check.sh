plsek@:~/fiji/scj-dan/oSCJ/tools/checker$ hg pull
#!/bin/bash                                                                                                                  

set -e
set -x
cd .. && ./build.sh && cd examples/                                                                                                                  

../localbin/javac -proc:only -cp ../lib/scj.jar:../lib/scj-checker.jar  -processor checkers.SCJChecker `find ./railsegment -name "*.java"` 


