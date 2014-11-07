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
package com.intland.codebeamer.wiki.plugins.dataset;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jfree.data.general.Dataset;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.controller.ControllerUtils;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;
import com.intland.codebeamer.wiki.plugins.dataset.formatter.GanttChartDataSetFormatter;
import com.intland.codebeamer.wiki.plugins.dataset.formatter.LineChartDataSetFormatter;
import com.intland.codebeamer.wiki.plugins.dataset.formatter.PieChartDataSetFormatter;
import com.intland.codebeamer.wiki.plugins.dataset.formatter.TimeSeriesChartDataSetFormatter;
import com.intland.codebeamer.wiki.plugins.dataset.formatter.WikiTableDataSetFormatter;
import com.intland.codebeamer.wiki.plugins.dataset.producer.CommitTrendProducer;
import com.intland.codebeamer.wiki.plugins.dataset.producer.DocumentAccessTrendProducer;
import com.intland.codebeamer.wiki.plugins.dataset.producer.GanttActivitiesProducer;
import com.intland.codebeamer.wiki.plugins.dataset.producer.GanttLabelsProducer;
import com.intland.codebeamer.wiki.plugins.dataset.producer.GanttStatsProducer;
import com.intland.codebeamer.wiki.plugins.dataset.producer.GanttStatusesProducer;
import com.intland.codebeamer.wiki.plugins.dataset.producer.ModificationsPerTrackerItemProducer;
import com.intland.codebeamer.wiki.plugins.dataset.producer.ProjectTrendProducer;
import com.intland.codebeamer.wiki.plugins.dataset.producer.TrackerItemChangeTrendProducer;
import com.intland.codebeamer.wiki.plugins.dataset.producer.TrackerItemHourTrendProducer;
import com.intland.codebeamer.wiki.plugins.dataset.producer.TrackerItemsByLabelProducer;
import com.intland.codebeamer.wiki.plugins.dataset.producer.TrackerItemsBySeverityProducer;
import com.intland.codebeamer.wiki.plugins.dataset.producer.TrackerItemsByStatusAndSeverityProducer;
import com.intland.codebeamer.wiki.plugins.dataset.producer.TrackerItemsPerSourceTrendProducer;

/**
 * Plugin to prepare statistical data to display in tabular format
 * or as charts.
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 */
public class DataSetPlugin extends AbstractCodeBeamerWikiPlugin {
	final static private Logger logger = Logger.getLogger(DataSetPlugin.class);

	/**
	 * Parameter specifying the data producer's name
	 */
	protected static final String PARAM_DATA = "data";
	/**
	 * Parameter specifying the formatter.
	 */
	protected static final String PARAM_FORMAT = "format";

	public static final String DATASET_COMMIT_TREND = "commitTrend";
	public static final String DATASET_DOCUMENT_ACCESS_TREND = "documentAccessTrend";
	public static final String DATASET_GANTT_ACTIVITIES = "ganttActivities";
	public static final String DATASET_GANTT_LABELS = "ganttLabels";
	public static final String DATASET_GANTT_STATS = "ganttStats";
	public static final String DATASET_GANTT_STATUSES = "ganttStatuses";
	public static final String DATASET_MODIFICATIONS_PER_TRACKER_ITEM_TREND = "modificationsPerTrackerItemTrend";
	public static final String DATASET_PROJECT_TREND = "projectTrend";
	public static final String DATASET_TRACKER_ITEM_CHANGE_TREND = "trackerItemChangeTrend";
	public static final String DATASET_TRACKER_ITEM_HOUR_TREND = "trackerItemHourTrend";
	public static final String DATASET_TRACKER_ITEMS_BY_LABEL = "trackerItemsByLabel";
	public static final String DATASET_TRACKER_ITEMS_BY_SEVERITY = "trackerItemsBySeverity";
	public static final String DATASET_TRACKER_ITEMS_BY_STATUS_AND_SEVERITY = "trackerItemsByStatusAndSeverity";
	public static final String DATASET_TRACKER_ITEMS_PER_SOURCE_TREND = "trackerItemsPerSourceTrend";

	public static final String FORMAT_BARCHART = "barChart";
	public static final String FORMAT_GANTTCHART = "ganttChart";
	public static final String FORMAT_LINECHART = "lineChart";
	public static final String FORMAT_MULTIPLE_PIECHART = "multiplePieChart";
	public static final String FORMAT_PIECHART = "pieChart";
	public static final String FORMAT_TABLE = "table";
	public static final String FORMAT_TIMESERIESCHART = "timeSeriesChart";

	private Dataset dataSet = null;

	protected static Map producers = new HashMap();
	private static Set<String> formatters = new HashSet<String>();

	static {
		producers.put(DATASET_COMMIT_TREND, CommitTrendProducer.class);
		producers.put(DATASET_DOCUMENT_ACCESS_TREND, DocumentAccessTrendProducer.class);
		producers.put(DATASET_GANTT_ACTIVITIES, GanttActivitiesProducer.class);
		producers.put(DATASET_GANTT_LABELS, GanttLabelsProducer.class);
		producers.put(DATASET_GANTT_STATS, GanttStatsProducer.class);
		producers.put(DATASET_GANTT_STATUSES, GanttStatusesProducer.class);
		producers.put(DATASET_MODIFICATIONS_PER_TRACKER_ITEM_TREND, ModificationsPerTrackerItemProducer.class);
		producers.put(DATASET_PROJECT_TREND, ProjectTrendProducer.class);
		producers.put(DATASET_TRACKER_ITEM_CHANGE_TREND, TrackerItemChangeTrendProducer.class);
		producers.put(DATASET_TRACKER_ITEM_HOUR_TREND, TrackerItemHourTrendProducer.class);
		producers.put(DATASET_TRACKER_ITEMS_BY_LABEL, TrackerItemsByLabelProducer.class);
		producers.put(DATASET_TRACKER_ITEMS_BY_SEVERITY, TrackerItemsBySeverityProducer.class);
		producers.put(DATASET_TRACKER_ITEMS_BY_STATUS_AND_SEVERITY, TrackerItemsByStatusAndSeverityProducer.class);
		producers.put(DATASET_TRACKER_ITEMS_PER_SOURCE_TREND, TrackerItemsPerSourceTrendProducer.class);

		formatters.add(FORMAT_BARCHART);
		formatters.add(FORMAT_GANTTCHART);
		formatters.add(FORMAT_LINECHART);
		formatters.add(FORMAT_MULTIPLE_PIECHART);
		formatters.add(FORMAT_PIECHART);
		formatters.add(FORMAT_TABLE);
		formatters.add(FORMAT_TIMESERIESCHART);
	}

	public String execute(WikiContext context, Map params) throws PluginException {
		try {
			return doExecute(context, params);
		} catch (Throwable ex) {
			String message = ex.getMessage();
			if (ex instanceof PluginException) {
				logger.warn(message, ex);
				throw (PluginException) ex;
			}

			logger.error(message, ex);
			throw new PluginException(message, ex);
		}
	}

	protected String doExecute(WikiContext context, Map params) throws PluginException {
		// get common parameters
		String dataSetType = getStringParameter(params, PARAM_DATA, producers.keySet(), null);
		if(dataSetType == null) {
			throw new PluginException("Parameter '" + PARAM_DATA + "' has a missing or invalid value='" + getStringParameter(params, PARAM_DATA, null) + "'");
		}

		ProjectDto project = null;

		if (!DATASET_DOCUMENT_ACCESS_TREND.equals(dataSetType)) {
			try {
				project = discoverProject(params, context);
			} catch (NamedPluginException ex) {
				return renderErrorTemplate(ex);
			}
		}

		String wikiVariable = getStringParameter(params, "wikiVariable", null);
		String format = getStringParameter(params, PARAM_FORMAT, formatters, FORMAT_TABLE);
		if(format == null) {
			throw new PluginException("Parameter '" + PARAM_FORMAT + "' has missing or invalid value");
		}

		UserDto user = getUserFromContext(context);

		// produce
		WikiDataSetProducer producer = createProducer(dataSetType);
		ControllerUtils.autoWire(producer, getApplicationContext(context));
		producer.setUser(user);
		producer.setProject(project);
		producer.setParams(params);

		dataSet = producer.produce(params);

		// format
		WikiDataSetFormatter formatter = createFormatter(format);
		String result = formatter.format(dataSet);
		// the "table" formatter produces Wiki markup for the table, convert it to HTML
		if (format.equals(FORMAT_TABLE)) {
			result = context.getEngine().textToHTML(context, result);
		}

		// save
		if (wikiVariable != null) {
			context.setVariable(wikiVariable, result);
			return "";
		}

		return result;
	}

	/**
	 * Returns intermediate data, arrived from producer and passed to formatter.
	 * This has package-access, primarily intended to use during unit-testing.
	 */
	Dataset getDataSet() {
		return dataSet;
	}

	private WikiDataSetProducer createProducer(String producerName) throws PluginException {
		Class clazz = (Class) producers.get(producerName);
		if (clazz == null) {
			throw new PluginException("Can't instantiate producer for this data set type");
		}

		WikiDataSetProducer producer = null;
		try {
			producer = (WikiDataSetProducer)clazz.newInstance();
		} catch (Exception ex) {
			throw new PluginException("SYSTEM: Can not create producer", ex);
		}

		return producer;
	}

	private WikiDataSetFormatter createFormatter(String formatName) {
		WikiDataSetFormatter formatter = null;

		if (formatName.equals(FORMAT_BARCHART)) {
			formatter = new LineChartDataSetFormatter();
		} else if (formatName.equals(FORMAT_GANTTCHART)) {
			formatter = new GanttChartDataSetFormatter();
		} else if (formatName.equals(FORMAT_LINECHART)) {
			formatter = new LineChartDataSetFormatter();
		} else if (formatName.equals(FORMAT_MULTIPLE_PIECHART)) {
			formatter = new LineChartDataSetFormatter();
		} else if (formatName.equals(FORMAT_PIECHART)) {
			formatter = new PieChartDataSetFormatter();
		} else if (formatName.equals(FORMAT_TABLE)) {
			formatter = new WikiTableDataSetFormatter();
		} else if (formatName.equals(FORMAT_TIMESERIESCHART)) {
			formatter = new TimeSeriesChartDataSetFormatter();
		}

		return formatter;
	}
}
