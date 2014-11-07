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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import com.intland.codebeamer.wiki.plugins.ArtifactActivityTrendsPlugin;
import com.intland.codebeamer.wiki.plugins.base.BeanToQueryParametersConverter.Ignored;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractTimeIntervalDisplayOptionCommand;
import com.intland.codebeamer.wiki.plugins.command.enums.Activity;

/**
 * Command class for {@link ArtifactActivityTrendsPlugin}.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public class ArtifactActivityTrendsCommand extends AbstractTimeIntervalDisplayOptionCommand {
	/**
	 * Mutually exclusive validity of "artifactId" and "tag" is not trivial with Spring
	 * abstraction of JSR-303. Falling back to simple Java validation in a separate method.
	 *
	 * @see #isOnlyOneOfArtifactIdsOrTagValid()
	 */
	private List<Integer> artifactId;

	/**
	 * Artifacts tagged with these will be the input.
	 */
	private List<String> tag;
	@Ignored
	private Activity activity = Activity.READ;

	public List<Integer> getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(List<Integer> artifactIds) {
		this.artifactId = artifactIds;
	}

	public List<String> getTag() {
		return tag;
	}

	public void setTag(List<String> tag) {
		this.tag = tag;
	}

	public Activity getActivity() {
		return activity;
	}

	public void setActivity(Activity activity) {
		this.activity = activity;
	}

	@AssertTrue(message = "{document.activity.onlyOneOfArtifactIdsOrTag}")
	public boolean isOnlyOneOfArtifactIdsOrTagValid() {
		boolean isArtifactIdValid = CollectionUtils.isNotEmpty(artifactId);
		boolean isTagValid = CollectionUtils.isNotEmpty(tag);

		return (isArtifactIdValid != isTagValid);
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
}
