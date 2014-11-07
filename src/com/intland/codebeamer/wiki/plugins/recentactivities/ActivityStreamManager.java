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
package com.intland.codebeamer.wiki.plugins.recentactivities;

import java.util.List;
import java.util.Map;

import com.intland.codebeamer.chart.data.DateIdentifiableMap;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.UserDto;

/**
 * Manager interface for collecting data for the {@link ActivityStreamPlugin}
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public interface ActivityStreamManager {
	/**
	 * Find the most active projects (from the specified projects) within the specified time period.
	 * @param user is user that requests the most active projects
	 * @param command contains the scope and period
	 * @return an ordered Map of activity score (value) per project (key), ordered descending by score
	 */
	Map<ProjectDto,Integer> findMostActiveProjects(UserDto user, MostActiveProjectsCommand command);

	/**
	 * Find the activity trends for the projects and time period specified by command
	 * @param user who asks for activity trends
	 * @param command defines the projects, time period and grouping
	 * @return the activity trends per reporting period and project
	 */
	DateIdentifiableMap<ProjectDto,Integer> getActivityTrends(UserDto user, ProjectActivityTrendsCommand command);

	/**
	 * Find the recent activities matching with the given filter bean. Ordered by the activity date descending (newest first).
	 * @param user The user who is performing the query
	 * @param command The filter bean
	 */
	List<Activity> findActivities(UserDto user, ActivityStreamCommand command);
}
