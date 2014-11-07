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
package com.intland.codebeamer.wiki.plugins.tasq;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.DataBinder;
import org.apache.commons.lang.StringUtils;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.controller.support.SimpleMessageResolver;
import com.intland.codebeamer.manager.UserManager;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.ui.view.PriorityRenderer;
import com.intland.codebeamer.utils.MultiValue;
import com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.DefaultWebBindingInitializer;
import com.intland.codebeamer.wiki.plugins.mycurrentissues.MyCurrentIssuesPlugin;

/**
 * Plugin shows task-Queue for a user.
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class TaskQPlugin extends AbstractCommandWikiPlugin<TaskQCommand>{

	private final static Logger logger = Logger.getLogger(TaskQPlugin.class);

	@Autowired
	private ITaskQManager taskQManager;
	@Autowired
	private UserManager userManager;

	public TaskQPlugin() {
		setWebBindingInitializer(new DefaultWebBindingInitializer());
	}

	@Override
	public TaskQCommand createCommand() throws PluginException {
		return new TaskQCommand();
	}

	@Override
	protected String getTemplateFilename() {
		return "taskQ-plugin.vm";
	}

	@Override
	protected Map populateModel(DataBinder binder, TaskQCommand command, Map params) throws PluginException {
		Map<String,Object> model = new HashMap();
		model.put("command", command);

		if (logger.isDebugEnabled()) {
			logger.debug("Populating model for " + command);
		}
		UserDto user = getUser();

		UserDto targetUser = userManager.findById(user, command.getUserId());
		if (logger.isDebugEnabled()) {
			logger.debug("Target user: " + targetUser);
		}
		if (targetUser == null) {
			throw new PluginException("User with id=" + command.getUserId() +" is not found.");
		}

		if (StringUtils.isEmpty(command.getTitle())) {
			String defaultTitle = getSimpleMessageResolver().getMessage("taskq.plugin.default.title", targetUser.getName(), command.getStatus());
			command.setTitle(defaultTitle);
		}

		MultiValue<List<TrackerItemDto>, Integer> issuesAndCount = taskQManager.findIssues(user, targetUser, command);
		List<TrackerItemDto> issues = issuesAndCount.getLeft();

		String displayedAndTotalIssues = MyCurrentIssuesPlugin.getDisplayedAndTotalIssuesText(issues.size(), issuesAndCount.getRight().intValue(), getSimpleMessageResolver());
		model.put("displayedAndTotalIssues", displayedAndTotalIssues);

		model.put("issues", issues);
		SimpleMessageResolver simpleMessageResolver = SimpleMessageResolver.getInstance(getWikiContext().getHttpRequest());
		model.put("priorityRenderer", new PriorityRenderer(getContextPath(), simpleMessageResolver));

		return model;
	}

}
