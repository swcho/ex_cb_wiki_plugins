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
package com.intland.codebeamer.wiki.plugins.bubble;

import com.intland.codebeamer.wiki.plugins.command.base.AbstractTimeIntervalDisplayOptionCommand;

/**
 * @author <a href="mailto:attila.banfi@intland.com">Attila Banfi</a>
 */
public class BubbleVisualizationCommand extends AbstractTimeIntervalDisplayOptionCommand {

	private Integer releaseId;
	private Integer xAxisFieldId;
	private Integer yAxisFieldId;
	private String xAxisLabel;
	private String yAxisLabel;
	private Integer bubbleSizeFieldId;
	private boolean showClosed;

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

	public final Integer getxAxisFieldId() {
		return xAxisFieldId;
	}

	public final Integer getyAxisFieldId() {
		return yAxisFieldId;
	}

	public final Integer getBubbleSizeFieldId() {
		return bubbleSizeFieldId;
	}

	public final void setxAxisFieldId(Integer xAxisFieldId) {
		this.xAxisFieldId = xAxisFieldId;
	}

	public final void setyAxisFieldId(Integer yAxisFieldId) {
		this.yAxisFieldId = yAxisFieldId;
	}

	public final void setBubbleSizeFieldId(Integer bubbleSizeFieldId) {
		this.bubbleSizeFieldId = bubbleSizeFieldId;
	}

	public final boolean isShowClosed() {
		return showClosed;
	}

	public final void setShowClosed(boolean showClosed) {
		this.showClosed = showClosed;
	}

	public final String getxAxisLabel() {
		return xAxisLabel;
	}

	public final String getyAxisLabel() {
		return yAxisLabel;
	}

	public final void setxAxisLabel(String xAxisLabel) {
		this.xAxisLabel = xAxisLabel;
	}

	public final void setyAxisLabel(String yAxisLabel) {
		this.yAxisLabel = yAxisLabel;
	}
}
