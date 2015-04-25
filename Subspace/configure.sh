#!/bin/bash
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 7F0CEB10
echo "deb http://repo.mongodb.org/apt/ubuntu "$(lsb_release -sc)"/mongodb-org/3.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-3.0.list
sudo apt-get install -y mongodb-org
sudo apt-get --quiet update || echo 'apt-get update failed. Continuing...'
sudo apt-get --assume-yes install python-pip build-essential python-dev python-twisted 
python setup.py install
echo "Subspace installation complete"
