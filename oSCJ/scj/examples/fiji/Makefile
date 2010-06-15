#  
# INSTRUCTIONS:
#  1) Check out Fiji: 
<<<<<<< .mine
#      hg clone -r 7db800193f83 ssh://zed.cs.purdue.edu//p/sss/fivmhg/fivm 
#     NOTE: The fiji hash should be the same as the revision number !!!!   
# 
=======
#      hg clone -r  7db800193f83 ssh://zed.cs.purdue.edu//p/sss/fivmhg/fivm 
#     NOTE: The fiji hash should be the same as the revision number !!!!   
# 
>>>>>>> .r13785
#  2) Patch Fiji: Patches for specific hashes are stored in $SCJ_HOME/examples/fiji/fiji-patch/$HASH.
#     Simply patch them in order. For example, for Fiji 5eac3e94a8a9:
#
#       $ patch -p1 < $SCJ_HOME/examples/fiji/fiji-patch/5eac3e94a8a9/1_fivmc_scj.diff
#       $ patch -p1 < $SCJ_HOME/examples/fiji/fiji-patch/5eac3e94a8a9/2_scj_minimal.diff
#       $ patch -p1 < $SCJ_HOME/examples/fiji/fiji-patch/5eac3e94a8a9/3_scj_wrapper.diff  
#       $ autoreconf -i
#       $ ./configure
#       $ make
#  
#  3) updating the old Patch (from http://drupal.org/patch/reverse)
#     You should also reverse a patch prior to adding a newer, updated version of the same patch. 
#     To reverse the patch, use the patch command with the -R option:
#       $ patch -p1 -R < path/file.patch 
#         
#  4) revision info
#     hg identify
#
#  5) Helloworld directory
#     scj/
#         bin/
#         ri/
#         examples/
#             helloworld/
#                 src/
#                 bin/
#                 build/
#
#  Incidentally, the argument passed to --scj-safelet is a class name,
#  not a filename; use MyHelloWorld (no .class extension).  You'll still
#  have to provide the MyHelloWorld.class file on the command line,
#  however:
#
#  fivmc -o hello -v 1 --scj "MyHelloWorld.class" "MyHelloWorld\$1.class" \
#    "MyHelloWorld\$WordHandler.class" --scj-safelet MyHelloWorld 
#
#  6) To compile and run HelloWorld, simply run make. You can change the path to Fiji by doing
#
#     $ make FIJI=/path/to/fiji
#
#  FijiVM - oSCJ CONTRACT specification:
#  Seee https://docs.google.com/Doc?docid=0ATNpaDzOYro-ZGRqcTZkNGdfMTE4Z3c4N3J2Zng&hl=en
#

CDX_HOME=../../../examples/minicdx
HELLO_HOME=../helloworld
HELLO_BUILD=../helloworld/build/

SCJ_HOME=../..
<<<<<<< .mine
#FIJI_HOME=/home/plsek/fiji/fivm

FIJI_HOME=/home/plsek/fiji/fivm-May14

=======


FIJI_HOME=/Users/plsek/_work/fiji/fivm-June/

>>>>>>> .r13785

FIVMCFLAGS=-v 1 

<<<<<<< .mine
=======
SCJFLAGS=--scj --scj-scope-backing 25m --g-def-immortal-mem 10m
>>>>>>> .r13785


############################    All    ##############################
all:
	echo "No all! Choose one: hello | cdx | tests"

############################    Compile    ##############################
<<<<<<< .mine
hello: clean 
	rm -rf hello.build
	mkdir hello.build
	javac -cp $(FIJI_HOME)/lib/scj.jar -d hello.build $(HELLO_HOME)/src/*.java
	$(FIJI_HOME)/bin/fivmc -o myhello -v 1 --scj --scj-scope-backing 15m --g-def-immortal-mem 1m --scj-safelet MyHelloWorld hello.build/*.class
	./myhello
=======
hello: clean sjar $(HELLO_BUILD)/MyHelloWorld.class
	rm -rf myhello.build
	mkdir myhello.build
	$(FIJI_HOME)/bin/fivmc -o myhello $(FIVMCFLAGS) $(SCJFLAGS) -m MyHelloWorld $(HELLO_BUILD)/*.class
	./myhello MyHelloWorld

$(HELLO_BUILD)/MyHelloWorld.class: $(HELLO_HOME)/src/MyHelloWorld.java
	javac -cp $(FIJI_HOME)/lib/scj.jar -d $(HELLO_BUILD) $(HELLO_HOME)/src/MyHelloWorld*.java





##############################################################
######################### CDx #####################
##############################################################
>>>>>>> .r13785

cdx: sjar
	rm -rf cdx.build
	mkdir cdx.build
	find $(CDX_HOME)/cdx -name *.java > list
	find $(CDX_HOME)/simulator -name *.java >> list
	find $(CDX_HOME)/utils -name *.java >> list
	javac -cp $(FIJI_HOME)/lib/scj.jar -d cdx.build @list
	find cdx.build -name *.class > list
	sed s/\\$$/\\\\$$/g list > list
	$(FIJI_HOME)/bin/fivmc -o mycdx -v 1 --scj $(shell cat class_list) --scj-scope-backing 25m --g-def-immortal-mem 10m --scj-safelet cdx.Level0Safelet
	./mycdx


##############################################################
######################### oSCJ TESTs #####################
##############################################################

TESTS=$(SCJ_HOME)/examples/tests

tests: tests-fiji 
	./tests

tests-compile: sjar
	echo "\n----------------------"
	echo "compiling tests"
	rm -rf $(TESTS)/build
	mkdir $(TESTS)/build
	find  $(TESTS)/src -name *.java > $(TESTS)/list
	javac -d $(TESTS)/build -cp $(FIJI_HOME)/lib/scj.jar @$(TESTS)/list
	find $(TESTS)/build -name *.class > $(TESTS)/build/tests_class_list
	sed s/\\$$/\\\\$$/g $(TESTS)/build/tests_class_list > $(TESTS)/build/tests_class_list2
	rm $(TESTS)/list

tests-fiji: tests-compile
	#$(FIJI_HOME)/bin/fivmc -o tests -v 1 --scj $(shell cat $(TESTS)/build/tests_class_list2) --g-def-max-mem=50M --scj-safelet test.HelloWorld 
	$(FIJI_HOME)/bin/fivmc -o tests -v 1 --scj $(shell cat $(TESTS)/build/tests_class_list2) --g-def-max-mem=50M -m test.Run_Tests






############################    SCJ library    ##############################
sjar: $(FIJI_HOME)/lib/scj.jar
	
$(FIJI_HOME)/lib/scj.jar:
	rm -rf scj.build
	mkdir scj.build
	find $(SCJ_HOME)/ri/spec/common/src -name *.java > list
	find $(SCJ_HOME)/ri/s3/common/src -name *.java >> list
	find $(SCJ_HOME)/ri/spec/fiji/src -name *.java >> list
	find $(SCJ_HOME)/ri/s3/fiji/src -name *.java >> list
	javac -d scj.build @list
	jar cf scj.jar -C scj.build edu -C scj.build javax
	cp scj.jar $(FIJI_HOME)/lib/
	rm -rf scj.jar
	rm -rf scj.build

############################    Run    ##############################
runHello:  
<<<<<<< .mine
	./$(NAME)

runCDx:
	./mycdx
=======
	./$(NAME)
>>>>>>> .r13785

<<<<<<< .mine
############################    Clean    ##############################
clean: 
	rm -rf scj.build
	rm -rf scj.jar
	rm -rf myhello.build
	rm -rf mycdx.build
	rm -rf myhello
	rm -rf tests
	rm -rf tests.build
	rm -rf mycdx
	rm -rf hello.build
	rm -rf cdx.build
	
=======
runCDx:
	./mycdx
>>>>>>> .r13785

############################    Clean    ##############################
clean: 
	rm -rf scj.build
	rm -rf scj.jar
	rm -rf myhello.build
	rm -rf mycdx.build
	rm -rf myhello
	rm -rf tests
	rm -rf tests.build
	rm -rf mycdx
	rm -rf hello.build
	rm -rf cdx.build
	rm -rf $(FIJI_HOME)/lib/scj.jar

