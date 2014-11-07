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


/**
 * @author <a href="mailto:akos.tajti@intland.com">Akos Tajti</a>
 *
 */
public class TestCasesByLastRunResultCommand extends AbstractTestManagementCommand {
	protected Integer configurationId;
	private Integer releaseId;
	/**
	 * @return the configurationId
	 */
	public Integer getConfigurationId() {
		return configurationId;
	}
	/**
	 * @param configurationId the configurationId to set
	 */
	public void setConfigurationId(Integer configurationId) {
		this.configurationId = configurationId;
	}
	/**
	 * @return the releaseId
	 */
	public Integer getReleaseId() {
		return releaseId;
	}
	/**
	 * @param releaseId the releaseId to set
	 */
	public void setReleaseId(Integer releaseId) {
		this.releaseId = releaseId;
	}
}
