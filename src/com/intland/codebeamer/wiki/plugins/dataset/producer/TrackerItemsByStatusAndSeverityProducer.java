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
import com.intland.codebeamer.persistence.dto.ChartTrackerItemsByStatusAndSeverityDto;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.util.PersistenceUtils;
import com.intland.codebeamer.wiki.plugins.dataset.WikiDataSetProducer;

/**
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id: aron.gombas 2008-11-13 10:34 +0000 19305:eea5b28151a5  $
 */
public class TrackerItemsByStatusAndSeverityProducer extends AbstractWikiDataSetProducer implements WikiDataSetProducer {
	public Dataset produce(Map<String, String> params) throws PluginException {
		UserDto user = getUser();
		ProjectDto project = getProject();
		if (project == null) {
			throw new PluginException("Parameter 'projectId' is required");
		}

		List<TrackerDto> trackers = TrackerManager.getInstance().findByProject(user, project, Boolean.FALSE);
		List<Integer> trackerIds = parseCommaSeparatedIds(params.get("trackerId"));
		if (trackerIds == null || trackerIds.isEmpty()) {
			trackerIds = PersistenceUtils.grabIds(trackers);
		} else {
			trackerIds.retainAll(PersistenceUtils.grabIds(trackers));
		}

		DefaultCategoryDataset defaultCategoryDataset = new DefaultCategoryDataset();
		if (trackerIds.size() > 0) {
			List<ChartTrackerItemsByStatusAndSeverityDto> trackerItemsBySeverities = ChartDaoImpl.getInstance().getTrackerItemsByStatusAndSeverity(null, trackerIds, null, null, null);
			for (ChartTrackerItemsByStatusAndSeverityDto trackerItemsBySeverity : trackerItemsBySeverities) {
				defaultCategoryDataset.addValue(trackerItemsBySeverity.getCount(), trackerItemsBySeverity.getSeverity(), trackerItemsBySeverity.getStatus());
			}

			// Fill empty cells with zero values, otherwise DataSetPlugin and Chart rendering fail
			for (Object rowKey : defaultCategoryDataset.getRowKeys()) {
				for (Object columnKey : defaultCategoryDataset.getColumnKeys()) {
					if (defaultCategoryDataset.getValue((Comparable)rowKey, (Comparable)columnKey) == null) {
						defaultCategoryDataset.addValue(Integer.valueOf(0), (Comparable)rowKey, (Comparable)columnKey);
					}
				}
			}
		}
		return defaultCategoryDataset;
	}
}