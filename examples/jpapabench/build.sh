#!/bin/bash

PAPA=jpapabench

if [ ! -d "$PAPA" ]; then
    hg clone https://petokmet-jpaparazzi.googlecode.com/hg/ jpaparazzi
else
    cd $PAPA && hg pull && hg up
fi


