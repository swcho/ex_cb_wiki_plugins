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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.log4j.Logger;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.manager.ObjectRatingManager;
import com.intland.codebeamer.persistence.dao.impl.EntityCache;
import com.intland.codebeamer.persistence.dto.ObjectRatingStatsDto;
import com.intland.codebeamer.ui.view.StarRenderer;
import com.intland.codebeamer.wiki.InterwikiReferenceTypeDescriptor;
import com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin;
import com.intland.codebeamer.wiki.plugins.command.base.ProjectAwareWikiPluginCommand;

/**
 * Plugin renders the best-content, i.e. which is best, because most highly rated by the users.
 * interwikiReferenceTypeToGroupType
 *
 * Parameters:
 * 	- entityType: provide the "Interwiki" styled entity type string
 * 			(for example entityType='WIKIPAGE' for wiki-pages) you want the most highly rated objects on.
 *  - title: optional title put above the ratings
 *  - projectId: optional parameter: a specific project's id to show entities from, if not provided then the current project is used
 *  - allProject: optional parameter, if set to "true" then all project's entities are shown, otherwise only the current projects'.
 *  - cssClass: optional parameter, add custom CSS class to the container table
 *  - cssStyle: optional parameter, add custom CSS style to the container table
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 * $Id$
 */
public class BestContentPlugin<Command extends BestContentPlugin.BestContentPluginCommand> extends AbstractCommandWikiPlugin<BestContentPlugin.BestContentPluginCommand> {
	private final static Logger logger = Logger.getLogger(BestContentPlugin.class);

	public final static String PARAM_ENTITY_TYPE = "entityType";
	public final static String PARAM_MAX = "max";

	protected ObjectRatingManager objectRatingManager;

	// the fixed entityType parameter value
	protected String forcedEntityType = null;

	@Override
	public String getTemplateFilename() {
		return "ratings/BestContentPlugin.vm";
	}

	/**
	 * BestContentPluginCommand bean for BestContentPlugin. The Wiki plugin parameters are bound to this object.
	 *
	 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
	 * $Id$
	 */
	public static class BestContentPluginCommand extends ProjectAwareWikiPluginCommand {
		private String entityType;
		// the resolved InterWikiReference descriptor
		private InterwikiReferenceTypeDescriptor descriptor;

		public String getEntityType() {
			return entityType;
		}

		public void setEntityType(String entityType) {
			this.entityType = entityType;
		}

		public InterwikiReferenceTypeDescriptor getDescriptor() {
			return descriptor;
		}

		public void setDescriptor(InterwikiReferenceTypeDescriptor descriptor) {
			this.descriptor = descriptor;
		}

		public String toString() {
			return ReflectionToStringBuilder.toString(this);
		}
	}

	/**
	 * Find the rating statistics.
	 * @param params
	 * @param command The command bean contains the parameters
	 * @return The list of ObjectRatingStatsDtos
	 */
	protected List<ObjectRatingStatsDto> findRatingStats(Map params, BestContentPluginCommand command) {
		return objectRatingManager.findMostHighlyRatedEntities(command.getDescriptor(), command.getProjectId(), command.getMax().intValue());
	}

	@Override
	public BestContentPluginCommand createCommand() {
		return new BestContentPluginCommand();
	}

	/**
	 * Validate parameters and command bean.
	 * @see com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin#validate(org.springframework.validation.DataBinder, java.lang.Object, java.util.Map)
	 */
	@Override
	protected void validate(DataBinder binder, BestContentPluginCommand command, Map params) {
		try {
			command.discoverProject(this, params);
		} catch (PluginException ex) {
			addError(binder, "projectId", ex.getMessage());
		}

		command.validate(binder, params);

		String entityType = StringUtils.trimToNull(command.entityType);
		if (forcedEntityType != null) {
			if (!StringUtils.isEmpty(entityType)) {
				String msg = "Must not provide " + PARAM_ENTITY_TYPE + " parameter, because fixed as " + forcedEntityType + " for this type. Use generic BestContentPlugin instead.";
				addError(binder, "entityType", msg);
			}
			entityType = forcedEntityType;
			command.entityType = forcedEntityType;
		}

		logger.debug("Rendering most-highly-rated entities of type: " + entityType);

		InterwikiReferenceTypeDescriptor descriptor = InterwikiReferenceTypeDescriptor.getInterwikiReferenceTypeDescriptor((entityType == null ? null : entityType.toUpperCase().trim()));
		command.descriptor = descriptor;
		if (entityType != null) {
			if  (descriptor == null) {
				String msg = "Invalid entityType parameter, please use the Interwiki styled entity type string (for example 'WIKIPAGE' for wiki pages). Invalid value: '" + entityType +"'";
				addError(binder, "entityType", msg);

			} else {
				validateEntityType(binder, descriptor);
			}
		}
	}

	/**
	 * Validate the entity-type.
	 * @param binder
	 * @param descriptor The descriptor about the entity-type
	 */
	protected void validateEntityType(DataBinder binder, InterwikiReferenceTypeDescriptor descriptor) {
		if (EntityCache.TRACKER_ITEM_TYPE.equals(descriptor.getEntityType())) {
			String msg = "Invalid entityType parameter, please use BestIssues plugin for entities, because this plugin is for Forums or Artifacts only!";
			addError(binder, "entityType", msg);
		}
	}

	/**
	 * Populating model.
	 * @see com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin#populateModel(org.springframework.validation.DataBinder, java.lang.Object, java.util.Map)
	 */
	@Override
	protected Map populateModel(DataBinder binder, BestContentPluginCommand command, Map params) {
		Map model = new HashMap();
		model.put("title", command.getTitle());

		logger.info("Rendering best-content of " + command.descriptor + ", project=" + command.getProjectId() +", allProjects=" + command.getProjectId());
		List<ObjectRatingStatsDto> bestContent = findRatingStats(params, command);
		logger.info("Best-content found #" + bestContent.size() +" elements.");
		// resolve referenced object from (entity-type,entity-id) pair, and put back to the stats
		EntityCache.getInstance(getUser()).resolve(bestContent, true /* remove unresolvable */);
		model.put("bestContent", bestContent);

		// star renderer will be used to render the rating stars
		model.put("starRenderer", new StarRenderer());
		model.put("cssClass", command.getCssClass());
		model.put("cssStyle", command.getCssStyle());
		return model;
	}

	/**
	 * Not null if the entity-type is forced/fixed.
	 * @return the forcedEntityType The forced entity-type for this plugin.
	 */
	public String getForcedEntityType() {
		return forcedEntityType;
	}

	// spring setters
	public void setObjectRatingManager(ObjectRatingManager objectRatingManager) {
		this.objectRatingManager = objectRatingManager;
	}
}
