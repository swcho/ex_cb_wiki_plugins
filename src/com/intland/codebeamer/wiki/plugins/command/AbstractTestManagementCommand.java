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

import com.intland.codebeamer.wiki.plugins.command.base.AbstractDisplayOptionCommand;

/**
 * @author <a href="mailto:akos.tajti@intland.com">Akos Tajti</a>
 *
 */
public abstract class AbstractTestManagementCommand extends AbstractDisplayOptionCommand {

	private Integer projectId;
	private List<Integer> trackerId;

	/**
	 * 
	 */
	public AbstractTestManagementCommand() {
		super();
	}

	/**
	 * @return the projectId
	 */
	public Integer getProjectId() {
		return projectId;
	}

	/**
	 * @param projectId the projectId to set
	 */
	public void setProjectId(Integer projectId) {
		this.projectId = projectId;
	}

	/**
	 * @return the trackerId
	 */
	public List<Integer> getTrackerId() {
		return trackerId;
	}

	/**
	 * @param trackerId the trackerId to set
	 */
	public void setTrackerId(List<Integer> trackerId) {
		this.trackerId = trackerId;
	}
}