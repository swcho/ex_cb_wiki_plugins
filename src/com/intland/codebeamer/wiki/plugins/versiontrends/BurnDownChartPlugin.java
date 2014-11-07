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
package com.intland.codebeamer.wiki.plugins.versiontrends;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import com.intland.codebeamer.persistence.dto.TrackerTypeDto;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.chart.ChartDataCalculator;
import com.intland.codebeamer.chart.renderer.AbstractChartRendererTemplate;
import com.intland.codebeamer.chart.renderer.BurnDownChartRenderer;
import com.intland.codebeamer.manager.TrackerItemManager;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.TrackerItemStatsDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.wiki.plugins.ajax.AbstractAjaxRefreshingWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.AbstractTimeIntervalCommandWikiPlugin.TimeIntervalBindingInitializer;
import com.intland.codebeamer.wiki.plugins.base.ChartSupport;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractTimeIntervalDisplayOptionCommand.StandardTimeIntervalsAndGrouping;
import com.intland.codebeamer.wiki.plugins.command.enums.DisplayType;

/**
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 */
public class BurnDownChartPlugin extends AbstractAjaxRefreshingWikiPlugin<BurnDownChartCommand> {
	@Autowired
	private ChartDataCalculator chartDataCalculator;
	@Autowired
	private TrackerItemManager trackerItemManager;

	public BurnDownChartPlugin() {
		setWebBindingInitializer(new BurnDownChartPluginBindingInitializer());
	}

	public static class BurnDownChartPluginBindingInitializer extends TimeIntervalBindingInitializer {
	}

	@Override
	protected void validate(DataBinder binder, BurnDownChartCommand command, Map params) throws NamedPluginException {
		TrackerItemDto release = discoverRelease(getUser(), command.getId(), binder);

		if(release != null) {
			command.setReleaseId(release.getId());
		}
		command.setDisplay(DisplayType.BOTH); // "table" is used to display warning messages

		super.validate(binder, command, params);
	}

	@Override
	public BurnDownChartCommand createCommand() throws PluginException {
		return new BurnDownChartCommand();
	}

	@Override
	protected Map populateModelInternal(DataBinder binder, BurnDownChartCommand command, Map params) throws PluginException {
		UserDto user = getUser();
		TrackerItemDto release = trackerItemManager.findById(user, command.getReleaseId());
		SortedMap<Date,TrackerItemStatsDto> data = chartDataCalculator.getSelectiveReleaseIssueStats(user, release, Collections.singleton(TrackerTypeDto.TESTRUN), true);

		Map model = new HashMap();
		model.put("command", command);
		model.put("release", release);
		if ((release != null) && (release.getStartDate() != null) && (release.getEndDate() != null) &&
				(DateUtils.isSameDay(release.getStartDate(), release.getEndDate())
					|| release.getStartDate().compareTo(release.getEndDate()) > 0)) {
			model.put("user", getUser());
			model.put("invalidReleaseDates", Boolean.TRUE);
		} else if (data.size() >= 2) {
			model.put("standardTimeIntervalsAndGrouping", StandardTimeIntervalsAndGrouping.values());
			model.put("chartSupport", new ChartSupport(this, command, getChartRenderer()));
		} else {
			model.put("daysToWait", Integer.valueOf(2 - data.size()));
		}

		return model;
	}

	/**
	 * This is a template method to return the chart renderer.
	 */
	protected AbstractChartRendererTemplate<BurnDownChartCommand, ?> getChartRenderer() {
		return new BurnDownChartRenderer();
	}

	@Override
	protected String getTemplateFilename() {
		return "burn-down-chart-plugin.vm";
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
}
