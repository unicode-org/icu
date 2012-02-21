#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
# Copyright (C) 2007-2012 IBM and Others. All Rights Reserved.
# All rights reserved.
#

from setuptools import setup, find_packages

PACKAGE = 'IcuCodeTools'
VERSION = '0.0.1'

setup(
    name=PACKAGE, version=VERSION,
    description='Miscellaneous ICU Extensions to Trac',
    author="Steven R. Loomis", author_email="srl@icu-project.org",
    license='BSD', url='http://icu-project.org',
    packages=find_packages(exclude=['ez_setup', '*.tests*']),
    package_data={
        'icucodetools': [
            'htdocs/css/*.css',
            'templates/*.html',
##            'htdocs/img/*.png',
           'htdocs/js/*.js',
        ]
    },
    entry_points = {
        'trac.plugins': [
            'icucodetools.ticketmgr = icucodetools.ticketmgr',
            'icucodetools.review = icucodetools.review',
            'icucodetools.tktlist = icucodetools.tktlist',
            'icucodetools.dcut = icucodetools.dcut'
        ]
    }
)
