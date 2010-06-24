/*
 *******************************************************************************
 * Copyright (C) 1998-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 09, 2003
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import java.io.PrintStream;
import java.util.Vector;

public class ThaiStateTable
{
    static Vector stateTable = null;
    static int nextState = 0;
    
    private final static int newState()
    {
        ThaiStateTransition[] stateRow = new ThaiStateTransition[ThaiCharacterClasses.cCount];
        
        for (int c = 0; c < ThaiCharacterClasses.cCount; c += 1) {
            stateRow[c] = null;
        }
        
        stateTable.addElement(stateRow);
        
        return nextState++;
    }
    
    private final static boolean isLegalHere(int state, char pairAction)
    {
        switch (pairAction) {
        case 'A':
            return state == 0;
            
        case 'C':
        case 'D':
        case 'E':
        case 'F':
        case 'G':
        case 'H':
            return true;
        
        case 'R':
        case 'S':
            return false;
        }
        
        return false;
    }
    
    private final static boolean composesWithAnything(int charClass)
    {
        for (int c = 0; c < ThaiCharacterClasses.cCount; c += 1) {
            char action = ThaiCharacterClasses.getPairAction(charClass, c);
            
            if (action >= 'C' && action <= 'I') {
                return true;
            }
        }
        
        return false;
    }
    
    private final static void fixNextStates()
    {
        ThaiStateTransition[] groundState = (ThaiStateTransition[]) stateTable.elementAt(0);
        
        for (int s = 1; s < stateTable.size(); s += 1) {
            ThaiStateTransition[] state = (ThaiStateTransition[]) stateTable.elementAt(s);
            
            for (int c = 0; c < ThaiCharacterClasses.cCount; c += 1) {
                ThaiStateTransition transition = state[c];
                
                if (transition.getNextState() == 0) {
                    transition.setNextState(groundState[c].getNextState());
                }
            }
        }
    }
    
    private final static int addState(int prevClass, int prevPrevClass)
    {
        int state = newState();
        ThaiStateTransition[] stateRow = (ThaiStateTransition[]) stateTable.elementAt(state);
        
        for (int c = 0; c < ThaiCharacterClasses.cCount; c += 1) {
            char pairAction = ThaiCharacterClasses.getPairAction(prevClass, c);
            int nextSt = 0;
            
            switch (pairAction) {
            case 'G':
                if (prevClass == ThaiCharacterClasses.NIK &&
                    prevPrevClass == ThaiCharacterClasses.AV1) {
                    pairAction = 'R';
                } else if (prevPrevClass != ThaiCharacterClasses.COA) {
                    pairAction = 'C';
                }
                break;
                
            case 'E':
                if (prevPrevClass == ThaiCharacterClasses.COA) {
                    pairAction = 'F';
                }
                break;
                
            case 'I':
                if (prevClass == ThaiCharacterClasses.TON &&
                    (prevPrevClass < ThaiCharacterClasses.CON ||
                     prevPrevClass > ThaiCharacterClasses.COD)) {
                    pairAction = 'S';
                } else {
                    pairAction = 'A';
                }
                break;
                
            default:
                break;
            }
            
            if (c != prevClass && isLegalHere(state, pairAction) && composesWithAnything(c)) {
                nextSt = addState(c, prevClass);
            }
            
            stateRow[c] = new ThaiStateTransition(nextSt, pairAction);
        }
        
        return state;
    }
    
    static
    {
        stateTable = new Vector();
            
        addState(ThaiCharacterClasses.NON, ThaiCharacterClasses.NON);
            
        fixNextStates();
    }
    
    public static ThaiStateTransition getTransition(int state, int currClass)
    {
        ThaiStateTransition[] row = (ThaiStateTransition[]) stateTable.elementAt(state);
        
        return row[currClass];
    }

    private static String header0 =
"const ThaiShaping::StateTransition ThaiShaping::thaiStateTable[][ThaiShaping::classCount] = {";

    private static String header1 =
"    //+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+";

    private static String header2 =
"    //|         N         C         C         C         L         F         F         F         B         B         B         T         A         A         A         N         A         A         A    |\n" +
"    //|         O         O         O         O         V         V         V         V         V         V         D         O         D         D         D         I         V         V         V    |\n" +
"    //|         N         N         A         D         O         1         2         3         1         2         I         N         1         2         3         K         1         2         3    |";
    
    public static void writeStateTable(PrintStream output)
    {
        System.out.print("Writing state table...");
        
        output.println(header0);
        output.println(header1);
        output.println(header2);
        output.println(header1);
        
        for (int state = 0; state < stateTable.size(); state += 1) {
            ThaiStateTransition[] row = (ThaiStateTransition[]) stateTable.elementAt(state);
            
            output.print("    /*");
            
            if (state < 10) {
                output.print("0");
            }
            
            output.print(state);
            
            output.print("*/ {");
            
            for (int c = 0; c < ThaiCharacterClasses.cCount; c += 1) {
                row[c].write(output);
                
                if (c < ThaiCharacterClasses.cCount - 1) {
                    output.print(", ");
                }
            }
            
            output.print("}");
                    
            if (state < stateTable.size() - 1) {
                output.print(",");
            }
                    
            output.println();
        }
        
        output.println("};\n");
        
        System.out.println(" done.");
    }
}
