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

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.chart.ChartDataCalculator;
import com.intland.codebeamer.chart.renderer.ArtifactActivityTrendsRenderer;
import com.intland.codebeamer.controller.support.CommaStringToIntegerListPropertyEditor;
import com.intland.codebeamer.controller.support.CommaStringToStringListPropertyEditor;
import com.intland.codebeamer.controller.support.LowerCaseNameBasedEnumEditor;
import com.intland.codebeamer.wiki.plugins.base.AbstractTimeIntervalCommandWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.ChartSupport;
import com.intland.codebeamer.wiki.plugins.base.DefaultWebBindingInitializer;
import com.intland.codebeamer.wiki.plugins.command.ArtifactActivityTrendsCommand;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractTimeIntervalDisplayOptionCommand.StandardTimeIntervalsAndGrouping;
import com.intland.codebeamer.wiki.plugins.command.enums.Activity;
import com.intland.codebeamer.wiki.plugins.command.enums.DisplayType;

/**
 * Plugin to display activity trends on artifacts.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public class ArtifactActivityTrendsPlugin extends AbstractTimeIntervalCommandWikiPlugin<ArtifactActivityTrendsCommand> {
	@Autowired
	private ChartDataCalculator chartDataCalculator;

	public ArtifactActivityTrendsPlugin() {
		setWebBindingInitializer(new ArtifactActivityTrendsBindingInitializer());
	}

	public static class ArtifactActivityTrendsPropertyEditorRegistrar extends TimeIntervalPropertyEditorRegistrar implements PropertyEditorRegistrar {
		public void registerCustomEditors(PropertyEditorRegistry registry) {
			super.registerCustomEditors(registry);
			registry.registerCustomEditor(List.class, "artifactId", new CommaStringToIntegerListPropertyEditor());
			registry.registerCustomEditor(List.class, "tag", new CommaStringToStringListPropertyEditor());
			registry.registerCustomEditor(Activity.class, new LowerCaseNameBasedEnumEditor(Activity.class));
			registry.registerCustomEditor(DisplayType.class, new LowerCaseNameBasedEnumEditor(DisplayType.class));
		}
	}

	public static class ArtifactActivityTrendsBindingInitializer extends DefaultWebBindingInitializer {
		public ArtifactActivityTrendsBindingInitializer() {
			addPropertyEditorRegistrar(new ArtifactActivityTrendsPropertyEditorRegistrar());
		}
	}

	@Override
	public ArtifactActivityTrendsCommand createCommand() throws PluginException {
		return new ArtifactActivityTrendsCommand();
	}

	@Override
	protected String getTemplateFilename() {
		return "artifactactivitytrends-plugin.vm";
	}

	@Override
	protected Map populateModel(DataBinder binder, ArtifactActivityTrendsCommand command, Map params) throws PluginException {
		Map model = new HashMap(4);

		model.put("command", command);
		model.put("standardTimeIntervalsAndGrouping", StandardTimeIntervalsAndGrouping.values());
		if (command.isTable()) {
			model.put("data", chartDataCalculator.getArtifactActivityTrends(getUser(), command));
		}
		model.put("chartSupport", new ChartSupport(this, command, new ArtifactActivityTrendsRenderer()));

		return model;
	}
}
