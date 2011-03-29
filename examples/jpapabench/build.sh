#!/bin/bash

PAPA=jpapabench

if [ ! -d "$PAPA" ]; then
    hg clone https://jpapabench.googlecode.com/hg/ $PAPA 
else
    cd $PAPA && hg pull && hg up
fi


