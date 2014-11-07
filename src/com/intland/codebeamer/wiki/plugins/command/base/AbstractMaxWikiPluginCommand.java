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
package com.intland.codebeamer.wiki.plugins.command.base;

/**
 * Command with support for the "max" parameter.
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public abstract class AbstractMaxWikiPluginCommand extends AbstractWikiPluginCommand {
	/**
	 * Default maximum number of elements listed in plugins.
	 */
	public final static Integer DEFAULT_MAX_ELEMENTS = Integer.valueOf(20);

	/**
	 * Maximum number of elements to show by the plugin.
	 */
	private Integer max = DEFAULT_MAX_ELEMENTS;

	public final Integer getMax() {
		return max;
	}

	public void setMax(Integer max) {
		this.max = max;
		if (max == null || max.intValue() <= 0) {
			max = DEFAULT_MAX_ELEMENTS; // ignoring not sensible value
		}
	}
}
