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

import java.util.Date;
import java.util.Map;

import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.Dataset;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.persistence.dao.impl.ChartDaoImpl;
import com.intland.codebeamer.persistence.dto.ChartGanttStatsDto;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.util.PersistenceUtils;
import com.intland.codebeamer.wiki.plugins.dataset.WikiDataSetProducer;

/**
 * @author <a href="mailto:aron.gombas@intland.com">Vitaly Sumenkov</a>
 * @version $Id$
 */
public class GanttStatsProducer extends AbstractWikiDataSetProducer implements WikiDataSetProducer {
	public Dataset produce(Map<String, String> params) throws PluginException {
		ProjectDto project = getProject();
		if(project == null) {
			throw new PluginException("Parameter 'projectId' is required");
		}

		// gather
		ChartGanttStatsDto ganttStats = ChartDaoImpl.getInstance().getGanntStats(PersistenceUtils.createSingleItemList(project.getId()), new Date());

		// convert
		DefaultCategoryDataset defaultCategoryDataset = new DefaultCategoryDataset();
		defaultCategoryDataset.addValue(ganttStats.getOverdueCount(), "Overdue", "Count");
		defaultCategoryDataset.addValue(ganttStats.getOvertimeCount(), "Overtime", "Count");
		defaultCategoryDataset.addValue(ganttStats.getPlannedCount(), "Planned", "Count");

		return defaultCategoryDataset;

	}
}
