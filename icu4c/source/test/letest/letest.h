// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2014, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  letest.h
 *
 *   created on: 11/06/2000
 *   created by: Eric R. Mader
 */

#ifndef __LETEST_H
#define __LETEST_H

#include <stdlib.h>

#define NEW_ARRAY(type,count) (type *) malloc((count) * sizeof(type))
#define DELETE_ARRAY(array) free((void *) (array))

#endif
