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
import java.util.Map;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.validation.DataBinder;
import org.springframework.validation.ObjectError;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.chart.renderer.AbstractTestManagementBarChartRenderer;
import com.intland.codebeamer.chart.renderer.TestRunsByConfigurationRenderer;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.wiki.plugins.ajax.AbstractAjaxRefreshingTestManagementPlugin;
import com.intland.codebeamer.wiki.plugins.base.AbstractTimeIntervalCommandWikiPlugin.TimeIntervalPropertyEditorRegistrar;
import com.intland.codebeamer.wiki.plugins.base.ChartSupport;
import com.intland.codebeamer.wiki.plugins.base.DefaultWebBindingInitializer;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;
import com.intland.codebeamer.wiki.plugins.command.TestRunsByConfigurationCommand;

/**
 * @author <a href="mailto:akos.tajti@intland.com">Akos Tajti</a>
 *
 */
public class TestRunsByConfigurationPlugin extends AbstractAjaxRefreshingTestManagementPlugin<TestRunsByConfigurationCommand> {

	public TestRunsByConfigurationPlugin() {
		setWebBindingInitializer(new TestRunsByConfigurationBindingInitializer());
	}

	public static class TestRunsByConfigurationPropertyEditorRegistrar extends TimeIntervalPropertyEditorRegistrar implements PropertyEditorRegistrar {
		public void registerCustomEditors(PropertyEditorRegistry registry) {
			super.registerCustomEditors(registry);
		}
	}

	public static class TestRunsByConfigurationBindingInitializer extends DefaultWebBindingInitializer {
		public TestRunsByConfigurationBindingInitializer() {
			addPropertyEditorRegistrar(new TestRunsByConfigurationPropertyEditorRegistrar());
		}
	}

	@Override
	public TestRunsByConfigurationCommand createCommand() throws PluginException {
		return new TestRunsByConfigurationCommand();
	}

	@Override
	protected Map populateModelInternal(DataBinder binder, TestRunsByConfigurationCommand command, Map params)
			throws PluginException {
		Map<String, Object> model = new HashMap<String, Object>();

		if (command.isTable()) {
			model.put("data", chartDataCalculator.getTestRunsByConfiguration(getUser(), command));
		}
		model.put("chartSupport", new ChartSupport(this, command, new TestRunsByConfigurationRenderer()));
		model.put("defaultOrder", Arrays.asList(AbstractTestManagementBarChartRenderer.defaultResultsInOrder));

		return model;
	}

	@Override
	protected String getTemplateFilename() {
		return "testRunsByConfiguration-plugin.vm";
	}

	@Override
	protected void validate(DataBinder binder, TestRunsByConfigurationCommand command, Map params) throws NamedPluginException {
		if (command.getProjectId() == null) {
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
