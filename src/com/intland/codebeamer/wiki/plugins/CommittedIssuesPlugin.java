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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.controller.bugs.VersionIssuesRenderer;
import com.intland.codebeamer.manager.TrackerItemManager;
import com.intland.codebeamer.persistence.dao.PaginatedDtoList;
import com.intland.codebeamer.persistence.dao.ScmChangeSetDao;
import com.intland.codebeamer.persistence.dto.ScmChangeSetDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.util.PersistenceUtils;
import com.intland.codebeamer.ui.view.IssueStatusStyles;
import com.intland.codebeamer.utils.MultiValue;
import com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin;

/**
 * Plugin show all issues referenced in commits of an SCM repository.
 * Note: this replaces the previous CommittedIssuesPlugin which is renamed to {@link SVNCommittedIssuesPlugin}.
 * This plugin automatically forwards to the old {@link SVNCommittedIssuesPlugin} if it detects the old plugins's parameters
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class CommittedIssuesPlugin extends AbstractCommandWikiPlugin<CommittedIssuesPluginCommand> {

	private final static Logger logger = Logger.getLogger(CommittedIssuesPlugin.class);

	@Autowired
	private ScmChangeSetDao scmChangeSetDao;
	@Autowired
	private TrackerItemManager trackerItemManager;
	@Autowired
	private IssueStatusStyles issueStatusStyles;

	@Override
	protected String execute(VelocityContext velocityContext, Map params) throws PluginException {
		if (params.get(SVNCommittedIssuesPlugin.REPOSITORY_URL_PARAM) != null) {
			SVNCommittedIssuesPlugin legacyPlugin = new SVNCommittedIssuesPlugin();
			logger.info("Calling to legacy " + legacyPlugin.getClass().getName() +" plugin, because " + SVNCommittedIssuesPlugin.REPOSITORY_URL_PARAM + " parameter appears in the argument list.");
			return legacyPlugin.execute(getWikiContext(), params);
		}
		return super.execute(velocityContext, params);
	}

	@Override
	public CommittedIssuesPluginCommand createCommand() throws PluginException {
		return new CommittedIssuesPluginCommand();
	}

	@Override
	protected String getTemplateFilename() {
		return "committed-issues-plugin.vm";
	}

	@Override
	protected Map populateModel(DataBinder binder, CommittedIssuesPluginCommand command, Map params) throws PluginException {
		UserDto user = getUser();
		HttpServletRequest request = getWikiContext().getHttpRequest();

		MultiValue<List<TrackerItemDto>, List<ScmChangeSetDto>> issuesAndChangesets = findIssuesAndChangeSetsAffected(user, command);
		List<TrackerItemDto> issues = issuesAndChangesets.getLeft();
		List<ScmChangeSetDto> changesets = issuesAndChangesets.getRight();

		// fully load the issues from the database, so all data of them is populated
		List<TrackerItemDto> issuesReloaded = trackerItemManager.findById(user, PersistenceUtils.grabIds(issues), false);
		Map<Integer, TrackerItemDto> issuesById = PersistenceUtils.createLookupMap(issuesReloaded);
		issuesReloaded = sortIssuesByOriginalOrder(issues, issuesById);

		// the key is the issue id
		Map<Integer, List<ScmChangeSetDto>> issuesAndCommits = groupIssuesAndCommits(changesets, issuesById);

		Map model = new HashMap<String, Object>();
		model.put("issues", issuesReloaded);
		model.put("issuesAndCommits", issuesAndCommits);

		List<Map<String, Object>> trackerItemsAsMaps = new VersionIssuesRenderer().renderTrackerItemsForVersionsToMaps(request, issuesReloaded, issueStatusStyles);
		model.put("trackerItemsAsMaps", trackerItemsAsMaps);

		if (command.isShowStats()) {
			addStatsToModel(issuesReloaded, model);
		}

		return model;
	}

	private List<TrackerItemDto> sortIssuesByOriginalOrder(List<TrackerItemDto> issuesInOriginalOrder, Map<Integer, TrackerItemDto> issuesById) {
		// unfortunately the trackerItemManager.findById is not keeping the order of the issues, so have to reoder them to the original order
		List<TrackerItemDto> issuesResorted = new ArrayList<TrackerItemDto>(issuesInOriginalOrder.size());
		for (TrackerItemDto issue: issuesInOriginalOrder) {
			TrackerItemDto issueReloaded = issuesById.get(issue.getId());
			if (issueReloaded != null) {
				issuesResorted.add(issueReloaded);
			}
		}
		return issuesResorted;
	}

	private void addStatsToModel(List<TrackerItemDto> issuesReloaded, Map model) {
		int resolvedAndClosedTrackerItems = 0;
		int openTrackerItems = 0;
		double progressPercentage = 0d;
		int numIssues = issuesReloaded.size();

		for (TrackerItemDto issue: issuesReloaded) {
			if (issue.isResolvedOrClosed()) {
				resolvedAndClosedTrackerItems++;
			}
		}
		if (numIssues > 0) {
			openTrackerItems = numIssues - resolvedAndClosedTrackerItems;
			progressPercentage = Math.round((resolvedAndClosedTrackerItems * 1000d) / numIssues) /10d; // round to 1 digit
		}

		model.put("resolvedAndClosedTrackerItems", Integer.valueOf(resolvedAndClosedTrackerItems));
		model.put("openTrackerItems", Integer.valueOf(openTrackerItems));
		model.put("progressPercentage", Double.valueOf(progressPercentage));
	}

	/**
	 * Fetch changesets and collect their affected issues.
	 *
	 * @return The list of TrackerItemDtos in order they appeared in the change-sets, and the list of changesets in descending order of their dates
	 */
	protected MultiValue<List<TrackerItemDto>,List<ScmChangeSetDto>> findIssuesAndChangeSetsAffected(UserDto user, CommittedIssuesPluginCommand command) {
		int page = 0;
		int pagesize = Math.min(command.getMax().intValue() * 5, 1000);
		List<ScmChangeSetDto> changesets = new ArrayList(pagesize);

		// because the revisions are orderable - at least for git, where these are just SHA hashes- we look for the date when the
		// revision has been committed, and filter by using that date. And then we drop those commits which happened before the "start" revision and after the "end" revision
		// if both the start/end date and start/end revision is specified then use their dates' intersections to be most precise
		Date startDate = maxDate(command.getStartDate(), getDateByRevision(user, command.getRepositoryId(), command.getStartRevision()));
		Date endDate = minDate(command.getEndDate(), getDateByRevision(user, command.getRepositoryId(), command.getEndRevision()));

		ArrayList<TrackerItemDto> issues = new ArrayList<TrackerItemDto>(pagesize);
		PaginatedDtoList<ScmChangeSetDto> commitsPage;
		boolean startRevisionFound = false;
		// load the changesets until we got that many issues as we want
		do {
			commitsPage = scmChangeSetDao.findChangeSetPage(user, command.getRepositoryId(),
					null, null, null, startDate, endDate,
					null, command.getBranchOrTag(), page , pagesize);
			List<ScmChangeSetDto> commits = commitsPage.getList();

			if (commits == null || commits.isEmpty()) {
				return new MultiValue(issues, changesets);
			}
			for (ScmChangeSetDto change: commits) {
				// drop any commit appears before the "startRevision"
				if (command.getStartRevision() != null && !startRevisionFound) {
					// drop every commit if we did not reach the start revision
					if (command.getStartRevision().equals(change.getRevision())) {
						continue;
					}
					startRevisionFound = true;
				}
				changesets.add(change);
				final Set<TrackerItemDto> trackerItems = change.getTrackerItems();
				if (trackerItems != null) {
					for (TrackerItemDto issue : trackerItems) {
						// count unique issues
						if (!issues.contains(issue)) {
							issues.add(issue);
							if (issues.size() >= command.getMax().intValue()) {
								return new MultiValue(issues, changesets);
							}
						}
					}
				}

				// if we find the "endRevision" the search is finished
				if (command.getEndRevision() != null && command.getEndRevision().equals(change.getRevision())) {
					new MultiValue(issues, changesets);
				}
			}
			page++;
		} while (commitsPage != null && commitsPage.hasNextPage());
		return new MultiValue(issues, changesets);
	}

	// choose min of two dates, nulls allowed
	private Date minDate(Date d1, Date d2) {
		if (d1 == null) {
			return d2;
		}
		if (d2 == null) {
			return d1;
		}
		return d1.before(d2) ? d1 : d2;
	}

	// choose max of tow dates, nulls allowed
	private Date maxDate(Date d1, Date d2) {
		if (d1 == null) {
			return d2;
		}
		if (d2 == null) {
			return d1;
		}
		return d1.before(d2) ? d2 : d1;
	}

	/**
	 * Get the date of a revision number
	 */
	private Date getDateByRevision(UserDto user, Integer repositoryId, String revision) {
		if (revision != null) {
			ScmChangeSetDto commitForRevision = scmChangeSetDao.findByRevision(user, repositoryId, revision);
			Date revisionDate = commitForRevision == null ? null : commitForRevision.getSubmittedAt();
			if (revisionDate != null) {
				logger.debug("Revision <" + revision +"> has been submitted on " + revisionDate +", using it for filtering");
				return revisionDate;
			}
		}
		return null;
	}

	/**
	 * Create a map which contains the affected issues as keys (keeping the commit order) and for each issue the list of commits for them.
	 */
	private Map<Integer, List<ScmChangeSetDto>> groupIssuesAndCommits(List<ScmChangeSetDto> commitsList,
			Map<Integer, TrackerItemDto> affectedIssuesMap) {
		// create an ordered map with the issues and their changesets
		Map<Integer, List<ScmChangeSetDto>> issuesAndCommits = new LinkedHashMap<Integer,List<ScmChangeSetDto>>();
		for (ScmChangeSetDto commit:commitsList) {
			final Set<TrackerItemDto> trackerItems = commit.getTrackerItems();
			if (trackerItems != null) {
				for (TrackerItemDto issue : trackerItems) {
					if (issue != null && issue.getId() != null) {
						TrackerItemDto issueWithData = affectedIssuesMap.get(issue.getId());
						List<ScmChangeSetDto> commitsForIssue = issuesAndCommits.get(issueWithData.getId());
						if (commitsForIssue == null) {
							commitsForIssue = new ArrayList<ScmChangeSetDto>();
							issuesAndCommits.put(issueWithData.getId(), commitsForIssue);
						}
						commitsForIssue.add(commit);
					}
				}
			}
		}
		return issuesAndCommits;
	}

}
