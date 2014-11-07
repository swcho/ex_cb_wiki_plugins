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

import javax.validation.constraints.AssertFalse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import com.intland.codebeamer.wiki.plugins.ArtifactActivityTrendsPlugin;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractTimeIntervalDisplayOptionCommand;
import com.intland.codebeamer.wiki.plugins.command.enums.ArtifactType;

/**
 * Command class for {@link ArtifactActivityTrendsPlugin}.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public class ArtifactCountTrendsCommand extends AbstractTimeIntervalDisplayOptionCommand {
	private List<Integer> projectId;
	/**
	 * Projects tagged with these will be the input.
	 */
	private List<String> tag;
	private ArtifactType type = ArtifactType.WIKIPAGE;

	public List<Integer> getProjectId() {
		return projectId;
	}

	public void setProjectId(List<Integer> projectId) {
		this.projectId = projectId;
	}

	public List<String> getTag() {
		return tag;
	}

	public void setTag(List<String> tag) {
		this.tag = tag;
	}

	public ArtifactType getType() {
		return type;
	}

	public void setType(ArtifactType type) {
		this.type = type;
	}

	@AssertFalse(message = "{document.trend.onlyOneOfProjectIdOrTag}")
	public boolean isOnlyOneOfProjectIdOrTagValid() {
		boolean isProjectIdValid = !CollectionUtils.isEmpty(projectId);
		boolean isTagValid = !CollectionUtils.isEmpty(tag);

		return isProjectIdValid && isTagValid;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
}
