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
package com.intland.codebeamer.wiki.plugins.issuecounttrends;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.DataBinder;
import org.springframework.validation.ObjectError;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.chart.ChartDataCalculator;
import com.intland.codebeamer.chart.renderer.IssueCountTrendsRenderer;
import com.intland.codebeamer.controller.support.CommaStringToIntegerListPropertyEditor;
import com.intland.codebeamer.controller.support.CommaStringToStringListPropertyEditor;
import com.intland.codebeamer.controller.support.LowerCaseNameBasedEnumEditor;
import com.intland.codebeamer.persistence.dto.ChartTrackerItemsTrendDto;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.wiki.plugins.ajax.AbstractAjaxRefreshingWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.ChartSupport;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;
import com.intland.codebeamer.wiki.plugins.base.AbstractTimeIntervalCommandWikiPlugin.TimeIntervalBindingInitializer;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractTimeIntervalDisplayOptionCommand.StandardTimeIntervalsAndGrouping;
import com.intland.codebeamer.wiki.plugins.command.enums.DisplayType;

/**
 * Plugin which renders issue count trends.
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class IssueCountTrendsPlugin extends AbstractAjaxRefreshingWikiPlugin<IssueCountTrendsCommand> {
	@Autowired
	ChartDataCalculator chartDataCalculator;

	public IssueCountTrendsPlugin() {
		setWebBindingInitializer(new IssueCountTrendsPluginBindingInitializer());
	}

	public static class IssueCountTrendsPluginBindingInitializer extends TimeIntervalBindingInitializer {
		public IssueCountTrendsPluginBindingInitializer() {
			addPropertyEditorRegistrar(new PropertyEditorRegistrar() {
				public void registerCustomEditors(PropertyEditorRegistry registry) {
					registry.registerCustomEditor(List.class, "projectId", new CommaStringToIntegerListPropertyEditor());
					registry.registerCustomEditor(List.class, "trackerId", new CommaStringToIntegerListPropertyEditor());
					registry.registerCustomEditor(List.class, "tag", new CommaStringToStringListPropertyEditor());
					registry.registerCustomEditor(DisplayType.class, new LowerCaseNameBasedEnumEditor(DisplayType.class));
				}
			});
		}
	}

	@Override
	protected void validate(DataBinder binder, IssueCountTrendsCommand command, Map params) throws NamedPluginException {
		if (CollectionUtils.isEmpty(command.getProjectId()) &&
			CollectionUtils.isEmpty(command.getTrackerId()) &&
			CollectionUtils.isEmpty(command.getTag())) {
			ProjectDto project = discoverProject(params, getWikiContext());
			if (project != null) {
				command.setProjectId(Arrays.asList(new Integer[] {project.getId()}));
			} else {
				binder.getBindingResult().addError(new ObjectError("command", "Project ID is missing."));
			}
		}
		super.validate(binder, command, params);
	}

	@Override
	public IssueCountTrendsCommand createCommand() throws PluginException {
		return new IssueCountTrendsCommand();
	}

	@Override
	protected Map populateModelInternal(DataBinder binder, IssueCountTrendsCommand command, Map params) throws PluginException {
		Map model = new HashMap();
		model.put("command", command);
		model.put("standardTimeIntervalsAndGrouping", StandardTimeIntervalsAndGrouping.values());

		model.put("chartSupport", new ChartSupport(this, command, new IssueCountTrendsRenderer()));

		if (command.isTable()) {
			SortedMap<Date, ChartTrackerItemsTrendDto> data = chartDataCalculator.getIssueCountTrends(getUser(), command);
			model.put("data", data);
		}
		return model;
	}

	@Override
	protected String getTemplateFilename() {
		return "issueCountTrends-plugin.vm";
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
}
