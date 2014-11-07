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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.velocity.VelocityContext;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.manager.TrackerManager;
import com.intland.codebeamer.persistence.dao.TrackerItemDao;
import com.intland.codebeamer.persistence.dao.impl.EntityCache;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.TrackerItemHistoryEventDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.util.PersistenceUtils;
import com.intland.codebeamer.wiki.plugins.base.AutoWiringCodeBeamerPlugin;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;

/**
 * Plugin to display a list of recently activated tracker items.
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id: aron.gombas 2009-01-05 16:58 +0000 19808:a84808eab6ec  $
 */
public class RecentTrackerItemsPlugin extends AutoWiringCodeBeamerPlugin {

	private TrackerManager trackerManager;
	private TrackerItemDao trackerItemDao;

	protected static final String UNSPECIFIED_TRACKER_PROJECT = "Either the projectId or trackerId parameter must be specified when using outside of any project context";

	private static final String RENDER_TEMPLATE = "recenttrackeritems-plugin.vm";

	private static final String INCLUDE_ASSIGNED_TO = "assignedTo";
	private static final String INCLUDE_SUBMITTED_BY = "submittedBy";
	private static final String INCLUDE_SUPERVISED_BY = "supervisedBy";
	private static final String ONLY_DIRECT_USER_ITEMS = "directlyToUser";
	private static final String INCLUDE_ALL = "all";

	private static final Map<Integer, String> actions = new HashMap<Integer, String>(7);

	static {
		actions.put(Integer.valueOf(TrackerItemHistoryEventDto.ITEM_CREATE), "created");
		actions.put(Integer.valueOf(TrackerItemHistoryEventDto.ITEM_COMMENT), "commented");
		actions.put(Integer.valueOf(TrackerItemHistoryEventDto.ITEM_HISTORY), "modified");
		actions.put(Integer.valueOf(TrackerItemHistoryEventDto.ITEM_ATTACHMENT), "attachment");
		actions.put(Integer.valueOf(TrackerItemHistoryEventDto.ITEM_SCM_COMMIT), "commit");
		actions.put(Integer.valueOf(TrackerItemHistoryEventDto.ITEM_ASSOCIATION), "associated");
		actions.put(Integer.valueOf(TrackerItemHistoryEventDto.ITEM_CLOSE), "closed");
	}

	@Override
	public String getTemplateFilename() {
		return RENDER_TEMPLATE;
	}

	@Override
	public void populateContext(VelocityContext velocityContext, Map params) throws PluginException {
		UserDto user = getUser();

		ProjectDto project = discoverProject(params, getWikiContext(), true);

		int trackerId = NumberUtils.toInt(getParameter(params, "trackerId"), 0);

		if (project == null && trackerId <= 0) {
			throw new NamedPluginException(this, UNSPECIFIED_TRACKER_PROJECT);
		}

		// parse params
		int max = NumberUtils.toInt(getParameter(params, "max"), 5);
		int sinceDays = NumberUtils.toInt(getParameter(params, "since"), 7);
		Date since = PersistenceUtils.getToday(0 - Math.abs(sinceDays));

		// Fetch the parameter for inclusion; default = none
		String include = getParameter(params, "include");
		boolean includeAssignedTo = false;
		boolean includeSubmittedBy = false;
		boolean includeSupervisedBy = false;
		boolean onlyDirectUserItems = false;

		if (StringUtils.isNotBlank(include)) {
			for (StringTokenizer parser = new StringTokenizer(include, ",; "); parser.hasMoreTokens(); ) {
				String token = parser.nextToken().trim();
				if (token.equalsIgnoreCase(INCLUDE_ASSIGNED_TO)) {
					includeAssignedTo = true;
				} else if (token.equalsIgnoreCase(INCLUDE_SUBMITTED_BY)) {
					includeSubmittedBy = true;
				} else if (token.equalsIgnoreCase(INCLUDE_SUPERVISED_BY)) {
					includeSupervisedBy = true;
				} else if (token.equalsIgnoreCase(ONLY_DIRECT_USER_ITEMS)) {
					onlyDirectUserItems = true;
				} else if (token.equalsIgnoreCase(INCLUDE_ALL)){
					includeAssignedTo = true;
					includeSubmittedBy = true;
					includeSupervisedBy = true;
					include = INCLUDE_ALL;
				}
			}
		}

		List<TrackerItemHistoryEventDto> recentEvents = findRecentTrackerItemHistory(user, project, max, since, trackerId, includeAssignedTo, includeSubmittedBy, includeSupervisedBy, onlyDirectUserItems);

		// set up Velocity context
		velocityContext.put("isCurrentProject", isEnclosingProject());
		velocityContext.put("include", include);
		velocityContext.put("project", project);
		velocityContext.put("list", recentEvents);
		velocityContext.put("actions", actions);
		velocityContext.put("contextPath", getApplicationContextPath(getWikiContext()));
	}

	/**
	 * Find the recent events with the given filters.
	 */
	protected List<TrackerItemHistoryEventDto> findRecentTrackerItemHistory(UserDto user, ProjectDto project, int max, Date since, int trackerId, boolean includeAssignedTo, boolean includeSubmittedBy, boolean includeSupervisedBy, boolean onlyDirectUserItems) {
		List<TrackerDto> trackers = null;
		List<TrackerItemDto> trackerItems = Collections.emptyList();
		List<TrackerItemHistoryEventDto> recentEvents = Collections.emptyList();

		if (trackerId > 0) {
			TrackerDto tracker = trackerManager.findById(user, new Integer(trackerId));
			if (tracker != null) {
				trackers = PersistenceUtils.createSingleItemList(tracker);
			}
		}

		// find all trackers in the current project
		if (trackers == null && project != null) {
			trackers = trackerManager.findByProject(user, project);
		}

		if (includeAssignedTo || includeSubmittedBy || includeSupervisedBy) {
			List<Integer> fieldIds = new ArrayList<Integer>(3);
			if (includeAssignedTo) {
				fieldIds.addAll(TrackerItemDao.ASSIGNED_TO);
			}
			if (includeSubmittedBy) {
				fieldIds.addAll(TrackerItemDao.SUBMITTED_BY);
			}
			if (includeSupervisedBy) {
				fieldIds.addAll(TrackerItemDao.SUPERVISED_BY);
			}
			// Find the max most recently modified tracker items assigned to or submitted by the specified user
			trackerItems = trackerItemDao.findRecentUserItems(user, fieldIds, user, onlyDirectUserItems, null, trackers, null, null, null, max);
		} else {
			// Find the max most recently modified items in the specified trackers
			trackerItems = trackerItemDao.findRecentTrackerItems(user, trackers, max, since);
		}

		if (!trackerItems.isEmpty()) {
			EntityCache cache = EntityCache.getInstance(user);
			Map<Integer, TrackerItemDto> items = PersistenceUtils.createLookupMap(trackerItems);

			recentEvents = trackerItemDao.getRecentItemHistoryEventsList(trackerItems);
			for (TrackerItemHistoryEventDto dto : recentEvents) {
				dto.setSubmitter(cache.getUser(dto.getSubmitterId()));
				dto.setTrackerItem(items.get(dto.getTaskId()));
			}
		}
		return recentEvents;
	}

	public void setTrackerManager(TrackerManager trackerManager) {
		this.trackerManager = trackerManager;
	}

	public void setTrackerItemDao(TrackerItemDao trackerItemDao) {
		this.trackerItemDao = trackerItemDao;
	}
}
