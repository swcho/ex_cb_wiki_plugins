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
import com.intland.codebeamer.persistence.dao.impl.ChartDaoImpl;
import com.intland.codebeamer.persistence.dto.ChartProjectTrendDto;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.util.PersistenceUtils;
import com.intland.codebeamer.wiki.plugins.dataset.WikiDataSetProducer;

/**
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id$
 */
public class ProjectTrendProducer extends AbstractWikiDataSetProducer implements WikiDataSetProducer {
	public Dataset produce(Map<String, String> params) throws PluginException {
		ProjectDto project = getProject();
		if(project == null) {
			throw new PluginException("Parameter 'projectId' is required");
		}

		// gather
		List<ChartProjectTrendDto> projectTrends = ChartDaoImpl.getInstance().getProjectsTrend(PersistenceUtils.createSingleItemList(project.getId()), getStartDate(), getEndDate());

		// convert
		TimeSeries usersTimeSeries = new TimeSeries("Users");
		TimeSeries artifactsTimeSeries = new TimeSeries("Artifacts");
		TimeSeries newArtifactsTimeSeries = new TimeSeries("New Artifacts");
		TimeSeries editedArtifactsTimeSeries = new TimeSeries("Edited Artifacts");
		TimeSeries downloadsTimeSeries = new TimeSeries("Downloads");
		TimeSeries successfulBuildsTimeSeries = new TimeSeries("Successful Builds");
		TimeSeries failedBuildsTimeSeries = new TimeSeries("Failed Builds");
		for (ChartProjectTrendDto projectTrend : projectTrends) {
			Day day = new Day(projectTrend.getDate());

			usersTimeSeries.add(day, projectTrend.getUsers());
			artifactsTimeSeries.add(day, projectTrend.getArtifacts());
			newArtifactsTimeSeries.add(day, projectTrend.getNewArtifacts());
			editedArtifactsTimeSeries.add(day, projectTrend.getEditedArtifacts());
			downloadsTimeSeries.add(day, projectTrend.getDownloadedArtifacts());
			successfulBuildsTimeSeries.add(day, projectTrend.getSuccessfulBuilds());
			failedBuildsTimeSeries.add(day, projectTrend.getFailedBuilds());
		}

		TimeSeriesCollection timeseriesCollection = new TimeSeriesCollection();
		timeseriesCollection.addSeries(usersTimeSeries);
		timeseriesCollection.addSeries(artifactsTimeSeries);
		timeseriesCollection.addSeries(newArtifactsTimeSeries);
		timeseriesCollection.addSeries(editedArtifactsTimeSeries);
		timeseriesCollection.addSeries(downloadsTimeSeries);
		timeseriesCollection.addSeries(successfulBuildsTimeSeries);
		timeseriesCollection.addSeries(failedBuildsTimeSeries);

		return timeseriesCollection;
	}
}
