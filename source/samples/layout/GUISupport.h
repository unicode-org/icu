/*
 *******************************************************************************
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others.
 * License & terms of use: http://www.unicode.org/copyright.html
 *
 *******************************************************************************
 *   file name:  GUISupport.h
 *
 *   created on: 11/06/2001
 *   created by: Eric R. Mader
 */

#ifndef __GUISUPPORT_H
#define __GUISUPPORT_H

class GUISupport
{
public:
    GUISupport() {};
    virtual ~GUISupport() {};

    virtual void postErrorMessage(const char *message, const char *title) = 0;
};

#endif
