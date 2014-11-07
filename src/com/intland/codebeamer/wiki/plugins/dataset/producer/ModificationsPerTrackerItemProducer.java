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
import com.intland.codebeamer.persistence.dto.ChartModificationsPerTrackerItemTrendDto;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.TrackerTypeDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.util.PersistenceUtils;
import com.intland.codebeamer.wiki.plugins.dataset.WikiDataSetProducer;

/**
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 */
public class ModificationsPerTrackerItemProducer extends AbstractWikiDataSetProducer implements WikiDataSetProducer {
	public Dataset produce(Map<String, String> params) throws PluginException {
		// validate
		UserDto user = getUser();
		ProjectDto project = getProject();
		if(project == null) {
			throw new PluginException("Parameter 'projectId' is required");
		}
		List<TrackerDto> trackers = TrackerManager.getInstance().findByProject(user, project);
		List<Integer> trackerIds = PersistenceUtils.grabIds(trackers);

		// gather
		List<ChartModificationsPerTrackerItemTrendDto> modificationsPerTrackerItems = ChartDaoImpl.getInstance().getModificationsPerTrackerItemTrend(TrackerTypeDto.REQUIREMENT.getId(), trackerIds);

		// convert
		TimeSeries timeSeries = new TimeSeries("Modifications");
		for (ChartModificationsPerTrackerItemTrendDto modificationPerTrackerItem : modificationsPerTrackerItems) {
			timeSeries.add(new Day(modificationPerTrackerItem.getDate()), modificationPerTrackerItem.getModifications());
		}

		TimeSeriesCollection timeSeriesCollection = new TimeSeriesCollection(timeSeries);
		return timeSeriesCollection;
	}
}
