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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.DataBinder;
import org.springframework.validation.ObjectError;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.chart.ChartDataCalculator;
import com.intland.codebeamer.chart.renderer.RequirementCoverageRenderer;
import com.intland.codebeamer.controller.support.CommaStringToIntegerListPropertyEditor;
import com.intland.codebeamer.controller.support.LowerCaseNameBasedEnumEditor;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.AbstractTimeIntervalCommandWikiPlugin.TimeIntervalPropertyEditorRegistrar;
import com.intland.codebeamer.wiki.plugins.base.ChartSupport;
import com.intland.codebeamer.wiki.plugins.base.DefaultWebBindingInitializer;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;
import com.intland.codebeamer.wiki.plugins.command.RequirementCoverageCommand;
import com.intland.codebeamer.wiki.plugins.command.enums.DisplayType;

/**
 * @author <a href="mailto:akos.tajti@intland.com">Akos Tajti</a>
 *
 */
public class RequirementCoveragePlugin extends AbstractCommandWikiPlugin<RequirementCoverageCommand> {
	@Autowired
	private ChartDataCalculator chartDataCalculator;

	public RequirementCoveragePlugin() {
		setWebBindingInitializer(new RequirementCoverageBindingInitializer());
	}

	public static class RequirementCoveragePropertyEditorRegistrar extends TimeIntervalPropertyEditorRegistrar implements PropertyEditorRegistrar {
		public void registerCustomEditors(PropertyEditorRegistry registry) {
			super.registerCustomEditors(registry);
			registry.registerCustomEditor(DisplayType.class, "display", new LowerCaseNameBasedEnumEditor(DisplayType.class));
			registry.registerCustomEditor(List.class, "trackerId", new CommaStringToIntegerListPropertyEditor());
		}
	}

	public static class RequirementCoverageBindingInitializer extends DefaultWebBindingInitializer {
		public RequirementCoverageBindingInitializer() {
			addPropertyEditorRegistrar(new RequirementCoveragePropertyEditorRegistrar());
		}
	}

	@Override
	public RequirementCoverageCommand createCommand() throws PluginException {
		return new RequirementCoverageCommand();
	}

	@Override
	protected Map populateModel(DataBinder binder, RequirementCoverageCommand command, Map params)
			throws PluginException {
		Map<String, Object> model = new HashMap<String, Object>();
		if (command.getProjectId() == null) {
			ProjectDto project = discoverProject(params, getWikiContext(), false);
			if (project != null) {
				command.setProjectId(project.getId());
			}
		}
		if (command.isTable()) {
			List<Integer> testCaseCounts = chartDataCalculator.getTestCaseCounts(getUser(), command); 
			model.put("data", createTableFromCounts(testCaseCounts));
		}
		model.put("chartSupport", new ChartSupport(this, command, new RequirementCoverageRenderer()));

		return model;
	}

	@Override
	protected String getTemplateFilename() {
		return "requirementCoverage-plugin.vm";
	}

	private Map<Integer, Integer> createTableFromCounts(List<Integer> counts) {
		Map<Integer, Integer> result = new TreeMap<Integer, Integer>(new Comparator<Integer>() {
			public int compare(Integer o1, Integer o2) {
				return o2.intValue() - o1.intValue();
			}
		});
		for (Integer count: counts) {
			if (!result.containsKey(count)) {
				result.put(count, Integer.valueOf(1));
			} else {
				result.put(count, Integer.valueOf(result.get(count).intValue() + 1));
			}
		}

		return result;
	}

	@Override
	protected void validate(DataBinder binder, RequirementCoverageCommand command, Map params) throws NamedPluginException {
		if (command.getProjectId() == null &&
			CollectionUtils.isEmpty(command.getTrackerId())) {
			ProjectDto project = discoverProject(params, getWikiContext());
			if (project != null) {
				command.setProjectId(project.getId());
			} else {
				binder.getBindingResult().addError(new ObjectError("command", "Project ID is missing."));
			}
		}
		super.validate(binder, command, params);
	}
}
