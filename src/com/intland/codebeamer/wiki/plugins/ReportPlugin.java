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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.displaytag.model.Row;
import org.displaytag.model.TableModel;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.manager.ArtifactManager;
import com.intland.codebeamer.manager.ReportManager;
import com.intland.codebeamer.manager.TrackerManager;
import com.intland.codebeamer.persistence.dao.PaginatedDtoList;
import com.intland.codebeamer.persistence.dto.ArtifactDto;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.TrackerLayoutLabelDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.remoting.ArtifactType;
import com.intland.codebeamer.servlet.report.Report;
import com.intland.codebeamer.servlet.report.ReportQuery;
import com.intland.codebeamer.servlet.report.TrackerReportQuery;
import com.intland.codebeamer.ui.view.table.TrackerReportDecorator;
import com.intland.codebeamer.utils.velocitytool.TextFormatter;
import com.intland.codebeamer.wiki.plugins.base.AutoWiringCodeBeamerPlugin;

/**
 * Plugin to generate a report output in wiki pages.
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 */
public class ReportPlugin extends AutoWiringCodeBeamerPlugin {
	private final static Logger log = Logger.getLogger(ReportPlugin.class);

	private ArtifactManager artifactManager;
	private ReportManager reportManager;
	private TrackerManager trackerManager;

	private static final String PLUGIN_TEMPLATE = "report-plugin.vm";
	private static final int	MAX_PAGE_SIZE = 500;

	// report attributes
	private static final String PARAM_ID = "id";
	private static final String PARAM_LIMIT = "limit";

	// table attributes
	private static final String PARAM_HEIGHT = "height";
	private static final String PARAM_WIDTH = "width";
	private static final String PARAM_BORDER = "border";
	private static final String PARAM_CELLPADDING = "cellpadding";
	private static final String PARAM_CELLSPACING = "cellspacing";
	private static final String PARAM_BACKGROUNDCOLOR = "backgroundcolor";
	private static final String PARAM_TABLECLASS = "tableclass";

	// table header attributes
	private static final String PARAM_HEADERCOLOR = "headercolor";
	private static final String PARAM_HEADERBGCOLOR = "headerbackgroundcolor";
	private static final String PARAM_HEADERCLASS = "headerclass";

	// even table row attributes
	private static final String PARAM_EVENROWCOLOR = "evenrowcolor";
	private static final String PARAM_EVENROWBGCOLOR = "evenrowbackgroundcolor";
	private static final String PARAM_EVENROWCLASS = "evenrowclass";

	// odd table row attributes
	private static final String PARAM_ODDROWCOLOR = "oddrowcolor";
	private static final String PARAM_ODDROWBGCOLOR = "oddrowbackgroundcolor";
	private static final String PARAM_ODDROWCLASS = "oddrowclass";

	private UserDto user = null;

	// default values
	private int limit = 50;
	private String height = null;
	private String width = null;
	private String border = "0";
	private String cellpadding = "0";
	private String cellspacing = "0";
	private String backgroundcolor = null;
	private String headercolor = null;
	private String headerbackgroundcolor = null;
	private String evenrowcolor = null;
	private String evenrowbackgroundcolor = null;
	private String oddrowcolor = null;
	private String oddrowbackgroundcolor = null;
	private String tableclass = "displaytag";
	private String headerclass = null;
	private String evenrowclass = "even";
	private String oddrowclass = "odd";

	/* (non-Javadoc)
	 * @see com.intland.codebeamer.wiki.plugins.base.AutoWiringCodeBeamerPlugin#getTemplateFilename()
	 */
	@Override
	public String getTemplateFilename() {
		return PLUGIN_TEMPLATE;
	}

	@Override
	public void populateContext(VelocityContext ctx, Map params) throws PluginException {
		user = getUser();
		HttpServletRequest request = getWikiContext().getHttpRequest();

		if (user == null || request == null) {
			String msg = "User and Request objects are required for plugin execution";
			log.warn(msg);

			throw new PluginException(msg);
		}

		Integer reportId = parsePositiveIntegerParameter(params, PARAM_ID);

		ArtifactDto doc = artifactManager.findById(user, reportId);

		if (doc == null) {
			String msg = "Report (id: " + reportId + ") not found.";
			log.warn(msg);

			throw new PluginException(msg);
		}

		if (!doc.isReadable()) {
			String msg = "Report (id: " + reportId + ") is not readable.";
			log.warn(msg);

			throw new PluginException(msg);
		}

		if (ArtifactType.REPORT != doc.getTypeId().intValue()) {
			String msg = "Report (id: " + reportId + ") has wrong type: " + doc.getTypeId();
			log.warn(msg);

			throw new PluginException(msg);
		}

		Report report = reportManager.findById(user, reportId);

		if (report == null) {
			String msg = "Report (id: " + reportId + ") cannot be executed.";
			log.warn(msg);

			throw new PluginException(msg);
		}

		initParameters(params);

		try {
			reportToHtml(request, report, ctx);
		} catch (Throwable ex) {
			log.warn(ex.toString(), ex);

			throw new PluginException(ex.toString());
		}

		ctx.put("report", report);
		ctx.put("doc", doc);
		ctx.put("textFormatter", new TextFormatter(request.getLocale()));

		ctx.put(PARAM_HEIGHT, height);
		ctx.put(PARAM_WIDTH, width);
		ctx.put(PARAM_BORDER, border);
		ctx.put(PARAM_CELLPADDING, cellpadding);
		ctx.put(PARAM_CELLSPACING, cellspacing);
		ctx.put(PARAM_BACKGROUNDCOLOR, backgroundcolor);
		ctx.put(PARAM_TABLECLASS, tableclass);

		// table header attributes
		ctx.put(PARAM_HEADERCOLOR, headercolor);
		ctx.put(PARAM_HEADERBGCOLOR, headerbackgroundcolor);
		ctx.put(PARAM_HEADERCLASS, headerclass);

		// even table row attributes
		ctx.put(PARAM_EVENROWCOLOR, evenrowcolor);
		ctx.put(PARAM_EVENROWBGCOLOR, evenrowbackgroundcolor);
		ctx.put(PARAM_EVENROWCLASS, evenrowclass);

		// odd table row attributes
		ctx.put(PARAM_ODDROWCOLOR, oddrowcolor);
		ctx.put(PARAM_ODDROWBGCOLOR, oddrowbackgroundcolor);
		ctx.put(PARAM_ODDROWCLASS, oddrowclass);
	}

	/**
	 * Initiates the plugin parameters.
	 */
	private void initParameters(Map params) {
		String paramLimit = getParameter(params, PARAM_LIMIT);
		if (StringUtils.isNotBlank(paramLimit)) {
			try {
				limit = Math.min(Math.max(1, Integer.parseInt(paramLimit)), MAX_PAGE_SIZE);
			} catch (Exception ex) {
				log.error("Invalid " + PARAM_LIMIT + " Integer <" + paramLimit + ">");
			}
		}

		// CSS styles
		tableclass = getParameter(params, PARAM_TABLECLASS, tableclass);
		headerclass = getParameter(params, PARAM_HEADERCLASS, headerclass);
		evenrowclass = getParameter(params, PARAM_EVENROWCLASS, evenrowclass);
		oddrowclass = getParameter(params, PARAM_ODDROWCLASS, oddrowclass);

		// table attributes
		height = getParameter(params, PARAM_HEIGHT, height);
		width = getParameter(params, PARAM_WIDTH, width);
		border = getParameter(params, PARAM_BORDER, border);
		cellpadding = getParameter(params, PARAM_CELLPADDING, cellpadding);
		cellspacing = getParameter(params, PARAM_CELLSPACING, cellspacing);
		backgroundcolor = getParameter(params, PARAM_BACKGROUNDCOLOR, backgroundcolor);

		// table header attributes
		headercolor = getParameter(params, PARAM_HEADERCOLOR, headercolor);
		headerbackgroundcolor = getParameter(params, PARAM_HEADERBGCOLOR, headerbackgroundcolor);
		if (headerbackgroundcolor != null) {
			headerbackgroundcolor = "background-color: " + headerbackgroundcolor;
		}

		// table even row attributes
		evenrowcolor = getParameter(params, PARAM_EVENROWCOLOR, evenrowcolor);
		evenrowbackgroundcolor = getParameter(params, PARAM_EVENROWBGCOLOR, evenrowbackgroundcolor);
		if (evenrowbackgroundcolor != null) {
			evenrowbackgroundcolor = "background-color: " + evenrowbackgroundcolor;
		}

		// table odd row attributes
		oddrowcolor = getParameter(params, PARAM_ODDROWCOLOR, oddrowcolor);
		oddrowbackgroundcolor = getParameter(params, PARAM_ODDROWBGCOLOR, oddrowbackgroundcolor);
		if (oddrowbackgroundcolor != null) {
			oddrowbackgroundcolor = "background-color: " + oddrowbackgroundcolor;
		}
	}

	private void reportToHtml(HttpServletRequest request, Report report, VelocityContext velocityContext) throws IllegalAccessException, InvocationTargetException {
		if (log.isDebugEnabled()) {
			log.debug("Render the Report output to HTML");
		}

		List<Map<String,Object>> queries = new ArrayList<Map<String,Object>>(report.getQueries().size());
		velocityContext.put("queries", queries);

		TrackerReportDecorator decorator = new TrackerReportDecorator(request);
		TableModel tableModel = new TableModel(null, null, null);
		decorator.init(null, null, tableModel);

		int totalSize = 0;
		for (ReportQuery rq : report.getQueries()) {
			TrackerReportQuery query = (TrackerReportQuery) rq;
			PaginatedDtoList<TrackerItemDto> page = new PaginatedDtoList<TrackerItemDto>(1, limit, null, 0);
			Map<String,Object> queryContext = new HashMap<String,Object>();

			queries.add(queryContext);

			query.setUser(user);
			query.setPage(page);

			List<TrackerItemDto> result = query.getItems();
			int total = query.getPage().getFullListSize();

			queryContext.put("size", Integer.valueOf(total));
			totalSize += total;

			TrackerDto tracker = trackerManager.findById(user, query.getTracker().getId());
			queryContext.put("tracker", tracker);
			queryContext.put("project", tracker != null ? tracker.getProject() : null);
			queryContext.put("layout", query.getFields());

			// write the header
			writeTableHeader(report, query, queryContext);

			List<Map<String,Object>> rows = new ArrayList<Map<String,Object>>();
			queryContext.put("rows", rows);
			queryContext.put("limited", Boolean.valueOf(total > result.size()));

			if (result.size() > 0) {
				int i = 0;
				List<Row> rowList = new ArrayList<Row>(result.size());
				for (TrackerItemDto item : result) {
					rowList.add(new Row(item, i++));
				}

				tableModel.setRowListPage(rowList);

				for (Row row : rowList) {
					Map<String,Object> rowContext = new HashMap<String,Object>();
					rows.add(rowContext);

					decorator.initRow(row.getObject(), row.getRowNumber(), 0);
					writeTableRow(report, query, (TrackerItemDto) row.getObject(), rowContext, decorator);
				}

				rowList.clear();
			}
		}
		velocityContext.put("totalSize", Integer.valueOf(totalSize));
	}

	/**
	 * Writes the table row.
	 */
	private void writeTableRow(Report report, TrackerReportQuery query, TrackerItemDto dto, Map<String,Object> rowContext, TrackerReportDecorator decorator) throws IllegalAccessException, InvocationTargetException {
		boolean isEven = true;
		if (decorator != null) {
			isEven = (decorator.getViewIndex() + 1) % 2 == 0;
		}
		String trClass = isEven ? evenrowclass : oddrowclass;
		String tdBgColor = isEven ? evenrowbackgroundcolor : oddrowbackgroundcolor;
		String tdColor = isEven ? evenrowcolor : oddrowcolor;

		List<Map<String,Object>> fields = new ArrayList<Map<String,Object>>();
		rowContext.put("layout", fields);
		rowContext.put("styleClass", trClass);
		rowContext.put("style", tdBgColor);
		rowContext.put("even", new Boolean(isEven));

		for (TrackerLayoutLabelDto field : query.getFields()) {
			String styleClass = field.getStyleClass();
			if (trClass != null) {
				styleClass = styleClass + " " + trClass;
			}
			String property = field.getProperty();
			try {
				Object value = (decorator != null ? PropertyUtils.getNestedProperty(decorator, property) : BeanUtils.getProperty(dto, property));
				String strval = (value != null ? value.toString() : null);
				if (log.isDebugEnabled()) {
					log.debug("Property <" + property + "> value <" + strval + ">");
				}
				// apply the text color
				if (tdColor != null) {
					value = "<font color=\"" + tdColor + "\">" + strval + "</font>";
				}

				Map<String,Object> fieldContext = new HashMap<String,Object>(4);
				fields.add(fieldContext);
				fieldContext.put("value", strval);
				fieldContext.put("styleClass", styleClass);
			} catch (NoSuchMethodException ex) {
				if (log.isDebugEnabled()) {
					log.debug("Cannot access property '" + property + "', ex");
				}
			}
		}
	}

	/**
	 * Writes the table header for the given query.
	 */
	private void writeTableHeader(Report report, TrackerReportQuery query, Map<String,Object> queryContext) {
		List<Map<String,Object>> headerLayout = new ArrayList<Map<String,Object>>();
		queryContext.put("header", headerLayout);
		for (TrackerLayoutLabelDto field : query.getFields()) {
			String styleClass = field.getHeaderStyleClass();
			if (headerclass != null) {
				styleClass = styleClass + " " + headerclass;
			}
			String value = field.getLabel();

			// apply the header text color
			if (headercolor != null) {
				value = "<font color=\"" + headercolor + "\">" + value + "</font>";
			}

			Map<String,Object> headerContext = new HashMap<String,Object>(4);
			headerLayout.add(headerContext);
			headerContext.put("styleClass", styleClass);
			headerContext.put("style", headerbackgroundcolor);
			headerContext.put("value", value);
		}
	}

	public void setTrackerManager(TrackerManager trackerManager) {
		this.trackerManager = trackerManager;
	}

	public void setReportManager(ReportManager reportManager) {
		this.reportManager = reportManager;
	}

	public void setArtifactManager(ArtifactManager artifactManager) {
		this.artifactManager = artifactManager;
	}
}
