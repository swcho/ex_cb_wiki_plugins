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
package com.intland.codebeamer.wiki.plugins.recentactivities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Predicate;
import com.intland.codebeamer.chart.data.DateIdentifiableMap;
import com.intland.codebeamer.manager.ProjectManager;
import com.intland.codebeamer.manager.TrackerItemManager;
import com.intland.codebeamer.persistence.dao.ActivityLogDao;
import com.intland.codebeamer.persistence.dao.EntityLabelDao;
import com.intland.codebeamer.persistence.dao.impl.EntityCache;
import com.intland.codebeamer.persistence.dto.ActivityLogEntryDto;
import com.intland.codebeamer.persistence.dto.ArtifactDto;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.TrackerItemRevisionDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.TrackerItemHistoryEntryDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.ActivityLogEntryDto.Type;
import com.intland.codebeamer.persistence.dto.base.DescribeableDto;
import com.intland.codebeamer.persistence.dto.base.IdentifiableDto;
import com.intland.codebeamer.persistence.dto.base.ReferableDto;
import com.intland.codebeamer.persistence.dto.base.ReferenceDto;
import com.intland.codebeamer.persistence.util.PersistenceUtils;
import com.intland.codebeamer.persistence.util.TrackerItemFieldHandler;
import com.intland.codebeamer.remoting.ArtifactType;
import com.intland.codebeamer.remoting.DescriptionFormat;
import com.intland.codebeamer.remoting.GroupType;
import com.intland.codebeamer.remoting.GroupTypeClassUtils;
import com.intland.codebeamer.wiki.plugins.recentactivities.Activity.Change;

/**
 * A RecentActivitiesPluginManager implementation based on the RecentActivityQuery
 *
 * @author <a href="mailto:klaus.mehling@intland.com">Klaus Mehling</a>
 */
public class ActivityStreamManagerImpl implements ActivityStreamManager {

	private final static Logger logger = Logger.getLogger(ActivityStreamManagerImpl.class);

	@Autowired
	private ProjectManager projectManager;

	@Autowired
	private TrackerItemManager trackerItemManager;

	@Autowired
	private EntityLabelDao entityLabelDao;

	@Autowired
	private ActivityLogDao activityLogDao;

	/**
	 * A special comparator to sort changes by {@link GroupType} and {@link ArtifactType}
	 */
	public static final Comparator<Change> ChangesComparator = new Comparator<Change>() {
		public int compare(Change change1, Change change2) {
			int diff = change1.getType().compareTo(change2.getType());
			if (diff != 0) {
				return diff;
			}
			diff = IdentifiableDto.compareInteger(getDetailType(change1), getDetailType(change2));
			if (diff != 0) {
				return diff;
			}

			if (change1.getDetail() instanceof ArtifactDto) {
				ArtifactDto obj1 = (ArtifactDto) change1.getDetail();
				ArtifactDto obj2 = (ArtifactDto) change2.getDetail();

				diff = IdentifiableDto.compareInteger(obj1.getTypeId(), obj2.getTypeId());
				if (diff != 0) {
					return diff;
				}

				return IdentifiableDto.compare(obj1, obj2);
			} else if (change1.getDetail() instanceof TrackerItemHistoryEntryDto) {
				TrackerItemHistoryEntryDto mod1 = (TrackerItemHistoryEntryDto) change1.getDetail();

				if (change2.getDetail() instanceof TrackerItemHistoryEntryDto) {
					TrackerItemHistoryEntryDto mod2 = (TrackerItemHistoryEntryDto) change2.getDetail();

					if ((diff = IdentifiableDto.compareInteger(mod1.getVersion(), mod2.getVersion())) == 0) {
						diff = IdentifiableDto.compare(mod1.getField(), mod2.getField());
					}
				} else {
					diff = 1;
				}
			} else if (change2.getDetail() instanceof TrackerItemHistoryEntryDto) {
				diff = -1;
			}
			return diff;
		}

		protected Integer getDetailType(Change change) {
			Integer result = null;
			Object detail = null;
			if (change != null && (detail = change.getDetail()) != null) {
				if (detail instanceof ReferenceDto) {
					detail = ((ReferenceDto<?>)detail).getDto();
				}
				result = GroupTypeClassUtils.objectToGroupType(detail);
			}
			return result;
		}
	};

	public static class ArtifactTypePredicate implements Predicate<ActivityLogEntryDto<?>> {
		private Set<Integer> artifactTypes;

		public ArtifactTypePredicate(Collection<Integer> artifactTypes) {
			this.artifactTypes = new HashSet<Integer>(artifactTypes);
		}

		@Override
		public boolean apply(ActivityLogEntryDto<?> entry) {
			return entry != null && (entry.getDto() instanceof ArtifactDto ? check((ArtifactDto)entry.getDto()) : entry.getDto() != null);
		}

		protected boolean check(ArtifactDto artifact) {
			 return artifactTypes.contains(artifact.getTypeId()) ||
					(artifact.isA(ArtifactType.ATTACHMENT) && artifact.getParent() != null && artifactTypes.contains(artifact.getParent().getTypeId()));

		}
	}

	/**
	 * Find the most active projects (from the specified projects) within the specified time period.
	 * @param user is user that requests the most active projects
	 * @param command contains the scope and period
	 * @return an ordered Map of activity score (value) per project (key), ordered descending by score
	 */
	public Map<ProjectDto,Integer> findMostActiveProjects(UserDto user, MostActiveProjectsCommand command) {
		Map<ProjectDto,Integer> result = Collections.emptyMap();
		Collection<Integer> projectIds = command.getProjectId();
		if (projectIds == null && command.getTag() != null) {
			projectIds = entityLabelDao.getTaggedEntityIds(user, command.getTag(), Collections.singletonList(EntityCache.PROJECT_TYPE)).get(EntityCache.PROJECT_TYPE);
		}

		List<ProjectDto> projects = (projectIds != null ? projectManager.findById(user, projectIds) : projectManager.findAll(user, Boolean.TRUE));
		if (projects != null && projects.size() > 0) {
			Date[] range = command.getPeriod().getEdges(null, null);
			Map<Integer,Integer> found = activityLogDao.findMostActiveProjects(projects, range[0], range[1], command.getMax());
			if (found != null && found.size() > 0) {
				Map<Integer,ProjectDto> projMap = PersistenceUtils.createLookupMap(projects);
				result = new LinkedHashMap<ProjectDto,Integer>(found.size());

				for (Map.Entry<Integer,Integer> entry : found.entrySet()) {
					ProjectDto project = projMap.get(entry.getKey());
					if (project != null) {
						result.put(project, entry.getValue());
					}
				}
			}
		}
		return result;
	}

	public DateIdentifiableMap<ProjectDto,Integer> getActivityTrends(UserDto user, ProjectActivityTrendsCommand command) {
		DateIdentifiableMap<ProjectDto, Integer> result = new DateIdentifiableMap<ProjectDto, Integer>();
		List<Date> dates = command.getDatesInRange();
		Collection<Integer> projectIds = command.getProjectId();
		if (projectIds == null && command.getTag() != null) {
			projectIds = entityLabelDao.getTaggedEntityIds(user, command.getTag(), Collections.singletonList(EntityCache.PROJECT_TYPE)).get(EntityCache.PROJECT_TYPE);
		}

		List<ProjectDto> projects = EntityCache.getInstance(user).get(ProjectDto.class, EntityCache.PROJECT_TYPE, projectIds);
		if (projects != null && projects.size() > 0 && dates != null && dates.size() > 0) {
			Date[] range = command.getRange(null, null);
			Map<Integer,SortedMap<Date,Integer>> activityTrends = activityLogDao.getActivityTrend(projects, range[0], range[1]);

			for (ProjectDto project : projects) {
				for (Date date : dates) {
					result.put(date, project, Integer.valueOf(0));
				}

				Map<Date,Integer> trends = activityTrends.get(project.getId());
				if (trends != null && trends.size() > 0) {
					for (Map.Entry<Date,Integer> trend : trends.entrySet()) {
						if (trend.getValue() != null && trend.getValue().intValue() > 0) {
							Date date = command.getGroup(trend.getKey());
							Integer value = result.get(date, project);
							if (value != null) {
								value = Integer.valueOf(value.intValue() + trend.getValue().intValue());
							} else {
								value = trend.getValue();
							}
							result.put(date, project, value);
						}
					}
				}
			}
		}

		return result;
	}

	public List<Activity> findActivities(UserDto user, ActivityStreamCommand command) {
		if (logger.isDebugEnabled()) {
			logger.debug("findActivities() user:<" + user +">, command=" + command);
		}
		List<Activity> result = Collections.emptyList();

		if (command != null && command.getFilter() != null) {
			Map<Integer,Collection<Integer>> filter = new HashMap<Integer,Collection<Integer>>(8);

			for (EntitiesFilter entityFilter : command.getFilter()) {
				for (Integer groupType: entityFilter.getGroupTypes()) {
					Collection<Integer> subTypes = filter.get(groupType);
					if (subTypes == null) {
						filter.put(groupType, subTypes = new ArrayList<Integer>());
					}
					subTypes.addAll(entityFilter.getArtifactTypes());
				}
			}

			if (!filter.isEmpty()) {
				Collection<Integer> projectIds = command.getProjectId();
				if (projectIds == null) {
					projectIds = entityLabelDao.getTaggedEntityIds(user, command.getTag(), Collections.singletonList(EntityCache.PROJECT_TYPE)).get(EntityCache.PROJECT_TYPE);
				}

				if (CollectionUtils.isEmpty(projectIds)) {
					projectIds = projectManager.findAll(user, false);
				}

				if (CollectionUtils.isNotEmpty(projectIds)) {
					Date[] range = command.getRange(null, user != null ? user.getTimeZone() : null);
					result = findActivities(user, projectIds,
											range != null ? range[0] : null,
											range != null ? range[1] : null,
											filter,
											command.getMax() != null ? command.getMax().intValue() : 100);
				}
			}
		}
		return result;
	}

	public List<Activity> findActivities(UserDto user, Collection<?> projects, Date from, Date until, Map<Integer, Collection<Integer>> filter, int limit) {
		Map<Integer,List<TrackerItemRevisionDto>> issueChangeSets = new HashMap<Integer,List<TrackerItemRevisionDto>>();
		Map<ReferenceDto<?>,Map<UserDto,Activity>> lastActivityPerTargetAndUser = new HashMap<ReferenceDto<?>,Map<UserDto,Activity>>();
		List<Activity> result = new ArrayList<Activity>(limit = Math.min(500, limit));

		int pageSize = limit;

		// If we look for artifact and/or issue activities, there is a chance that activities consist of more than one change,
		// e.g. an update plus a comment and/or (multiple) attachments. To reduce the number of database queries, it is better
		// to fetch 25% more than the requested number of changes
		if (filter == null || filter.containsKey(EntityCache.ARTIFACT_TYPE) || filter.containsKey(EntityCache.TRACKER_ITEM_TYPE)) {
			pageSize += (limit/4 + limit%4);
		} else {
			pageSize++;
		}
		pageSize = Math.max(pageSize, 50);

		ArtifactTypePredicate artifactTypePredicate = null;
//		Collection<Integer> artifactTypes = (filter != null ? filter.get(EntityCache.ARTIFACT_TYPE) :  null);
//		if (artifactTypes != null && artifactTypes.size() > 0) {
//			artifactTypePredicate = new ArtifactTypePredicate(artifactTypes);
//		}

		// Count the total number of available log entries (without checking user access permissions !!!)
		int total = activityLogDao.countLogEntries(projects, from, until, filter);

		for (int page = 0; (page * pageSize) < total && result.size() <= limit; ++page) {
			// Get the next page of matching activity log entries
			List<ActivityLogEntryDto<?>> logEntries = activityLogDao.getLogEntries(user, projects, from, until, filter, page, pageSize);
			if (logEntries != null && logEntries.size() > 0) {
				ActivityLogEntryDto lastEntry = null;

				// If the log entries contain issue updates, get the missing issue changes sets first
				if (filter == null || filter.containsKey(EntityCache.TRACKER_ITEM_TYPE)) {
					Set<TrackerItemDto> changedIssues = new HashSet<TrackerItemDto>();
					for (ActivityLogEntryDto<?> logEntry : logEntries) {
						if (ActivityLogEntryDto.Type.Modify.equals(logEntry.getType()) && logEntry.getDto() instanceof TrackerItemDto && !issueChangeSets.containsKey(logEntry.getId())) {
							changedIssues.add((TrackerItemDto) logEntry.getDto());
						}
					}
					issueChangeSets.putAll(trackerItemManager.getRevisionHistory(user, changedIssues, null, false, true));
				}

				// Now group the log entries into changes per target, user and date
				for (ActivityLogEntryDto<?> logEntry : logEntries) {
					// REQ-37573 : Activity Stream should not show same events after each other
					if ((artifactTypePredicate != null && !artifactTypePredicate.apply(logEntry)) || isRepetition(logEntry, lastEntry)) {
						continue;
					}

					Activity activity = convertToActivity(user, logEntry, issueChangeSets);
					if (activity == null) {
						continue;
					}

					ReferenceDto<?> target = ReferenceDto.of((ReferableDto)activity.getTarget());

					// Insert/Updates are grouped by target, user and date, but deletes are grouped by target type, user and date
					if (Type.Delete.equals(activity.getType())) {
						target.setId(null);
					}

					Map<UserDto,Activity> userTargetActivities = lastActivityPerTargetAndUser.get(target);
					if (userTargetActivities == null) {
						lastActivityPerTargetAndUser.put(target, userTargetActivities = new HashMap<UserDto,Activity>());
					}

					Activity lastUserActivityOnTarget = userTargetActivities.get(activity.getMadeBy());
					if (lastUserActivityOnTarget == null || Math.abs(lastUserActivityOnTarget.getDate().getTime() - activity.getDate().getTime()) > 999L) {
						// Make current activity the grouping target
						userTargetActivities.put(activity.getMadeBy(), activity);
						result.add(activity);
					} else {
						List<Change> changes = lastUserActivityOnTarget.getChanges();
						changes.addAll(activity.getChanges());

						if (changes.size() > 1) {
							Collections.sort(changes, ChangesComparator);
						}

						if (Type.Create.equals(activity.getType())) {
							lastUserActivityOnTarget.setType(activity.getType());
							lastUserActivityOnTarget.setDate(activity.getDate());
						}
					}

					lastEntry = logEntry;
				}
			}
		}
		return result.size() > limit ? result.subList(0, limit) : result;
	}

	/*
	 * REQ-37573 : Activity Stream should not show same events after each other
 	 * E.g. if the same user changes the content of the same wiki page several times and nobody else makes any changes
 	 * between the number of changes of the user should be shown but only one event should appear in the activity stream output
 	 * @param logEntry is the current log entry
 	 * @param lastEntry is the last log entry, or null
 	 * @return true if logEntry is a repetition of lastEntry
 	 */
	private boolean isRepetition(ActivityLogEntryDto logEntry, ActivityLogEntryDto lastEntry) {
		return logEntry != null && lastEntry != null &&
				logEntry.getType() == lastEntry.getType() &&
				EntityCache.ARTIFACT_TYPE.equals(logEntry.getTypeId()) &&
				IdentifiableDto.equals(logEntry.getUser(), lastEntry.getUser()) &&
				ReferenceDto.equals(logEntry, lastEntry);
	}

	protected Activity convertToActivity(UserDto user, ActivityLogEntryDto<?> entry, Map<Integer,List<TrackerItemRevisionDto>> issueChangeHistory) {
		Activity activity = null;
		String subject = entry.getComment();
		String format  = DescriptionFormat.PLAIN_TEXT;

		if (subject == null && entry.getDto() instanceof DescribeableDto) {
			DescribeableDto target = (DescribeableDto) entry.getDto();
			subject = target.getDescription();
			format  = target.getDescriptionFormat();
		}

		if (entry.getDto() instanceof ArtifactDto) {
			ArtifactDto artifact = (ArtifactDto) entry.getDto();
			if (artifact.isA(ArtifactType.ATTACHMENT)) {
				if (artifact.getFileSize() != null) {
					subject = artifact.getName();
					format  = DescriptionFormat.PLAIN_TEXT;
				}
				activity = new Activity(entry.getAt(), entry.getUser(), Type.Modify, artifact.getParent());
			} else {
				activity = new Activity(entry.getAt(), entry.getUser(), entry.getType(), artifact);
			}
			activity.getChanges().add(new Change(entry.getType(), subject, artifact).setSubjectFormat(format));
		} else if (EntityCache.TRACKER_ITEM_TYPE.equals(entry.getTypeId())) {
			activity = new Activity(entry.getAt(), entry.getUser(), entry.getType(), entry.getDto());
			if (Type.Modify.equals(entry.getType())) {
				List<TrackerItemRevisionDto> changeHistory = issueChangeHistory.get(entry.getId());
				if (changeHistory != null) {
					for (TrackerItemRevisionDto changeSet : changeHistory) {
						if ((entry.getVersion() != null && entry.getVersion().equals(changeSet.getVersion())) ||
							(entry.getVersion() == null	&& IdentifiableDto.equals(entry.getUser(), changeSet.getSubmitter()) && entry.getAt().equals(changeSet.getSubmittedAt()))) {
							if (changeSet.getChanges() != null && changeSet.getChanges().size() > 0) {
								for (TrackerItemHistoryEntryDto changed : changeSet.getChanges()) {
									// Handle some special cases here
									if (changed.getField() != null) {
										if (TrackerItemFieldHandler.ATTACHMENTS_LABEL_ID.equals(changed.getField().getId())) {
											addCommentsAndAttachments(changeSet.getDto(), changed, activity.getChanges());
											continue;
										}

										if (TrackerItemFieldHandler.SPENT_H_LABEL_ID.equals(changed.getField().getId()) && changeSet.getChanges().size() == 1) {
											// If only the spent hours changed, most probably due to a time recording,
											// we skip this activity, to avoid flooding the activity stream
											return null;
										}
									}
									activity.getChanges().add(new Change(Type.Modify, changed.getFieldName(), changed));
								}
							}
						}
					}
				}
			}
			if (activity.getChanges().isEmpty()) {
				activity.getChanges().add(new Change(entry.getType(), subject, entry.getDto()).setSubjectFormat(format));
			}
		} else {
			activity = new Activity(entry.getAt(), entry.getUser(), entry.getType(), entry.getDto());
			activity.getChanges().add(new Change(entry.getType(), subject, entry.getDto()).setSubjectFormat(format));
		}

		return activity;
	}

	protected void addCommentsAndAttachments(TrackerItemDto item, TrackerItemHistoryEntryDto change, List<Change> changes) {
		if (item != null && change != null && change.getField() != null && changes != null) {
			List<? extends ArtifactDto> attachments = item.getAttachments();
			Map<Integer,ReferenceDto<?>> removed = PersistenceUtils.createLookupMap(TrackerItemFieldHandler.decodeFieldReferences(change.getField().getId(), change.getOldValue()));
			Map<Integer,ReferenceDto<?>> added   = PersistenceUtils.createLookupMap(TrackerItemFieldHandler.decodeFieldReferences(change.getField().getId(), change.getNewValue()));
			added.keySet().removeAll(removed.keySet());

			if (added.size() > 0 && attachments != null && attachments.size() > 0) {
				boolean first = true;

				for (ArtifactDto attachment : attachments) {
					if (attachment != null && added.containsKey(attachment.getId())) {
						if (first) {
							if (StringUtils.isNotBlank(attachment.getDescription())) {
								Change comment = new Change(Type.Modify, attachment.getDescription(), null);
								comment.setSubjectFormat(attachment.getDescriptionFormat());
								changes.add(0, comment);
							}
							first = false;
						}

						if (attachment.getFileSize() != null) {
							changes.add(new Change(Type.Modify, attachment.getName(), attachment));
						}
					}
				}
			}
		}
	}

}
