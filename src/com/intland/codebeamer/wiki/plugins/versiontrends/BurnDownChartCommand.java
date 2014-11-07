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

import com.intland.codebeamer.wiki.plugins.command.base.AbstractTimeIntervalDisplayOptionCommand;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 */
public class BurnDownChartCommand extends AbstractTimeIntervalDisplayOptionCommand {
	private Integer releaseId;
	private boolean showBurnDown = true;
	private boolean showVelocity = false;
	private boolean showNew = false;
	private boolean showChartOnly = false;
	private boolean disableLegend = false;

	@Override
	public List<Date> getDatesInRange() {
		// If there is no period and grouping, don't use default range because it makes no sense but will truncate
		// old data when the burn down chart is being calculated.
		// See com.intland.codebeamer.chart.renderer.BurnDownChartRenderer.createJFreeChartDataSet()
		return period == null && grouping == null ? Collections.<Date>emptyList() : super.getDatesInRange();
	}

	public Integer getReleaseId() {
		return releaseId;
	}

	public void setReleaseId(Integer releaseId) {
		this.releaseId = releaseId;
	}

	public Integer getId() {
		return getReleaseId();
	}

	public void setId(Integer id) {
		setReleaseId(id);
	}

	public boolean isShowBurnDown() {
		return showBurnDown;
	}

	public void setShowBurnDown(boolean showBurnDown) {
		this.showBurnDown = showBurnDown;
	}

	public boolean isShowVelocity() {
		return showVelocity;
	}

	public void setShowVelocity(boolean showVelocity) {
		this.showVelocity = showVelocity;
	}

	public boolean isShowNew() {
		return showNew;
	}

	public void setShowNew(boolean showNew) {
		this.showNew = showNew;
	}

	public boolean isShowChartOnly() {
		return showChartOnly;
	}

	public void setShowChartOnly(boolean showChartOnly) {
		this.showChartOnly = showChartOnly;
	}

	public boolean isDisableLegend() {
		return disableLegend;
	}

	public void setDisableLegend(boolean disableLegend) {
		this.disableLegend = disableLegend;
	}
}
