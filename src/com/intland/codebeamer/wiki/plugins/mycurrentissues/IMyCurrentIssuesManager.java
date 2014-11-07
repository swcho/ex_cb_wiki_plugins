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
package com.intland.codebeamer.wiki.plugins.mycurrentissues;

import java.util.List;

import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.UserDto;

/**
 * Interface for manager finding the current-issues for the {@link MyCurrentIssuesPlugin}
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 *
 */
public interface IMyCurrentIssuesManager {

	/**
	 * Find the issues assigned to the user, and not in the (closed, resolved) status.
	 * @param user whose issues to find
	 * @param command contains optional selection parameters.
	 * @return The issues ordered by priority desc, lastModifiedAt desc
     */
	List<TrackerItemDto> findIssues(UserDto user, MyCurrentIssuesCommand command);


}
