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
package com.intland.codebeamer.wiki.plugins.tasq;

import java.util.List;

import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.utils.MultiValue;

/**
 * Manager for {@link TaskQPlugin}
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public interface ITaskQManager {

	/**
	 * Return tasks/issues for a user in a "queue" (with certain statuses).
	 * Sorted by: priority desc, est. time desc, summary desc.
	 *
	 * @param user The user who is executing the query. NOT the user whose issues are displayed!
	 * @param command The plugin's parameters
	 *
	 * @return 2 values: the left is the issues found & matching (limited by max), the right is the total number of matching issues
	 */
	MultiValue<List<TrackerItemDto>, Integer> findIssues(UserDto user, UserDto targetUser, TaskQCommand command);

}
