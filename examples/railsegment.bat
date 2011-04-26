
cd .. && ant jar && cd examples\

..\localbin\javac.bat -proc:only -cp ..\lib\scj.jar;..\lib\scj-checker.jar  -processor checkers.SCJChecker `find .\railsegment\ -name "*.java"`
