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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import com.intland.codebeamer.utils.AnchoredPeriod;
import com.intland.codebeamer.utils.CalendarUnit;
import com.intland.codebeamer.utils.URLCoder;
import com.intland.codebeamer.utils.AnchoredPeriod.Edge;
import com.intland.codebeamer.wiki.plugins.base.BeanToQueryParametersConverter.Ignored;

/**
 * Command mixin with "time interval" and table/chart display options.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public abstract class AbstractTimeIntervalDisplayOptionCommand extends AbstractDisplayOptionCommand implements Cloneable {

	/**
	 * Specifies time period grouping (granularity) for data items.
	 */
	public static enum TimePeriodGrouping {
		DAILY   (CalendarUnit.Day),
		WEEKLY  (CalendarUnit.Week),
		MONTHLY (CalendarUnit.Month);

		private CalendarUnit unit;

		private TimePeriodGrouping(CalendarUnit unit) {
			this.unit = unit;
		}

		/**
		 * Get the calendar unit per grouping period
		 * @return {@link CalendarUnit.Day}, {@link CalendarUnit.Week} or {@link CalendarUnit.Month}
		 */
		public CalendarUnit getUnit() {
			return unit;
		}

		@Override
		public String toString() {
			return super.toString().toLowerCase();
		}

		/**
		 * Get the (begin date of the) grouping period where the specified date belongs to (in the specified time zone)
		 * @param date is the date whose grouping period (begin date) to find (must be already truncated to calendar day 00:00:00)
		 * @param timezone is the time zone for a user specific reporting period, or null to use default/server time zone
		 * @return (the begin of) the grouping period the specified date falls into
		 */
		public Date getPeriodBegin(Date date, TimeZone timezone) {
			Date result = date;
			CalendarUnit groupBy = getUnit();

			if (date != null && !CalendarUnit.Day.equals(groupBy)) {
				result = groupBy.getBegin(date, timezone).getTime();
			}
			return result;
		}
	}

	/**
	 * The standard/sensible combinations of the "since" and "grouping".
	 *
	 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
	 */
	public static enum StandardTimeIntervalsAndGrouping {
		THIS_WEEK_BY_DAY		("This week",		TimePeriodGrouping.DAILY, 	"This week"),
		THIS_MONTH_BY_DAY		("This month",		TimePeriodGrouping.DAILY, 	"This month"),
		THIS_QUARTER_BY_DAY		("This quarter",	TimePeriodGrouping.DAILY, 	"This quarter"),
		THIS_YEAR_BY_MONTH		("This year", 		TimePeriodGrouping.MONTHLY, "This year"),
		PAST_7_DAYS_BY_DAY		("Past 7 days",		TimePeriodGrouping.DAILY, 	"Past 7 days"),
		PAST_30_DAYS_BY_DAY		("Past 30 days",	TimePeriodGrouping.DAILY, 	"Past 30 days"),
		PAST_60_DAYS_BY_DAY		("Past 60 days",	TimePeriodGrouping.DAILY, 	"Past 60 days"),
		LAST_4_WEEKS_BY_WEEK	("Last 4 weeks",	TimePeriodGrouping.WEEKLY,  "Last 4 weeks"),
		LAST_13_WEEKS_BY_WEEK	("Last 13 weeks",	TimePeriodGrouping.WEEKLY,  "Last 13 weeks"),
		LAST_52_WEEKS_BY_WEEK	("Last 52 weeks",	TimePeriodGrouping.WEEKLY,  "Last 52 weeks"),
		LAST_12_MONTHS_BY_MONTH	("Last 12 months",	TimePeriodGrouping.MONTHLY, "Last 12 months"),
		LAST_36_MONTHS_BY_MONTH	("Last 36 months",	TimePeriodGrouping.MONTHLY, "Last 36 months");

		private AnchoredPeriod period;
		private TimePeriodGrouping grouping;
		private String decription;

		private StandardTimeIntervalsAndGrouping(String definition, TimePeriodGrouping grouping, String description) {
			this.period = AnchoredPeriod.decode(definition);
			this.grouping = (grouping != null ? grouping : TimePeriodGrouping.DAILY);
			this.decription = (description != null ? description : definition);
		}

		public AnchoredPeriod getPeriod() {
			return period;
		}

		public TimePeriodGrouping getGrouping() {
			return grouping;
		}

		public String getDecription() {
			return decription;
		}

		/**
		 * Get the url params will contain the "since" and "grouping" values selected by this enum
		 * @return
		 */
		public String getUrlParams() {
			return "period=" + URLCoder.encode(period.toString()) +"&grouping=" + grouping.toString();
		}
	}


	// Default since/grouping pair
	private final static StandardTimeIntervalsAndGrouping DEFAULT = StandardTimeIntervalsAndGrouping.PAST_30_DAYS_BY_DAY;

	@Ignored
	protected AnchoredPeriod period;

	@Ignored
	protected TimePeriodGrouping grouping;

	/**
	 * Get the chart/reporting period
	 * @return the chart/reporting period
	 */
	public AnchoredPeriod getPeriod() {
		return period != null ? period : DEFAULT.getPeriod();
	}

	/**
	 * Set the number of reporting periods (in the granularity defined by {@link #getGrouping()}).
	 * E.g. if {@link #grouping} were {@link TimePeriodGrouping.WEEKLY}, this would be the number of weeks to show
	 * @param period is the chart/reporting period
	 */
	public void setPeriod(AnchoredPeriod period) {
		this.period = period;
	}

	/**
	 * Get the granularity of the reporting periods. The default is {@link TimePeriodGrouping.DAILY}
	 * @return the granularity of the reporting periods
	 */
	public TimePeriodGrouping getGrouping() {
		return grouping != null ? grouping : DEFAULT.getGrouping();
	}

	/**
	 * Set the granularity of the reporting periods. This is also the unit for {@link since}
	 * @param grouping is the granularity of the reporting periods
	 */
	public void setGrouping(TimePeriodGrouping grouping) {
		this.grouping = grouping;
	}

	/**
	 * Get the absolute range/edges of the reporting/chart period (according to {@link #getPeriod()} and {@link #getGrouping()}).<p>
	 * Note: The begin of the returned range will be the begin of the first {@link #getGrouping()}) period, where the begin of the specified
	 * {@link #getPeriod()} falls within, and can therefore be before the user-specified begin. The end of the returned period will be the
	 * minimum of the user-defined period and the end of the current day.
	 * @param relativeTo is the anchor date of the reporting period, or null to use current system date
	 * @param timezone is the time zone for a user specific reporting period, or null to use default/server time zone
	 * @return an array with the absolute begin and end date of the reporting/chart period <code>[begin .. end)</code>
	 */
	public Date[] getRange(Date relativeTo, TimeZone timezone) {
		Date[] range = getPeriod().getEdges(relativeTo, timezone);

		// Expand begin to the begin of the first grouping unit
		range[0] = getPeriod(range[0], timezone);

		// Truncate end of the range to end of current day
		Date today = Edge.End.of(AnchoredPeriod.TODAY, relativeTo, timezone);
		if (range[1].after(today)) {
			range[1] = today;
		}
		return range;
	}

	/**
	 * Get a list with the begins of all reporting/chart periods (according to {@link #getPeriod()} and {@link #getGrouping()})
	 */
	public List<Date> getDatesInRange() {
		return getPeriods(null, null);
	}

	/**
	 * Get a list with the begins of all reporting periods of this granularity relative to the specified date (in the specified time zone)
	 * @param relativeTo is the anchor date of the reporting period, or null to use current system date
	 * @param timezone is the time zone for a user specific reporting period, or null to use default/server time zone
	 * @return a List with the begins of all reporting periods, ordered ascending by date
	 */
	public List<Date> getPeriods(Date relativeTo, TimeZone timezone) {
		List<Date> dates = Collections.emptyList();

		Date[] range = getRange(relativeTo, timezone);
		if (range[0].before(range[1])) {
			CalendarUnit groupBy = getGrouping().getUnit();
			Calendar cal = groupBy.getBegin(range[0], timezone);

			dates = new ArrayList<Date>();
			for (Date date = cal.getTime(); date.before(range[1]); date = groupBy.shift(cal, 1).getTime()) {
				dates.add(date);
			}
		}
		return dates;
	}

	/**
	 * Get the (begin date of the) grouping period where the specified date belongs to (in the specified time zone)
	 * @param date is the date whose grouping period (begin date) to find (must be already truncated to calendar day 00:00:00)
	 * @param timezone is the time zone for a user specific reporting period, or null to use default/server time zone
	 * @return (the begin of) the grouping period the specified date falls into
	 */
	public Date getPeriod(Date date, TimeZone timezone) {
		return getGrouping().getPeriodBegin(date, timezone);
	}

	/**
	 * Get the (begin date of the) grouping period the specified date falls into
	 * @param date whose grouping period date to find
	 * @return the (begin date of the) grouping period the specified date falls into
	 */
	public Date getGroup(Date date) {
		return getPeriod(date, null);
	}

	public final static void clearHours(Calendar date) {
		date.set(Calendar.HOUR_OF_DAY, 0);
		date.set(Calendar.MINUTE, 0);
		date.set(Calendar.SECOND, 0);
		date.set(Calendar.MILLISECOND, 0);
	}

	@Override
	public AbstractTimeIntervalDisplayOptionCommand clone() throws CloneNotSupportedException {
		return (AbstractTimeIntervalDisplayOptionCommand) super.clone();
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
}
