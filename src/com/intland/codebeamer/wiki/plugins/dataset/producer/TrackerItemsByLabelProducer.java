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

import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.Dataset;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.manager.TrackerManager;
import com.intland.codebeamer.persistence.dao.impl.ChartDaoImpl;
import com.intland.codebeamer.persistence.dto.ChartTrackerItemsByLabelDto;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.TrackerLayoutLabelDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.util.PersistenceUtils;
import com.intland.codebeamer.wiki.plugins.dataset.WikiDataSetProducer;

/**
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id$
 */
public class TrackerItemsByLabelProducer extends AbstractWikiDataSetProducer implements WikiDataSetProducer {
	public Dataset produce(Map<String, String> params) throws PluginException {
		UserDto user = getUser();
		ProjectDto project = getProject();
		if(project == null) {
			throw new PluginException("Parameter 'projectId' is required");
		}
		Integer labelId = getLabelIdByLabelName(params.get("label"));
		List<Integer> trackerIds = parseCommaSeparatedIds(params.get("trackerId"));
		List<TrackerDto> trackers = trackerIds.isEmpty() ? TrackerManager.getInstance().findByProject(user, project) : TrackerManager.getInstance().findById(user, trackerIds);

		// gather
		List<ChartTrackerItemsByLabelDto> trackerItemsByLabels = ChartDaoImpl.getInstance().getTrackerItemsByLabel(labelId, null, PersistenceUtils.grabIds(trackers));

		// convert
		DefaultCategoryDataset defaultCategoryDataset = new DefaultCategoryDataset();
		for (ChartTrackerItemsByLabelDto trackerItemsByLabel : trackerItemsByLabels) {
			// If grouped by status, only show the total count
			if (labelId.intValue() != TrackerLayoutLabelDto.STATUS_LABEL_ID) {
				defaultCategoryDataset.addValue(trackerItemsByLabel.getOpenCount(),   trackerItemsByLabel.getLabelName(), "Open");
				defaultCategoryDataset.addValue(trackerItemsByLabel.getClosedCount(), trackerItemsByLabel.getLabelName(), "Closed");
			}
			defaultCategoryDataset.addValue(trackerItemsByLabel.getTotalCount(), trackerItemsByLabel.getLabelName(), "Total");
		}

		return defaultCategoryDataset;
	}
}
