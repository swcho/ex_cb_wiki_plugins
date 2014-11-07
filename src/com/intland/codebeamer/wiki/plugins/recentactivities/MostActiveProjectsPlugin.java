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
package com.intland.codebeamer.wiki.plugins.recentactivities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.controller.support.CommaStringToIntegerListPropertyEditor;
import com.intland.codebeamer.controller.support.CommaStringToStringListPropertyEditor;
import com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.DefaultWebBindingInitializer;

/**
 * Plugin to display the most active projects
 *
 * @author <a href="mailto:klaus.mehling@intland.com">Klaus Mehling</a>
 */
public class MostActiveProjectsPlugin extends AbstractCommandWikiPlugin<MostActiveProjectsCommand> {
	@Autowired
	private ActivityStreamManager activityStreamManager;

	public MostActiveProjectsPlugin() {
		setWebBindingInitializer(new MostActiveProjectsBindingInitializer());
	}

	public static class MostActiveProjectsPropertyEditorRegistrar implements PropertyEditorRegistrar {
		public void registerCustomEditors(PropertyEditorRegistry registry) {
			registry.registerCustomEditor(List.class, "projectId", new CommaStringToIntegerListPropertyEditor());
			registry.registerCustomEditor(List.class, "tag", new CommaStringToStringListPropertyEditor());
		}
	}

	public static class MostActiveProjectsBindingInitializer extends DefaultWebBindingInitializer {
		public MostActiveProjectsBindingInitializer() {
			addPropertyEditorRegistrar(new MostActiveProjectsPropertyEditorRegistrar());
		}
	}

	@Override
	public MostActiveProjectsCommand createCommand() throws PluginException {
		return new MostActiveProjectsCommand();
	}

	@Override
	protected String getTemplateFilename() {
		return "mostactiveprojects-plugin.vm";
	}

	@Override
	protected Map populateModel(DataBinder binder, MostActiveProjectsCommand command, Map params) throws PluginException {
		Map model = new HashMap(4);

		model.put("command", command);
		model.put("projects", activityStreamManager.findMostActiveProjects(getUser(), command).entrySet());

		return model;
	}
}
