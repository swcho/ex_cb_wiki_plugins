/*
 * Copyright by Intland Software
 *
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Intland Software. ("Confidential Information"). You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Intland.
 */
package com.intland.codebeamer.wiki.plugins;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginManager;
import com.intland.codebeamer.jasper.JasperReportManager;
import com.intland.codebeamer.manager.ArtifactManager;
import com.intland.codebeamer.persistence.dao.TrackerDao;
import com.intland.codebeamer.persistence.dao.impl.TrackerDaoImpl;
import com.intland.codebeamer.persistence.dto.ArtifactDto;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.utils.Common;
import com.intland.codebeamer.wiki.CodeBeamerWikiContext;
import com.intland.codebeamer.wiki.plugins.base.AbstractArtifactAwareWikiPlugin;

/**
 * Plugin to deliver reports rendered from <a href="http://www.jaspersoft.com/JasperSoft_JasperReports.html">JasperReports</a> templates.
 * <p>
 * Report templates (jrxml) can be either read from codeBeamer documents
 * (see parameter <code>docId</code>), or from URLs (see parameter <code>src</code>).
 * Report data comes from codeBeamer reports' output.
 *
 * Examples:
 * [{JasperReports docId='document-containing-jrxml' reportId='codeBeamer Report'}]
 * [{JasperReports docId='document-containing-jrxml' trackerId='codeBeamer tracker-id'}]
 *
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 * @version $Id$
 */
public class JasperReportsPlugin extends AbstractArtifactAwareWikiPlugin {
	private final static Logger logger = Logger.getLogger(JasperReportsPlugin.class);

	private final static JasperReportManager jasperReportManager = JasperReportManager.getInstance();
	private final static TrackerDao trackerDao = TrackerDaoImpl.getInstance();
	private final static ArtifactManager artifactManager = ArtifactManager.getInstance();

	private final static String ENCODING = "UTF8";

	private Collection<TrackerDto> trackers;
	private Integer reportId;
	private String src;
	private String body;
	private Integer docId;
	private UserDto user;

	private String contextPath;

	public String execute(WikiContext wcontext, Map params) {
		processParameters(params);

		CodeBeamerWikiContext context = (CodeBeamerWikiContext)wcontext;

		user = getUserFromContext(context);
		contextPath = getContextPath(context);

		try {
			if (docId != null) {
				// Using CodeBeamer document.
				InputStream inputStream = getInputStream(context);
				body = Common.readFileToString(inputStream, ENCODING);
			}
			return process();
		} catch (Throwable ex) {
			logger.warn(ex.toString(), ex);

			return StringEscapeUtils.escapeHtml(ex.toString());
		}
	}

	protected void printLink(PrintWriter print, String label, int format) {
		print.print("<a href=\"" + contextPath + "/jasperReportsExport.spr?doc_id=" + docId + "&amp;format=" + format);

		if (trackers != null) {
			for (TrackerDto tracker : trackers) {
				print.print("&amp;tracker_id=" + tracker.getId());
			}
		} else if (reportId != null) {
			print.print("&amp;report_id=" + reportId);
		}

		print.print("\">" + StringEscapeUtils.escapeHtml(label) + "</a> ");
	}

	protected String process() throws IOException {
		ByteArrayInputStream input = new ByteArrayInputStream(body.getBytes(ENCODING));
		ByteArrayOutputStream html = new ByteArrayOutputStream();

		StringWriter out = new StringWriter();
		PrintWriter print = new PrintWriter(out);

		if (trackers != null) {
			try {
				jasperReportManager.compileFillExport(user, trackers, input, null, JasperReportManager.EXPORT_HTML, html);
			} catch (Throwable ex) {
				logger.warn(ex.getMessage(), ex);

				html.write(StringEscapeUtils.escapeHtml(ex.getMessage()).getBytes());
			}
		} else if (reportId != null) {
			ArtifactDto report = artifactManager.findById(user, reportId);

			try {
				jasperReportManager.compileFillExport(user, report, input, null, JasperReportManager.EXPORT_HTML, html);
			} catch (Throwable ex) {
				logger.warn(ex.getMessage(), ex);

				html.write(StringEscapeUtils.escapeHtml(ex.getMessage()).getBytes());
			}
		}

		out.write(html.toString());

		if (docId != null) {
			print.println("<table border=\"0\" cellspacing=\"1\" cellpadding=\"1\">");
			print.println("<tr>");
			print.print("<td>");

			printLink(print, "Excel", JasperReportManager.EXPORT_EXCEL);
			printLink(print, "Pdf", JasperReportManager.EXPORT_PDF);
			printLink(print, "Rtf", JasperReportManager.EXPORT_RTF);

			print.println("</td>");
			print.println("</tr>");
			print.println("</table>");
		}

		return out.toString();
	}

	protected void processParameters(Map params) {
		try {
			reportId = Integer.valueOf(getParameter(params, "reportId"));
		} catch (Exception ex) {
			try {
				reportId = Integer.valueOf(getParameter(params, "report_id"));
			} catch (Exception ex2) {
			}
		}

		try {
			docId = Integer.valueOf(getParameter(params, "id"));
		} catch (Exception ex) {
			try {
				docId = Integer.valueOf(getParameter(params, "docId"));
			} catch (Exception ex2) {
			}
		}

		String trackerIdsString = getParameter(params, "trackerId");
		if (trackerIdsString == null) {
			trackerIdsString = getParameter(params, "trackers");
		}

		if (trackerIdsString != null) {
			List<Integer> trackerIds = new ArrayList();
			for (StringTokenizer st = new StringTokenizer(trackerIdsString.replace(',', ' ')); st.hasMoreTokens(); ) {
				try {
					trackerIds.add(Integer.valueOf(st.nextToken()));
				} catch (Throwable ex) {
				}
			}

			trackers = trackerDao.findById(user, trackerIds);
		}

		src = getParameter(params, "src");

		body = StringUtils.trimToEmpty((String)params.get(PluginManager.PARAM_BODY));
	}

	/**
	 * Unused.
	 */
	@Override
	protected Integer getArtifactId() {
		return docId;
	}

	/**
	 * Unused.
	 */
	@Override
	protected String getIconName() {
		return null;
	}

	/**
	 * Unused.
	 */
	@Override
	protected String getSrc() {
		return src;
	}
}
