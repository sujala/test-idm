<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:vc="http://www.w3.org/2007/XMLSchema-versioning"
 	xmlns:html="http://www.w3.org/1999/xhtml"
	xmlns:atom="http://www.w3.org/2005/Atom"
	xmlns:sp="http://service-registry.api.rackspace.com/service-profile">

	<xsl:template match="/">
		<html>
			<body>
				<br />
			
				<div align="center">
					<table>
						<tr>
							<td><img src="psd_erl_13.gif" height="60px"></img></td>
							<td><span style="font-family:verdana"><h1>Service Profile - <xsl:value-of select="sp:service-profile/@name" /></h1></span></td>
						</tr>
					</table>
					
					<table cellspacing="5" cellpadding="5" border="2" width="70%" style="border:2px solid #000000; border-collapse:collapse; font-family:verdana">
			
						<tr style="vertical-align: top">
							<td width="15%">
								<strong>Canonical Name</strong>
							</td>
							<td style="padding: 10px; font-family: verdana" colspan="2">
								<xsl:value-of select="sp:service-profile/@canonical-name" />
							</td>
						</tr>
						
						<tr style="vertical-align: top">
							<td >
								<strong>Service Model</strong>
							</td>
							<td style="padding: 10px; font-family: verdana" colspan="2">
								<xsl:value-of select="sp:service-profile/@service-model" />
							</td>
						</tr>
						
						<tr style="vertical-align: top">
							<td>
								<strong>Links</strong>
							</td>
							<td style="padding: 10px;">
								<xsl:for-each select="sp:service-profile/atom:link">	
									<a>
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
									</a>
									<br />
									
								</xsl:for-each>
							</td>
						</tr>
	
						<tr style="vertical-align: top">
							<td>
								<strong>Summary</strong>
							</td>
							<td style="padding: 10px; font-family: verdana" colspan="2">
								<xsl:value-of select="sp:service-profile/sp:short-description" />
							</td>
						</tr>
						
						<tr style="vertical-align: top">
							<td>
								<strong>Description</strong>
							</td>
							<td style="padding: 10px; font-family: verdana" colspan="2">
								<xsl:value-of select="sp:service-profile/sp:detailed-description" />
							</td>
						</tr>
						
						<tr style="vertical-align: top">
							<td  style="padding: 5px">
								<strong>Contracts</strong>
							</td>
							<td>
								<table border="1" width="100%" cellpadding="8" style="border:1px solid #c3c3c3; border-collapse:collapse;">
								<xsl:for-each select="sp:service-profile/sp:contract">
									<tr style="background-color:#e5eecc; padding:3px; vertical-align:top;">
										<td>Version</td>
										<td>Status</td>
										<td>Updated</td>
										<td>Links</td>
										<td>Media Type</td>
									</tr>
									<tr height="40">
										<td>
											<xsl:value-of select="@version" />
										</td>
										<td>
											<xsl:value-of select="@status" />
										</td>
										<td>
											<xsl:value-of select="@updated" />
										</td>
										<td style="font-size: 10pt">
											<xsl:for-each select="atom:link">
												<a>
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
												</a>
												<br />
											</xsl:for-each>
										</td>
										<td>
											<xsl:for-each select="sp:media-types/sp:media-type">
											<table style="font-size: 10pt">
												<tr>
													<td><b>Base</b></td>
													<td><xsl:value-of select="@base" /></td>
												</tr>
												<tr>
													<td><b>Type</b></td>
													<td><xsl:value-of select="@type" /></td>
												</tr>
												<tr>
													<td><b>Link(s)</b></td>
													<td>
														<xsl:for-each select="atom:link">
														<a>
															<xsl:attribute name="href">
			   													<xsl:value-of select="@href" />
															</xsl:attribute>
															<xsl:choose>
													          <xsl:when test="@title">
													          	<xsl:value-of select="@title" />
													          </xsl:when>
													          <xsl:otherwise>
													    	  	<xsl:call-template name="filename-only">
                        											<xsl:with-param name="path" select="@href"/>
                    											</xsl:call-template>
													          </xsl:otherwise>
													        </xsl:choose>																
														</a>
														</xsl:for-each>
													</td>
												</tr>
											</table>
											</xsl:for-each>
										</td>
									</tr>
								</xsl:for-each>
									
								<xsl:for-each select="sp:service-profile/sp:contracts">
									<tr>
										<td colspan="5" >
											<strong><xsl:value-of select="@serviceInterface" /></strong>
										</td>
									</tr>
									
									<tr style="background-color:#e5eecc;padding:3px;vertical-align:top;">
										<td>Version</td>
										<td>Status</td>
										<td>Updated</td>
										<td>Links</td>
										<td>Media Type</td>
									</tr>
									
									<xsl:for-each select="sp:contract">
										<tr height="40">
											<td>
												<xsl:value-of select="@version" />
											</td>
											<td>
												<xsl:value-of select="@status" />
											</td>
											<td>
												<xsl:value-of select="@updated" />
											</td>
											<td style="font-size: 10pt">
												<xsl:for-each select="atom:link">
													<a>
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
													</a>
													<br />
												</xsl:for-each>
											</td>
											<td>
												<xsl:for-each select="sp:media-types/sp:media-type">
													<table style="font-size: 10pt">
														<tr>
															<td><b>Base</b></td>
															<td><xsl:value-of select="@base" /></td>
														</tr>
														<tr>
															<td><b>Type</b></td>
															<td><xsl:value-of select="@type" /></td>
														</tr>
														<tr>
															<td><b>Link(s)</b></td>
															<td>
																<xsl:for-each select="atom:link">
																	<a>
																		<xsl:attribute name="href">
																			<xsl:value-of select="@href" />
																		</xsl:attribute>
																		<xsl:choose>
																			<xsl:when test="@title">
																				<xsl:value-of select="@title" />
																			</xsl:when>
																			<xsl:otherwise>
																				<xsl:call-template name="filename-only">
																					<xsl:with-param name="path" select="@href"/>
																				</xsl:call-template>
																			</xsl:otherwise>
																		</xsl:choose>																
																	</a>
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
	<xsl:template name="filename-only">
		<xsl:param name="path" />
		<xsl:choose>
			<xsl:when test="contains($path, '/')">
				<xsl:call-template name="filename-only">
					<xsl:with-param name="path" select="substring-after($path, '/')" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$path" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
</xsl:stylesheet>
