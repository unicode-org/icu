<!--
* © 2017 and later: Unicode, Inc. and others.
* License & terms of use: http://www.unicode.org/copyright.html
-->
<!--
/*
*******************************************************************************
* Copyright (C) 2016 and later: Unicode, Inc. and others.
* License & terms of use: http://www.unicode.org/copyright.html
* Copyright (C) 2008-2013, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
* This is the XSLT for the API Report.
*/
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:param name="leftVer" />
  <xsl:param name="rightVer" />
  <xsl:param name="dateTime" />
  <xsl:param name="rightMilestone" />
  <xsl:param name="leftMilestone" />
  <xsl:param name="notFound" />
  <!-- <xsl:param name="ourYear" /> -->

  <xsl:output method="html" version="4.0"  doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
doctype-system="http://www.w3.org/TR/html4/loose.dtd"
	omit-xml-declaration="yes"	encoding="utf-8" indent="yes"/>


  <xsl:template match="/">
    <html>
	<xsl:comment>
     Copyright © 2016 and later: Unicode, Inc. and others.
     License &amp; terms of use: http://www.unicode.org/copyright.html
	</xsl:comment>
    <head>
    <title>ICU4C API Comparison: <xsl:value-of select="$leftVer"/><xsl:value-of select="$leftMilestone" /> with <xsl:value-of select="$rightVer" /><xsl:value-of select="$rightMilestone" /> </title>
    <link rel="stylesheet" href="icu4c.css" type="text/css" />
    </head>

    <body>

    <a name="#_top"></a>

    <h1>ICU4C API Comparison: <xsl:value-of select="$leftVer"/><xsl:value-of select="$leftMilestone" /> with <xsl:value-of select="$rightVer" /><xsl:value-of select="$rightMilestone" /> </h1>

    <div id="toc">
	    <ul>
	    	<li><a href="#removed">Removed from <xsl:value-of select="$leftVer"/></a></li>
	    	<li><a href="#deprecated">Deprecated or Obsoleted in <xsl:value-of select="$rightVer" /></a></li>
	    	<li><a href="#changed">Changed in  <xsl:value-of select="$rightVer" /></a></li>
	    	<li><a href="#promoted">Promoted to stable in <xsl:value-of select="$rightVer" /></a></li>
	    	<li><a href="#added">Added in <xsl:value-of select="$rightVer" /></a></li>
	    	<li><a href="#other">Other existing drafts in <xsl:value-of select="$rightVer" /></a></li>
	    	<li><a href="#simplifications">Signature Simplifications</a></li>
	    </ul>
	    <hr />
	</div>

	<a name="removed"></a>
	    <h2>Removed from <xsl:value-of select="$leftVer"/> </h2>
        <xsl:call-template name="genTable">
            <xsl:with-param name="nodes" select="/list/func[@rightStatus=$notFound]"/>
        </xsl:call-template>
    <P/><a href="#_top">(jump back to top)</a><hr/>

	<a name="deprecated"></a>
    <h2>Deprecated or Obsoleted in <xsl:value-of select="$rightVer" /></h2>
        <xsl:call-template name="genTable">
            <xsl:with-param name="nodes" select="/list/func[(@rightStatus='Deprecated' and @leftStatus!='Deprecated') or (@rightStatus='Obsolete' and @leftStatus!='Obsolete')]"/>
        </xsl:call-template>
    <P/><a href="#_top">(jump back to top)</a><hr/>

	<a name="changed"></a>
    <h2>Changed in  <xsl:value-of select="$rightVer" /> (old, new)</h2>
        <xsl:call-template name="genTable">
            <xsl:with-param name="nodes" select="/list/func[(@leftStatus != $notFound) and (@rightStatus != $notFound) and ( (@leftStatus != @rightStatus) or (@leftVersion != @rightVersion) ) and not ( (@leftStatus = 'Draft') and (@rightStatus = 'Stable') and (@rightVersion = $rightVer) )]"/>
        </xsl:call-template>
    <P/><a href="#_top">(jump back to top)</a><hr/>

	<a name="promoted"></a>
    <h2>Promoted to stable in <xsl:value-of select="$rightVer" /></h2>
        <xsl:call-template name="genTable">
            <xsl:with-param name="nodes" select="/list/func[@leftStatus != 'Stable' and  @rightStatus = 'Stable']"/>
        </xsl:call-template>
    <P/><a href="#_top">(jump back to top)</a><hr/>

    <a name="added"></a>
    <h2>Added in <xsl:value-of select="$rightVer" /></h2>
        <xsl:call-template name="genTable">
            <xsl:with-param name="nodes" select="/list/func[@leftStatus=$notFound]"/>
        </xsl:call-template>
    <P/><a href="#_top">(jump back to top)</a><hr/>

    <a name="other"></a>
    <h2>Other existing drafts in <xsl:value-of select="$rightVer" /></h2>
    <div class='other'>
        <xsl:call-template name="infoTable"> <!--  note: note genTable -->
            <xsl:with-param name="nodes" select="/list/func[@rightStatus = 'Draft' and @rightVersion != $rightVer]"/>
        </xsl:call-template>
    </div>
    <P/><a href="#_top">(jump back to top)</a><hr/>

    <a name="simplifications"></a>
    <h2>Signature Simplifications</h2>
    <i>This section shows cases where the signature was "simplified" for the sake of comparison. The simplified form is in bold, followed by
    	all possible variations in "original" form.</i>
    <div class='other'>
    	<ul>
    		<xsl:for-each select="/list/simplifications/simplification">
    			<li><b><xsl:value-of select="base" /></b>
    			<br />
    				<xsl:for-each select="change">
    					<xsl:value-of select="text()" /><br />
    				</xsl:for-each>
    			</li>
    		</xsl:for-each>
    	</ul>
    </div>
    <P/><a href="#_top">(jump back to top)</a><hr/>
<!--

-->

    <p><i><font size="-1">Contents generated by StableAPI tool on <xsl:value-of select="$dateTime" /><br/>
        Copyright © 2017 and later: Unicode, Inc. and others.<br/>
        License &amp; terms of use: http://www.unicode.org/copyright.html
    </font></i></p>
    </body>
    </html>
  </xsl:template>

  <!-- almost all sutables are generated by this -->
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
<!--
            <xsl:comment>
              @prototype: <xsl:value-of select="@prototype" />
              @leftStatus: <xsl:value-of select="@leftStatus" />
              @leftVersion: <xsl:value-of select="@leftVersion" />
              @rightStatus: <xsl:value-of select="@rightStatus" />
              @rightVersion: <xsl:value-of select="@rightVersion" />
            </xsl:comment>
-->
            <tr>
                <xsl:attribute name="class">
                    <xsl:value-of select="'row'"/>
                    <xsl:value-of select="(position() mod 2)"/> <!-- for even-odd row colorings -->
                    <!--
                    <xsl:choose>
                        <xsl:when test="(position() mod 2) = 0"><xsl:value-of select="row0" /></xsl:when>
                        <xsl:otherwise><xsl:value-of select="row1" /></xsl:otherwise>
                    </xsl:choose>
                    -->
                </xsl:attribute>
                <td class='file'> <xsl:value-of select="@file" /> </td>
                <td class='proto'> <xsl:value-of disable-output-escaping="yes" select="@prototype" /> </td>
                <td>
                    <xsl:attribute name="class">
                        <xsl:if test ="@leftStatus = 'Stable'">
                                <xsl:value-of select="'stabchange'" />
                        </xsl:if>
                    </xsl:attribute>
                   	<xsl:if  test = "@leftStatus = 'Draft' and @rightStatus = 'Stable' and @leftVersion = @rightVersion">
	                    <xsl:attribute name="colspan">2</xsl:attribute>
	                    <xsl:attribute name="align">center</xsl:attribute>
                   	</xsl:if>

                    <xsl:value-of select="@leftStatus" />
                    <xsl:if  test = "@leftStatus = 'Draft' and @rightStatus = 'Stable' and @leftVersion = @rightVersion">&#x2192;Stable</xsl:if>
                    <xsl:if test="@leftVersion != '' and @leftVersion != '.'">
	              <br/>
	              <xsl:value-of select="@leftVersion" />
	            </xsl:if>
                    <xsl:if test="@leftStatus = '' and @leftVersion = ''">
                      <i>(untagged)</i>
                    </xsl:if>
                </td>
           	<xsl:if  test = "@leftStatus != 'Draft' or @rightStatus != 'Stable' or @leftVersion != @rightVersion">
                <td> <xsl:value-of select="@rightStatus" />
                    <br/>
                    <span>
                        <xsl:attribute name="class">
                            <xsl:if test ="@leftVersion != @rightVersion and @leftVersion != '' and @rightVersion != '' and @rightVersion != '.'">
                                <xsl:value-of select="'verchange'" />
                            </xsl:if>
                        </xsl:attribute>
                        <xsl:if test="@rightVersion != '.'">
	                        <span>
	                            <xsl:value-of select="@rightVersion" />
	                        </span>
                        </xsl:if>
                        <xsl:if test ="@leftVersion != @rightVersion and @leftVersion != '' and @rightVersion != '' and @rightStatus = 'Stable' and not ( (@leftStatus = 'Draft') and (@rightStatus = 'Stable') and (@rightVersion = $rightVer) )">
                            <br/><b title='A stable API changed version.' class='bigwarn'>(changed)</b>
                        </xsl:if>
                        <xsl:if test ="@rightStatus = 'Draft' and @rightVersion != $rightVer">
                            <br/><b title='A draft API has the wrong version.' class='bigwarn'>(should be <xsl:value-of select="$rightVer"/>)</b>
                        </xsl:if>
                        <xsl:if test="@leftStatus = $notFound and @rightVersion = '' and @rightStatus != 'Internal' and @rightStatus != 'Deprecated'">
                        	<br/><b title='A new API was introduced that was not tagged.' class='bigwarn'>(untagged)</b>
                        </xsl:if>
                    </span>
                </td>
           </xsl:if>
           	<xsl:if  test = "@rightStatus = 'Stable' and @rightVersion = $rightVer">
                  <td class='bornstable'>
                    <b class='bigwarn'><xsl:attribute name="title">A new API was introduced as stable in <xsl:value-of select='$rightVer'/>.</xsl:attribute>(Born Stable)</b>
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
                <td class='proto'> <xsl:value-of disable-output-escaping="yes" select="@prototype" /> </td>
                <td>
                    <xsl:attribute name="class">
                        <xsl:if test ="@leftStatus = 'Stable'">
                                <xsl:value-of select="'stabchange'" />
                        </xsl:if>
                    </xsl:attribute>

                   	<xsl:if  test = "@leftStatus = @rightStatus and @leftVersion = @rightVersion">
	                    <xsl:attribute name="colspan">2</xsl:attribute>
	                    <xsl:attribute name="align">center</xsl:attribute>
                   	</xsl:if>

                    <xsl:value-of select="@leftStatus" />
                    <br/> <xsl:value-of select="@leftVersion" />
                </td>
                <xsl:if test = "@leftStatus != @rightStatus or @leftVersion != @rightVersion">
                <td> <xsl:value-of select="@rightStatus" />
                    <br/>
                    <span>
                        <xsl:attribute name="class">
                            <xsl:if test ="@leftVersion != @rightVersion and @leftVersion != '' and @rightVersion != '' and @rightVersion != '.'">
                                <xsl:value-of select="'verchange'" />
                            </xsl:if>
                        </xsl:attribute>
                        <span>
                        	<xsl:if test = "@rightVersion != '.'">
	                            <xsl:value-of select="@rightVersion" />
	                        </xsl:if>
                        </span>
                   <!--
                        <xsl:if test ="@leftVersion != @rightVersion and @leftVersion != '' and @rightVersion != '' and @rightStatus = 'Stable'">
                            <br/><b title='A stable API changed version.' class='bigwarn'>(changed)</b>
                        </xsl:if>
                        <xsl:if test ="@rightStatus = 'Draft' and @rightVersion != $rightVer">
                            <br/><b title='A draft API has the wrong version.' class='bigwarn'>(should be <xsl:value-of select="$rightVer"/>)</b>
                        </xsl:if>
                        <xsl:if test="@leftStatus = $notFound and @rightVersion = ''">
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


    <xsl:template name="pureVirtualTable">
    <xsl:param name="nodes" />
    <table class='genTable' BORDER="1">
    <!--
    <THEAD>
        <tr>
            <th> <xsl:value-of select="'File'" /> </th>
            <th> <xsl:value-of select="'API'" /> </th>
            <th> <xsl:value-of select="$leftVer" /> </th>
            <th> <xsl:value-of select="$rightVer" /> </th>
        </tr>
    </THEAD>
    -->

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
                <td class='proto'> <xsl:value-of disable-output-escaping="yes" select="@prototype" /> </td>
                <td>
                    <xsl:attribute name="class">
                        <xsl:if test ="@leftStatus = 'Stable'">
                                <xsl:value-of select="'stabchange'" />
                        </xsl:if>
                    </xsl:attribute>

                   	<xsl:if  test = "@leftStatus = @rightStatus and @leftVersion = @rightVersion">
	                    <xsl:attribute name="colspan">2</xsl:attribute>
	                    <xsl:attribute name="align">center</xsl:attribute>
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
                        <xsl:if test="@leftStatus = $notFound and @rightVersion = ''">
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
