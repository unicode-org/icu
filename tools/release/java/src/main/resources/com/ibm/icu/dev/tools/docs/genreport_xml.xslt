<?xml version="1.0" encoding="utf-8"?>
<!--
* © 2017 and later: Unicode, Inc. and others.
* License & terms of use: http://www.unicode.org/copyright.html
-->
<!--
/*
*******************************************************************************
* Copyright (C) 2016 and later: Unicode, Inc. and others.
* License & terms of use: http://www.unicode.org/copyright.html
* Copyright (C) 2008-2010, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
* This is the XSLT for the API Report, XML style
*/
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!--
  <xsl:param name="leftStatus" />
  <xsl:param name="rightStatus" />
-->
  <xsl:param name="leftVer" />
  <xsl:param name="rightVer" />
  <xsl:param name="dateTime" />
  <xsl:param name="nul" />

  <!-- <xsl:param name="ourYear" /> -->
  

  <xsl:template match="/">
    <xsl:comment>
      Copyright © 2017 and later: Unicode, Inc. and others.
      License &amp; terms of use: http://www.unicode.org/copyright.html
    </xsl:comment>
    <changeReport>
      <identity>
	<xsl:attribute name="left">
	   <xsl:value-of select="$leftVer"/>
	</xsl:attribute>
	<xsl:attribute name="right">
	  <xsl:value-of select="$rightVer" />
	</xsl:attribute>
	<xsl:attribute name="generated-date">
	  <xsl:value-of select="$dateTime" />
	</xsl:attribute>
      </identity>

      <!-- <link rel="stylesheet" href="icu4c.css" type="text/css" /> -->

      <functions name="removed">
        <xsl:call-template name="genTable">
            <xsl:with-param name="nodes" select="/list/func[@rightStatus=$nul]"/>
        </xsl:call-template>
      </functions>

      <functions name="deprecated">
        <xsl:call-template name="genTable">
            <xsl:with-param name="nodes" select="/list/func[(@rightStatus='Deprecated' and @leftStatus!='Deprecated') or (@rightStatus='Obsolete' and @leftStatus!='Obsolete')]"/>
        </xsl:call-template>
      </functions>

      <functions name="changed">
        <xsl:call-template name="genTable">
            <xsl:with-param name="nodes" select="/list/func[(@leftStatus != $nul) and (@rightStatus != $nul) and ( (@leftStatus != @rightStatus) or (@leftVersion != @rightVersion) )]"/>
        </xsl:call-template>
      </functions>

      <functions name="promoted">
        <xsl:call-template name="genTable">
            <xsl:with-param name="nodes" select="/list/func[@leftStatus != 'Stable' and  @rightStatus = 'Stable']"/>
        </xsl:call-template>
      </functions>
    
      <functions name="added">
        <xsl:call-template name="genTable">
            <xsl:with-param name="nodes" select="/list/func[@leftStatus=$nul]"/>
        </xsl:call-template>
      </functions>

      <functions name="draft">
        <xsl:call-template name="infoTable">
            <xsl:with-param name="nodes" select="/list/func[@rightStatus = 'Draft' and @rightVersion != $rightVer]"/>
        </xsl:call-template>
      </functions>

    </changeReport>
  </xsl:template>

  <xsl:template name="genTable">
    <xsl:param name="nodes" />
    <table class='genTable' BORDER="1">
    <THEAD>
        <tr>
            <th> <xsl:value-of select="'File'" /> </th>
            <th> <xsl:value-of select="'API'" /> </th>
            <th> <xsl:value-of select="$leftVer" /> </th>
            <th> <xsl:value-of select="$rightVer" /> </th>
        </tr>
    </THEAD>

        <xsl:for-each select="$nodes">
            <xsl:sort select="@file" />
            
            <tr>
                <xsl:attribute name="class">
                    <xsl:value-of select="'row'"/>
                    <xsl:value-of select="(position() mod 2)"/>
                    <!-- 
                    <xsl:choose>
                        <xsl:when test="(position() mod 2) = 0"><xsl:value-of select="row0" /></xsl:when>
                        <xsl:otherwise><xsl:value-of select="row1" /></xsl:otherwise>
                    </xsl:choose>
                    -->
                </xsl:attribute>
                <td class='file'> <xsl:value-of select="@file" /> </td>
                <td class='proto'> <xsl:value-of select="@prototype" /> </td>
                <td>
                    <xsl:attribute name="class">
                        <xsl:if test ="@leftStatus = 'Stable'">
                                <xsl:value-of select="'stabchange'" />
                        </xsl:if>
                    </xsl:attribute>
                   	<xsl:if  test = "@leftStatus = 'Draft' and @rightStatus = 'Stable' and @leftVersion = @rightVersion">
	                    <xsl:attribute name="colspan">
       	            		2
       	            	</xsl:attribute>
	                    <xsl:attribute name="align">
       	            		center
       	            	</xsl:attribute>
                   	</xsl:if>
                
                    <xsl:value-of select="@leftStatus" /><xsl:if  test = "@leftStatus = 'Draft' and @rightStatus = 'Stable' and @leftVersion = @rightVersion">&gt;Stable</xsl:if>
                    <br/> <xsl:value-of select="@leftVersion" />
                </td>
           	<xsl:if  test = "@leftStatus != 'Draft' or @rightStatus != 'Stable' or @leftVersion != @rightVersion">
                <td> <xsl:value-of select="@rightStatus" /> 
                    <br/> 
                    <span>
                        <xsl:attribute name="class">
                            <xsl:if test ="@leftVersion != @rightVersion and @leftVersion != '' and @rightVersion != ''">
                                <xsl:value-of select="'verchange'" />                                
                            </xsl:if>
                        </xsl:attribute>              
                        <span>              
                            <xsl:value-of select="@rightVersion" />
                        </span>
                        <xsl:if test ="@leftVersion != @rightVersion and @leftVersion != '' and @rightVersion != '' and @rightStatus = 'Stable'">
                            <br/><b title='A stable API changed version.' class='bigwarn'>(changed)</b>
                        </xsl:if>
                        <xsl:if test ="@rightStatus = 'Draft' and @rightVersion != $rightVer">
                            <br/><b title='A draft API has the wrong version.' class='bigwarn'>(should be <xsl:value-of select="$rightVer"/>)</b>
                        </xsl:if>
                        <xsl:if test="@leftStatus = 'None' and @rightVersion = ''">
                        	<br/><b title='A new API was introduced that was not tagged.' class='bigwarn'>(untagged)</b>
                        </xsl:if>
                    </span>
                </td>
           </xsl:if>
            </tr>
        </xsl:for-each>
    </table>
  </xsl:template>
  
    <xsl:template name="infoTable">
    <xsl:param name="nodes" />
    <table class='genTable' BORDER="1">
    <THEAD>
        <tr>
            <th> <xsl:value-of select="'File'" /> </th>
            <th> <xsl:value-of select="'API'" /> </th>
            <th> <xsl:value-of select="$leftVer" /> </th>
            <th> <xsl:value-of select="$rightVer" /> </th>
        </tr>
    </THEAD>

        <xsl:for-each select="$nodes">
            <xsl:sort select="@file" />
            
            <tr>
                <xsl:attribute name="class">
                    <xsl:value-of select="'row'"/>
                    <xsl:value-of select="(position() mod 2)"/>
                    <!-- 
                    <xsl:choose>
                        <xsl:when test="(position() mod 2) = 0"><xsl:value-of select="row0" /></xsl:when>
                        <xsl:otherwise><xsl:value-of select="row1" /></xsl:otherwise>
                    </xsl:choose>
                    -->
                </xsl:attribute>
                <td class='file'> <xsl:value-of select="@file" /> </td>
                <td class='proto'> <xsl:value-of select="@prototype" /> </td>
                <td>
                    <xsl:attribute name="class">
                        <xsl:if test ="@leftStatus = 'Stable'">
                                <xsl:value-of select="'stabchange'" />
                        </xsl:if>
                    </xsl:attribute>
                    
                   	<xsl:if  test = "@leftStatus = @rightStatus and @leftVersion = @rightVersion">
	                    <xsl:attribute name="colspan">
       	            		2
       	            	</xsl:attribute>
	                    <xsl:attribute name="align">
       	            		center
       	            	</xsl:attribute>
                   	</xsl:if>
               
                    <xsl:value-of select="@leftStatus" />
                    <br/> <xsl:value-of select="@leftVersion" />
                </td>
                <xsl:if test = "@leftStatus != @rightStatus or @leftVersion != @rightVersion">
                <td> <xsl:value-of select="@rightStatus" /> 
                    <br/> 
                    <span>
                        <xsl:attribute name="class">
                            <xsl:if test ="@leftVersion != @rightVersion and @leftVersion != '' and @rightVersion != ''">
                                <xsl:value-of select="'verchange'" />                                
                            </xsl:if>
                        </xsl:attribute>              
                        <span>              
                            <xsl:value-of select="@rightVersion" />
                        </span>
                   <!-- 
                        <xsl:if test ="@leftVersion != @rightVersion and @leftVersion != '' and @rightVersion != '' and @rightStatus = 'Stable'">
                            <br/><b title='A stable API changed version.' class='bigwarn'>(changed)</b>
                        </xsl:if>
                        <xsl:if test ="@rightStatus = 'Draft' and @rightVersion != $rightVer">
                            <br/><b title='A draft API has the wrong version.' class='bigwarn'>(should be <xsl:value-of select="$rightVer"/>)</b>
                        </xsl:if>
                        <xsl:if test="@leftStatus = 'None' and @rightVersion = ''">
                        	<br/><b title='A new API was introduced that was not tagged.' class='bigwarn'>(untagged)</b>
                        </xsl:if>
                     -->
                    </span>
                </td>
                </xsl:if>
            </tr>
        </xsl:for-each>
    </table>
  </xsl:template>
  
</xsl:stylesheet>




