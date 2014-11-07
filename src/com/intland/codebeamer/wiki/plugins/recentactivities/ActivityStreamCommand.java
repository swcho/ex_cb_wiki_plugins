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
package com.intland.codebeamer.wiki.plugins.recentactivities;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.validation.constraints.AssertFalse;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;

import com.intland.codebeamer.controller.support.CommaStringLowerCaseNameBasedEnumSetEditor;
import com.intland.codebeamer.controller.support.CommaStringToIntegerListPropertyEditor;
import com.intland.codebeamer.controller.support.CommaStringToStringListPropertyEditor;
import com.intland.codebeamer.utils.AnchoredPeriod;
import com.intland.codebeamer.utils.CalendarUnit;
import com.intland.codebeamer.wiki.plugins.base.AbstractTimeIntervalCommandWikiPlugin.TimeIntervalPropertyEditorRegistrar;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractMaxWikiPluginCommand;

/**
 * Command bean for RecentActivities plugin.
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class ActivityStreamCommand extends AbstractMaxWikiPluginCommand implements PropertyEditorRegistrar {

	/**
	 * List of project-ids to search for.
	 */
	private List<Integer> projectId;
	/**
	 * Projects tagged with these will be the input.
	 */
	private List<String> tag;
	/**
	 * Period to show activities for
	 */
	private AnchoredPeriod period = new AnchoredPeriod(AnchoredPeriod.Anchor.Last, 7, CalendarUnit.Day);

	/**
	 * Entity types of which the activities should be displayed.
	 */
	private Set<EntitiesFilter> filter = EnumSet.allOf(EntitiesFilter.class);

	/**
	 * If invalid EntitiesFilter values are just ignored
	 */
	protected boolean ignoreInvalidEntitiesFilters = false;

	public List<Integer> getProjectId() {
		return projectId;
	}

	public void setProjectId(List<Integer> projectId) {
		this.projectId = projectId;
	}

	public List<String> getTag() {
		return tag;
	}

	public void setTag(List<String> tag) {
		this.tag = tag;
	}

	/**
	 * Get the period from where to show the most recent activities
	 * @return the period from where to show the most recent activities, or null, consider whose project lifetime.
	 */
	public AnchoredPeriod getPeriod() {
		return period;
	}

	/**
	 * Set the period from where to show the most recent activities
	 * @param period is the period from where to show the most recent activities, or null, to consider whose project lifetime
	 */
	public void setPeriod(AnchoredPeriod period) {
		this.period = period;
	}

	public Set<EntitiesFilter> getFilter() {
		return filter;
	}

	public void setFilter(Set<EntitiesFilter> filter) {
		this.filter = filter;
	}

	/**
	 * Get the absolute range/edges of the period (according to {@link #getPeriod()} from where to show the most recent activities.
	 * @param relativeTo is the anchor date of the reporting period, or null to use current system date
	 * @param timezone is the time zone for a user specific reporting period, or null to use default/server time zone
	 * @return an array with the absolute begin and end date of the reporting/chart period <code>[begin .. end)</code>, or null to show recent activities from whole project lifetime
	 */
	public Date[] getRange(Date relativeTo, TimeZone timezone) {
		return period != null ? period.getEdges(relativeTo, timezone) : null;
	}

	@AssertFalse(message = "{activity.stream.onlyOneOfProjectIdOrTag}")
	public boolean isOnlyOneOfProjectIdOrTagValid() {
		boolean isProjectIdValid = !CollectionUtils.isEmpty(projectId);
		boolean isTagValid = !CollectionUtils.isEmpty(tag);

		return isProjectIdValid && isTagValid;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.PropertyEditorRegistrar#registerCustomEditors(org.springframework.beans.PropertyEditorRegistry)
	 */
	public void registerCustomEditors(PropertyEditorRegistry registry) {
		new TimeIntervalPropertyEditorRegistrar().registerCustomEditors(registry);
		registry.registerCustomEditor(List.class, "projectId", new CommaStringToIntegerListPropertyEditor());
		registry.registerCustomEditor(List.class, "tag", new CommaStringToStringListPropertyEditor());

		CommaStringLowerCaseNameBasedEnumSetEditor entitiesFilterEditor = new CommaStringLowerCaseNameBasedEnumSetEditor(EntitiesFilter.class);
		entitiesFilterEditor.setSkipInvalidEnumValues(ignoreInvalidEntitiesFilters);
		registry.registerCustomEditor(Set.class, entitiesFilterEditor);
	}

}
