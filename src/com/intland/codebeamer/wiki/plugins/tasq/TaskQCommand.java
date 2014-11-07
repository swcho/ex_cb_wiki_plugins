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
package com.intland.codebeamer.wiki.plugins.tasq;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;

import com.intland.codebeamer.controller.support.CommaStringToIntegerListPropertyEditor;
import com.intland.codebeamer.controller.support.CommaStringToStringListPropertyEditor;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractMaxWikiPluginCommand;

/**
 * Command bean for {@link TaskQPlugin}
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class TaskQCommand extends AbstractMaxWikiPluginCommand implements PropertyEditorRegistrar {

	/**
	 * The user whose task-queue should be shown
	 */
	private Integer userId;

	/**
	 * Names of the statuses of the issues to be shown
	 */
	private List<String> status = new ArrayList<String>(0);

	/**
	 * Optional version-ids filter for the tracker
	 */
	private List<Integer> versionId = new ArrayList<Integer>(0);

	/**
	 * Optional max age of the issue in days, if this is specified, then only those issues that have their very last status update in the (now - maxAge) period must be returned.
	 */
	private Integer maxAge;

	@NotNull
	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public List<String> getStatus() {
		return status;
	}
	public void setStatus(List<String> status) {
		this.status = status;
	}

	public List<Integer> getVersionId() {
		return versionId;
	}
	public void setVersionId(List<Integer> versionId) {
		this.versionId = versionId;
	}

	public Integer getMaxAge() {
		return maxAge;
	}
	public void setMaxAge(Integer maxAge) {
		this.maxAge = maxAge;
	}

	@AssertTrue(message="{taskq.plugin.one.status.required}")
	public boolean hasStatus() {
		return status != null && status.isEmpty() == false;
	}

	public void registerCustomEditors(PropertyEditorRegistry registry) {
		registry.registerCustomEditor(List.class, "status", new CommaStringToStringListPropertyEditor());
		registry.registerCustomEditor(List.class, "versionId", new CommaStringToIntegerListPropertyEditor());
	}

}
