/*
 *******************************************************************************
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others.
 * License & terms of use: http://www.unicode.org/copyright.html
 *
 *******************************************************************************
 *   file name:  GDIGUISupport.h
 *
 *   created on: 11/06/2001
 *   created by: Eric R. Mader
 */

#ifndef __GDIGUISUPPORT_H
#define __GDIGUISUPPORT_H

#include "GUISupport.h"

class GDIGUISupport : public GUISupport
{
public:
    GDIGUISupport() {};
    virtual ~GDIGUISupport() {};

    virtual void postErrorMessage(const char *message, const char *title);
};

#endif
