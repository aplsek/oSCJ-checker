#! /bin/bash
#
#
#
#   To generate the rm and rc files, run this script inside the /helloworld/build directory!
#
#
#
#


OVM_BUILD=/home/ales/scj-test2/build
LAUNCHER=Launcher
BOOT_CLASSPATH=$OVM_BUILD/src/syslib/user/ovm_scj/ovm_rt_user_scj.jar:$OVM_BUILD/src/syslib/user/ovm_rt_user.jar:$OVM_BUILD/src/syslib/user/ovm_platform/ovm_platform.jar

I=0
while [ $I -lt 1 ] ; do

	echo "run $I"

  I=`expr $I + 1`
  cp rm rm.$I
  $OVM_BUILD/bin/gen-ovm -threads=RealtimeJVM -model=TriPizloPtrStack-FakeScopesWithAreaOf-Bg_Mf_F_H_S -ioconfigurator=SelectSockets_PollingOther -main=$LAUNCHER -classpath=./ -bootclasspath=$BOOT_CLASSPATH -reflective-class-trace="rclasses" -reflective-method-trace="rmethods" -ud-reflective-classes=@rc -ud-reflective-methods=@rm  > gen.out.$I 2>&1

	echo "compiled! gen-ovm OK!"
  
  if grep -q SIGBUS gen.out.$I ; then
  	sleep 2m
	echo "sleeps"
  	continue
  fi
  
#  ./ovm img edu.purdue.scj.Launcher HelloWorld /home/tomas/svn/scj-project/scj/example/hw.prop > ovm.out.$I 2>&1
#  ./ovm edu.purdue.scj.Launcher HelloWorld /home/tomas/svn/scj-project/scj/example/hw.prop > ovm.out.$I 2>&1

  ./ovm Launcher MyHelloWorld > ovm.out.$I 2>&1
  
	echo "after ovm run"

  AGAIN=no
  
  cat rmethods rm | sort -u > rm.new
  cat rm | sort -u > rm.old
  
  
  cat rclasses rc | sort -u > rc.new
  cat rc | sort -u > rc.old
  

  if diff -q rc.new rc.old && diff -q rm.new rm.old ; then
  	true
  else
  	cp rc.new rc
  	cp rm.new rm
  	
  	rm -rf scratch
  	AGAIN=yes
  fi


  LLINE=`tail -1 ovm.out.$I`
#  LMETHOD=`tail -1 rm`
#  RMETHOD=`tail -1 rmethods`
 
#  if [ "X$LMETHOD" != "X$RMETHOD" ] ; then
#  	tail -1 rmethods >> rm
#   	tail -1 rmethods >> found_methods
#  fi
  
  if grep -q 'defineClass:' ovm.out.$I ; then
  	CLASS=`echo $LLINE  | sed -e 's/defineClass: L\([^;]\+\);.*/\1/g' | tr '/' '.'`
  	echo "$CLASS" >> rc
  	echo "$CLASS" >> found_classes
	AGAIN=yes
  fi
  		
  if echo "$LLINE" | grep -q 'try_to_add_method:'  ; then
  	tail -1 ovm.out.$I | sed -e 's/.*try_to_add_method: \(.*\)/\1/g' >> rm
  	tail -1 ovm.out.$I | sed -e 's/.*try_to_add_method: \(.*\)/\1/g' >> found_methods
  	AGAIN=yes
  fi

  if echo "$LLINE" | grep -q 'try_to_add_class:'  ; then
  	tail -1 ovm.out.$I | sed -e 's/.*try_to_add_class: \([^;]\+\);.*/\1/g' >> rm
  	tail -1 ovm.out.$I | sed -e 's/.*try_to_add_class: \([^;]\+\);.*/\1/g' >> found_classes
  	AGAIN=yes
  fi

  
  if [ X$AGAIN == Xyes ] ; then
  	continue
  fi

  # bail out, either we are done, or there is some problem to solve manually

  exit
done  

