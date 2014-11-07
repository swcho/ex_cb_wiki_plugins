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
package com.intland.codebeamer.wiki.plugins.base;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.intland.codebeamer.license.LicenseCode;
import com.intland.codebeamer.manager.TrackerManager;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.TrackerStatsDto;
import com.intland.codebeamer.persistence.dto.UserDto;

/**
 * Mixin for the so-called "guide plugins".
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public abstract class AbstractGuidePlugin extends AutoWiringCodeBeamerPlugin {
	private final static Logger logger = Logger.getLogger(AbstractGuidePlugin.class);

	@Autowired
	protected LicenseCode licenseCode;

	@Autowired
	protected TrackerManager trackerManager;

	protected boolean areRmAndTmLicensed() {
		return licenseCode.isEnabled(LicenseCode.TM) && licenseCode.isEnabled(LicenseCode.RM);
	}

	protected TrackerDto discoverTracker(UserDto user, ProjectDto project, String parameterName, String trackerName, Function<Void, TrackerDto> discoveryFunction) throws NamedPluginException {
		TrackerDto tracker = null;
		if (parameterName != null && pluginParams.get(parameterName) != null) {
			Integer trackerId = null;
			Object value = pluginParams.get(parameterName);
			logger.debug("Using plugin parameter " + parameterName + "=" + value + " configuration parameters for " + trackerName + " tracker");
			try {
				trackerId = Integer.valueOf((String) value);
			} catch (Exception ex) {
				throw new NamedPluginException(this,  "Invalid tracker id:" + parameterName + "=" + pluginParams.get(parameterName), ex);
			}
			if (trackerId != null) {
				tracker = trackerManager.findById(user, trackerId);
				if (tracker == null) {
					return tracker;
				}
				throw new NamedPluginException(this,  "Invalid tracker id:" + parameterName + "=" + pluginParams.get(parameterName));
			}
		}

		logger.info("Discovering tracker because '" + parameterName +"' configuration is missing.");
		tracker = discoveryFunction.apply(null);
		if (tracker == null) {
			throw new NamedPluginException(this, "No " + trackerName +" found in " + project.getName());
		}
		logger.debug("Tracker found for '" + trackerName +"' is: " + tracker);
		return tracker;
	}

	protected TrackerStatsDto findTrackerStatsForTracker(List<TrackerStatsDto> trackerStats, TrackerDto tracker) {
		return Iterables.find(trackerStats, new FindTrackerStatsByTrackerIdPredicate(tracker));
	}

	private static class FindTrackerStatsByTrackerIdPredicate implements Predicate<TrackerStatsDto> {
		private Integer trackerId;

		public FindTrackerStatsByTrackerIdPredicate(TrackerDto tracker) {
			this.trackerId = tracker.getId();
		}

		public boolean apply(TrackerStatsDto trackerStats) {
			return trackerStats.getTracker().getId().equals(trackerId);
		}
	}
}
