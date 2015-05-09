#!/bin/bash

if [ $1 == "start" ] ; then
	echo "Subspace starting..."
	cd "$HOME/.subspace"
	if [ "$2" == "--noisy" ] ; then
		twistd -noy subspaced.py
	else 
		twistd -oy subspaced.py
	fi
elif [ $1 == "stop" ] ; then
	echo "Stopping subspace..."
	cd "$HOME/.subspace"
	kill -INT `cat twistd.pid`
else 
	cd "$HOME/.subspace"
	python subspace-cli.py $*
fi

