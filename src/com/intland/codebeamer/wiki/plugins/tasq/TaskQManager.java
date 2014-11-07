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
package com.intland.codebeamer.wiki.plugins.tasq;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.intland.codebeamer.manager.MultiTrackerItemDtoComparator;
import com.intland.codebeamer.persistence.dao.TrackerItemDao;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto.Flag;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.base.IdentifiableDto;
import com.intland.codebeamer.persistence.util.PersistenceUtils;
import com.intland.codebeamer.utils.MultiValue;

/**
 * Implementation for {@link ITaskQManager}
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class TaskQManager implements ITaskQManager{

	private final static Logger logger = Logger.getLogger(TaskQManager.class);

	@Autowired
	TrackerItemDao trackerItemDao;
	private Cache cache;

	// sort by priority desc, estimated time desc, summary desc
	private static Ordering<TrackerItemDto> sorter =
				Ordering.from(MultiTrackerItemDtoComparator.compareByPriority)		// order by priority desc
				.compound(MultiTrackerItemDtoComparator.compareByEstimatedHoursDesc)	// estimated hours desc
				.compound(MultiTrackerItemDtoComparator.compareByNameIgnoreCase)	// summary/name
				.compound(new IdentifiableDto.IdComparator(true));	// id descending

	public MultiValue<List<TrackerItemDto>, Integer> findIssues(UserDto user, UserDto targetUser, TaskQCommand command) {
		Stopwatch sw = new Stopwatch();
		sw.start();
		List<TrackerItemDto> issues = findIssuesAssignedToUser(user, targetUser);
		sw.stop();
		if (logger.isDebugEnabled()) {
			logger.debug("findIssuesAssignedToUser(" + targetUser +") call took " + sw.elapsed(TimeUnit.MILLISECONDS) + "ms");
		}

		Predicate<TrackerItemDto> filters = Predicates.and(
						filterIssuesByStatusNames(issues, command.getStatus()),
						filterIssuesByVersion(issues, command.getVersionId()),
						filterIssuesByMaxAge(issues, command.getMaxAge())
					);
		issues = Lists.newArrayList(Collections2.filter(issues, filters));

		// sort issues
		Collections.sort(issues, sorter);

		// and limit the number of issues
		int totalMatchingIssues = issues.size();
		if (command.getMax() != null && command.getMax().intValue() >-1) {
			issues = issues.subList(0, Math.min(issues.size(), command.getMax().intValue()));
		}

		return new MultiValue<List<TrackerItemDto>, Integer>(issues, Integer.valueOf(totalMatchingIssues));
	}

	// TODO: it would be nice to use declarative caching here
	private List<TrackerItemDto> findIssuesAssignedToUser(UserDto user, UserDto targetUser) {
		final String cacheKey = user.getId() +"-" + targetUser.getId();
		if (cache != null) {
			Element el = cache.get(cacheKey);
			if (el != null) {
				return (List<TrackerItemDto>) el.getObjectValue();
			}
		}

		final List<Integer> fieldIds = TrackerItemDao.ASSIGNED_TO; // find issues assigned to directly the user
		final boolean onlyDirectUserItems = true;  // only directly
		final Boolean cmdb = Boolean.FALSE;
		final Set<Flag> flags = null; // EnumSet.of(Flag.Deleted, Flag.Resolved, Flag.Closed);

		Collection<?> trackers = null;
		Collection<?> projects = null;

		List<TrackerItemDto> issues = trackerItemDao.findByUser(user, fieldIds, targetUser, onlyDirectUserItems, projects, trackers, cmdb, flags, null, TrackerItemDao.TF_NOFILTER);

		if (cache != null) {
			cache.put(new Element(cacheKey, issues));
		}
		return issues;
	}

	private Predicate<TrackerItemDto> filterIssuesByStatusNames(List<TrackerItemDto> issues, final List<String> status) {
		return new Predicate<TrackerItemDto>() {
			public boolean apply(TrackerItemDto issue) {
				if (issue.getStatus() == null) {
					return status.contains("--");
				}
				String statusName = issue.getStatus().getName();
				if (statusName == null) {
					return status.contains("--");
				}
				for (String s:status) {
					if (s.equalsIgnoreCase(statusName)) {
						return true;
					}
				}
				return false;
			}
		};
	}

	private Predicate<TrackerItemDto> filterIssuesByMaxAge(List<TrackerItemDto> issues, Integer maxAge) {
		if (maxAge == null) {
			return Predicates.alwaysTrue();
		}
		Calendar agingCalendar = Calendar.getInstance();
		agingCalendar.add(Calendar.DAY_OF_YEAR, - maxAge.intValue());
		final Date agingDate = agingCalendar.getTime();

		return new Predicate<TrackerItemDto>() {

			public boolean apply(TrackerItemDto issue) {
				return issue.getModifiedAt() == null || issue.getModifiedAt().after(agingDate);
			}
		};
	}

	private Predicate<TrackerItemDto> filterIssuesByVersion(List<TrackerItemDto> issues, final List<Integer> versionIdFilter) {
		if (versionIdFilter == null || versionIdFilter.isEmpty()) {
			return Predicates.alwaysTrue();
		}
		return new Predicate<TrackerItemDto>() {
			public boolean apply(TrackerItemDto issue) {
				List<Integer> versionIdsOfIssue = PersistenceUtils.grabIds(issue.getVersions());
				if (versionIdsOfIssue != null) {
					// check if any of the versionIds are matching with the filters
					if (Iterables.any(versionIdsOfIssue, Predicates.in(versionIdFilter))) {
						return true;
					}
				}
				return false;
			}
		};
	}

	public void setCache(Cache cache) {
		this.cache = cache;
	}

}
