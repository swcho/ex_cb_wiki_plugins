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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.velocity.VelocityContext;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.manager.EntityLabelManager;
import com.intland.codebeamer.persistence.dto.LabelDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.wiki.plugins.base.AutoWiringCodeBeamerPlugin;

/**
 * Plugin to generate a project dependent label cloud.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id$
 */
public class TagCloudPlugin extends AutoWiringCodeBeamerPlugin {
	private EntityLabelManager entityLabelManager;

	public void setEntityLabelManager(EntityLabelManager entityLabelManager) {
		this.entityLabelManager = entityLabelManager;
	}

	@Override
	public String getTemplateFilename() {
		return "tagcloud-plugin.vm";
	}

	@Override
	public void populateContext(VelocityContext velocityContext, Map params) throws PluginException {
		UserDto user = getUser();
		Integer max = parsePositiveIntegerParameter(params, "max");
		List<LabelDto> labels = (user != null) ? entityLabelManager.findMostRelevantLabelsByUser(user, max) : Collections.EMPTY_LIST;

		// find popularity and usage date extremes
		Integer minPopularity = null, maxPopularity = null;
		Date minUsedAt = null, maxUsedAt = null;
		for(LabelDto label : labels) {
			if(label.getPopularity() != null) {
				if((minPopularity == null) || (label.getPopularity().compareTo(minPopularity) < 0)) {
					minPopularity = label.getPopularity();
				}
				if((maxPopularity == null) || (label.getPopularity().compareTo(maxPopularity) > 0)) {
					maxPopularity = label.getPopularity();
				}
			}

			if(label.getMostRecentlyUsedAt() != null) {
				if((minUsedAt == null) || label.getMostRecentlyUsedAt().before(minUsedAt)) {
					minUsedAt = label.getMostRecentlyUsedAt();
				}
				if((maxUsedAt == null) || label.getMostRecentlyUsedAt().after(maxUsedAt)) {
					maxUsedAt = label.getMostRecentlyUsedAt();
				}
			}
		}

		// calculate scalers based on the extremes
		// TODO coloring and sizing formulas should be reviewed (see here and the .vm)
		double fontSizeScaler = (minPopularity != null) && (maxPopularity != null) && !maxPopularity.equals(minPopularity) ? (1.0 / (maxPopularity.intValue() - minPopularity.intValue())) : 0.0;
		double colorScaler = (minUsedAt != null) && (maxUsedAt != null) &&  !maxUsedAt.equals(minUsedAt) ? (1.0 / (maxUsedAt.getTime() - minUsedAt.getTime())) : 0.0;

		// populate model
		velocityContext.put("labels", labels);
		velocityContext.put("fontSizeScaler", Double.valueOf(fontSizeScaler));
		velocityContext.put("colorScaler", Double.valueOf(colorScaler));
		velocityContext.put("minUsedAt", minUsedAt);
	}
}
