<?xml version="1.0" encoding="UTF-8"?>
<!--
* © 2017 and later: Unicode, Inc. and others.
* License & terms of use: http://www.unicode.org/copyright.html
-->
<!-- Copyright (C) 2009-2011, International Business Machines Corporation and others. All Rights Reserved. -->
<!-- List all cpp public functions generated from the 'index.xml' -->
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:param name="docFolder" />

	<xsl:template match="/">
		<list>
			<xsl:variable name="files_node"
				select="/doxygenindex/compound[@kind='class']/@refid" />
			<xsl:for-each select="$files_node">
				<xsl:variable name="file"
					select="concat($docFolder, '/', . , '.xml')" />
				<xsl:variable name="funcs_node"
					select="document($file)/doxygen/compounddef[@prot='public']/sectiondef/memberdef[@prot='public'][@kind='function']" />
				<xsl:variable name="compoundname"
					select="document($file)/doxygen/compounddef[@prot='public']/compoundname/text()" />
				<xsl:for-each select="$funcs_node">
					<cppfunc>
						<xsl:copy-of select="@id" />
						<xsl:attribute name="status"><xsl:value-of
							select="detaileddescription/para/xrefsect/xreftitle/text()" /></xsl:attribute>
						<xsl:attribute name="version"><xsl:value-of
							select="detaileddescription/para/xrefsect/xrefdescription/para/text()" /></xsl:attribute>
						<xsl:attribute name="prototype"><xsl:value-of
							select="concat(definition/text(), argsstring/text())" /></xsl:attribute>
						<xsl:copy-of select="location/@file" />
						<xsl:copy-of select="@static" />
						<xsl:copy-of select="@inline" />
					</cppfunc>
				</xsl:for-each>
				
				<!--  now const static publics -->
				<xsl:variable name="sconst_node"
					select="document($file)/doxygen/compounddef[@prot='public']/sectiondef/memberdef[@prot='public'][@kind='variable'][@static='yes']" />
				<xsl:for-each select="$sconst_node">
					<cppfunc>
						<xsl:copy-of select="@id" />
						<xsl:attribute name="status"><xsl:value-of
							select="detaileddescription/para/xrefsect/xreftitle/text()" /></xsl:attribute>
						<xsl:attribute name="version"><xsl:value-of
							select="detaileddescription/para/xrefsect/xrefdescription/para/text()" /></xsl:attribute>
						<xsl:attribute name="prototype">static <xsl:value-of
							select="definition/text()" /></xsl:attribute>
						<xsl:copy-of select="location/@file" />
						<xsl:copy-of select="@static" />
						<xsl:copy-of select="@inline" />
					</cppfunc>
				</xsl:for-each>

		<!-- 
				//No real non-static variables - and also, doxygen is confused about nested structs/unions
				
				<xsl:variable name="const_node"
					select="document($file)/doxygen/compounddef[@prot='public']/sectiondef/memberdef[@prot='public'][@kind='variable'][@static='no']" />
				<xsl:for-each select="$const_node">
					<cppfunc>
						<xsl:copy-of select="@id" />
						<xsl:attribute name="status"><xsl:value-of
							select="detaileddescription/para/xrefsect/xreftitle/text()" /></xsl:attribute>
						<xsl:attribute name="version"><xsl:value-of
							select="detaileddescription/para/xrefsect/xrefdescription/para/text()" /></xsl:attribute>
						<xsl:attribute name="prototype">/* not static */ <xsl:value-of
							select="definition/text()" /></xsl:attribute>
						<xsl:copy-of select="location/@file" />
						<xsl:copy-of select="@static" />
						<xsl:copy-of select="@inline" />
					</cppfunc>
				</xsl:for-each>
		-->
				<!--  now enums -->
				<xsl:variable name="enum_node"
					select="document($file)/doxygen/compounddef[@prot='public']/sectiondef/memberdef[@kind='enum'][@prot='public']" />
				<xsl:for-each select="$enum_node">

					<!--  use a name, else '(anonymous)' -->
					<xsl:variable name="enum_node_name"><xsl:choose>					
							<xsl:when test="contains(name/text(), '@')">(anonymous)</xsl:when>
							<xsl:otherwise><xsl:value-of select="name/text()" /></xsl:otherwise>
						</xsl:choose></xsl:variable>

					<xsl:variable name="enum_status" select="detaileddescription/para/xrefsect/xreftitle/text()"/>
					<xsl:variable name="enum_version" select="detaileddescription/para/xrefsect/xrefdescription/para/text()"/>

					<cppfunc>
						<xsl:copy-of select="@id" />
						<xsl:attribute name="status"><xsl:value-of
							select="$enum_status" /></xsl:attribute>
						<xsl:attribute name="version"><xsl:value-of
							select="$enum_version" /></xsl:attribute>
						<xsl:attribute name="prototype">enum 
							<xsl:value-of select="$compoundname"/>::<xsl:value-of
							select="$enum_node_name" /> {}</xsl:attribute>
						<xsl:copy-of select="location/@file" />
					</cppfunc>
		
					<xsl:variable name="enum_node_file" select="location/@file" />
		
		
					<xsl:variable name="enum_member" select="enumvalue[@prot='public']"/>
					
					<!--  for each member -->
					<xsl:for-each select="$enum_member">
						<cppfunc>
							<xsl:copy-of select="@id" />
							<!--  status and version: only override if set. -->
							<xsl:attribute name="status">
								<xsl:choose>
									<xsl:when test="detaileddescription/para/xrefsect/xreftitle/text() != ''">
										<xsl:value-of select="detaileddescription/para/xrefsect/xreftitle/text()"/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="$enum_status" />
									</xsl:otherwise>
								</xsl:choose>
							</xsl:attribute>
							<xsl:attribute name="version">
								<xsl:choose>
									<xsl:when test="detaileddescription/para/xrefsect/xrefdescription/para/text() != ''">
										<xsl:value-of select="detaileddescription/para/xrefsect/xrefdescription/para/text()"/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="$enum_version" />
									</xsl:otherwise>
								</xsl:choose>
							</xsl:attribute>
							<xsl:attribute name="prototype">enum 
							<xsl:value-of select="$compoundname"/>::<xsl:value-of
								select="$enum_node_name" />::<xsl:value-of
								select="name/text()" /></xsl:attribute>
							<xsl:attribute name="file"><xsl:value-of select="$enum_node_file" /></xsl:attribute>
						</cppfunc>
					
					</xsl:for-each> <!--  done with enum member -->
					 
				</xsl:for-each> <!--  done with enums -->

			</xsl:for-each> <!--  done with file -->

		</list>
	</xsl:template>
</xsl:stylesheet>


