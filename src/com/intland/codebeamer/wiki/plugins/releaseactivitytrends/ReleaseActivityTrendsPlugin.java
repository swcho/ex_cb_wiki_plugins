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
package com.intland.codebeamer.wiki.plugins.releaseactivitytrends;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.controller.ControllerUtils;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.wiki.plugins.ajax.AbstractAjaxRefreshingWikiPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.validation.DataBinder;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A plugin to display the project related tracker item counts grouped by releases.
 *
 * @author <a href="mailto:levente.cseko@intland.com">Levente Cseko</a>
 * @version $Id$
 */
@Component
public class ReleaseActivityTrendsPlugin extends AbstractAjaxRefreshingWikiPlugin<ReleaseActivityTrendsCommand> {

	@Autowired
	private ReleaseActivityTrendsManager manager;

	@Override
	public ReleaseActivityTrendsCommand createCommand() {
		return new ReleaseActivityTrendsCommand();
	}

	@Override
	protected String getTemplateFilename() {
		return isAjaxRendering() ? "releaseactivitytrends-table-plugin.vm" : "releaseactivitytrends-plugin.vm";
	}

	@Override
	protected Map populateModelInternal(DataBinder binder, ReleaseActivityTrendsCommand command, Map params) throws PluginException {

		WikiContext context = getWikiContext();
		UserDto user = getUser();

		validateParameters(command, params, context);

		boolean empty = true;
		List<ReleaseActivityTrendsManager.ReleaseParameters> releases = new ArrayList<ReleaseActivityTrendsManager.ReleaseParameters>();
		if (isAjaxRendering()) {
			empty = false;
			releases.addAll(manager.getReleaseParameters(user, command.getProjectId(), command.getReleaseStatus()));
		}

		Map<String, Integer> unscheduledItems = new HashMap<String, Integer>();
		if (Boolean.TRUE.equals(command.getShowUnscheduledItems()) && isAjaxRendering()) {
			unscheduledItems.putAll(manager.getUnscheduledItems(user, command.getProjectId()));
		}


		Map<String, Object> model = new HashMap<String, Object>();
		model.put("command", command);
		model.put("user", user);
		model.put("wikiContext", context);
		model.put("contextPath", getContextPath());
		model.put("empty", Boolean.valueOf(empty));
		model.put("releases", releases);
		model.put("unscheduledItems", unscheduledItems);
		model.put("trackerItemStatuses", ReleaseActivityTrendsManager.TRACKER_ITEM_STATUSES);
		model.put("releaseStatuses", ReleaseActivityTrendsManager.RELEASE_STATUSES);

		String actualReleaseStatus = command.getReleaseStatus();
		model.put("releaseStatusAjaxUrls", getReleaseStatusAjaxUrls(command));
		command.setReleaseStatus(actualReleaseStatus);

		return model;

	}

	private void validateParameters(ReleaseActivityTrendsCommand command, Map params, WikiContext context) throws PluginException {

		HttpServletRequest request = context.getHttpRequest();
		MessageSource messageSource = ControllerUtils.getMessageSource(request);
		Locale locale = request.getLocale();
		if (command.getTitle() == null) {
			command.setTitle(messageSource.getMessage("", null, "Release Activity Trends", locale));
		}

		if (command.getProjectId() == null) {
			ProjectDto project = discoverProject(params, context);
			if (project != null) {
				command.setProjectId(project.getId());
			} else {
				throw new PluginException(new IllegalArgumentException("Could not discover project, add projectId parameter!"));
			}
		}

		if (!ReleaseActivityTrendsManager.TRACKER_ITEM_STATUSES.contains(command.getTrackerItemStatus())) {
			command.setTrackerItemStatus(ReleaseActivityTrendsManager.DEFAULT_TRACKER_ITEM_STATUS);
		}

		if (!ReleaseActivityTrendsManager.RELEASE_STATUSES.contains(command.getReleaseStatus())) {
			command.setTrackerItemStatus(ReleaseActivityTrendsManager.DEFAULT_RELEASE_STATUS);
		}

	}

	private Map<String, String> getReleaseStatusAjaxUrls(ReleaseActivityTrendsCommand command) {
		Map<String, String> releaseStatusAjaxUrls = new HashMap<String, String>();
		for (String status : ReleaseActivityTrendsManager.RELEASE_STATUSES) {
			command.setReleaseStatus(status);
			releaseStatusAjaxUrls.put(status, getAjaxRefreshURL(command));
		}
		return releaseStatusAjaxUrls;
	}
}
