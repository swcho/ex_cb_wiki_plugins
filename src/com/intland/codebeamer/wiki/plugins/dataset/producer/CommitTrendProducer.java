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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.math.NumberUtils;
import org.jfree.data.general.Dataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.springframework.beans.factory.annotation.Autowired;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.manager.ScmRepositoryManager;
import com.intland.codebeamer.persistence.dao.ChartDao;
import com.intland.codebeamer.persistence.dto.ChartCommitTrendDto;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.ScmRepositoryDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.wiki.plugins.dataset.WikiDataSetProducer;

/**
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 */
public class CommitTrendProducer extends AbstractWikiDataSetProducer implements WikiDataSetProducer {
	@Autowired
	private ChartDao chartDao;

	@Autowired
	private ScmRepositoryManager scmRepositoryManager;

	public Dataset produce(Map<String, String> params) throws PluginException  {
		UserDto user = getUser();

		List<ScmRepositoryDto> repositories = new ArrayList<ScmRepositoryDto>();

		Integer repositoryId = NumberUtils.createInteger(params.get("repositoryId"));
		if(repositoryId == null) {
			ProjectDto project = getProject();
			if (project == null || project.getId() == null) {
				throw new PluginException("Parameter 'repositoryId' or 'projectId' is required");
			}

			repositories = scmRepositoryManager.findByProject(user, project.getId());
		} else {
			ScmRepositoryDto repository = scmRepositoryManager.findById(user, repositoryId);
			if (repository != null) {
				repositories.add(repository);
			}
		}

		if (repositories.isEmpty()) {
			throw new PluginException("Permission denied for repositories");
		}

		boolean groupByUser = Boolean.parseBoolean(params.get("groupByUser"));

		// gather
		List<ChartCommitTrendDto> commitStats = chartDao.getCommitTrend(repositories, Boolean.valueOf(groupByUser), getStartDate(), getEndDate());

		// convert
		TimeSeriesCollection timeSeriesCollection = new TimeSeriesCollection();
		for (ChartCommitTrendDto commitStat : commitStats) {
			String key = (commitStat.getUserName() != null) ? commitStat.getUserName() : "Commits";

			TimeSeries timeSeries = timeSeriesCollection.getSeries(key);
			if(timeSeries == null) {
				timeSeries = new TimeSeries(key);
				timeSeriesCollection.addSeries(timeSeries);
			}

			timeSeries.add(new Day(commitStat.getDate()), commitStat.getCommitCount());
		}

		return timeSeriesCollection;
	}
}
