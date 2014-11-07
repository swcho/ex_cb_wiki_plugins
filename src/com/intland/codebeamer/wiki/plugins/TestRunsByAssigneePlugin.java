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

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.chart.renderer.AbstractTestManagementBarChartRenderer;
import com.intland.codebeamer.chart.renderer.TestRunsByAssigneeRenderer;
import com.intland.codebeamer.controller.support.CommaStringToIntegerListPropertyEditor;
import com.intland.codebeamer.wiki.plugins.ajax.AbstractAjaxRefreshingTestManagementPlugin;
import com.intland.codebeamer.wiki.plugins.base.AbstractTimeIntervalCommandWikiPlugin.TimeIntervalPropertyEditorRegistrar;
import com.intland.codebeamer.wiki.plugins.base.ChartSupport;
import com.intland.codebeamer.wiki.plugins.base.DefaultWebBindingInitializer;
import com.intland.codebeamer.wiki.plugins.command.TestRunsByAssigneeCommand;

/**
 * @author <a href="mailto:akos.tajti@intland.com">Akos Tajti</a>
 *
 */
public class TestRunsByAssigneePlugin extends AbstractAjaxRefreshingTestManagementPlugin<TestRunsByAssigneeCommand> {
	public TestRunsByAssigneePlugin() {
		setWebBindingInitializer(new TestRunsByAssigneeBindingInitializer());
	}

	public static class TestRunsByAssigneePropertyEditorRegistrar extends TimeIntervalPropertyEditorRegistrar implements PropertyEditorRegistrar {
		public void registerCustomEditors(PropertyEditorRegistry registry) {
			super.registerCustomEditors(registry);
			registry.registerCustomEditor(List.class, "trackerId", new CommaStringToIntegerListPropertyEditor());
		}
	}

	public static class TestRunsByAssigneeBindingInitializer extends DefaultWebBindingInitializer {
		public TestRunsByAssigneeBindingInitializer() {
			addPropertyEditorRegistrar(new TestRunsByAssigneePropertyEditorRegistrar());
		}
	}
	@Override
	public TestRunsByAssigneeCommand createCommand() throws PluginException {
		return new TestRunsByAssigneeCommand();
	}

	@Override
	protected Map populateModelInternal(DataBinder binder, TestRunsByAssigneeCommand command, Map params) throws PluginException {
		Map<String, Object> model = new HashMap<String, Object>();

		if (command.isTable()) {
			model.put("data", chartDataCalculator.getTestRunsByAssignee(getUser(), command));
		}
		model.put("chartSupport", new ChartSupport(this, command, new TestRunsByAssigneeRenderer()));
		model.put("defaultOrder", Arrays.asList(AbstractTestManagementBarChartRenderer.defaultResultsInOrder));

		return model;
	}

	@Override
	protected String getTemplateFilename() {
		return "testRunsByAssignee-plugin.vm";
	}

}
