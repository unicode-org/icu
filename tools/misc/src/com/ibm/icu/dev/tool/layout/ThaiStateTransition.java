/*
 *******************************************************************************
 * Copyright (C) 1998-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 09, 2003
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import java.io.PrintStream;

public class ThaiStateTransition
{
    int nextState;
    char action;
        
    public ThaiStateTransition(int nextState, char action)
    {
        this.nextState = nextState;
        this.action = action;
    }
        
    public final int getNextState()
    {
        return nextState;
    }
        
    public final char getAction()
    {
        return action;
    }
        
    public final void setNextState(int newNextState)
    {
        nextState = newNextState;
    }
        
    public final void setAction(char newAction)
    {
        action = newAction;
    }

    public String toString()
    {
        return ((nextState < 10) ? "0" : "") + nextState + "/" + action + " ";
    }
    
    public void write(PrintStream output)
    {
        output.print("{");
        
        if (nextState < 10) {
            output.print(" ");
        }
        
        output.print(nextState);
        
        output.print(", t");
        output.print(action);
        output.print("}");
    }
    
}
