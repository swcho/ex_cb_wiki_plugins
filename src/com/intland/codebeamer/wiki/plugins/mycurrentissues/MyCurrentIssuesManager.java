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
 *
 */
package com.intland.codebeamer.wiki.plugins.mycurrentissues;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.springframework.beans.factory.annotation.Autowired;

import com.intland.codebeamer.manager.MultiTrackerItemDtoComparator;
import com.intland.codebeamer.manager.TrackerItemManager;
import com.intland.codebeamer.persistence.dao.EntityLabelDao;
import com.intland.codebeamer.persistence.dao.TrackerItemDao;
import com.intland.codebeamer.persistence.dao.impl.EntityCache;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto.Flag;
import com.intland.codebeamer.persistence.dto.base.NamedDto;
import com.intland.codebeamer.persistence.util.PersistenceUtils;

/**
 * @author <a href="mailto:klaus.mehling@intland.com">Klaus Mehling</a>
 * @version $Id: $
 */
public class MyCurrentIssuesManager implements IMyCurrentIssuesManager {
	@Autowired
	private EntityLabelDao entityLabelDao;
	@Autowired
	private TrackerItemDao trackerItemDao;
	@Autowired
	private TrackerItemManager trackerItemManager;

	/**
	 * Find the issues assigned to the user, and not in the (closed, resolved) status.
	 * @param user whose issues to find
	 * @param command contains optional selection parameters.
	 * @return The issues ordered by priority desc, lastModifiedAt desc
     */
	public List<TrackerItemDto> findIssues(final UserDto user, MyCurrentIssuesCommand command) {
		Collection<Integer> trackers = CollectionUtils.isEmpty(command.getTrackerId()) ? null : command.getTrackerId();
		Collection<Integer> projects = CollectionUtils.isEmpty(command.getProjectId()) ? null : command.getProjectId();
		Collection<Integer> releaseIds = command.getReleaseId();

		Set<TrackerItemDto> result = new HashSet<TrackerItemDto>();

		Predicate notResolvedAndAssignedIssue = new Predicate() {
			public boolean evaluate(Object object) {
				TrackerItemDto issue = (TrackerItemDto) object;
				if (!issue.isResolvedOrClosed()) {
					List<? extends NamedDto> assignees = issue.getAssignedTo();
					return assignees != null && assignees.contains(user);
				}
				return false;
			}
		};

		if (CollectionUtils.isNotEmpty(releaseIds)) {
			for (Integer releaseId : releaseIds) {
				// TODO: the database should be used, however this doesn't cost too much CPU.
				List<TrackerItemDto> releaseIssues = trackerItemManager.findTrackerItemsByRelease(user, releaseId, true);
				// filter only NOT resolved/closed issues assigned to the user.
				CollectionUtils.filter(releaseIssues, notResolvedAndAssignedIssue);
				result.addAll(releaseIssues);
			}
		} else {
			boolean searchedForTags = trackers == null && projects == null && CollectionUtils.isNotEmpty(command.getTag());
			if (searchedForTags) {
				Map<Integer,Set<Integer>> taggedObjects = entityLabelDao.getTaggedEntityIds(user, command.getTag(), Arrays.asList(EntityCache.PROJECT_TYPE, EntityCache.TRACKER_TYPE, EntityCache.TRACKER_ITEM_TYPE));

				trackers = CollectionUtils.isEmpty(taggedObjects.get(EntityCache.TRACKER_TYPE)) ? null : taggedObjects.get(EntityCache.TRACKER_TYPE);
				projects = CollectionUtils.isEmpty(taggedObjects.get(EntityCache.PROJECT_TYPE)) ? null : taggedObjects.get(EntityCache.PROJECT_TYPE);
				Collection<Integer> taggedIssuesIds = CollectionUtils.isEmpty(taggedObjects.get(EntityCache.TRACKER_ITEM_TYPE)) ? null : taggedObjects.get(EntityCache.TRACKER_ITEM_TYPE);
				if (CollectionUtils.isNotEmpty(taggedIssuesIds)) {
					List<TrackerItemDto> taggedIusses = trackerItemDao.findById(user, taggedIssuesIds);
					CollectionUtils.filter(taggedIusses, notResolvedAndAssignedIssue);

					result.addAll(taggedIusses);
				}
			} else if (CollectionUtils.isNotEmpty(trackers)) {
				trackers = PersistenceUtils.grabIds(EntityCache.getInstance(user).get(TrackerDto.class, EntityCache.TRACKER_TYPE, trackers));
			}

			if (!searchedForTags || (CollectionUtils.isNotEmpty(trackers) || CollectionUtils.isNotEmpty(projects))) {
				final List<Integer> fieldIds = TrackerItemDao.ASSIGNED_TO; // Should be a command property
				final boolean onlyDirectUserItems = true;  // (According to Zsolt at sprint meeting of Jun-16-2010)
				final Boolean cmdb = Boolean.FALSE;		 // Should be a command property
				final Set<Flag> flags = EnumSet.of(Flag.Deleted, Flag.Resolved, Flag.Closed);

				List<TrackerItemDto> issues = trackerItemDao.findByUser(user, fieldIds, user, onlyDirectUserItems, trackers != null ? null : projects, trackers, cmdb, flags, null, TrackerItemDao.TF_NOFILTER);
				result.addAll(issues);
			}
		}

		List<TrackerItemDto> foundIssues = new ArrayList<TrackerItemDto>(result);

		Collections.sort(foundIssues, new MultiTrackerItemDtoComparator(MultiTrackerItemDtoComparator.SORT_BY_RESOLVED_PRIORITY));	// TODO inefficient: should be in the DB

		return foundIssues;
	}
}
