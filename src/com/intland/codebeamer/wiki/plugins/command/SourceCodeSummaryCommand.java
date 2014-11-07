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
package com.intland.codebeamer.wiki.plugins.command;

import java.util.List;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;

import com.intland.codebeamer.controller.support.CommaStringToIntegerListPropertyEditor;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractDisplayOptionCommand;

/**
 * Command for {@link SourceCodeSummaryCommand}
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class SourceCodeSummaryCommand extends AbstractDisplayOptionCommand implements PropertyEditorRegistrar {

	// project-id is used during auto discovery, and added for backwards compatibility
	// do not delete, or the plugin will complain about if this parameter is present
	private Integer projectId;

	private List<Integer> repositoryId;

	@Deprecated // Do not use, only for backwards compatibility
	public void setProjectId(Integer projectId) {
		this.projectId = projectId;
	}

	public Integer getProjectId() {
		return projectId;
	}

	public List<Integer> getRepositoryId() {
		return repositoryId;
	}
	public void setRepositoryId(List<Integer> repositoryId) {
		this.repositoryId = repositoryId;
	}

	@Override
	public void registerCustomEditors(PropertyEditorRegistry registry) {
		super.registerCustomEditors(registry);
		registry.registerCustomEditor(List.class, "repositoryId", new CommaStringToIntegerListPropertyEditor());
	}

}
