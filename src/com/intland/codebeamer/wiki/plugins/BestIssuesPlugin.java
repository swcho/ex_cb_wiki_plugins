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

import java.util.List;
import java.util.Map;

import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.validation.DataBinder;

import com.intland.codebeamer.persistence.dto.ObjectRatingStatsDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.wiki.InterwikiReferenceTypeDescriptor;

/**
 * Subclass of BestContentPlugin shows only Issues with some issue specific filtering.
 *
 * Additionally to {@link BestContentPlugin} this supports following parameters :
 * - trackerId=<trackerId> Optional integer Id for a tracker. If provided only those issues which belong to that tracker are listed.
 * - allowedStatuses
 * - deniedStatuses
 *
 * @see BestContentPlugin
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 * $Id$
 */
public class BestIssuesPlugin extends BestContentPlugin<BestIssuesPlugin.BestIssuesPluginCommand> {
	public BestIssuesPlugin() {
		forcedEntityType = TrackerItemDto.INTERWIKI_LINK_TYPE;
	}

	public String getTemplateFilename() {
		return "ratings/BestVotings.vm";
	}

	/**
	 * Command bean for Best-Issues plugin, which extends on BestContentPlugin's capabilities,
	 * but adds the additional filtering paramters for issues.
	 *
	 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
	 * $Id$
	 */
	public static class BestIssuesPluginCommand extends com.intland.codebeamer.wiki.plugins.BestContentPlugin.BestContentPluginCommand {
		// the id of the tracker, only those issues which are in this tracker are shown
		private Integer trackerId = null;

		// names for allowed issue statuses, comma separated
		private String[] allowedStatuses = null;
		// names for denied statuses
		private String[] deniedStatuses = null;

		public Integer getTrackerId() {
			return trackerId;
		}

		public void setTrackerId(Integer trackerId) {
			this.trackerId = trackerId;
		}

		public String[] getAllowedStatuses() {
			return allowedStatuses;
		}

		public void setAllowedStatuses(String[] allowedStatuses) {
			this.allowedStatuses = allowedStatuses;
		}

		public String[] getDeniedStatuses() {
			// when neither allowedStatuses and deniedStatuses is provided then then the default is (allowedStatuses="", deniedStatuses="Closed")
			if ((allowedStatuses == null || allowedStatuses.length == 0) && (deniedStatuses == null || deniedStatuses.length == 0)) {
				return new String[] {"Closed"};
			}

			return deniedStatuses;
		}

		public void setDeniedStatuses(String[] deniedStatuses) {
			this.deniedStatuses = deniedStatuses;
		}
	}

	@Override
	public BestIssuesPluginCommand createCommand() {
		return new BestIssuesPluginCommand();
	}

	@Override
	protected void initBinder(DataBinder binder, BestContentPluginCommand command, Map params) {
		super.initBinder(binder, command, params);
		// custom binding of "allowedStatuses", "deniedStatuses" allow "," separated strings to be slit to string arrays
		binder.registerCustomEditor(String[].class, "allowedStatuses", new StringArrayPropertyEditor());
		binder.registerCustomEditor(String[].class, "deniedStatuses", new StringArrayPropertyEditor());
	}

	/**
	 * Find the rating statistics.
	 * @param params
	 * @param command The command bean contains the parameters
	 * @return The list of ObjectRatingStatsDtos
	 */
	@Override
	protected List<ObjectRatingStatsDto> findRatingStats(Map params, BestContentPluginCommand command) {
		BestIssuesPluginCommand myCommand = (BestIssuesPluginCommand) command;
		List<ObjectRatingStatsDto> result = objectRatingManager.findMostHighlyRatedTrackerItems(myCommand.getProjectId(), myCommand.getTrackerId(), myCommand.getMax().intValue(), myCommand.getAllowedStatuses(), myCommand.getDeniedStatuses() );
		return result;
	}

	/**
	 * Validate the entity-type.
	 * @param binder
	 * @param descriptor The entity-type descriptor
	 */
	@Override
	protected void validateEntityType(DataBinder binder, InterwikiReferenceTypeDescriptor descriptor) {
		// nothing, just override
	}
}
