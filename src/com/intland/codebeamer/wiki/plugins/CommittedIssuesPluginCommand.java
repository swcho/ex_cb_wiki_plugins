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

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.validation.constraints.NotNull;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.propertyeditors.CustomDateEditor;

import com.intland.codebeamer.wiki.plugins.command.base.AbstractMaxWikiPluginCommand;

/**
 * Command bean for {@link SVNCommittedIssuesPlugin}
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class CommittedIssuesPluginCommand extends AbstractMaxWikiPluginCommand implements PropertyEditorRegistrar {

	private Date startDate;
	private Date endDate;

	// only supports a single repository
	private Integer repositoryId;

	/**
	 * The name of the branch or tag to filter issues by
	 */
	private String branchOrTag;

	// optional start SCM revision
	private String startRevision;
	// optional end SCM revision
	private String endRevision;

	// if the statistics is shown
	private boolean showStats = true;

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	@NotNull
	public Integer getRepositoryId() {
		return repositoryId;
	}
	public void setRepositoryId(Integer repositoryId) {
		this.repositoryId = repositoryId;
	}

	public String getBranchOrTag() {
		return branchOrTag;
	}
	public void setBranchOrTag(String branchOrTag) {
		this.branchOrTag = branchOrTag;
	}

	public boolean isShowStats() {
		return showStats;
	}
	public void setShowStats(boolean showStats) {
		this.showStats = showStats;
	}

	public String getStartRevision() {
		return startRevision;
	}
	public void setStartRevision(String startRevision) {
		this.startRevision = startRevision;
	}
	public String getEndRevision() {
		return endRevision;
	}
	public void setEndRevision(String endRevision) {
		this.endRevision = endRevision;
	}

	/**
	 * Register custom editors
	 */
	public void registerCustomEditors(PropertyEditorRegistry registry) {
		registry.registerCustomEditor(Date.class, new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"), true));
		registry.registerCustomEditor(Date.class, new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd HH:mm"), true));
	}

}
