#!/bin/bash

if [ "$1" == "start" ] ; then
	echo "  _________    ___."                                      
	echo " /   _____/__ _\_ |__  _________________   ____  ____ " 
	echo " \_____  \|  |  \ __ \/  ___/\____ \__  \_/ ___\/ __ \ "
	echo " /        \  |  / \_\ \___ \ |  |_> > __ \  \__\  ___/ "
	echo "/_______  /____/|___  /____ >|   __(____ /\___  >___  >"
	echo "        \/          \/    \/ |__|       \/    \/    \/"  
	cd "$HOME/.subspace"
	if [ "$2" == "--noisy" ] ; then
		twistd -noy subspaced.py
	else 
		twistd -oy subspaced.py
	fi
elif [ "$1" == "stop" ] ; then
	echo "Stopping subspace..."
	cd "$HOME/.subspace"
	kill -INT `cat twistd.pid`
else 
	cd "$HOME/.subspace"
	python subspace-cli.py $*
fi

