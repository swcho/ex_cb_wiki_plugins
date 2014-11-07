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

import java.util.Map;

import org.jfree.data.general.Dataset;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.wiki.plugins.dataset.WikiDataSetProducer;

/**
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id$
 */
public class TrackerItemsPerSourceTrendProducer extends AbstractWikiDataSetProducer implements WikiDataSetProducer {
	public Dataset produce(Map<String, String> params) throws PluginException {
		throw new PluginException("TrackerItemsPerSourceTrendProducer not refactored yet");
/* TODO refactor or remove

		UserDto user = getUser();
		ProjectDto project = getProject();
		if(project == null) {
			throw new PluginException("Parameter 'projectId' is required");
		}
		List<Integer> trackerIds = parseCommaSeparatedIds(params.get("trackerId"));
		List<TrackerDto> trackers = trackerIds.isEmpty() ? TrackerManager.getInstance().findByProject(user, project) : TrackerManager.getInstance().findById(user, trackerIds);

		// gather
		List<ChartTrackerItemsPerSourceDto> trackerItemsPerSources = ChartDaoImpl.getInstance().getTrackerItemsPerSourceTrend(null, PersistenceUtils.grabIds(trackers));

		// convert
		TimeSeries timeSeries = new TimeSeries("Tracker Items per Source");
		for (ChartTrackerItemsPerSourceDto trackerItemsPerSource : trackerItemsPerSources) {
			timeSeries.add(new Day(trackerItemsPerSource.getDate()), trackerItemsPerSource.getIssuePerSourceRatio());
		}

		TimeSeriesCollection timeSeriesCollection = new TimeSeriesCollection(timeSeries);
		return timeSeriesCollection;
*/

	}
}
