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
package com.intland.codebeamer.wiki.plugins.base;

import java.text.SimpleDateFormat;
import java.util.Date;


import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.propertyeditors.CustomDateEditor;

import com.intland.codebeamer.controller.support.LowerCaseNameBasedEnumEditor;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractTimeIntervalDisplayOptionCommand;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractTimeIntervalDisplayOptionCommand.TimePeriodGrouping;

/**
 * Mixin for command wiki plugins, with support for "time interval" parameters,
 * like <code>startDate</code>, <code>endDate</code>, <code>since</code> and such.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public abstract class AbstractTimeIntervalCommandWikiPlugin<Command extends AbstractTimeIntervalDisplayOptionCommand> extends AbstractCommandWikiPlugin<Command> {
	/**
	 * Pattern for all date-type parameters.
	 */
	public final static String YMD_DATEFORMAT_PATTERN = "yyyy-MM-dd";

	public AbstractTimeIntervalCommandWikiPlugin() {
		setWebBindingInitializer(new TimeIntervalBindingInitializer());
	}

	public static class TimeIntervalPropertyEditorRegistrar implements PropertyEditorRegistrar {
		public void registerCustomEditors(PropertyEditorRegistry registry) {
			registry.registerCustomEditor(Date.class, new CustomDateEditor(new SimpleDateFormat(YMD_DATEFORMAT_PATTERN), true));
			registry.registerCustomEditor(TimePeriodGrouping.class, new LowerCaseNameBasedEnumEditor(TimePeriodGrouping.class));
		}
	}

	public static class TimeIntervalBindingInitializer extends DefaultWebBindingInitializer {
		public TimeIntervalBindingInitializer() {
			addPropertyEditorRegistrar(new TimeIntervalPropertyEditorRegistrar());
		}
	}
}
