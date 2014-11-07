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
package com.intland.codebeamer.wiki.plugins.command;

import java.util.List;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import com.intland.codebeamer.wiki.plugins.IssueCountByFieldPlugin;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractDisplayOptionCommand;
import com.intland.codebeamer.wiki.plugins.command.enums.TrackerItemField;

/**
 * Command class for {@link IssueCountByFieldPlugin}.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public class IssueCountByFieldCommand extends AbstractDisplayOptionCommand {
	/**
	 * Note: either projectId or trackerId can have value, but not mixed!
	 */
	private List<Integer> projectId = null;
	private List<Integer> trackerId = null;
	/**
	 * Projects tagged with these will be the input.
	 */
	private List<String> tag;
	@NotNull
	private TrackerItemField field = TrackerItemField.STATUS;
	private boolean includeClosed = false;

	public List<Integer> getProjectId() {
		return projectId;
	}

	public void setProjectId(List<Integer> projectId) {
		this.projectId = projectId;
	}

	public List<Integer> getTrackerId() {
		return trackerId;
	}

	public void setTrackerId(List<Integer> trackerId) {
		this.trackerId = trackerId;
	}

	public List<String> getTag() {
		return tag;
	}

	public void setTag(List<String> tag) {
		this.tag = tag;
	}

	public TrackerItemField getField() {
		return field;
	}

	public void setField(TrackerItemField field) {
		this.field = field;
	}

	public boolean isIncludeClosed() {
		return includeClosed;
	}

	public void setIncludeClosed(boolean includeClosedIssues) {
		this.includeClosed = includeClosedIssues;
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
