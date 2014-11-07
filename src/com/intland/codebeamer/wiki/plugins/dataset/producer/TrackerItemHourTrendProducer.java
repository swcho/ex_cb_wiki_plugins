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
package com.intland.codebeamer.wiki.plugins.dataset.producer;

import java.util.List;
import java.util.Map;

import org.jfree.data.general.Dataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.manager.TrackerManager;
import com.intland.codebeamer.persistence.dao.impl.ChartDaoImpl;
import com.intland.codebeamer.persistence.dto.ChartTrackerTrendDto;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.wiki.plugins.dataset.WikiDataSetProducer;

/**
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 */
public class TrackerItemHourTrendProducer extends AbstractWikiDataSetProducer implements WikiDataSetProducer {
	@SuppressWarnings("deprecation")
	public Dataset produce(Map<String, String> params) throws PluginException {
		UserDto user = getUser();
		ProjectDto project = getProject();
		if(project == null) {
			throw new PluginException("Parameter 'projectId' is required");
		}
		List<Integer> trackerIds = parseCommaSeparatedIds(params.get("trackerId"));
		List<TrackerDto> trackers = trackerIds.isEmpty() ? TrackerManager.getInstance().findByProject(user, project) : TrackerManager.getInstance().findById(user, trackerIds);

		// gather
		List<ChartTrackerTrendDto> trackerTrends = ChartDaoImpl.getInstance().getTrackerTrend(null, trackers, getStartDate(), getEndDate());

		// convert
		TimeSeries openTimeSeries = new TimeSeries("Open Tasks");
		TimeSeries openEstimatedTimeSeries = new TimeSeries("Est. Hours");
		TimeSeries openSpentTimeSeries = new TimeSeries("Spent Hours");
		for (ChartTrackerTrendDto trend : trackerTrends) {
			Day day = new Day(trend.getDate());

			openTimeSeries.add(day, trend.getOpen());
			openEstimatedTimeSeries.add(day, trend.getOpenEstimatedHours());
			openSpentTimeSeries.add(day, trend.getOpenSpentHours());
		}

		TimeSeriesCollection timeSeriesCollection = new TimeSeriesCollection();
		timeSeriesCollection.addSeries(openTimeSeries);
		timeSeriesCollection.addSeries(openEstimatedTimeSeries);
		timeSeriesCollection.addSeries(openSpentTimeSeries);
		return timeSeriesCollection;
	}
}
