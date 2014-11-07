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
package com.intland.codebeamer.wiki.plugins.ajax;

import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.ajax.wikiplugin.AjaxWikiPluginController;
import com.intland.codebeamer.wiki.plugins.base.AbstractAgileWikiPlugin;

/**
 * Base class for wiki plugins which can refresh/rerender themselves via an Ajax call.
 * The ajax refreshes are served by the {@link AjaxWikiPluginController}
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public abstract class AbstractAjaxRefreshingWikiPlugin<Command> extends AbstractAgileWikiPlugin<Command> {

	private final static Logger logger = Logger.getLogger(AbstractAjaxRefreshingWikiPlugin.class);

	// If being rendered for ajax request
	private boolean ajaxRendering = false;

	@Override
	public abstract Command createCommand() throws PluginException;

	@Override
	protected final Map populateModel(DataBinder binder, Command command, Map params) throws PluginException {
		Map model = populateModelInternal(binder, command, params);

		String ajaxRefreshURL = getAjaxRefreshURL(command);
		model.put("ajaxRefreshURL", ajaxRefreshURL);

		return model;
	}

	public boolean isAjaxRendering() {
		return ajaxRendering;
	}

	public void setAjaxRendering(boolean ajaxRendering) {
		this.ajaxRendering = ajaxRendering;
	}

	@Override
	protected void initBinder(DataBinder binder, Command command, Map params) {
		if (ajaxRendering) {
			logger.debug("Disabling validation for unknown-fields, because rendering for ajax");
			binder.setIgnoreUnknownFields(true);
		}
	}

	protected abstract Map populateModelInternal(DataBinder binder, Command command, Map params) throws PluginException;

	@Override
	protected abstract String getTemplateFilename();

	protected String getAjaxRefreshURL(Command command) {
		final String pluginType = getClass().getName();
		String baseURL = getContextPath() + "/ajaxPluginRenderer.spr?" + AjaxWikiPluginController.PARAM_PLUGIN_TYPE + "=" + pluginType;
		String url = baseURL + "&" + getRequestParamsByCommand(command, false) +"&pluginId=" + getPluginId();
		return url;
	}

	public void setPluginId(String pluginId) {
		super.pluginId = pluginId;
	}

	@Override
	protected String execute(VelocityContext velocityContext, Map params) throws PluginException {
		String html = super.execute(velocityContext, params);
		// add an html element around the plugin, this will be replaced
		html = String.format("<div class='ajaxplugin' id='ajaxplugin_%s'>%s</div>", pluginId, html);
		return html;
	}
}
