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
package com.intland.codebeamer.wiki.plugins.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.DataBinder;
import org.springframework.validation.ObjectError;

import com.intland.codebeamer.manager.TrackerItemManager;
import com.intland.codebeamer.manager.support.AgileSupport;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.UserDto;

/**
 * Mixin to support wiki plugins to implement agile functionality.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public abstract class AbstractAgileWikiPlugin<C> extends AbstractCommandWikiPlugin<C> {
	@Autowired
	private TrackerItemManager trackerItemManager;
	@Autowired
	private AgileSupport agileSupport;

	/**
	 * Returns the release specified by the passed ID, or looks up the "first release" if no ID was passed, or
	 * return <code>null</code> if no release is available.
	 */
	public TrackerItemDto discoverRelease(UserDto user, Integer releaseId, DataBinder binder) {
		TrackerItemDto release = null;
		if(releaseId != null) {
			release = trackerItemManager.findById(user, releaseId);
		} else {
			// use the "first" release if no release was specified
			ProjectDto project = getProject();
			if(project != null) {
				release = agileSupport.findFirstRelease(getUser(), project.getId());
			}
		}

		if ((release != null) && ((release.getTracker() == null) || !release.getTracker().isVersionCategory())) {
			binder.getBindingResult().addError(new ObjectError("command", "Unsupported tracker of release " + releaseId)); // TODO better message
			return null;
		}

		return release;
	}
}
