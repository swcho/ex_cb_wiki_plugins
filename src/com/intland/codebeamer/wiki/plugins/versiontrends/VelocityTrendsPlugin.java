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

import java.util.Map;

import org.springframework.validation.DataBinder;

import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;

/**
 * As the functionality of this plugin is a subset of that of {@link BurnDownChartPlugin}, this is
 * implemented in the following way: all functionality is kept in BurnDownChart, it just overrides the
 * parametrization and uses a separate Velocity template.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public class VelocityTrendsPlugin extends BurnDownChartPlugin {
	@Override
	protected void validate(DataBinder binder, BurnDownChartCommand command, Map params) throws NamedPluginException {
		// hide burndown and show velocity explicitely
		command.setShowBurnDown(false);
		command.setShowNew(true);
		command.setShowVelocity(true);

		super.validate(binder, command, params);
	}

	@Override
	protected String getTemplateFilename() {
		return "velocity-trends-plugin.vm";
	}
}
