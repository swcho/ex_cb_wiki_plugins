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

import javax.validation.constraints.AssertTrue;

import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.Range;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;

import com.intland.codebeamer.controller.support.LowerCaseNameBasedEnumEditor;
import com.intland.codebeamer.wiki.plugins.command.enums.DisplayType;

/**
 * Command mixin with table/chart display options.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public abstract class AbstractDisplayOptionCommand extends AbstractWikiPluginCommand implements PropertyEditorRegistrar{
	final public static int WIDTH_DEFAULT_VALUE = 400;
	final public static int HEIGHT_DEFAULT_VALUE = 200;

	/**
	 * Optional tooltip appears as tooltip when mouse hovers over the image.
	 */
	private String tooltip = null;
	/**
	 * Whether to display chart or table or both.
	 */
	private DisplayType display = DisplayType.CHART;
	@Range(min = 20, max = 2000)
	private int width = WIDTH_DEFAULT_VALUE;
	@Range(min = 20, max = 1000)
	private int height = HEIGHT_DEFAULT_VALUE;

	/**
	 * Returns the tooltip or the title if no tooltip was specified.
	 */
	public String getTooltip() {
		if (StringUtils.isEmpty(tooltip)) {
			return getTitle();
		}
		return tooltip;
	}

	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

	public boolean isTable() {
		return display.equals(DisplayType.TABLE) || display.equals(DisplayType.BOTH);
	}

	public boolean isChart() {
		return display.equals(DisplayType.CHART) || display.equals(DisplayType.BOTH);
	}

	public void setDisplay(DisplayType display) {
		this.display = display;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	/**
	 * Forces that "width" and "height" can only be used when charts are enabled via "display".
	 */
	@AssertTrue(message = "{chart.geometry.assertion}")
	public boolean isWidthAndHeightWithCharts() {
		if((width != WIDTH_DEFAULT_VALUE) || (height != HEIGHT_DEFAULT_VALUE)) {
			return isChart();
		}

		return true;
	}

	public void registerCustomEditors(PropertyEditorRegistry registry) {
		registry.registerCustomEditor(DisplayType.class, new LowerCaseNameBasedEnumEditor(DisplayType.class));
	}
}
