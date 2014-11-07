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

import com.intland.codebeamer.wiki.plugins.command.base.AbstractWikiPluginCommand;

/**
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public class SprintBreakDownCommand extends AbstractWikiPluginCommand {
	private Integer releaseId;

	public Integer getReleaseId() {
		return releaseId;
	}

	public void setReleaseId(Integer releaseId) {
		this.releaseId = releaseId;
	}

	public Integer getId() {
		return getReleaseId();
	}

	public void setId(Integer id) {
		setReleaseId(id);
	}
}
