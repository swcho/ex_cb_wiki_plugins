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
package com.intland.codebeamer.wiki.plugins.issuecounttrends;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.AssertTrue;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import com.intland.codebeamer.wiki.plugins.command.base.AbstractTimeIntervalDisplayOptionCommand;

/**
 * Command bean for {@link IssueCountTrendsPlugin}
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class IssueCountTrendsCommand extends AbstractTimeIntervalDisplayOptionCommand {
	/**
	 * Note: either projectId or trackerId can have value, but not mixed!
	 */
	private List<Integer> projectId = Collections.EMPTY_LIST;
	private List<Integer> trackerId = Collections.EMPTY_LIST;
	/**
	 * Trackers tagged with these will be the input.
	 */
	private List<String> tag;

	public List<Integer> getProjectId() {
		return projectId;
	}

	public void setProjectId(List<Integer> projectIds) {
		this.projectId = projectIds;
	}

	public List<Integer> getTrackerId() {
		return trackerId;
	}

	public void setTrackerId(List<Integer> trackerIds) {
		this.trackerId = trackerIds;
	}

	public List<String> getTag() {
		return tag;
	}

	public void setTag(List<String> tag) {
		this.tag = tag;
	}

	@AssertTrue(message = "{issue.count.onlyOneOfProjectIdAndTrackerIdAndTag}")
	public boolean isOnlyOneOfProjectIdAndTrackerIdAndTagValid() {
		boolean isProjectIdValid = !CollectionUtils.isEmpty(projectId);
		boolean isTrackerIdValid = !CollectionUtils.isEmpty(trackerId);
		boolean isTagValid = !CollectionUtils.isEmpty(tag);

		return (!isProjectIdValid && !isTrackerIdValid && !isTagValid) || (isProjectIdValid ^ isTrackerIdValid ^ isTagValid);
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
}
