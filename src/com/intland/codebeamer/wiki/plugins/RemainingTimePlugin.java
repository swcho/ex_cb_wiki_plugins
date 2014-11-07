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

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.VersionStatsDto;
import com.intland.codebeamer.wiki.plugins.base.AbstractAgileWikiPlugin;

/**
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public class RemainingTimePlugin extends AbstractAgileWikiPlugin<RemainingTimeCommand> {
	@Override
	public RemainingTimeCommand createCommand() throws PluginException {
		return new RemainingTimeCommand();
	}

	@Override
	protected Map populateModel(DataBinder binder, RemainingTimeCommand command, Map params) throws PluginException {
		TrackerItemDto release = discoverRelease(getUser(), command.getId(), binder);
		Integer daysRemaining =  ((release != null) && !release.isClosed() && (release.getEndDate() != null)) ? Integer.valueOf(new VersionStatsDto(release.getEndDate(), null, null, null).getDaysLeft()) : null;

		Map model = new HashMap();
		model.put("command", command);
		model.put("release", release);
		model.put("daysRemaining", daysRemaining);

		return model;
	}

	@Override
	protected String getTemplateFilename() {
		return "remainingtime-plugin.vm";
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
}
