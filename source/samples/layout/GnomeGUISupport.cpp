/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  GnomeGUISupport.h
 *
 *   created on: 11/06/2001
 *   created by: Eric R. Mader
 */

#include <stdio.h>

#include "GnomeGUISupport.h"

void GnomeGUISupport::postErrorMessage(const char *message, const char *title)
{
    fprintf(stderr, "%s: %s\n", title, message);
}


