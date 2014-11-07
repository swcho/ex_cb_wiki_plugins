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
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;

import com.intland.codebeamer.controller.TrackerItemFlagsOption;
import com.intland.codebeamer.persistence.dao.TrackerItemDao;


/**
 * Plugin to display/select open items on all projects by different criterias.
 *
 * This plugin is only kept for backwards compatibility and functionally equivalent with {@link MyIssueSummaryPlugin},
 * except for the default value for the parameter <code>show</code>, which is <code>"Open"</code>
 * (as the plugin name implies).
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 * @deprecated since CB-5.5 replaced by MyIssueSummaryPlugin
 */
public class MyOpenTrackerItemsPlugin extends MyIssueSummaryPlugin {
	@Override
	protected TrackerItemFlagsOption getFlagsParameter(Map params, String paramId, String defaultValue) {
		return super.getFlagsParameter(params, paramId, "Open");
	}

	@Override
	protected List<Selection> makeFilters(MessageSource messageSource, Locale locale) {
		List<Selection> result = new ArrayList<Selection>();
		Selection sel;

		sel = new Selection(messageSource.getMessage("issue.filter.all.label", null, "All", locale), "");
		sel.addParametersPair(ITEMS_FILTER, Integer.valueOf(TrackerItemDao.TF_NOFILTER));
		result.add(sel);

		sel = new Selection(messageSource.getMessage("issue.filter.overdue.label", null, "Overdue", locale), "Overdue");
		sel.addParametersPair(ITEMS_FILTER, Integer.valueOf(TrackerItemDao.TF_OVERDUE));
		result.add(sel);

		sel = new Selection(messageSource.getMessage("issue.filter.today_and_overdue.label", null, "Today Overdue", locale), "TodayAndOverdue");
		sel.addParametersPair(ITEMS_FILTER, Integer.valueOf(TrackerItemDao.TF_TODAY_AND_OVERDUE));
		result.add(sel);

		sel = new Selection(messageSource.getMessage("issue.filter.today.label", null, "Today", locale), "Today");
		sel.addParametersPair(ITEMS_FILTER, Integer.valueOf(TrackerItemDao.TF_TODAY));
		result.add(sel);

		sel = new Selection(messageSource.getMessage("issue.filter.tomorrow.label", null, "Tomorrow", locale), "Tomorrow");
		sel.addParametersPair(ITEMS_FILTER, Integer.valueOf(TrackerItemDao.TF_TOMORROW));
		result.add(sel);

		sel = new Selection(messageSource.getMessage("issue.filter.next_7_days_and_overdue.label", null, "Next 7 Days Overdue", locale), "Next7DaysAndOverdue");
		sel.addParametersPair(ITEMS_FILTER, Integer.valueOf(TrackerItemDao.TF_NEXT_7_DAYS_AND_OVERDUE));
		result.add(sel);

		sel = new Selection(messageSource.getMessage("issue.filter.next_7_days.label", null, "Next 7 Days", locale), "Next7Days");
		sel.addParametersPair(ITEMS_FILTER, Integer.valueOf(TrackerItemDao.TF_NEXT_7_DAYS));
		result.add(sel);

		sel = new Selection(messageSource.getMessage("issue.filter.current_month.label", null, "This Month", locale), "CurrentMonth");
		sel.addParametersPair(ITEMS_FILTER, Integer.valueOf(TrackerItemDao.TF_CURRENT_MONTH));
		result.add(sel);

		return result;
	}
}
