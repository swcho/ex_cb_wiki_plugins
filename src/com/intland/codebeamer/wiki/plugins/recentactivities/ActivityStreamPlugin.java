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

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.controller.ControllerUtils;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.TrackerItemHistoryEntryDto;
import com.intland.codebeamer.persistence.dto.TrackerItemRevisionDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.WikiPageDto;
import com.intland.codebeamer.rss.AbstractCodebeamerAtomFeedView;
import com.intland.codebeamer.ui.view.table.TrackerItemHistoryDecorator;
import com.intland.codebeamer.utils.velocitytool.DateBucketTool;
import com.intland.codebeamer.utils.velocitytool.UserPhotoTool;
import com.intland.codebeamer.wiki.plugins.ajax.AbstractAjaxRefreshingWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.DefaultWebBindingInitializer;
import com.intland.codebeamer.wiki.plugins.recentactivities.Activity.Change;

/**
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public class ActivityStreamPlugin extends AbstractAjaxRefreshingWikiPlugin<ActivityStreamCommand> {
	private final static Logger logger = Logger.getLogger(ActivityStreamPlugin.class);

	@Autowired
	private ActivityStreamManager recentActivitiesPluginManager;

	public ActivityStreamPlugin() {
		setWebBindingInitializer(new DefaultWebBindingInitializer());
	}

	@Override
	public ActivityStreamCommand createCommand() throws PluginException {
		return new ActivityStreamCommand();
	}

	@Override
	protected String getTemplateFilename() {
		return "activitystream-plugin.vm";
	}

	@Override
	protected Map populateModelInternal(DataBinder binder, ActivityStreamCommand command, Map params) throws PluginException {
		UserDto user = getUser();
		WikiContext context = getWikiContext();
		HttpServletRequest request = context.getHttpRequest();
		Locale locale = request.getLocale();
		MessageSource messageSource = ControllerUtils.getMessageSource(request);
		WikiPageDto wikiPage = getPageFromContext(context);
		boolean isUserPage = wikiPage != null && wikiPage.isUserPage();

		if(CollectionUtils.isEmpty(command.getProjectId()) && CollectionUtils.isEmpty(command.getTag())) {
			if (isUserPage) {
				command.setProjectId(projectManager.findAll(user, false));
			} else {
				ProjectDto project = discoverProject(params, getWikiContext());
				command.setProjectId(Arrays.asList(project.getId()));
			}
		}

		long startTime = System.currentTimeMillis();
		List<Activity> recentActivities = recentActivitiesPluginManager.findActivities(user, command);

		NumberFormat nf = NumberFormat.getIntegerInstance(Locale.US);
		logger.info("Elapsed: " + nf.format(System.currentTimeMillis() - startTime) + " ms.");

		if (recentActivities != null && recentActivities.size() > 0) {
			TrackerItemHistoryDecorator historyDecorator = new TrackerItemHistoryDecorator();
			historyDecorator.init(request, user, locale);

			for (Activity activity : recentActivities) {
				List<Change> changes = activity.getChanges();
				if (changes != null && changes.size() > 0 && activity.getTarget() instanceof TrackerItemDto) {
					historyDecorator.setItemRevision(new TrackerItemRevisionDto((TrackerItemDto)activity.getTarget(), null, null));

					for (Change change : changes) {
						if (change.getDetail() instanceof TrackerItemHistoryEntryDto) {
							historyDecorator.initRow(change.getDetail(), 0, 0);

							TrackerItemHistoryEntryDto rendered = new TrackerItemHistoryEntryDto();
							rendered.setOldValue(historyDecorator.getOldValue());
							rendered.setNewValue(historyDecorator.getNewValue());

							change.setSubject(historyDecorator.getFieldName());
							change.setDetail(rendered);
						}
					}
				}
			}
		}

		Map<String,Object> model = new HashMap<String,Object>(8);
		model.put("command", command);
		model.put("recentActivities", recentActivities);
		model.put("dateBucketTool", new DateBucketTool(user, locale, messageSource, recentActivities));
		model.put("userPhotoTool", new UserPhotoTool(wikiContext.getHttpRequest()));

		List<Integer> projectIds = command.getProjectId();
		if (isUserPage) {
			// Remove project IDs thus they not get part of the URL.
			command.setProjectId(null);
		}
		String feedURI = "/rss/activityStream.spr?" + getRequestParamsByCommand(command, false);

		// Restore project IDs
		command.setProjectId(projectIds);

		String feedURL = AbstractCodebeamerAtomFeedView.createFeedUrl(wikiContext.getHttpRequest(), feedURI);
		model.put("feedUrl", feedURL);

		return model;
	}
}
