## Makefile for emma instrumental
##
## Targets:
## Set RTSHOME to the home directory of the real-time Java package before make

instr:
	java -cp lib/emma.jar emma instr  -ix -javax.safetycritical.S3*,-javax.safetycritical.Launcher* -d instrbin -ip ../bin

report:
	java -cp lib/emma.jar emma report -r txt,html -in coverage.em -in coverage.ec -sp ..:../RI

clean:
	rm -r instrbin/*
	rm -r coverage.ec coverage.em coverage.txt coverage
