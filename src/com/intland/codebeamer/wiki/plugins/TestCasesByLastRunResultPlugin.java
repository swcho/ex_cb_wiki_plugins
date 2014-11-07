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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.validation.DataBinder;
import org.springframework.validation.ObjectError;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.chart.renderer.TestCasesByLastRunResultRenderer;
import com.intland.codebeamer.controller.support.CommaStringToIntegerListPropertyEditor;
import com.intland.codebeamer.persistence.dao.impl.ChartDaoImpl;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.TrackerLayoutLabelDto;
import com.intland.codebeamer.persistence.dto.TrackerTypeDto;
import com.intland.codebeamer.wiki.plugins.ajax.AbstractAjaxRefreshingTestManagementPlugin;
import com.intland.codebeamer.wiki.plugins.base.AbstractTimeIntervalCommandWikiPlugin.TimeIntervalPropertyEditorRegistrar;
import com.intland.codebeamer.wiki.plugins.base.ChartSupport;
import com.intland.codebeamer.wiki.plugins.base.DefaultWebBindingInitializer;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;
import com.intland.codebeamer.wiki.plugins.command.TestCasesByLastRunResultCommand;

/**
 * @author <a href="mailto:akos.tajti@intland.com">Akos Tajti</a>
 *
 */
public class TestCasesByLastRunResultPlugin extends AbstractAjaxRefreshingTestManagementPlugin<TestCasesByLastRunResultCommand> {
	public TestCasesByLastRunResultPlugin() {
		setWebBindingInitializer(new TestCasesByLastRunResultBindingInitializer());
	}

	public static class TestCasesByLastRunResultPropertyEditorRegistrar extends TimeIntervalPropertyEditorRegistrar implements PropertyEditorRegistrar {
		public void registerCustomEditors(PropertyEditorRegistry registry) {
			super.registerCustomEditors(registry);
			registry.registerCustomEditor(List.class, "trackerId", new CommaStringToIntegerListPropertyEditor());
		}
	}

	public static class TestCasesByLastRunResultBindingInitializer extends DefaultWebBindingInitializer {
		public TestCasesByLastRunResultBindingInitializer() {
			addPropertyEditorRegistrar(new TestCasesByLastRunResultPropertyEditorRegistrar());
		}
	}

	@Override
	public TestCasesByLastRunResultCommand createCommand() throws PluginException {
		return new TestCasesByLastRunResultCommand();
	}

	@Override
	protected String getTemplateFilename() {
		return "testCasesByLastRunResult-plugin.vm";
	}

	@Override
	protected void validate(DataBinder binder, TestCasesByLastRunResultCommand command, Map params) throws NamedPluginException {
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

	@Override
	protected Map populateModelInternal(DataBinder binder, TestCasesByLastRunResultCommand command, Map params)
			throws PluginException {
		Map<String, Object> model = new HashMap<String, Object>();

		if (command.isTable()) {
			model.put("data", chartDataCalculator.getTestRunCountByResult(getUser(), command));
		}
		model.put("chartSupport", new ChartSupport(this, command, new TestCasesByLastRunResultRenderer()));

		if (command.getProjectId() != null) {
			ProjectDto project = projectManager.findById(getUser(), command.getProjectId());
			findConfigurations(model, project);
			findReleases(model, project);
		}

		return model;
	}

	@Override
	protected void findReleases(Map<String, Object> model, ProjectDto project) {
		TrackerDto testRunTracker = trackerManager.findDefaultTrackerOfType(getUser(), project.getId(), TrackerTypeDto.TESTRUN);
		TrackerDto releaseTracker = trackerManager.findDefaultRelatedTrackerForATracker(getUser(), testRunTracker, TrackerLayoutLabelDto.VERSION_LABEL_ID, TrackerTypeDto.TESTRUN, TrackerTypeDto.RELEASE);
		if (releaseTracker != null) {
			List<TrackerItemDto> releases = new ArrayList<TrackerItemDto>(trackerItemManager.findByTracker(getUser(), Collections.singletonList(releaseTracker.getId()), null));
			Collections.sort(releases, new ChartDaoImpl.NamedDtoByNameComparator());
			Map<Integer, String> releaseMap = createMap(releases);
			model.put("releases", releaseMap);
		}
	}
}
