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

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.manager.SubscriptionManager;
import com.intland.codebeamer.manager.TrackerManager;
import com.intland.codebeamer.manager.TrackerViewManager;
import com.intland.codebeamer.persistence.dao.impl.EntityCache;
import com.intland.codebeamer.persistence.dto.*;
import com.intland.codebeamer.persistence.util.PersistenceUtils;
import com.intland.codebeamer.ui.view.ColoredEntityIconProvider;
import com.intland.codebeamer.wiki.plugins.base.AutoWiringCodeBeamerPlugin;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.velocity.VelocityContext;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.text.Collator;
import java.util.*;

/**
 * Plugin to generate a list of trackers with their statistics.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id$
 */
public class TrackerListPlugin extends AutoWiringCodeBeamerPlugin {

	public static final String DETAILED_LAYOUT_PARAMETER_NAME = "detailed_layout";

	private static final String TRACKER_WORK_ITEMS_LABEL = "Issues";

	private static final String TRACKER_CONFIGURATION_ITEMS_LABEL = "CMDB";

	private static final String TRACKER_TYPE_PARAM = "items";

	// used by the new layout
	private final List<TrackerTypeDto> trackerOrder = Arrays.asList(
		// configuration items
		TrackerTypeDto.RELEASE,
		TrackerTypeDto.TESTCASE,
		TrackerTypeDto.TESTSET,
		TrackerTypeDto.TESTCONF,
		TrackerTypeDto.PLATFORM,
		TrackerTypeDto.COMPONENT,

		// work items
		TrackerTypeDto.REQUIREMENT,
		TrackerTypeDto.USERSTORY,
		TrackerTypeDto.CHANGE,
		TrackerTypeDto.TASK,
		TrackerTypeDto.BUG,
		TrackerTypeDto.TESTRUN,
		TrackerTypeDto.WORKLOG
	);

	@Autowired
	private TrackerManager trackerManager;
	@Autowired
	private SubscriptionManager subscriptionManager;
	@Autowired
	private ColoredEntityIconProvider coloredEntityIconProvider;

	private enum ItemsType {
		work,
		configuration,
		both
	}

	@Override
	public void populateContext(final VelocityContext velocityContext, Map params) throws PluginException {
		UserDto user = getUser();
		ProjectDto project = discoverProject(params, getWikiContext(), true);
		if (project == null) {
			throw new NamedPluginException(this, "The projectId parameter must be specified when using outside of any project context");
		}

		EntityCache cache = EntityCache.getInstance(user);
		boolean showAll = getBooleanParameter(params, "showAll");

		String type = getStringParameter(params, TRACKER_TYPE_PARAM, ItemsType.both.name());

		List<TrackerStatsDto> trackerStats = new ArrayList<TrackerStatsDto>();
		Map<String, List<TrackerStatsDto>> stats = new HashMap<String, List<TrackerStatsDto>>(4);

		// calculates aggregated statistics when project is NULL
		if ((type.equals(ItemsType.both.name()) || type.equals(ItemsType.work.name())) && cache.hasPermission(project, ProjectPermission.tracker_view, ProjectPermission.tracker_admin)) {
			List<TrackerStatsDto> workItemStats = findTrackerStats(user, project, null, Boolean.FALSE);
			if (workItemStats != null && workItemStats.size() > 0) {
				trackerStats.addAll(workItemStats);
				localizeAndSort((Locale) velocityContext.get("locale"), workItemStats);
			}
			stats.put(TRACKER_WORK_ITEMS_LABEL, workItemStats);
		}

		if ((type.equals(ItemsType.both.name()) || type.equals(ItemsType.configuration.name())) && cache.hasPermission(project, ProjectPermission.cmdb_view, ProjectPermission.cmdb_admin)) {
			List<TrackerStatsDto> configurationItemStats = findTrackerStats(user, project, null, Boolean.TRUE);
			if (configurationItemStats != null && configurationItemStats.size() > 0) {
				trackerStats.addAll(configurationItemStats);
				localizeAndSort((Locale) velocityContext.get("locale"), configurationItemStats);
			}
			stats.put(TRACKER_CONFIGURATION_ITEMS_LABEL, configurationItemStats);
		}

		Boolean hasHidden = Boolean.FALSE;
		for (TrackerStatsDto stat : trackerStats) {
			if (stat.getTracker().getVisible() != null && !stat.getTracker().getVisible().booleanValue()) {
				hasHidden = Boolean.TRUE;
				break;
			}
		}

		if (!renderDetailedLayout()) {
			stats = adjustStatsToNewLayout(stats);
		}

		HttpServletRequest request = wikiContext.getHttpRequest();
		request.setAttribute("trackerOverview", Boolean.TRUE);

		velocityContext.put("stats", stats);
		velocityContext.put("request", wikiContext.getHttpRequest());
		velocityContext.put("showAll", Boolean.valueOf(showAll));
		velocityContext.put("hasHidden", hasHidden);
		velocityContext.put("items", type);
		velocityContext.put("trackerStats", trackerStats);
		for(Map.Entry<String, Integer> entry : findTrackerViewConstants().entrySet()) {
			velocityContext.put(entry.getKey(), entry.getValue());
		}
		velocityContext.put("docViewTrackers", TrackerTypeDto.DOCUMENT_VIEW_TYPES);
		velocityContext.put("GroupType_TRACKER", EntityCache.TRACKER_TYPE);
		if (renderDetailedLayout()) {
			velocityContext.put("trackerSubscriptionsMap", findTrackerSubscriptions(user, trackerStats));
		}
		velocityContext.put("coloredEntityIconProvider", coloredEntityIconProvider);

		velocityContext.put("LABEL_WORK_ITEM", TRACKER_WORK_ITEMS_LABEL);
		velocityContext.put("LABEL_CONFIGURATION_ITEM", TRACKER_CONFIGURATION_ITEMS_LABEL);
	}

	@Override
	public String getTemplateFilename() {
		return renderDetailedLayout() ? "tracker-list-plugin.vm" : "tracker-list-plugin-new-layout.vm";
	}

	protected void localizeAndSort(final Locale locale, List<TrackerStatsDto> trackerStats) {
		if (trackerStats != null && trackerStats.size() > 0) {
			for (TrackerStatsDto stats : trackerStats) {
				TrackerDto tracker = stats.getTracker();
				if (tracker != null) {
					TrackerDto localized = (TrackerDto) tracker.clone();
					localized.setName(trackerManager.getName(locale, tracker));
					localized.setDescription(trackerManager.getDescription(locale, tracker));

					stats.setTracker(localized);
				}
			}

			if (trackerStats.size() > 1) {
				Collections.sort(trackerStats, new Comparator<TrackerStatsDto>() {
					private Collator collator = Collator.getInstance(locale);

					public int compare(TrackerStatsDto stats1, TrackerStatsDto stats2) {
						return collator.compare(stats1.getTracker().getName(), stats2.getTracker().getName());
					}
				});
			}
		}
	}

	/**
	 * Returns the trackers in the given project that are visible for the user.
	 * @param visible whether to show only visible (TRUE), hidden (FALSE) or all (null) trackers of the specified type in the project
	 */
	protected List<TrackerStatsDto> findTrackerStats(UserDto user, ProjectDto project, Boolean visible, Boolean cmdb) {
		boolean groupByProject = false;

		// get tracker stats
		List<TrackerStatsDto> data = trackerManager.getTrackerItemStatsByTracker(user, project, null, groupByProject, renderDetailedLayout(), cmdb, visible);
		List<TrackerStatsDto> result;
		if (groupByProject) {
			if (data != null && data.size() == 1) {
				result = PersistenceUtils.createList(data.get(0));
			} else {
				result = null;
			}
		} else {
			result = data;
		}

		return result;
	}

	/**
	 * Returns the user's subscription settings for the trackers passed.
	 */
	protected Map<Integer,SubscriptionDto> findTrackerSubscriptions(UserDto user, List<TrackerStatsDto> trackerStats) {
		List<TrackerDto> trackers = new ArrayList<TrackerDto>(trackerStats.size());
		for(TrackerStatsDto trackerStat : trackerStats) {
			trackers.add(trackerStat.getTracker());
		}
		return subscriptionManager.findSubscriptionsByUserAndEntities(user, EntityCache.TRACKER_TYPE, trackers);
	}

	/**
	 * Returns the constants to add to the model.
	 */
	protected Map<String, Integer> findTrackerViewConstants() {
		Map<String, Integer> model = new HashMap<String, Integer>(6);

		model.put("ROOT_VIEW", TrackerViewManager.ATV_OPEN_TOPLEVEL_ITEMS);
		model.put("DOCUMENT_VIEW", TrackerViewManager.ATV_DOCUMENT_VIEW);
		model.put("OPEN_VIEW", TrackerViewManager.ATV_OPEN);
		model.put("ALL_ITEMS_VIEW", TrackerViewManager.ATV_ALL_ITEMS);
		model.put("ASSIGNED_TO_ME_VIEW", TrackerViewManager.ATV_ASSIGNED_TO_ME);
		model.put("SUBMITTED_BY_ME_VIEW", TrackerViewManager.ATV_SUBMITTED_BY_ME);

		return model;
	}

	private Map<String, List<TrackerStatsDto>> adjustStatsToNewLayout(Map<String, List<TrackerStatsDto>> stats) {
		Map<String, List<TrackerStatsDto>> result = new LinkedHashMap<String, List<TrackerStatsDto>>();

		// put work and configuration items to the beginning in that order

		List<TrackerStatsDto> workItems = stats.remove(TRACKER_WORK_ITEMS_LABEL);
		if (workItems != null) {
			reorder(workItems);
			result.put(TRACKER_WORK_ITEMS_LABEL, workItems);
		}


		List<TrackerStatsDto> configurationItems = stats.remove(TRACKER_CONFIGURATION_ITEMS_LABEL);
		if (configurationItems != null) {
			reorder(configurationItems);
			result.put(TRACKER_CONFIGURATION_ITEMS_LABEL, configurationItems);
		}

		result.putAll(stats);

		return result;
	}

	private void reorder(List<TrackerStatsDto> trackerStats) {
		if (CollectionUtils.isNotEmpty(trackerStats)) {
			List<TrackerStatsDto> remaining = new ArrayList<TrackerStatsDto>(trackerStats);
			List<TrackerStatsDto> fixed = new ArrayList<TrackerStatsDto>();
			for (TrackerTypeDto type : trackerOrder) {
				List<TrackerStatsDto> found = findAllTrackersOfType(remaining, type);
				fixed.addAll(found);
				remaining.removeAll(found);
			}
			trackerStats.clear();
			trackerStats.addAll(fixed);
			trackerStats.addAll(remaining);
		}
	}

	private List<TrackerStatsDto> findAllTrackersOfType(List<TrackerStatsDto> trackerStats, TrackerTypeDto type) {
		List<TrackerStatsDto> result = new ArrayList<TrackerStatsDto>();
		for (TrackerStatsDto trackerStat : trackerStats) {
			if (trackerStat.getTracker().getType().equals(type)) {
				result.add(trackerStat);
			}
		}
		return result;
	}

	private boolean renderDetailedLayout() {
		HttpServletRequest request = wikiContext.getHttpRequest();
		return (request.getParameterMap().containsKey(DETAILED_LAYOUT_PARAMETER_NAME));
	}
}
