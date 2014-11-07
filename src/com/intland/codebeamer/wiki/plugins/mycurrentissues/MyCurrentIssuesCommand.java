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
package com.intland.codebeamer.wiki.plugins.mycurrentissues;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.AssertFalse;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;

import com.intland.codebeamer.controller.support.CommaStringToIntegerListPropertyEditor;
import com.intland.codebeamer.controller.support.CommaStringToStringListPropertyEditor;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractMaxWikiPluginCommand;

/**
 * Command bean for {@link MyCurrentIssuesPlugin}
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class MyCurrentIssuesCommand extends AbstractMaxWikiPluginCommand implements PropertyEditorRegistrar {
	/**
	 * Note: either projectId or trackerId or tag or versionId can have value but only one of them
	 */
	private List<Integer> projectId = Collections.EMPTY_LIST;
	private List<Integer> trackerId = Collections.EMPTY_LIST;
	private List<Integer> releaseId = Collections.EMPTY_LIST;
	/**
	 * The list of tags (parsed from comma separated tag values)
	 */
	private List<String> tag = null;

	/**
	 * @return the versionId
	 * @deprecated
	 */
	public List<Integer> getVersionId() {
		return getReleaseId();
	}

	/**
	 * @param versionId the versionId to set
	 * @deprecated
	 */
	public void setVersionId(List<Integer> versionId) {
		setReleaseId(versionId);
	}

	/**
	 * @return the releaseId
	 */
	public List<Integer> getReleaseId() {
		return releaseId;
	}

	/**
	 * @param versionId the versionId to set
	 */
	public void setReleaseId(List<Integer> releaseId) {
		this.releaseId = releaseId;
	}

	/**
	 * The list of project-ids the issues should be in
	 * @return the projectId
	 */
	public List<Integer> getProjectId() {
		return projectId;
	}

	public void setProjectId(List<Integer> projectId) {
		this.projectId = projectId;
	}

	/**
	 * The list of tracker-ids the issues should be in
	 * @return the trackerId
	 */
	public List<Integer> getTrackerId() {
		return trackerId;
	}

	public void setTrackerId(List<Integer> trackerId) {
		this.trackerId = trackerId;
	}

	/**
	 * 	The names of the tags the issues must have
	 */
	public List<String> getTag() {
		return tag;
	}

	public void setTag(List<String> tag) {
		this.tag = tag;
	}

	/**
	 * Forces that either projectId/trackerId/tag can be provided
	 */
	@AssertFalse(message = "{my.current.issues.no.mixed.params}")
	public boolean isMixedParameters() {
		int mix = 0;
		if (!CollectionUtils.isEmpty(projectId)) {
			mix++;
		}
		if (!CollectionUtils.isEmpty(trackerId)) {
			mix++;
		}
		if (!CollectionUtils.isEmpty(tag)) {
			mix++;
		}
		if (!CollectionUtils.isEmpty(releaseId)) {
			mix++;
		}
		return mix > 1;
	}

	/**
	 * Registering binding for my fields.
	 * @see org.springframework.beans.PropertyEditorRegistrar#registerCustomEditors(org.springframework.beans.PropertyEditorRegistry)
	 */
	public void registerCustomEditors(PropertyEditorRegistry registry) {
		registry.registerCustomEditor(List.class, "projectId", new CommaStringToIntegerListPropertyEditor());
		registry.registerCustomEditor(List.class, "trackerId", new CommaStringToIntegerListPropertyEditor());
		registry.registerCustomEditor(List.class, "versionId", new CommaStringToIntegerListPropertyEditor());
		registry.registerCustomEditor(List.class, "releaseId", new CommaStringToIntegerListPropertyEditor());
		registry.registerCustomEditor(List.class, "tag", new CommaStringToStringListPropertyEditor());
	}
}
