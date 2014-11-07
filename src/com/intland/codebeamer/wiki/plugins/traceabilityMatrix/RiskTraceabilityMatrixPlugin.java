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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.intland.codebeamer.controller.support.TraceabilityBrowserSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.persistence.dao.TrackerDao;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.TrackerTypeDto;
import com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin;

/**
 * Risk Traceability Matrix wiki plugin.
 * @author <a href="mailto:gabor.nagy@intland.com">Gabor Nagy</a>
 */
@Component
public class RiskTraceabilityMatrixPlugin extends AbstractCommandWikiPlugin<RiskTraceabilityMatrixCommand> {

	private static final String TEMPLATE_FILENAME = "riskTraceabilityMatrix-plugin.vm";

	@Autowired
	private TrackerDao trackerDao;
	@Autowired
	private TraceabilityBrowserSupport traceabilityBrowserSupport;

	@Override
	public RiskTraceabilityMatrixCommand createCommand() throws PluginException {
		return new RiskTraceabilityMatrixCommand();
	}

	@Override
	protected Map populateModel(DataBinder binder, RiskTraceabilityMatrixCommand command, Map params) throws PluginException {
		List<TraceabilityBrowserSupport.TrackerParameter> parameters = command.getTrackersAsParameterList();
		if (parameters == null) {
			throw new PluginException(new IllegalArgumentException("No tracker IDs specified"));
		}

		if (parameters.isEmpty()) {
			throw new PluginException(new IllegalArgumentException("At least 1 tracker must be specified to use this plugin"));
		}

		Set<Integer> testCaseTrackerIds = new HashSet<Integer>();
		List<TrackerDto> trackerList = new ArrayList<TrackerDto>(parameters.size());
		for (TraceabilityBrowserSupport.TrackerParameter param : parameters) {
			final TrackerDto tracker = trackerDao.findById(getUser(), param.getTrackerId());
			if (tracker == null) {
				throw new PluginException(new IllegalArgumentException("Tracker not found with ID = " + param.getTrackerId()));
			}
			if (tracker.getType().equals(TrackerTypeDto.TESTCASE)) {
				testCaseTrackerIds.add(tracker.getId());
			}
			trackerList.add(tracker);
		}

		traceabilityBrowserSupport.setUser(getUser());
		traceabilityBrowserSupport.setTestCaseTrackerIds(testCaseTrackerIds);
		traceabilityBrowserSupport.setContextPath(getContextPath());
		traceabilityBrowserSupport.setEmptyColumnsCleanup(false);
		int depth = trackerList.size();

		Map<String, Object> model = new HashMap<String, Object>();
		model.put("contextPath", getContextPath());
		model.put("command", command);
		model.put("trackers", trackerList);
		model.put("rows", traceabilityBrowserSupport.getHtmlRows(parameters, depth));
		return model;
	}

	@Override
	protected String getTemplateFilename() {
		return TEMPLATE_FILENAME;
	}

}
