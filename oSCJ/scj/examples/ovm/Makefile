#
# INSTALLATION:
# 1. distribution directory structure 
#			
#  oSCJ/  
#    * OpenVM/ -- contains distribution of our SCJ-compliant VM  --- svn checkout  svn+ssh://plsek@zed.cs.purdue.edu/exports/u119/sss/common/svn/ovm/branches/SCJ/OpenVM
#    * build/ -- the SCJ-OVM configured for will be installed into this directory
#          o x86/ -- oSCJ compiled for x86
#          o leon3/ -- oSCJ compiled for leon3 
#    * doc/ -- documentation directory
#    * examples/ -- examples of SCJ applications
#          o HelloWorld/ -- hello world application
#				o leon3/ -- compiled hello world application for leon3
#				o build/ -- compiled hello world application for x86
#          o minicdj -- the miniCDj benchmark distribution  --  svn checkout svn+ssh://plsek@zed.cs.purdue.edu/exports/u119/sss/common/svn/scj-project/minicdx-j4
#				o leon3/ -- compiled cdx for leon3
#				o build/ -- compiled cdx for x86
#    * scj/			--- svn checkout svn+ssh://plsek@zed.cs.purdue.edu/exports/u119/sss/common/svn/scj-project/scj
#          o ri/ -- SCJ library implementation
#          o ri_rtsj/ -- SCJ library implementation based on RTSJ 
#    * tools/
#          o checker/ -- Static Checker distribution
#          o tck/ TCK for SCJ 
#    * readme
#    * LGPL-License.txt 
#
#   
#      
# 2. set up JAVA5 variable (or install JAVA5 if you dont have it)
#				-add JAva 5 to your PATH
#
# 3. move this Makefile to the root direcotory scj-ovm/
#
# 4. make, make cdx, make board, etc.
# 	 	- type make
#		
#		
#
# Targets:
# - make: compile ovm, helloworld, and run it - [x86]
#		- make hello = compiles helloworld for x86
#		- make run   = runs helloworld on x86
# - make cdx: compile and run miniCDj [x86]
# - make board: compile ovm, scj and helloworld  [leon 3 board]
# - make board_cdx :  compile ovm, scj and miniCDj [leon 3 board]
#
#
HOME=$(CURDIR)

OVM_HOME=$(HOME)/OpenVM
OVM_BUILD=$(HOME)/build/x86
OVM_BOARD_BUILD=$(HOME)/build/leon3

BOOT_CLASSPATH=$(OVM_BUILD)/src/syslib/user/ovm_scj/ovm_rt_user_scj.jar:$(OVM_BUILD)/src/syslib/user/ovm_rt_user.jar:$(OVM_BUILD)/src/syslib/user/ovm_platform/ovm_platform.jar

SCJ_SRC=./scj/ri

# just a temporary dirs, will be deleted after the compilation
SCJ_NEW=./riJ4
TEMP=$(shell basename $(SCJ_SRC))


#
# ! MAke sure you have Java 5 installed and that it is in you $PATH! !
#
#
JAVA5=/usr/lib/jvm/jdk1.5.0_22/bin



#
# Special OVM parameters
#
# MODEL - specifies the memory and object model used by oSCJ
#		- we have a specific SCJManager memory model
#		- for object model we use B_M, the B_Mf_F_H is also possible
#
ENGINE=j2c
OPT=run  
#MODEL=SCJManager-B_Mf_F_H
MODEL=SCJManager-B_M
THREADS=SCJJVM
IOCONF=SIGIOSockets_PollingOther


#
#
#  MEMORY PARAMETERS for SCJMemory Manager
#
#	- in oSCJ is no GC, therefore 
#				HEAP memory == IMMORTAL memory
#					- when you see HEAP, it means immortal
#
#	- scope = total space for allocation of private/mission memory areas
#	- pad - this is just a technical parameter, it must be bigger than the ovm image
#
IMMORTAL=4m #2
SCOPE=14m  #14
PAD=8m

#
# this is the name of the Main method of the application running on top of oSCJ
#
MAIN=MyHelloWorld


#
# HOME directories of example applications provided in the distribution
#
#
CDX=$(HOME)/examples/minicdx
HELLOWORLD=$(HOME)/examples/helloworld


##############################################################
####################### MAKING OVM - x86 ###########################


all: clean prepare_scj ovm hello run

ovm: $(OVM_BUILD)/bin/gen-ovm

#env CFLAGS="-mcpu=v8 -mno-fpu" $(OVM_BUILD)/configure --host=sparc-rtems4.9 --with-image-location=0x40001224 RTEMS=/opt/rtems-4.9.3/bsis

config: prepare_scj
	rm -rf $(OVM_BUILD)
	mkdir $(OVM_BUILD)
	cd $(OVM_BUILD) && $(OVM_HOME)/configure


$(OVM_BUILD)/bin/gen-ovm: 
	rm -rf $(OVM_BUILD)
	mkdir $(OVM_BUILD)
	cd $(OVM_BUILD) && $(OVM_HOME)/configure && make 



##############################################################
######################### RTEMS - board -CDX #####################
##############################################################

board_cdx: clean_board board_make board_cdx_gen


CDX_LAUNCHER=Launcher

BOARD_BOOT_CLASSPATH=$(OVM_BOARD_BUILD)/src/syslib/user/ovm_scj/ovm_rt_user_scj.jar:$(OVM_BOARD_BUILD)/src/syslib/user/ovm_rt_user.jar:$(OVM_BOARD_BUILD)/src/syslib/user/ovm_platform/ovm_platform.jar

init_board:
	mkdir -p $(OVM_BOARD_BUILD)

board_conf: init_board
	cd $(OVM_BOARD_BUILD) && env CFLAGS="-mcpu=v8 -mno-fpu" $(OVM_HOME)/configure --host=sparc-rtems4.9 --with-image-location=0x40001224 RTEMS=/opt/rtems-4.9.3/bsis 

board_make : board_conf
	cd $(OVM_BOARD_BUILD) && make 


OVM_BOARD_CDX=$(CDX)/leon3

prepare_board_cdx:
	mkdir -p $(OVM_BOARD_CDX)	
	find $(CDX) -name *.java > $(CDX)/list
	javac -source 1.4 -target 1.4 -d $(OVM_BOARD_CDX) -cp $(OVM_BUILD)/src/syslib/user/ovm_scj/ovm_rt_user_scj.jar:$(OVM_BUILD)/src/syslib/user/ovm_rt_user.jar:$(OVM_BUILD)/src/syslib/user/ovm_platform/ovm_platform.jar:$(CDX)/cdx:$(CDX)/utils:$(CDX)/simulator @$(CDX)/list




board_cdx_gen: clean_board_cdx prepare_board_cdx
	cd $(OVM_BOARD_CDX) && $(OVM_BOARD_BUILD)/bin/gen-ovm -threads=$(THREADS) \
			    -model=$(MODEL) \
			    -io=$(IOCONF) \
			    -pad-image-size=$(PAD) \
			    -scope-size=$(SCOPE) \
			    -heap-size=$(IMMORTAL) \
			    -main=$(CDX_LAUNCHER) \
			    -opt=$(OPT) \
			    -linker-options=-Ttext=0x40000000 \
			    -classpath=./:./cdx/:./collision/:./statetable:./javacp/ \
			-bootclasspath=$(BOARD_BOOT_CLASSPATH) 

			    

clean_board_cdx:
	rm -rf $(OVM_BOARD_CDX)
	mkdir -p $(OVM_BOARD_CDX)



##############################################################
######################### RTEMS - SCJ Helloworld #####################
##############################################################

board: clean_board board_make board_hello

OVM_BOARD_HW=$(HELLOWORLD)/leon3


prepare_board_hello:
	mkdir -p $(OVM_BOARD_HW)	
	cd $(HELLOWORLD)/src && javac -source 1.4 -cp $(OVM_BOARD_BUILD)/src/syslib/user/ovm_scj/ -d $(OVM_BOARD_HW) *.java



board_hello: clean_board_hw prepare_board_hello
	cd $(OVM_BOARD_HW) && $(OVM_BOARD_BUILD)/bin/gen-ovm -threads=$(THREADS) \
			    -model=$(MODEL) \
			    -io=$(IOCONF) \
			    -pad-image-size=$(PAD) \
			    -scope-size=$(SCOPE) \
			    -heap-size=$(IMMORTAL) \
			    -main=$(MAIN) \
			    -opt=$(OPT) \
			    -linker-options=-Ttext=0x40000000 \
			    -classpath=./:./cdx/:./collision/:./statetable:./javacp/ \
			-bootclasspath=$(BOARD_BOOT_CLASSPATH) 



clean_board_hw:
	rm -rf $(OVM_BOARD_CDX)
	mkdir -p $(OVM_BOARD_CDX)

clean_board: clean_board_hw clean_board_cdx
	rm -rf $(OVM_BOARD_BUILD)
	mkdir -p $(OVM_BOARD_BUILD)




##############################################################
######################### FOR HelloWorld #####################
##############################################################




LAUNCHER=MyHelloWorld
SAFELET=MyHelloWorld

run: hello
	cd $(HELLOWORLD)/build && ./ovm $(LAUNCHER) $(SAFELET)

hello: $(HELLOWORLD)/build/ovm 


$(HELLOWORLD)/build/ovm: 
	rm -rf $(HELLOWORLD)/build
	mkdir $(HELLOWORLD)/build
	cp -f $(HELLOWORLD)/ovm-config/* $(HELLOWORLD)/build
	cd $(HELLOWORLD)/src && javac -source 1.4 -cp $(OVM_BUILD)/src/syslib/user/ovm_scj/ -d ../build *.java
	cd $(HELLOWORLD)/build && $(realpath $(OVM_BUILD))/bin/gen-ovm \
		-threads=$(THREADS) \
		-opt=$(OPT) -panic-trace -dumpAll\
		-model=$(MODEL) \
		-engine=$(ENGINE) \
		-io=$(IOCONF) \
		-pad-image-size=$(PAD) \
		-main=$(LAUNCHER) -classpath=./ \
		-bootclasspath=$(BOOT_CLASSPATH) \
		-heap-size=$(IMMORTAL) \
		-scope-size=$(SCOPE) \
		#-reflective-class-trace="rclasses" \
		#-reflective-method-trace="rmethods" \
		#-ud-reflective-classes=@rc -ud-reflective-methods=@rm \
		 > gen.out 2>&1








##############################################################
############################## miniCDx #######################
##############################################################

CDX_LAUNCHER=Launcher
CDX_BOOT_CLASSPATH=$(OVM_BUILD)/src/syslib/user/ovm_scj/ovm_rt_user_scj.jar:$(OVM_BUILD)/src/syslib/user/ovm_rt_user.jar:$(OVM_BUILD)/src/syslib/user/ovm_platform/ovm_platform.jar

SCJ_J4=$(OVM_HOME)/src/syslib/user/ovm_scj/

cdx: compile_CDx gen_CDx run_CDx

compile_CDx:
	echo "\n----------------------"
	echo "compiling miniCDx"
	rm -rf $(CDX)/build
	mkdir $(CDX)/build
	find  $(CDX) -name *.java > $(CDX)/list
	cp -f $(CDX)/rc $(CDX)/build
	cp -f $(CDX)/rm $(CDX)/build
	javac -source 1.4 -target 1.4 -d $(CDX)/build -cp $(OVM_BUILD)/src/syslib/user/ovm_scj/ovm_rt_user_scj.jar:$(OVM_BUILD)/src/syslib/user/ovm_rt_user.jar:$(OVM_BUILD)/src/syslib/user/ovm_platform/ovm_platform.jar:$(CDX)/cdx:$(CDX)/utils:$(CDX)/simulator @$(CDX)/list
	rm $(CDX)/list
	
gen_CDx: compile_CDx
	cd $(CDX)/build  && $(realpath $(OVM_BUILD))/bin/gen-ovm \
		-threads=$(THREADS) \
		-opt=$(OPT) -panic-trace -dumpAll\
		-model=$(MODEL) \
		-engine=$(ENGINE) \
		-io=$(IOCONF) \
		-main=$(CDX_LAUNCHER) -classpath=./simulator/:./utils/:./cdx/:./ \
		-bootclasspath=$(CDX_BOOT_CLASSPATH) \
		-heap-size=$(IMMORTAL) \
		-scope-size=$(SCOPE)
		#-reflective-class-trace="rclasses" \
		#-reflective-method-trace="rmethods" \
		#-ud-reflective-classes=@rc -ud-reflective-methods=@rm \
		# > gen.out 2>&1

run_CDx:
	cd $(CDX)/build && ./ovm $(CDX_LAUNCHER) > miniCDx_out_x86




##############################################################
############################## ANY other Application #######################
##############################################################
#
#
#  src files must be in APP/src
#

APP=$(HOME)/examples/minicdx

APP_MAIN=Launcher

APP_CLASSPATH=.

APP_BOOT_CLASSPATH=$(OVM_BUILD)/src/syslib/user/ovm_scj/ovm_rt_user_scj.jar:$(OVM_BUILD)/src/syslib/user/ovm_rt_user.jar:$(OVM_BUILD)/src/syslib/user/ovm_platform/ovm_platform.jar

APP_SRC=$(APP)/src:$(APP_CLASSPATH)


SCJ_J4=$(OVM_HOME)/src/syslib/user/ovm_scj/

app: compile_app gen_app run_app

compile_app:
	echo "\n----------------------"
	echo "compiling the application"
	rm -rf $(APP)/build
	mkdir $(APP)/build
	find  $(APP) -name *.java > $(APP)/list
	cp -f $(APP)/rc $(APP)/build
	cp -f $(APP)/rm $(APP)/build
	javac -source 1.4 -target 1.4 -d $(APP)/build -cp $(OVM_BUILD)/src/syslib/user/ovm_scj/ovm_rt_user_scj.jar:$(OVM_BUILD)/src/syslib/user/ovm_rt_user.jar:$(OVM_BUILD)/src/syslib/user/ovm_platform/ovm_platform.jar:$(APP_SRC):./simulator/:./utils/:./cdx/:./ @$(APP)/list
	rm $(APP)/list
	
gen_app: compile_app
	cd $(APP)/build  && $(realpath $(OVM_BUILD))/bin/gen-ovm \
		-threads=$(THREADS) \
		-opt=$(OPT) -panic-trace -dumpAll\
		-model=$(MODEL) \
		-engine=$(ENGINE) \
		-io=$(IOCONF) \
		-main=$(APP_MAIN) -classpath=$(APP_SRC)/src:./simulator/:./utils/:./cdx/:./ \
		-bootclasspath=$(APP_BOOT_CLASSPATH) \
		-heap-size=$(IMMORTAL) \
		-scope-size=$(SCOPE)
		#-reflective-class-trace="rclasses" \
		#-reflective-method-trace="rmethods" \
		#-ud-reflective-classes=@rc -ud-reflective-methods=@rm \
		# > gen.out 2>&1

run_app:
	cd $(APP)/build && ./ovm $(APP_LAUNCHER) 



##############################################################
######################## SCJ #################################
##############################################################


prepare_scj: clean_scj copy_scj_here convert_scj copy_scj_to_ovm clean_after_scj

clean_scj_ovm:
	rm -rf $(OVM_BUILD)/src/syslib/user/ovm_scj/

	
copy_scj_to_ovm:
	counter=0; \
	for FILE in `find $(SCJ_NEW)/src -maxdepth 1 -type d`; do \
		if [ $$counter -eq 0 ]; then \
			counter=1; \
			continue; \
		fi; \
		cp -R $$FILE $(OVM_HOME)/src/syslib/user/ovm_scj/; \
	done

compile_scj:
	find $(SCJ_NEW)/src -name *.java > list
	javac -source 1.4 -target 1.4 -d $(SCJ_NEW)/bin @list

copy_scj_here:
	cp -R $(SCJ_SRC) ./
	rm -rf $(TEMP)/s3/fiji
	rm -rf $(TEMP)/spec/fiji
	rm -rf $(TEMP)/spec/common/src/javax/safetycritical/annotate
	rm -rf $(TEMP)/s3/common/src/edu/purdue/scj/annotate
	find $(TEMP) -name ".svn" -type d | xargs rm -rf
	mkdir $(SCJ_NEW)
	mkdir $(SCJ_NEW)/bin
	mkdir $(SCJ_NEW)/src
	for FILE in `find $(TEMP) -not -path "*svn*" -type d -name "src"`; do \
		counter=0; \
		#echo "$$FILE"; \
		for SUBDIR in `find $$FILE -maxdepth 1 -not -path "*svn*" -type d`; do \
			if [ $$counter -eq 0 ]; then \
				counter=1; \
				continue; \
			fi; \
			cp -R $$SUBDIR $(SCJ_NEW)/src; \
			#secho "$$SUBDIR"; \
		done; \
	done;

convert_scj:
	for file in `find $(SCJ_NEW)/src -name "*.java"`; \
	do \
		#echo "$${file}"; \
		sed -e 's/import javax.safetycritical.annotate./\/\/import javax.safetycritical.annotate./g' $${file} > temp; \
		sed -e 's/import static/\/\/import \/*static*\//g' temp > temp2; \
		sed -e '/^\s*@.*[\(].*[^\)a]$$/ { N;;s/\n/\n\/\//g }' temp2 > temp; \
		sed -e '/^\s*@/ s/^/\/\//g' temp > temp2; \
		sed -e 's/assert/\/\/assert/g' temp2 > temp; \
		sed -e '/edu.purdue.scj.annotate.NoScopeChecks;/ s/^/\/\//g' temp > temp2; \
		mv temp2 $${file}; \
		rm -rf temp; \
	done	

clean_after_scj:
	rm -rf ./ri
	rm -rf ./riJ4



##############################################################
######################### CLEAN #####################
##############################################################


clean-hello:	
	rm -rf $(HELLOWORLD)/build
	mkdir $(HELLOWORLD)/build
	rm -rf $(HELLOWORLD)/build/gen.ovm

clean: clean-hello  clean_scj
	rm -rf $(OVM_BUILD)

clean_scj:
	rm -rf $(TEMP)
	rm -rf $(SCJ_NEW)

