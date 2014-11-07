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
package com.intland.codebeamer.wiki.plugins.releaseactivitytrends;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.intland.codebeamer.persistence.dao.TrackerDao;
import com.intland.codebeamer.persistence.dao.TrackerItemDao;
import com.intland.codebeamer.persistence.dao.TrackerItemStatisticsDao;
import com.intland.codebeamer.persistence.dao.impl.EntityCache;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.TrackerItemStatsDto;
import com.intland.codebeamer.persistence.dto.TrackerLayoutLabelDto;
import com.intland.codebeamer.persistence.dto.TrackerTypeDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.base.DescribeableDto;
import com.intland.codebeamer.persistence.util.Criteria;
import com.intland.codebeamer.persistence.util.PersistenceUtils;
import com.intland.codebeamer.persistence.util.TrackerItemChoiceCriterion;
import com.intland.codebeamer.persistence.util.TrackerItemFieldHandler;
import com.intland.codebeamer.persistence.util.TrackerItemRestrictions;
import com.intland.codebeamer.persistence.util.VersionComparatorByDate;

/**
 * Manager class for plugin to display the project related tracker item counts grouped by releases.
 *
 * @author <a href="mailto:levente.cseko@intland.com">Levente Cseko</a>
 * @version $Id$
 */
@Component
public class ReleaseActivityTrendsManager {

	public final static String DEFAULT_TRACKER_ITEM_STATUS = "Unresolved";
	public final static String DEFAULT_RELEASE_STATUS = "Unreleased";
	public final static Boolean DEFAULT_SHOW_UNSCHEDULED_ITEMS = Boolean.FALSE;
	public final static Boolean DEFAULT_SHOW_BURN_DOWN_CHART = Boolean.TRUE;
	public final static Boolean DEFAULT_SHOW_DROPDOWN_FILTERS = Boolean.TRUE;

	public final static TrackerLayoutLabelDto RELEASE_FIELD = new TrackerLayoutLabelDto(TrackerLayoutLabelDto.VERSION_LABEL_ID);

	public final static Map<Integer,List<Integer>> VERSION_ITEM_STATS = Collections.singletonMap(TrackerItemFieldHandler.VERSION_LABEL_ID, (List<Integer>)null);

	public final static List<String> RELEASE_STATUSES = Collections.unmodifiableList(Arrays.asList("Unreleased", "Released", "All"));

	public final static List<String> TRACKER_ITEM_STATUSES = Collections.unmodifiableList(Arrays.asList("Unresolved", "Resolved", "All"));

	public final static List<TrackerTypeDto> TRACKER_ITEM_TYPES = Collections.unmodifiableList(Arrays.asList(
			TrackerTypeDto.REQUIREMENT,
			TrackerTypeDto.USERSTORY,
			TrackerTypeDto.CHANGE,
			TrackerTypeDto.TASK,
			TrackerTypeDto.BUG,
			TrackerTypeDto.ISSUE
	));

	@Autowired
	private TrackerDao trackerDao;
	@Autowired
	private TrackerItemDao trackerItemDao;
	@Autowired
	private TrackerItemStatisticsDao trackerItemStatisticsDao;


	public List<ReleaseParameters> getReleaseParameters(UserDto user, Integer projectId, String releaseStatus) {
		List<ReleaseParameters> result = Collections.emptyList();

		List<TrackerItemDto> releases = getReleases(user, projectId);
		if (releases != null && releases.size() > 0) {
			Boolean 						 released     = "Released".equalsIgnoreCase(releaseStatus) ? Boolean.TRUE : "Unreleased".equalsIgnoreCase(releaseStatus) ? Boolean.FALSE : null;
			Map<Integer,TrackerItemStatsDto> releaseStats = trackerItemStatisticsDao.getReferringIssueStats(user, getReleased(releases, released), VERSION_ITEM_STATS, TRACKER_ITEM_TYPES, null, null, true);

			result = buildReleaseParameters(new ArrayList<ReleaseParameters>(releases.size()), user, released, getTopLevel(releases), releaseStats, 0);
		}
		return result;
	}

	public Map<String, Integer> getUnscheduledItems(UserDto user, Integer projectId) {
		int allItems = 0;
		int resolved = 0;

		if (projectId != null) {
			Set<Integer> trackerIds = PersistenceUtils.grabIdSet(trackerDao.findByProjectAndTypes(user, projectId, TRACKER_ITEM_TYPES));
			if (trackerIds != null && trackerIds.size() > 0) {
				Criteria criteria = new Criteria();
				criteria.add(new TrackerItemChoiceCriterion(user, false).addLabelReferenceValue(RELEASE_FIELD, EntityCache.TRACKER_ITEM_TYPE, null));

				if ((allItems = trackerItemDao.countByCriteria(user, trackerIds, criteria, null)) > 0) {
					criteria.add(TrackerItemRestrictions.getFlagsCriterion(EnumSet.of(TrackerItemDto.Flag.Resolved, TrackerItemDto.Flag.Closed), true, false));

					resolved = trackerItemDao.countByCriteria(user, trackerIds, criteria, null);
				}
			}
		}

		Map<String, Integer> numberOfIssues = new HashMap<String, Integer>(4);
		numberOfIssues.put("All", 		 Integer.valueOf(allItems));
		numberOfIssues.put("Resolved", 	 Integer.valueOf(resolved));
		numberOfIssues.put("Unresolved", Integer.valueOf(allItems - resolved));

		return numberOfIssues;
	}

	private List<TrackerItemDto> getReleases(UserDto user, Integer projectId) {
		List<TrackerItemDto> releases = Collections.emptyList();
		if (projectId != null) {
			List<TrackerDto> trackers = trackerDao.findByProjectAndTypes(user, projectId, Collections.singletonList(TrackerTypeDto.RELEASE));
			if (trackers != null && trackers.size() > 0) {
				releases = trackerItemDao.findByTrackers(user, trackers, null);
			}
		}
		return releases;
	}

	private List<TrackerItemDto> getTopLevel(List<TrackerItemDto> releases) {
		List<TrackerItemDto> result = Collections.emptyList();
		if (releases != null && releases.size() > 0) {
			result = new ArrayList<TrackerItemDto>(releases.size());

			for (TrackerItemDto release : releases) {
				if (release != null && release.getParent() == null) {
					result.add(release);
				}
			}

			if (result.size() > 1) {
				Collections.sort(result, new VersionComparatorByDate());
			}
		}
		return result;
	}

	private List<TrackerItemDto> getReleased(List<TrackerItemDto> releases, Boolean released) {
		List<TrackerItemDto> result = releases;

		if (releases != null && releases.size() > 0 && released != null) {
			result = new ArrayList<TrackerItemDto>(releases.size());

			for (TrackerItemDto release : releases) {
				if (release != null && release.isResolvedOrClosed() == released.booleanValue()) {
					result.add(release);
				}
			}
		}

		return result;
	}

	private List<ReleaseParameters> buildReleaseParameters(List<ReleaseParameters> releaseParameters, UserDto user, Boolean released, List<TrackerItemDto> releases, Map<Integer,TrackerItemStatsDto> releaseStats, int level) {
		if (releases != null && releases.size() > 0) {
			for (TrackerItemDto release : releases) {
				if (release != null) {
					if (released == null || release.isResolvedOrClosed() == released.booleanValue()) {
						releaseParameters.add(new ReleaseParameters(user, release, releaseStats.get(release.getId()), level));
					}

					List<TrackerItemDto> children = trackerItemDao.findById(user, release.getChildren());
					if (children != null && children.size() > 0) {
						buildReleaseParameters(releaseParameters, user, released, children, releaseStats, level + 1);
					}
				}
			}
		}
		return releaseParameters;
	}


	public class ReleaseParameters extends DescribeableDto {
		private boolean released;
		private Date 	releaseDate;
		private Date 	actualReleaseDate;
		private int 	daysLeft;
		private String 	url;
		private Integer level;
		private Map<String,Integer> numberOfIssues;

		public ReleaseParameters(UserDto user, TrackerItemDto release, TrackerItemStatsDto itemStats, int level) {
			super(release);
			released = release.isResolvedOrClosed();
			releaseDate = release.getEndDate();
			actualReleaseDate = convertStringToDate(user, release.getCustomField(0));
			daysLeft = countDaysLeft();
			url = release.getUrlLink();
			this.level = Integer.valueOf(level);

			numberOfIssues = new HashMap<String, Integer>(4);
			numberOfIssues.put("All", 		 Integer.valueOf(itemStats != null ? itemStats.getAllItems()					  : 0));
			numberOfIssues.put("Resolved",   Integer.valueOf(itemStats != null ? itemStats.getResolvedAndClosedTrackerItems() : 0));
			numberOfIssues.put("Unresolved", Integer.valueOf(itemStats != null ? itemStats.getOpenItems() 					  : 0));
		}

		private Date convertStringToDate(UserDto user, String str) {
			Date date = null;
			try {
				date = (Date) TrackerLayoutLabelDto.decodeString(user, str, TrackerLayoutLabelDto.DATE);
			} catch (Exception ex) {
				// Ignoring all exceptions.
			}
			return date;
		}

		private int countDaysLeft() {
			return 1 + Days.daysBetween(new DateTime(), new DateTime(releaseDate)).getDays();
		}


		public boolean isReleased() {
			return released;
		}

		public Date getReleaseDate() {
			return releaseDate;
		}

		public Date getActualReleaseDate() {
			return actualReleaseDate;
		}

		public int getDaysLeft() {
			return daysLeft;
		}

		public Map<String,Integer> getNumberOfIssues() {
			return numberOfIssues;
		}

		public String getUrl() {
			return url;
		}

		public Integer getLevel() {
			return level;
		}

	}

}
