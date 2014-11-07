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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.chart.renderer.ProjectActivityTrendsRenderer;
import com.intland.codebeamer.controller.support.CommaStringToIntegerListPropertyEditor;
import com.intland.codebeamer.controller.support.CommaStringToStringListPropertyEditor;
import com.intland.codebeamer.controller.support.LowerCaseNameBasedEnumEditor;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.wiki.plugins.base.AbstractTimeIntervalCommandWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.ChartSupport;
import com.intland.codebeamer.wiki.plugins.base.DefaultWebBindingInitializer;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractTimeIntervalDisplayOptionCommand.StandardTimeIntervalsAndGrouping;
import com.intland.codebeamer.wiki.plugins.command.enums.DisplayType;

/**
 * Plugin to display project activity trends
 *
 * @author <a href="mailto:klaus.mehling@intland.com">Klaus Mehling</a>
 */
public class ProjectActivityTrendsPlugin extends AbstractTimeIntervalCommandWikiPlugin<ProjectActivityTrendsCommand> {
	@Autowired
	private ActivityStreamManager activityStreamManager;

	public ProjectActivityTrendsPlugin() {
		setWebBindingInitializer(new ProjectActivityTrendsBindingInitializer());
	}

	public static class ProjectActivityTrendsPropertyEditorRegistrar extends TimeIntervalPropertyEditorRegistrar implements PropertyEditorRegistrar {
		public void registerCustomEditors(PropertyEditorRegistry registry) {
			super.registerCustomEditors(registry);
			registry.registerCustomEditor(List.class, "projectId", new CommaStringToIntegerListPropertyEditor());
			registry.registerCustomEditor(List.class, "tag", new CommaStringToStringListPropertyEditor());
			registry.registerCustomEditor(DisplayType.class, new LowerCaseNameBasedEnumEditor(DisplayType.class));
		}
	}

	public static class ProjectActivityTrendsBindingInitializer extends DefaultWebBindingInitializer {
		public ProjectActivityTrendsBindingInitializer() {
			addPropertyEditorRegistrar(new ProjectActivityTrendsPropertyEditorRegistrar());
		}
	}

	@Override
	public ProjectActivityTrendsCommand createCommand() throws PluginException {
		return new ProjectActivityTrendsCommand();
	}

	@Override
	protected String getTemplateFilename() {
		return "projectactivitytrends-plugin.vm";
	}

	@Override
	protected Map populateModel(DataBinder binder, ProjectActivityTrendsCommand command, Map params) throws PluginException {
		Map model = new HashMap(4);

		if(CollectionUtils.isEmpty(command.getProjectId()) && CollectionUtils.isEmpty(command.getTag())) {
			ProjectDto project = discoverProject(params, getWikiContext());
			command.setProjectId(Arrays.asList(project.getId()));
		}

		model.put("command", command);
		model.put("standardTimeIntervalsAndGrouping", StandardTimeIntervalsAndGrouping.values());
		if (command.isTable()) {
			model.put("data", activityStreamManager.getActivityTrends(getUser(), command));
		}
		model.put("chartSupport", new ChartSupport(this, command, new ProjectActivityTrendsRenderer()));

		return model;
	}
}
