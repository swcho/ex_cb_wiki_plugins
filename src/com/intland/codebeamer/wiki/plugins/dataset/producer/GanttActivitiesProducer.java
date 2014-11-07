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

import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.jfree.data.general.Dataset;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.persistence.dao.impl.ChartDaoImpl;
import com.intland.codebeamer.persistence.dto.ChartGanttActivityDto;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.util.PersistenceUtils;
import com.intland.codebeamer.wiki.plugins.dataset.WikiDataSetProducer;

/**
 * @author <a href="mailto:aron.gombas@intland.com">Vitaly Sumenkov</a>
 * @version $Id$
 */
public class GanttActivitiesProducer extends AbstractWikiDataSetProducer implements WikiDataSetProducer {
	public Dataset produce(Map<String, String> params) throws PluginException {
		ProjectDto project = getProject();
		if(project == null) {
			throw new PluginException("Parameter 'projectId' is required");
		}

		// gather
		List<ChartGanttActivityDto> ganttActivities = ChartDaoImpl.getInstance().getGanttActivities(PersistenceUtils.createSingleItemList(project.getId()));

		// convert
		TaskSeries taskSeries = new TaskSeries("Tasks");
		for (ChartGanttActivityDto ganttActivity : ganttActivities) {
			Task task = new Task(ganttActivity.getTrackerItem().getInterwikiLink() + " " + ganttActivity.getTrackerItem().getName(), ganttActivity.getStartDate(), ganttActivity.getEndDate());
			task.setPercentComplete(ganttActivity.getPercentage());

			taskSeries.add(task);
		}

		TaskSeriesCollection taskSeriesCollection = new TaskSeriesCollection();
		taskSeriesCollection.add(taskSeries);
		return taskSeriesCollection;
	}
}
