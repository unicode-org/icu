/*
 * (C) Copyright IBM Corp. 1998-2004.  All Rights Reserved.
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */
package com.ibm.richtext.test;

import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.styledtext.StyledText;
import com.ibm.richtext.textlayout.attributes.AttributeMap;

/**
 * This class contains the first four paragraphs of the Declaration
 * of Independence, as both styled and unstyled text.  The views
 * expressed therein are not necessarily those of the programmer or of
 * his/her employer.  No criticism of any monarchy, oligarchy, dictatorship,
 * autocracy, plutocracy, theocracy, anarchist territory, colonial power,
 * or any other nondemocratic form of government is intended.  This document
 * is provided "as-is" without any warranty expressed or implied.
 */
public final class Declaration {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    public static final String fgDeclarationStr = "In Congress, July 4, 1776, THE UNANIMOUS DECLARATION OF THE THIRTEEN UNITED STATES OF AMERICA\n" +
        "\n" +
        "When in the Course of human Events, it becomes necessary for one People to dissolve the Political Bands which have connected them with another, and to assume among the Powers of the Earth, the separate and equal Station to which the Laws of Nature and of Nature's God entitle them, a decent Respect to the Opinions of Mankind requires that they should declare the causes which impel them to the Separation.\n" +
        "\n" +
        "We hold these Truths to be self-evident, that all Men are created equal, that they are endowed by their Creator with certain unalienable Rights, that among these are Life, Liberty, and the Pursuit of Happiness.\n" +
        "\n" +
        "That to secure these Rights, Governments are instituted among Men, deriving their just Powers from the Consent of the Governed, that whenever any Form of Government becomes destructive of these Ends, it is the Right of the People to alter or to abolish it, and to institute new Government, laying its Foundation on such Principles, and organizing its Powers in such Form, as to them shall seem most likely to effect their Safety and Happiness. Prudence, indeed, will dictate that Governments long established should not be changed for light and transient Causes; and accordingly all Experience hath shewn, that Mankind are more disposed to suffer, while Evils are sufferable, than to right themselves by abolishing the Forms to which they are accustomed. But when a long Train of Abuses and Usurpations, pursuing invariably the same Object, evinces a Design to reduce them under absolute Despotism, it is their Right, it is their Duty, to throw off such Government, and to provide new Guards for their future Security.\n" +
        "\n";

    public static final MConstText fgDeclaration = new StyledText(fgDeclarationStr, AttributeMap.EMPTY_ATTRIBUTE_MAP);
}
