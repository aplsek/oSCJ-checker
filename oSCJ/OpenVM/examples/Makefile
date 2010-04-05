# Set current directory 
THISDIR:=$(shell pwd)

#  Set the OVM config directory (i.e., where OVM is to be configured)
OVM_CONF_DIR=${THISDIR}/config

#  Set the OVM install directory (i.e., where OVM is to be installed)
OVM_INST_DIR=${THISDIR}/install


# Set OVM bin directory
OVM_BIN_DIR=${THISDIR}/bin

# Set sources (all java files in the current directory and its subdirs)
SOURCES:=$(shell find ./ -name *.java)

# Set classes directory
CLASSDIR=${THISDIR}/classes

# Set path to OVM classpath runtime libraries
RTCLASSES=${OVM_INST_DIR}/ovm/classes/ovm_rt_user.jar
# If you have an application that uses RTSJ features, use this
#RTCLASSES=${OVM_INST_DIR}/ovm/classes/ovm_rt_user.jar:${OVM_INST_DIR}/ovm/classes/ovm_rt_user_realtime.jar

# Set which engine (one of j2c, simplejit, interpreter) is used
ENGINE=j2c

# Setptimization flags
OPT=

all : clean install compile build run

# OVM installation target
install:
	@mkdir -p ${OVM_CONF_DIR}
	cd ${OVM_CONF_DIR} ; \
	../../configure --prefix=${OVM_INST_DIR} ; \
	make all install

# Java sources compilation target
compile :
	@-mkdir -p ${CLASSDIR}
	@javac -g -classpath ${CLASSDIR} -bootclasspath ${RTCLASSES} -d ${CLASSDIR} ${SOURCES}

# Build target to have an image with given java main classes
build :
	@mkdir -p ${OVM_BIN_DIR}
	cd ${OVM_BIN_DIR} ; ${OVM_INST_DIR}/bin/gen-ovm     \
                -classpath=${CLASSDIR}                      \
                -main=HelloWorld                            \
                -engine=$(ENGINE)                           \
	        $(OPT)

# Run HelloWorld target
run:
	${OVM_BIN_DIR}/ovm ${OVM_BIN_DIR}/img HelloWorld

# Clean target
clean:
	@rm -rf ${OVM_INST_DIR}
	@rm -rf ${OVM_CONF_DIR}
	@rm -rf ${OVM_BIN_DIR}
	@rm -rf ${CLASSDIR}
