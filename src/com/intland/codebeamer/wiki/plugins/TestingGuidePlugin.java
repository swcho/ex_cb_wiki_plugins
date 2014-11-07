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
import java.util.List;
import java.util.Map;

import org.apache.velocity.VelocityContext;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.google.common.base.Function;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.TrackerStatsDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.wiki.plugins.base.AbstractGuidePlugin;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;

/**
 * Plugin to guide users through Test Management and to provide quick links
 * when they are already familiar with the concepts.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public class TestingGuidePlugin extends AbstractGuidePlugin {
	@Override
	public void populateContext(final VelocityContext velocityContext, Map params) throws PluginException {
		final UserDto user = getUser();
		final ProjectDto project = discoverProject(params, getWikiContext(), true);
		if (project == null) {
			throw new NamedPluginException(this, "The projectId parameter must be specified when using outside of any project context");
		}
		boolean isLicensed = areRmAndTmLicensed();

		velocityContext.put("request", wikiContext.getHttpRequest());
		velocityContext.put("isLicensed", Boolean.valueOf(isLicensed));

		if(!isLicensed) {
			return;
		}

		// get trackers
		final TrackerDto requirementTracker = discoverTracker(user, project, "requirementId", "requirement tracker", new Function<Void,TrackerDto>() {
			public TrackerDto apply(Void any) {
				return trackerManager.findDefaultRequirementTracker(user, project);
			}
		});

		final TrackerDto testCaseTracker = discoverTracker(user, project, "testCaseId", "test case tracker", new Function<Void,TrackerDto>() {
			public TrackerDto apply(Void any) {
				return trackerManager.findDefaultTestPlanTracker(user, requirementTracker);
			}
		});

		final TrackerDto testConfigurationTracker = discoverTracker(user, project, "testConfigId", "test configuration tracker", new Function<Void,TrackerDto>() {
			public TrackerDto apply(Void any) {
				return trackerManager.findDefaultTestConfigurationTracker(user, requirementTracker);
			}
		});

		final TrackerDto releaseTracker = discoverTracker(user, project, "releaseId", "release tracker", new Function<Void,TrackerDto>() {
			public TrackerDto apply(Void any) {
				return trackerManager.findDefaultReleaseTracker(user, requirementTracker);
			}
		});

		final TrackerDto testSetTracker = discoverTracker(user, project, "testSetId", "test set tracker", new Function<Void,TrackerDto>() {
			public TrackerDto apply(Void any) {
				return trackerManager.findDefaultTestSetTracker(user, testCaseTracker);
			}
		});

		final TrackerDto testRunTracker = discoverTracker(user, project, "testRunId", "test run tracker", new Function<Void,TrackerDto>() {
			public TrackerDto apply(Void any) {
				return trackerManager.findDefaultTestRunTracker(user, testSetTracker);
			}
		});

		// get stats
		List<TrackerStatsDto> trackerStats = trackerManager.getTrackerItemStatsByTracker(user, Arrays.asList(requirementTracker.getId(), testCaseTracker.getId(), testConfigurationTracker.getId(), releaseTracker.getId(), testSetTracker.getId(), testRunTracker.getId()), false, false);

		// populate context
		velocityContext.put("requirementTracker", requirementTracker);
		velocityContext.put("requirementStats", findTrackerStatsForTracker(trackerStats, requirementTracker));

		velocityContext.put("testCaseTracker", testCaseTracker);
		velocityContext.put("testCaseStats", findTrackerStatsForTracker(trackerStats, testCaseTracker));

		velocityContext.put("testConfigurationTracker", testConfigurationTracker);
		velocityContext.put("testConfigurationStats", findTrackerStatsForTracker(trackerStats, testConfigurationTracker));

		velocityContext.put("releaseTracker", releaseTracker);
		velocityContext.put("releaseStats", findTrackerStatsForTracker(trackerStats, releaseTracker));

		velocityContext.put("testSetTracker", testSetTracker);
		velocityContext.put("testSetStats",  findTrackerStatsForTracker(trackerStats,testSetTracker));

		velocityContext.put("testRunTracker", testRunTracker);
		velocityContext.put("testRunStats", findTrackerStatsForTracker(trackerStats, testRunTracker));
	}

	@Override
	public String getTemplateFilename() {
		return "testing-guide-plugin.vm";
	}
}
