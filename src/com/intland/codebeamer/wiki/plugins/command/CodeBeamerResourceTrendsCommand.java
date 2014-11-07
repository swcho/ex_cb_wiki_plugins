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

import javax.validation.constraints.AssertTrue;

import com.intland.codebeamer.utils.AnchoredPeriod;
import com.intland.codebeamer.utils.CalendarUnit;
import com.intland.codebeamer.utils.AnchoredPeriod.Anchor;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractTimeIntervalDisplayOptionCommand;

/**
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 */
public class CodeBeamerResourceTrendsCommand extends AbstractTimeIntervalDisplayOptionCommand {

	public CodeBeamerResourceTrendsCommand() {
		super();

		setPeriod(new AnchoredPeriod(Anchor.Last, 1, CalendarUnit.Day));
		setHeight(400);
		setWidth(1000);
	}

	@AssertTrue(message = "{resource.trend.period.supported}")
	public boolean isPeriodSupported() {
		AnchoredPeriod prd = getPeriod();
		Anchor anchor = prd.getAnchor();
		int size = prd.getSize();
		return (anchor == Anchor.Current || anchor == Anchor.Last || anchor == Anchor.Past)
		       && (size == 1 || size == 2 || size == 3 || size == 7) && prd.getUnit().equals(CalendarUnit.Day);
	}
}
