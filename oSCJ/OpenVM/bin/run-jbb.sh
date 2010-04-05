#! /bin/bash

JBB_DIR=${JBB_DIR:-/home/kalibera/SPECjbb05}
OVM=${OVM:-`dirname $0`/ovm}


$OVM spec.jbb.JBBmain -propfile SPECjbb-4x100.props
#$OVM spec.jbb.JBBmain -propfile SPECjbb-4x2000.props
#$OVM spec.jbb.JBBmain -propfile SPECjbb-4x50000.props