/*
 *******************************************************************************
 *
 *   Copyright (C) 1999-2001, International Business Machines
 *   Corporation and others.  All Rights Reserved.
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
    ~GUISupport() {};

    virtual void postErrorMessage(const char *message, const char *title) = 0;
};

#endif
