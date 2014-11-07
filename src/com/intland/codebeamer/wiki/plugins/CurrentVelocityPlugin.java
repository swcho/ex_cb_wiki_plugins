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

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedMap;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.chart.ChartDataCalculator;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.TrackerItemStatsDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.wiki.plugins.base.AbstractAgileWikiPlugin;

/**
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public class CurrentVelocityPlugin extends AbstractAgileWikiPlugin<CurrentVelocityCommand> {
	@Autowired
	private ChartDataCalculator chartDataCalculator;

	@Override
	public CurrentVelocityCommand createCommand() throws PluginException {
		return new CurrentVelocityCommand();
	}

	@Override
	protected Map populateModel(DataBinder binder, CurrentVelocityCommand command, Map params) throws PluginException {
		UserDto user = getUser();
		TrackerItemDto release = discoverRelease(getUser(), command.getId(), binder);
		SortedMap<Date,TrackerItemStatsDto> data = chartDataCalculator.getReleaseIssueStats(user, release, null, true);
		Pair<Date, Integer> lastKnownData = getLastKnownVelocity(data);

		Map model = new HashMap();
		model.put("command", command);
		model.put("release", release);
		model.put("lastKnownDate", lastKnownData.getLeft());
		model.put("lastKnownVelocity", lastKnownData.getRight());

		return model;
	}

	@Override
	protected String getTemplateFilename() {
		return "currentVelocity-plugin.vm";
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}

	/**
	 * Computes the last know Velocity from the passed stats.
	 * (It does not check whether data contains recent or old information, and doesn't check if the two dates used
	 * for the computation are consecutive. It assumes that things are uptodate and continuous.)
	 */
	private Pair<Date, Integer> getLastKnownVelocity(SortedMap<Date,TrackerItemStatsDto> data) {
		LinkedList<Date> dates = new LinkedList<Date>(data.keySet());

		Date lastKnownDate = null;
		Date dateBeforeLastKnownDate = null;
		for(Iterator<Date> it = dates.descendingIterator(); it.hasNext();) {
			if(lastKnownDate == null) {
				lastKnownDate = it.next();
			} else if(dateBeforeLastKnownDate == null) {
				dateBeforeLastKnownDate = it.next();
			} else {
				break;
			}
		}

		return Pair.of(lastKnownDate, ((lastKnownDate != null) && (dateBeforeLastKnownDate != null)) ? Integer.valueOf(data.get(dateBeforeLastKnownDate).getOpenItems() - data.get(lastKnownDate).getOpenItems()) : null);
	}
}
