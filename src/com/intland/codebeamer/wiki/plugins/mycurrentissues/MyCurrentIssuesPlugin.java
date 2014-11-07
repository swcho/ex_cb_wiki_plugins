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
package com.intland.codebeamer.wiki.plugins.mycurrentissues;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.controller.support.SimpleMessageResolver;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.ui.view.PriorityRenderer;
import com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.DefaultWebBindingInitializer;

/**
 * Plugin displays a flat list of the issues assigned to me, and not in the (closed, resolved) statuses. Ordering: by priority desc, updatedAt desc.
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class MyCurrentIssuesPlugin extends AbstractCommandWikiPlugin<MyCurrentIssuesCommand> {

	private final static Logger logger = Logger.getLogger(MyCurrentIssuesPlugin.class);

	@Autowired
	IMyCurrentIssuesManager myCurrentIssuesManager;

	public MyCurrentIssuesPlugin() {
		setWebBindingInitializer(new DefaultWebBindingInitializer());
	}

	@Override
	protected String getTemplateFilename() {
		return "myCurrentIssues-plugin.vm";
	}

	@Override
	public MyCurrentIssuesCommand createCommand() throws PluginException {
		return new MyCurrentIssuesCommand();
	}

	public static String getDisplayedAndTotalIssuesText(int displayed, int total, SimpleMessageResolver simpleMessageResolver) {
		NumberFormat format = NumberFormat.getNumberInstance();

		String displayedAndTotalIssues;
		if (displayed < total) {
			displayedAndTotalIssues = simpleMessageResolver.getMessage("subset.x.of.y", format.format(displayed) , format.format(total));
		} else {
			displayedAndTotalIssues = format.format(displayed);
		}
		return displayedAndTotalIssues;
	}

	@Override
	protected Map populateModel(DataBinder binder, MyCurrentIssuesCommand command, Map params) throws PluginException {
		Map model = new HashMap();

		model.put("command", command);
		List<TrackerItemDto> issues = myCurrentIssuesManager.findIssues(getUser(), command);
		if (logger.isDebugEnabled()) {
			logger.debug("Found issues for <" + command +"> are: " + issues);
		}

		int numberofFoundIssues = issues.size();

		int max = command.getMax() != null ? command.getMax().intValue() : -1;
		if (max > 0 && numberofFoundIssues > max) {
			issues = issues.subList(0, max);
		}

		String displayedAndTotalIssues = getDisplayedAndTotalIssuesText(issues.size(), numberofFoundIssues, getSimpleMessageResolver());
		model.put("displayedAndTotalIssues", displayedAndTotalIssues);

		model.put("issues", issues);
		SimpleMessageResolver simpleMessageResolver = SimpleMessageResolver.getInstance(getWikiContext().getHttpRequest());
		model.put("priorityRenderer", new PriorityRenderer(getContextPath(), simpleMessageResolver));

		return model;
	}
}
