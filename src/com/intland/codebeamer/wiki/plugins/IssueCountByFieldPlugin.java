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
import com.intland.codebeamer.chart.ChartDataCalculator;
import com.intland.codebeamer.chart.renderer.IssueCountByFieldRenderer;
import com.intland.codebeamer.controller.support.CommaStringToIntegerListPropertyEditor;
import com.intland.codebeamer.controller.support.CommaStringToStringListPropertyEditor;
import com.intland.codebeamer.controller.support.LowerCaseNameBasedEnumEditor;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.ChartSupport;
import com.intland.codebeamer.wiki.plugins.base.DefaultWebBindingInitializer;
import com.intland.codebeamer.wiki.plugins.command.IssueCountByFieldCommand;
import com.intland.codebeamer.wiki.plugins.command.enums.DisplayType;
import com.intland.codebeamer.wiki.plugins.command.enums.TrackerItemField;

/**
 * Plugin to display issue counts by one specific field.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public class IssueCountByFieldPlugin extends AbstractCommandWikiPlugin<IssueCountByFieldCommand> {
	@Autowired
	private ChartDataCalculator chartDataCalculator;

	public IssueCountByFieldPlugin() {
		setWebBindingInitializer(new IssueCountByFieldBindingInitializer());
	}

	public static class IssueCountByFieldPropertyEditorRegistrar implements PropertyEditorRegistrar {
		public void registerCustomEditors(PropertyEditorRegistry registry) {
			registry.registerCustomEditor(List.class, "projectId", new CommaStringToIntegerListPropertyEditor());
			registry.registerCustomEditor(List.class, "trackerId", new CommaStringToIntegerListPropertyEditor());
			registry.registerCustomEditor(List.class, "tag", new CommaStringToStringListPropertyEditor());
			registry.registerCustomEditor(TrackerItemField.class, new LowerCaseNameBasedEnumEditor(TrackerItemField.class));
			registry.registerCustomEditor(DisplayType.class, new LowerCaseNameBasedEnumEditor(DisplayType.class));			
		}
	}

	public static class IssueCountByFieldBindingInitializer extends DefaultWebBindingInitializer {
		public IssueCountByFieldBindingInitializer() {
			addPropertyEditorRegistrar(new IssueCountByFieldPropertyEditorRegistrar());
		}
	}

	@Override
	public IssueCountByFieldCommand createCommand() throws PluginException {
		return new IssueCountByFieldCommand();
	}

	@Override
	protected String getTemplateFilename() {
		return "issuecountbyfield-plugin.vm";
	}

	@Override
	protected Map populateModel(DataBinder binder, IssueCountByFieldCommand command, Map params) throws PluginException {
		if(CollectionUtils.isEmpty(command.getProjectId()) && CollectionUtils.isEmpty(command.getTrackerId()) && CollectionUtils.isEmpty(command.getTag())) {
			ProjectDto project = discoverProject(params, getWikiContext());
			command.setProjectId(Arrays.asList(project.getId()));
		}
		
		Map model = new HashMap(3);
		model.put("command", command);
		if (command.isTable()) {	
			model.put("data", chartDataCalculator.getIssueCountByField(getUser(), command));
		}
		model.put("chartSupport", new ChartSupport(this, command, new IssueCountByFieldRenderer()));
		
		return model;
	}
}
