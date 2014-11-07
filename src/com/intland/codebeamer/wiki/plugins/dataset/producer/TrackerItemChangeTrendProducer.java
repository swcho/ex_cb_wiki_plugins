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
import com.intland.codebeamer.persistence.dao.ChartDao;
import com.intland.codebeamer.persistence.dto.ChartTrackerTrendDto;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.wiki.plugins.dataset.WikiDataSetProducer;

/**
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 */
public class TrackerItemChangeTrendProducer extends AbstractWikiDataSetProducer implements WikiDataSetProducer {
	private ChartDao chartDao;
	private TrackerManager trackerManager;

	public Dataset produce(Map<String, String> params) throws PluginException {
		UserDto user = getUser();
		ProjectDto project = getProject();
		if(project == null) {
			throw new PluginException("Parameter 'projectId' is required");
		}
		List<Integer> trackerIds = parseCommaSeparatedIds(params.get("trackerId"));
		List<TrackerDto> trackers = trackerIds.isEmpty() ? trackerManager.findByProject(user, project) : trackerManager.findById(user, trackerIds);

		// gather
		List<ChartTrackerTrendDto> trackerTrends = chartDao.getTrackerTrend(null, trackers, getStartDate(), getEndDate());

		// convert
		TimeSeries submittedTimeSeries = new TimeSeries("New");
		TimeSeries editedTimeSeries = new TimeSeries("Edited");
		TimeSeries closedTimeSeries = new TimeSeries("Closed");
		for (ChartTrackerTrendDto trend : trackerTrends) {
			Day day = new Day(trend.getDate());

			submittedTimeSeries.add(day, trend.getSubmitted());
			editedTimeSeries.add(day, trend.getEdited());
			closedTimeSeries.add(day, trend.getClosed());
		}

		TimeSeriesCollection timeSeriesCollection = new TimeSeriesCollection();
		timeSeriesCollection.addSeries(submittedTimeSeries);
		timeSeriesCollection.addSeries(editedTimeSeries);
		timeSeriesCollection.addSeries(closedTimeSeries);
		return timeSeriesCollection;
	}

	public void setChartDao(ChartDao chartDao) {
		this.chartDao = chartDao;
	}

	public void setTrackerManager(TrackerManager trackerManager) {
		this.trackerManager = trackerManager;
	}
}