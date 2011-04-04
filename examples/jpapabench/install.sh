#!/bin/bash

PAPA=jpapabench

if [ ! -d "$PAPA" ]; then
    hg clone https://jpaparazzi.jpapabench.googlecode.com/hg/ jpapabench-jpaparazzi 
else
    cd $PAPA && hg pull && hg up
fi


