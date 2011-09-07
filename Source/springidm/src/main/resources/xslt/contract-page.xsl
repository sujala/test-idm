<?xml version="1.0" encoding="ISO-8859-1"?>
	<!-- Edited by XMLSpy® -->
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:sp="http://service-registry.api.rackspace.com/service-profile"
	xmlns:v="http://service-registry.api.rackspace.com/versioning"
	xmlns:atom="http://www.w3.org/2005/Atom">

	<xsl:template match="/">
		<html>
			<body>
				<div align="center">

					<table border="1" width="70%" cellpadding="8" style="border:1px solid #c3c3c3;border-collapse:collapse;">
						<tr>
							<td colspan="5" >
									<span style="font-family: Copperplate; font-size:15pt; color:#660000"><b><xsl:value-of select="sp:contract/@name" /></b></span>
							</td>
						</tr>
						<tr style="background-color:#e5eecc;padding:3px;vertical-align:top;">
							<td>
								Version
							</td>
							<td>
								Status
							</td>
							<td>
								Updated
							</td>
							<td>
								Links
							</td>
							<td>
								Media Type
							</td>
						</tr>
						<xsl:for-each select="sp:contract/v:versions/v:version">
							<tr height="40">
								<td>
									<xsl:value-of select="@id" />
								</td>
								<td>
									<xsl:value-of select="@status" />
								</td>
								<td>
									<xsl:value-of select="@updated" />
								</td>
								<td>
									<xsl:for-each select="atom:link">
										<link>
											<xsl:attribute name="href">
											    <xsl:value-of select="@href" />
											</xsl:attribute>
											
											<xsl:choose>
									          <xsl:when test="@title">
									          	<xsl:value-of select="@title" />
									          </xsl:when>
									          <xsl:otherwise>
									    	    <xsl:value-of select="@href" />
									          </xsl:otherwise>
									        </xsl:choose>
										</link>
										<br />
									</xsl:for-each>
								</td>
								<td>
									<xsl:for-each select="v:media-types/v:media-type">
									<table style="font-size: 10pt">
										<tr>
											<td>
												<b>Base</b>
											</td>
											<td>
												<xsl:value-of select="@base" />
											</td>
										</tr>
										<tr>
											<td>
												<b>Type</b>
											</td>
											<td>
												<xsl:value-of select="@type" />
											</td>
										</tr>
										<tr>
											<td>
												<b>Link(s)</b>
											</td>
											<td>
												<xsl:for-each select="atom:link">
												<link>
													<xsl:attribute name="href">
	   													<xsl:value-of select="@href" />
	  												</xsl:attribute>
													<xsl:value-of select="@rel" />
												</link>
												</xsl:for-each>
											</td>
										</tr>
									</table>
									</xsl:for-each>
								</td>
							</tr>
						</xsl:for-each>
					</table>
				</div>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
