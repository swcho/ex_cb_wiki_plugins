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
package com.intland.codebeamer.wiki.plugins.releaseactivitytrends;

import com.intland.codebeamer.wiki.plugins.command.base.AbstractWikiPluginCommand;

/**
 * Command class for plugin to display the project related tracker item counts grouped by releases.
 *
 * @author <a href="mailto:levente.cseko@intland.com">Levente Cseko</a>
 * @version $Id$
 */
public class ReleaseActivityTrendsCommand extends AbstractWikiPluginCommand {

	private Integer projectId;

	private String trackerItemStatus = ReleaseActivityTrendsManager.DEFAULT_TRACKER_ITEM_STATUS;

	private String releaseStatus = ReleaseActivityTrendsManager.DEFAULT_RELEASE_STATUS;

	private Boolean showUnscheduledItems = ReleaseActivityTrendsManager.DEFAULT_SHOW_UNSCHEDULED_ITEMS;

	private Boolean showBurnDownChart = ReleaseActivityTrendsManager.DEFAULT_SHOW_BURN_DOWN_CHART;

	private Boolean showDropdownFilters = ReleaseActivityTrendsManager.DEFAULT_SHOW_DROPDOWN_FILTERS;

	public Integer getProjectId() {
		return projectId;
	}

	public void setProjectId(Integer projectId) {
		this.projectId = projectId;
	}

	public String getTrackerItemStatus() {
		return trackerItemStatus;
	}

	public void setTrackerItemStatus(String trackerItemStatus) {
		this.trackerItemStatus = trackerItemStatus;
	}

	public String getReleaseStatus() {
		return releaseStatus;
	}

	public void setReleaseStatus(String releaseStatus) {
		this.releaseStatus = releaseStatus;
	}

	public Boolean getShowUnscheduledItems() {
		return showUnscheduledItems;
	}

	public void setShowUnscheduledItems(Boolean showUnscheduledItems) {
		this.showUnscheduledItems = showUnscheduledItems;
	}

	public Boolean getShowBurnDownChart() {
		return showBurnDownChart;
	}

	public void setShowBurnDownChart(Boolean showBurnDownChart) {
		this.showBurnDownChart = showBurnDownChart;
	}

	public Boolean getShowDropdownFilters() {
		return showDropdownFilters;
	}

	public void setShowDropdownFilters(Boolean showDropdownFilters) {
		this.showDropdownFilters = showDropdownFilters;
	}

}
