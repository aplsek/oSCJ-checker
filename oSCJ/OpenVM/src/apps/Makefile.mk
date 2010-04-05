JAVA_CLASSPATH=.:$(srcdir)

# currently apps are compiled into their own jar - there seems no reason to
# do this otherwise.
# When compiling we don't know if it is a real-time app or not so we must
# have both runtime jar files in the bootclasspath - with the realtime jar
# second just so we don't pick up any realtime classes unintentionally.
# When we gen a real-time image then the realtime jar comes first
JAVA_BOOTCLASSPATH=../syslib/user/ovm_rt_user.jar:../syslib/user/ovm_realtime/ovm_rt_user_realtime.jar

JAVA_JARFILE=ovm_apps.jar

all: $(JAVA_JARFILE)

clean: java-clean

install: java-install
