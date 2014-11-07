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
package com.intland.codebeamer.wiki.plugins.base;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.velocity.VelocityContext;
import org.springframework.context.ApplicationContext;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.controller.ControllerUtils;
import com.intland.codebeamer.controller.support.SimpleMessageResolver;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.WikiPageDto;
import com.intland.codebeamer.wiki.CodeBeamerWikiContext;

/**
 * Enhanced codebeamer plugin with autowires Spring dependencies.
 * This is more convenient because:
 * - will autowire
 * - will init a velocityContext for you
 * - will automatically render the template after the velocity-context is populated.
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 * $Id$
 */
public abstract class AutoWiringCodeBeamerPlugin extends AbstractCodeBeamerWikiPlugin {
	protected WikiContext wikiContext;
	// The actual parameters
	protected Map pluginParams;

	// if the autowiring is enabled
	private boolean autowire = true;

	/**
	 * Current wiki context.
	 * @return the wikiContext
	 */
	public WikiContext getWikiContext() {
		return wikiContext;
	}

	public void setWikiContext(WikiContext wikiContext) {
		this.wikiContext = wikiContext;
	}

	/**
	 * Get the actual plugin parameters
	 * @return the pluginParams
	 */
	public Map getPluginParams() {
		return pluginParams;
	}

	public UserDto getUser() {
		return super.getUserFromContext(wikiContext);
	}

	public WikiPageDto getPage() {
		return super.getPageFromContext(wikiContext);
	}

	public ProjectDto getProject() {
		return ((CodeBeamerWikiContext)getWikiContext()).getProject();
	}

	public ApplicationContext getApplicationContext() {
		return super.getApplicationContext(wikiContext);
	}

	/**
	 * Get the current context-path
	 * @return
	 */
	protected String getContextPath() {
		if (getWikiContext() == null || getWikiContext().getHttpRequest() == null) {
			return null;
		}
		return getWikiContext().getHttpRequest().getContextPath();
	}

	/**
	 * Get the message resolver can be used to resolve i18n messages
	 */
	protected SimpleMessageResolver getSimpleMessageResolver() {
		final HttpServletRequest request = getWikiContext().getHttpRequest();
		return SimpleMessageResolver.getInstance(request);
	}

	/**
	 * Get the name of the velocity template will be rendered with.
	 * @return the templateFilename
	 */
	protected abstract String getTemplateFilename();

	final public String execute(WikiContext context, Map params) throws PluginException {
		this.wikiContext = context;
		this.pluginParams = params;

		autowire();

		VelocityContext velocityContext = getDefaultVelocityContextFromContext(wikiContext);
		velocityContext.put("contextPath", getApplicationContextPath(wikiContext));

		return execute(velocityContext, params);
	}

	/**
	 * Same as the other execute(), just more convenient as it will have already a standard VelocityContext initialized.
	 * @param velocityContext
	 * @param params
	 * @return
	 */
	protected String execute(VelocityContext velocityContext, Map params) throws PluginException {
		populateContext(velocityContext, params);
		String html = renderPluginTemplate(getTemplateFilename(), velocityContext);
		return html;
	}

	/**
	 * Populate the velocity context, before the template gets rendered.
	 * @param velocityContext
	 * @param params
	 */
	protected abstract void populateContext(VelocityContext velocityContext, Map params) throws PluginException;

	/**
	 * Autowire dependencies if not yet done.
	 */
	private void autowire() {
		autowire(getApplicationContext());
	}

	public void autowire(ApplicationContext ctx) {
		if (autowire) {
			ControllerUtils.autoWire(this, ctx);
			autowire = false; // next time don't autowire, already done
		}
	}

	/**
	 * If the autowiring enabled.
	 * @return the autowire
	 */
	public boolean isAutowire() {
		return autowire;
	}

	/**
	 * If the autowiring enabled.
	 * @param autowire the autowire to set
	 */
	public void setAutowire(boolean autowire) {
		this.autowire = autowire;
	}

	// utility methods
	/**
	 * Parse a positive integer value.
	 * @param params The params
	 * @param paramName The parameter name
	 * @return The Integer value or null if not provided
	 * @throws PluginException
	 */
	protected Integer parsePositiveIntegerParameter(Map params, String paramName) throws PluginException {
		String value = getParameter(params, paramName);
		if (value != null) {
			try {
				int intValue = Integer.parseInt(value);
				if (intValue <=0) {
					throw new PluginException("Invalid " + paramName + " parameter value, positive integer expected! value=" + value);
				}
				return Integer.valueOf(intValue);

			} catch (NumberFormatException ex) {
				throw new PluginException("Invalid " + paramName + " parameter value, only integer values are allowed! value=" + value, ex);
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
}
