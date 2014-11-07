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
package com.intland.codebeamer.wiki.plugins;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.google.common.collect.Multimap;
import com.intland.codebeamer.controller.support.CardboardSupport;
import com.intland.codebeamer.manager.TrackerItemManager;
import com.intland.codebeamer.manager.support.AgileSupport;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.base.NamedDto;
import com.intland.codebeamer.taglib.tagwrappers.ui.ProgressBarWrapper;
import com.intland.codebeamer.wiki.plugins.base.AbstractAgileWikiPlugin;

/**
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public class SprintBreakDownPlugin extends AbstractAgileWikiPlugin<SprintBreakDownCommand> {
	@Autowired
	private TrackerItemManager trackerItemManager;
	@Autowired
	private AgileSupport agileSupport;
	@Autowired
	private CardboardSupport cardboardSupport;

	@Override
	public SprintBreakDownCommand createCommand() throws PluginException {
		return new SprintBreakDownCommand();
	}

	@Override
	protected Map populateModel(DataBinder binder, SprintBreakDownCommand command, Map params) throws PluginException {
		UserDto user = getUser();
		TrackerItemDto release = discoverRelease(getUser(), command.getReleaseId(), binder);

		String progressBarHtml = null;
		Map<String, Object> storyProgressModel = null;
		if(release != null) {
			List<TrackerItemDto> issues = trackerItemManager.findTrackerItemsByRelease(user, release.getId(), true);
			if(CollectionUtils.isNotEmpty(issues)) {
				Multimap<NamedDto, TrackerItemDto> issuesByColumn = cardboardSupport.groupIssuesToColumns(getWikiContext().getHttpRequest(), issues);
				storyProgressModel = agileSupport.buildStoryProgressModel(issues, issuesByColumn);

				// render the bar without titles
				ProgressBarWrapper wrapper = new ProgressBarWrapper();
				wrapper.percentages = (Integer[])storyProgressModel.get("percentages");
				wrapper.bgcolors = (String[])storyProgressModel.get("bgcolors");
				wrapper.fontcolors = (String[])storyProgressModel.get("fontcolors");
				wrapper.cssClasses = (String[])storyProgressModel.get("classes");
				wrapper.totalPercentage = Integer.valueOf(100);
				progressBarHtml = wrapper.render(getWikiContext().getHttpRequest());
			}
		}

		Map model = new HashMap();
		model.put("command", command);
		model.put("release", release);
		model.put("progressBarHtml", progressBarHtml);
		model.put("storyProgressModel", storyProgressModel);

		return model;
	}

	@Override
	protected String getTemplateFilename() {
		return "sprintBreakDown-plugin.vm";
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
}
