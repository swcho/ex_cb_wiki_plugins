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
package com.intland.codebeamer.wiki.plugins.ajax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.DataBinder;
import org.springframework.validation.ObjectError;

import com.intland.codebeamer.chart.ChartDataCalculator;
import com.intland.codebeamer.manager.TestingManager;
import com.intland.codebeamer.manager.TrackerItemManager;
import com.intland.codebeamer.manager.TrackerManager;
import com.intland.codebeamer.persistence.dao.impl.ChartDaoImpl;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.TrackerTypeDto;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;
import com.intland.codebeamer.wiki.plugins.command.AbstractTestManagementCommand;


/**
 * @author <a href="mailto:akos.tajti@intland.com">Akos Tajti</a>
 *
 */
public abstract class AbstractAjaxRefreshingTestManagementPlugin<Command extends AbstractTestManagementCommand> extends AbstractAjaxRefreshingWikiPlugin<Command> {
	@Autowired
	protected ChartDataCalculator chartDataCalculator;

	@Autowired
	protected TestingManager testingManager;

	@Autowired
	protected TrackerItemManager trackerItemManager;

	@Autowired
	protected TrackerManager trackerManager;

	protected void findConfigurations(Map<String, Object> model, ProjectDto project) {
		TrackerDto configurationTracker = trackerManager.findDefaultTrackerOfType(getUser(), project.getId(), TrackerTypeDto.TESTCONF);
		if (configurationTracker != null) {
			List<TrackerItemDto> configurations = new ArrayList<TrackerItemDto>(trackerItemManager.findByTracker(getUser(), Collections.singletonList(configurationTracker.getId()), null));
			Collections.sort(configurations, new ChartDaoImpl.NamedDtoByNameComparator());
			Map<Integer, String> configurationMap = createMap(configurations);
			model.put("configurations", configurationMap);
		}
	}

	protected void findReleases(Map<String, Object> model, ProjectDto project) {
		TrackerDto releaseTracker = trackerManager.findDefaultTrackerOfType(getUser(), project.getId(), TrackerTypeDto.RELEASE);
		if (releaseTracker != null) {
			List<TrackerItemDto> releases = new ArrayList<TrackerItemDto>(trackerItemManager.findByTracker(getUser(), Collections.singletonList(releaseTracker.getId()), null));
			Collections.sort(releases, new ChartDaoImpl.NamedDtoByNameComparator());
			Map<Integer, String> releaseMap = createMap(releases);
			model.put("releases", releaseMap);
		}
	}

	protected Map<Integer, String> createMap(List<TrackerItemDto> items) {
		Map<Integer, String> urlMap = new LinkedHashMap<Integer, String>();
		for (TrackerItemDto item: items) {
			urlMap.put(item.getId(), item.getName());
		}

		return urlMap;
	}

	@Override
	protected void validate(DataBinder binder, Command command, Map params) throws NamedPluginException {
		if (command.getProjectId() == null) {
			ProjectDto project = discoverProject(params, getWikiContext());
			if (project != null) {
				command.setProjectId(project.getId());
			} else {
				binder.getBindingResult().addError(new ObjectError("command", "Project ID is missing."));
			}
		}
		super.validate(binder, command, params);
	}
}
