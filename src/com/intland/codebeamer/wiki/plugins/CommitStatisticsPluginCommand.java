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
import java.util.List;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import com.intland.codebeamer.wiki.plugins.command.base.AbstractWikiPluginCommand;

/**
 * Command bean for {@link CommitStatisticsPlugin}
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class CommitStatisticsPluginCommand extends AbstractWikiPluginCommand {
	// project-id is used during auto discovery, and added for backwards compatibility
	// do not delete, or the plugin will complain about if this parameter is present
	private Integer projectId;

	private List<Integer> repositoryId = new ArrayList<Integer>();
	private String layout;
	// show commits statistics per repository. Available only when the layout is NOT "column"
	private boolean detailed = false;

	public List<Integer> getRepositoryId() {
		return repositoryId;
	}
	public void setRepositoryId(List<Integer> repositoryId) {
		this.repositoryId = repositoryId;
	}

	public String getLayout() {
		return layout;
	}
	public void setLayout(String layout) {
		this.layout = layout;
	}

	public boolean isDetailed() {
		return detailed;
	}
	public void setDetailed(boolean detailed) {
		this.detailed = detailed;
	}

	@Deprecated // Do not use, only for backwards compatibility
	public Integer getProjectId() {
		return projectId;
	}
	public void setProjectId(Integer projectId) {
		this.projectId = projectId;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}

}
