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

import java.util.List;

import javax.validation.constraints.AssertFalse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import com.intland.codebeamer.utils.AnchoredPeriod;
import com.intland.codebeamer.utils.CalendarUnit;
import com.intland.codebeamer.utils.AnchoredPeriod.Anchor;
import com.intland.codebeamer.wiki.plugins.base.BeanToQueryParametersConverter.Ignored;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractMaxWikiPluginCommand;

/**
 * Command class for {@link MostActiveProjectsPlugin}.
 *
 * @author <a href="mailto:klaus.mehling@intland.com">Klaus Mehling</a>
 */
public class MostActiveProjectsCommand extends AbstractMaxWikiPluginCommand {
	public static final AnchoredPeriod DEFAULT_PERIOD = new AnchoredPeriod(Anchor.Last, 30, CalendarUnit.Day);

	private List<Integer> projectId;
	/**
	 * Projects tagged with these will be the input.
	 */
	private List<String> tag;

	@Ignored
	private AnchoredPeriod period;

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

	public AnchoredPeriod getPeriod() {
		return period != null ? period : DEFAULT_PERIOD;
	}

	public void setPeriod(AnchoredPeriod period) {
		this.period = period;
	}


	@AssertFalse(message = "{activity.stream.onlyOneOfProjectIdOrTag}")
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
