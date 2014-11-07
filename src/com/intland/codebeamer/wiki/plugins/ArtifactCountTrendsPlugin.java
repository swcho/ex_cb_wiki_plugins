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
import org.springframework.validation.ObjectError;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.chart.ChartDataCalculator;
import com.intland.codebeamer.chart.renderer.ArtifactCountTrendsRenderer;
import com.intland.codebeamer.controller.support.CommaStringToIntegerListPropertyEditor;
import com.intland.codebeamer.controller.support.CommaStringToStringListPropertyEditor;
import com.intland.codebeamer.controller.support.LowerCaseNameBasedEnumEditor;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.wiki.plugins.base.AbstractTimeIntervalCommandWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.ChartSupport;
import com.intland.codebeamer.wiki.plugins.base.DefaultWebBindingInitializer;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;
import com.intland.codebeamer.wiki.plugins.command.ArtifactCountTrendsCommand;
import com.intland.codebeamer.wiki.plugins.command.enums.ArtifactType;
import com.intland.codebeamer.wiki.plugins.command.enums.DisplayType;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractTimeIntervalDisplayOptionCommand.StandardTimeIntervalsAndGrouping;

/**
 * Plugin to display count trends on artifacts.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public class ArtifactCountTrendsPlugin extends AbstractTimeIntervalCommandWikiPlugin<ArtifactCountTrendsCommand> {
	@Autowired
	private ChartDataCalculator chartDataCalculator;

	public ArtifactCountTrendsPlugin() {
		setWebBindingInitializer(new ArtifactCountTrendsBindingInitializer());
	}

	public static class ArtifactCountTrendsPropertyEditorRegistrar extends TimeIntervalPropertyEditorRegistrar implements PropertyEditorRegistrar {
		public void registerCustomEditors(PropertyEditorRegistry registry) {
			super.registerCustomEditors(registry);
			registry.registerCustomEditor(List.class, "projectId", new CommaStringToIntegerListPropertyEditor());
			registry.registerCustomEditor(List.class, "tag", new CommaStringToStringListPropertyEditor());
			registry.registerCustomEditor(ArtifactType.class, new LowerCaseNameBasedEnumEditor(ArtifactType.class));
			registry.registerCustomEditor(DisplayType.class, new LowerCaseNameBasedEnumEditor(DisplayType.class));
		}
	}

	public static class ArtifactCountTrendsBindingInitializer extends DefaultWebBindingInitializer {
		public ArtifactCountTrendsBindingInitializer() {
			addPropertyEditorRegistrar(new ArtifactCountTrendsPropertyEditorRegistrar());
		}
	}

	@Override
	public ArtifactCountTrendsCommand createCommand() throws PluginException {
		return new ArtifactCountTrendsCommand();
	}

	@Override
	protected void validate(DataBinder binder, ArtifactCountTrendsCommand command, Map params) throws NamedPluginException {
		if (CollectionUtils.isEmpty(command.getProjectId()) && CollectionUtils.isEmpty(command.getTag())) {
			// none of "projectId" and "tag" was specified
			ProjectDto project = discoverProject(params, getWikiContext());
			if (project != null) {
				command.setProjectId(Arrays.asList(new Integer[] {project.getId()}));
			} else {
				binder.getBindingResult().addError(new ObjectError("command", "either 'projectId' or 'tag' must be used"));
			}
		}

		super.validate(binder, command, params);
	}

	@Override
	protected String getTemplateFilename() {
		return "artifactcounttrends-plugin.vm";
	}

	@Override
	protected Map populateModel(DataBinder binder, ArtifactCountTrendsCommand command, Map params) throws PluginException {
		Map model = new HashMap(4);

		model.put("command", command);
		model.put("standardTimeIntervalsAndGrouping", StandardTimeIntervalsAndGrouping.values());
		if (command.isTable()) {
			model.put("data", chartDataCalculator.getArtifactCountTrends(getUser(), command));
		}
		model.put("chartSupport", new ChartSupport(this, command, new ArtifactCountTrendsRenderer()));

		return model;
	}
}
