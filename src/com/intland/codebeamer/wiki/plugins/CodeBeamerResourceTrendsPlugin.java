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

import java.util.HashMap;
import java.util.Map;

import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.chart.renderer.CodeBeamerResourceTrendsRenderer;
import com.intland.codebeamer.utils.AnchoredPeriod;
import com.intland.codebeamer.utils.CalendarUnit;
import com.intland.codebeamer.utils.AnchoredPeriod.Anchor;
import com.intland.codebeamer.wiki.plugins.base.AbstractTimeIntervalCommandWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.ChartSupport;
import com.intland.codebeamer.wiki.plugins.base.DefaultWebBindingInitializer;
import com.intland.codebeamer.wiki.plugins.command.CodeBeamerResourceTrendsCommand;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractTimeIntervalDisplayOptionCommand.TimePeriodGrouping;

/**
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 * @version $Id$
 */
public class CodeBeamerResourceTrendsPlugin extends AbstractTimeIntervalCommandWikiPlugin<CodeBeamerResourceTrendsCommand> {
	public CodeBeamerResourceTrendsPlugin() {
		setWebBindingInitializer(new CodeBeamerResourceTrendsBindingInitializer());
	}

	public static class CodeBeamerResourceTrendsBindingInitializer extends DefaultWebBindingInitializer {
		public CodeBeamerResourceTrendsBindingInitializer() {
			addPropertyEditorRegistrar(new TimeIntervalPropertyEditorRegistrar());
		}
	}

	@Override
	public CodeBeamerResourceTrendsCommand createCommand() throws PluginException {
		return new CodeBeamerResourceTrendsCommand();
	}

	@Override
	protected Map populateModel(DataBinder binder, CodeBeamerResourceTrendsCommand command, Map params) throws PluginException {
		Map model = new HashMap();

		model.put("chartSupport", new ChartSupport(this, command, new CodeBeamerResourceTrendsRenderer()));
		model.put("command", command);
		model.put("standardTimeIntervalsAndGrouping", SuppertedTimeIntervalsAndGrouping.values());

		return model;
	}

	public static enum SuppertedTimeIntervalsAndGrouping {
		LAST_DAY(AnchoredPeriod.YESTERDAY, "last day"),
		LAST_3_DAYs(new AnchoredPeriod(Anchor.Last, 3, CalendarUnit.Day), "last 3 days"),
		LAST_7_DAYS(new AnchoredPeriod(Anchor.Last, 7, CalendarUnit.Day), "last 7 days");

		private AnchoredPeriod period;
		private String decription;

		private SuppertedTimeIntervalsAndGrouping(AnchoredPeriod period, String decription) {
			this.period = period;
			this.decription = decription;
		}

		public AnchoredPeriod getPeriod() {
			return period;
		}

		public TimePeriodGrouping getGrouping() {
			return TimePeriodGrouping.DAILY;
		}

		public String getDecription() {
			return decription;
		}

		/**
		 * Get the url params will contain the "since" and "grouping" values selected by this enum
		 * @return
		 */
		public String getUrlParams() {
			return "period=" + period +"&grouping=" + getGrouping().toString();
		}
	}

	@Override
	protected String getTemplateFilename() {
		return "codebeamerresourcetrends-plugin.vm";
	}
}
