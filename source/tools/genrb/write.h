/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1998, 1999     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*
* File write.h
*
* Modification History:
*
*   Date        Name        Description
*   06/01/99    stephen     Creation.
*******************************************************************************
*/

#ifndef WRITE_H
#define WRITE_H 1

#include "utypes.h"
#include "rblist.h"
#include "filestrm.h"

/* Write a resource bundle item list to a file */
void rb_write(FileStream *f, struct SRBItemList *data, UErrorCode *status);

#endif
