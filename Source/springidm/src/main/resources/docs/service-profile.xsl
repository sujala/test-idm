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
				<h1 align="center">Service Profile</h1>
				<div align="center">
					<table cellspacing="5" cellpadding="8" border="2" width="70%" style="border:2px solid #c3c3c3;border-collapse:collapse;font-family:verdana">
						<tr style="vertical-align: top;">
							<td width="20%" style="background-color: #e5eecc">
								<strong>LINKS</strong>
							</td>
							<td style="padding-bottom: 10px;">
								<xsl:for-each select="sp:service-profile/atom:link">
									<link>
										<xsl:attribute name="href">
		    								<xsl:value-of select="@href" />
		  								</xsl:attribute>
										<xsl:value-of select="@href" />
									</link>
									<br />
								</xsl:for-each>
							</td>
						</tr>
						<tr style="vertical-align: top">
							<td width="20%" >
								<strong>SUMMARY</strong>
							</td>
							<td style="padding-bottom: 10px;">
								<xsl:value-of select="sp:service-profile/sp:short-description" />
							</td>
						</tr>
						<tr style="vertical-align: top">
							<td style="background-color: #e5eecc">
								<strong>DESCRIPTION</strong>
							</td>
							<td style="padding-bottom: 10px; font-family: verdana">
								<xsl:value-of select="sp:service-profile/sp:detailed-description" />
							</td>
						</tr>
						<tr height="60" style="vertical-align: top">
							<td style="background-image:url('/docs/xslt/psd_erl_13.gif');">
								<strong>CONTRACT</strong>
							</td>
							<td>
								<table border="1" width="100%" cellpadding="8" style="border:1px solid #c3c3c3;border-collapse:collapse;">
								<xsl:for-each select="sp:service-profile/sp:contract">
									<tr>
										<td colspan="5" >
												<span style="font-family: Copperplate; font-size:15pt; color:#660000"><b><xsl:value-of select="@name" /></b></span>
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
									<xsl:for-each select="v:versions/v:version">
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
											<td style="font-size: 10pt">
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
								</xsl:for-each>
								</table>
						
							</td>
						</tr>
					</table>
				</div>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
