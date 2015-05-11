#!/usr/bin/env python
from setuptools import setup, find_packages
from subspace import version

setup(
    name="subspace",
    version=0.2,
    description="A light-weight P2P anonymous messaging protocol",
    author="Chris Pacia",
    author_email="ctpacia@gmail.com",
    license="MIT",
    url="http://github.com/cpacia/subspace",
    packages=find_packages(),
    requires=["twisted"],
    install_requires=['twisted>=14.0']
)
