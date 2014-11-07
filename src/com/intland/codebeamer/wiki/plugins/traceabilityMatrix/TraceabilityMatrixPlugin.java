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
package com.intland.codebeamer.wiki.plugins.traceabilityMatrix;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.activemq.util.ByteArrayInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.Ostermiller.util.ExcelCSVPrinter;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.intland.codebeamer.controller.ControllerUtils;
import com.intland.codebeamer.controller.TraceabilityMatrixController;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.TrackerViewDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.util.TrackerItemFieldHandler;
import com.intland.codebeamer.utils.DownloadUtils;
import com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.BeanToQueryParametersConverter;
import com.intland.codebeamer.wiki.plugins.base.DefaultWebBindingInitializer;

/**
 * Plugin displaying Traceability Matrix like {@link TraceabilityMatrixController}.
 * Also acts as web controller to export the plugin data to CVS/Excel for easier printing of such big matrixes...
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
@Controller
public class TraceabilityMatrixPlugin extends AbstractCommandWikiPlugin<TraceabilityMatrixCommand> {

	private final static Logger logger = Logger.getLogger(TraceabilityMatrixPlugin.class);

	@Autowired
	private TraceabilityMatrixController traceabilityMatrixController;

	public TraceabilityMatrixPlugin() {
		// use default binding and javax bean validation
		setWebBindingInitializer(new DefaultWebBindingInitializer());
	}

	@Override
	protected String getTemplateFilename() {
		return "traceabilityMatrix-plugin.vm";
	}

	@Override
	public TraceabilityMatrixCommand createCommand() throws PluginException {
		return new TraceabilityMatrixCommand();
	}

	/**
	 * Simple predicate to exlcude the folders
	 */
	private static Predicate<TrackerItemDto> excludeFolders = new Predicate<TrackerItemDto>() {
		public boolean apply(TrackerItemDto input) {
			return input == null || input.isFolder() == false;
		}
	};

	@Override
	protected Map<String, Object> populateModel(DataBinder binder, TraceabilityMatrixCommand command, Map params) throws PluginException {
		HttpServletRequest request = getWikiContext().getHttpRequest();
		Map<String,Object> model = populateModel(request, getUser(), command, params);

		// put tracker-item-handler to scope, so scripts can resolve custom field values
		TrackerItemFieldHandler resolver = TrackerItemFieldHandler.getInstance(getUser());
		model.put("resolver", resolver);

		model.put("allowEditingTraceabilityMatrix", Boolean.valueOf(command.isAllowEditing()));
		// render the jsp as matrix, pass all variables to request
		for (Entry<String, Object> entry: model.entrySet()) {
			request.setAttribute(entry.getKey(), entry.getValue());
		}
		// TODO: now all is editable, that should be dynamically determined for the actual row/column
		request.setAttribute("editable", Boolean.TRUE);
		request.setAttribute("cellDecoratorScript", StringUtils.trimToNull(command.getPluginBody()));

		// TODO: check when is a cell editable ?

		// put "Export to CSV" link to top-left corner of the matrix
		String exportToCSVUrl = request.getContextPath() + getExportToCSVUrl(command);
		request.setAttribute("topleftCorner", "<a href='" + exportToCSVUrl +"'>Export to CSV</a>");	// TODO: i18n

		String matrix = StringUtils.trimToEmpty(ControllerUtils.renderJSP(request, "/bugs/tracker/traceabilityMatrix/traceabilityMatrix.jsp"));
		model.put("matrix",  matrix);

		if (logger.isDebugEnabled()) {
			logger.debug("Model <" + model +">");
		}

		return model;
	}

	protected Map<String, Object> populateModel(HttpServletRequest request, UserDto user, TraceabilityMatrixCommand command, Map params) {
		logger.info("Rendering plugin for <" + command +">");

		Map<String, Object> model = new HashMap<String, Object>();

		Predicate<TrackerItemDto> filter = command.isIncludeFolders() ? null : excludeFolders;

		List<TrackerItemDto> horizontalTrackerItems = findTraceableTrackerItems(request, user, command.parseHorizontalTrackersAndViews(), filter, command);
		List<TrackerItemDto> verticalTrackerItems = findTraceableTrackerItems(request, user, command.parseVerticalTrackersAndViews(), filter, command);

		model.putAll(traceabilityMatrixController.populateModel(user, horizontalTrackerItems, verticalTrackerItems, true));
		return model;
	}

	/**
	 * Find the trace-able issues in all trackers/views configured
	 * @param trackerAndViewIds
	 * @param filter
	 * @param command
	 * @return
	 */
	private List<TrackerItemDto> findTraceableTrackerItems(HttpServletRequest request, UserDto user, List<Pair<Integer, Integer>> trackerAndViewIds, Predicate<TrackerItemDto> filter, TraceabilityMatrixCommand command) {
		List<TrackerItemDto> result = new ArrayList<TrackerItemDto>(100);
		if (filter == null) {
			filter = Predicates.alwaysTrue();
		}

		for (Pair<Integer, Integer> trackerAndViewId: trackerAndViewIds) {
			Integer trackerId = trackerAndViewId.getLeft();
			Integer viewId = trackerAndViewId.getRight();
			TrackerViewDto view = traceabilityMatrixController.findTrackerAndView(request, new TrackerDto(trackerId, null), viewId, user);
			List<TrackerItemDto> issues = traceabilityMatrixController.findTraceableTrackerItems(user, trackerId, view, request);
			for (TrackerItemDto issue: issues) {
				if (filter.apply(issue)) {
					result.add(issue);

					// limit the number of issues on each axis!
					if (command.getMax() != null && result.size() >= command.getMax().intValue()) {
						// TODO: should show some warning message when the results are limited ?
						return result;
					}
				}
			}
		}
		return result;
	}

	private final static String EXPORT_TO_CSV_URL = "/proj/tracker/traceabilitymatrix/exportCSV.spr";

	private String getExportToCSVUrl(TraceabilityMatrixCommand command) {
		String url = EXPORT_TO_CSV_URL;
		BeanToQueryParametersConverter beanToQueryConverter = getBeanToQueryParametersConverter();
		beanToQueryConverter.setExcludedFieldNames("pluginBody" /* dont' export the body, not used, and script can can be really long won't fit to a GET request */);

		// make a clone to remove the "max" value
		TraceabilityMatrixCommand clone;
		try {
			clone = (TraceabilityMatrixCommand) command.clone();
			// export ALL!! items to CSV
			clone.setMax(Integer.valueOf(Integer.MAX_VALUE));
		} catch (CloneNotSupportedException e) {
			clone = command;
		}
		String params = beanToQueryConverter.convertToQueryParameters(clone);
		return url + "?" + params;
	}

	// character to force UTF-8 encoding in the CSV file
	final static String BOM = "\ufeff";

	/**
	 * Export the output of {@link TraceabilityMatrixPlugin} to CSV so using Excel that can be nicely printed
	 * @param request
	 * @param response
	 * @param command
	 * @throws PluginException
	 */
	@RequestMapping(value = EXPORT_TO_CSV_URL, method = RequestMethod.GET)
	public void exportToCSV(HttpServletRequest request, HttpServletResponse response, TraceabilityMatrixCommand command) throws IOException, PluginException {
		logger.info("Exporting plugin's result to CSV:" + command);
		UserDto user = ControllerUtils.getCurrentUser(request);

		Map<String, Object> model = populateModel(request, user, command, Collections.EMPTY_MAP);

		StringWriter csv = new StringWriter(10000);

		// Note: it seems that Word 2010 is better with the defaults, while Word 2008 likes ";" as separator better...
		ExcelCSVPrinter printer = new ExcelCSVPrinter(csv);
		printer.setLineEnding("\r\n");
		model.put("csv", printer);

		// call the Velocity script actuall renders the CVS by calling the methods on the ExcelCSVPrinter above
		String template = "/wiki-plugin/traceabilityMatrix-exportToCSV.vm";
		VelocityContext context = new VelocityContext(model);
		templateRenderer.renderTemplateOnPath(template, context, null);

		printer.flush();
		String content = csv.getBuffer().toString();
		// trying to force excel to use Unicode/UTF-8
		// see: http://stackoverflow.com/questions/155097/microsoft-excel-mangles-diacritics-in-csv-files
		content = BOM + content;

		String fileName = StringUtils.defaultIfBlank(command.getTitle(), "export") +".csv";
		DownloadUtils.downloadBinaryContent(request, response, fileName, new ByteArrayInputStream(content.getBytes("UTF-8")), Long.valueOf(content.length()));
	}

}



