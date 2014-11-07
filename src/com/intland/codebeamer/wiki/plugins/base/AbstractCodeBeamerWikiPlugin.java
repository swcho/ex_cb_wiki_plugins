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

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.velocity.VelocityContext;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.WikiPlugin;
import com.intland.codebeamer.controller.ControllerUtils;
import com.intland.codebeamer.event.util.EmailNotifications;
import com.intland.codebeamer.manager.ProjectManager;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.WikiPageDto;
import com.intland.codebeamer.remoting.DescriptionFormat;
import com.intland.codebeamer.utils.TemplateRenderer;
import com.intland.codebeamer.utils.TemplateRenderer.Parameters;
import com.intland.codebeamer.wiki.CodeBeamerWikiContext;
import com.intland.codebeamer.wiki.WikiMarkupProcessor;
import com.intland.codebeamer.wiki.plugins.util.WikiPluginUtils;

/**
 * Each CodeBeamer-specific JSPWiki plugin should extend this.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public abstract class AbstractCodeBeamerWikiPlugin implements WikiPlugin {
	protected static TemplateRenderer templateRenderer = TemplateRenderer.getInstance();
	protected static ProjectManager projectManager = ProjectManager.getInstance();

	public static final String PROJECT_ID = "projectId";
	protected static final String WRONG_ID = "wiki.wrong.id.message";
	protected static final String UNSPECIFIED_ID = "wiki.unspecified.id.message";
	protected static final String COMMON_PLUGIN_ERROR_TEMPLATE = "errormessage.vm";

	// id generator for unique chart ids
	private static volatile int idgen = 0;

	protected String pluginId = null;

	/**
	 * Unique id for this plugin instance.
	 * Can be used for generating unique HTML ids in the generated markup.
	 * @return
	 */
	public String getPluginId() {
		if (pluginId == null) {
			pluginId = Integer.toHexString(idgen++);
		}
		return pluginId;
	}

	/**
	 * Returns the user who initiated this Wiki rendering.
	 */
	protected UserDto getUserFromContext(WikiContext context) {
		return ((CodeBeamerWikiContext)context).getUser();
	}

	/**
	 * Returns the page where this Wiki rendering is executed.
	 */
	protected WikiPageDto getPageFromContext(WikiContext context) {
		return ((CodeBeamerWikiContext)context).getOwnerAsWikiPage();
	}

	/**
	 * Returns the Velocity context initialized with some default beans
	 * to use when rendering plugin templates.
	 */
	protected VelocityContext getDefaultVelocityContextFromContext(WikiContext context) {
		VelocityContext velocityContext = new VelocityContext();
		if (context.getHttpRequest() != null) {
			velocityContext.put(EmailNotifications.REMOTE_ADDRESS, context.getHttpRequest().getRemoteAddr());
			velocityContext.put("locale", context.getHttpRequest().getLocale());
		}
		velocityContext.put("user", getUserFromContext(context));
		velocityContext.put("wikiPage", getPageFromContext(context));
		// generate an unique id for this chart, can be used for ensuring the unique ids in the generated htmls
		velocityContext.put("pluginId", getPluginId());
		velocityContext.put("plugin", this);
		return EmailNotifications.populateBeansForVmGlobalLibrary(velocityContext, getUserFromContext(context));
	}

	/**
	 * Loads the plugin template and renders it.
	 * @param templateFilename is relative to the <i>templates/wiki-plugin</i> folder.
	 */
	protected String renderPluginTemplate(String templateFilename, VelocityContext context, Locale locale) {
		String rendered = templateRenderer.renderTemplateOnPath("wiki-plugin/" + templateFilename, context, new Parameters(locale, false));
		return rendered;
	}

	protected String renderPluginTemplate(String templateFilename, VelocityContext context) {
		return renderPluginTemplate(templateFilename, context, (Locale) context.get("locale"));
	}

	protected String getParameter(Map params, String paramId) {
		return WikiPluginUtils.getParameter(params, paramId);
	}

	protected String getParameter(Map params, String paramId, String defaultValue) {
		return WikiPluginUtils.getParameter(params, paramId, defaultValue);
	}

	protected boolean getBooleanParameter(Map params, String paramId) {
		return getBooleanParameter(params, paramId, false);
	}
	
	protected boolean getBooleanParameter(Map params, String paramId, boolean defaultValue) {
		String value = getStringParameter(params, paramId, String.valueOf(defaultValue));
		return Boolean.parseBoolean(value);
	}

	protected String getStringParameter(Map params, String paramId, String defaultValue) {
		return WikiPluginUtils.getStringParameter(params, paramId, defaultValue);
	}

	protected String getStringParameter(Map params, String paramId, Collection<String> allowedValues, String defaultValue) {
		return WikiPluginUtils.getStringParameter(params, paramId, allowedValues, defaultValue);
	}

	/**
	 * Returns the application's context path
	 */
	protected final String getApplicationContextPath(WikiContext context) {
		HttpServletRequest httpRequest = context.getHttpRequest();
		return (httpRequest != null ? httpRequest.getContextPath() : "");
	}

	protected final String getContextPath(WikiContext context) {
		return getApplicationContextPath(context);
	}

	/**
	 * Returns the Velocity context for plugin fatal error message
	 */
	protected VelocityContext getErrorMessageContext(WikiPlugin plugin, String msg) {
		VelocityContext cont = new VelocityContext();
		cont.put("pluginName", StringUtils.substringAfterLast(plugin.getClass().getName(), "."));
		cont.put("message", msg);
		return cont;
	}

	/**
	 * Render the error template for an exception.
	 * @param ex The exception.
	 * @return The rendered html markup
	 */
	protected String renderErrorTemplate(Throwable th) {
		String pluginName = null;
		if (th instanceof NamedPluginException) {
			NamedPluginException ne = (NamedPluginException) th;
			pluginName = ne.getPluginName();
		} else {
			pluginName = NamedPluginException.getPluginName(this);
		}

		VelocityContext context = new VelocityContext();
		context.put("pluginName", pluginName);
		context.put("message", th.getMessage());

		return renderPluginTemplate(COMMON_PLUGIN_ERROR_TEMPLATE, context, null);
	}

	private Boolean isEnclosingProject = Boolean.FALSE;

	/**
	 * If the discoverProject() method is using the enclosing project.
	 * @return
	 */
	public Boolean isEnclosingProject() {
		return isEnclosingProject;
	}

	/**
	 * Discovering current project. The project is NOT optional.
	 * @see AbstractCodeBeamerWikiPlugin#discoverProject(Map, WikiContext, boolean)
	 *
	 * @param params
	 * @param context
	 * @return
	 * @throws NamedPluginException
	 */
	public ProjectDto discoverProject(Map params, WikiContext context) throws NamedPluginException {
		return discoverProject(params, context, false);
	}

	/**
	 * Discovering current project. Replaces old ProjectIdAware....
	 * When discovering the current project it will set the isEnclosingProject boolean property too, which indicates if the discovered project is the current one.
	 * Also sets isEnclosingProject() boolean property.
	 *
	 * @param params
	 * @param context
	 * @param optional When the project is optional, so it is OK to have a non-specified project Id.
	 *
	 * @return The found project. If the project is not found, and it is not "optional" then it throws a PluginException for missing plugin.
	 * @throws NamedPluginException Throws a plugin-exception the project can not be found, or has invalid parameters
	 */
	public ProjectDto discoverProject(Map params, WikiContext context, boolean optional) throws NamedPluginException {
		ProjectDto project = null;
		isEnclosingProject = Boolean.FALSE;

		// get the project ID from parameters (if specified)
		String projectIdPassedString = getParameter(params, PROJECT_ID);
		int projectIdPassed = 0;
		if (projectIdPassedString != null) {
			projectIdPassed = NumberUtils.toInt(projectIdPassedString, -1);
			if (projectIdPassed <= 0) {
				throw new NamedPluginException(this, WRONG_ID);
			}
		}

		if (projectIdPassed > 0) {
			UserDto user = getUserFromContext(context);
			// check if ID is correct and the user has access to the project.
			project = projectManager.findById(user, Integer.valueOf(projectIdPassed));
			if (project == null) {
				throw new NamedPluginException(this, WRONG_ID);
			}
		} else {
			// discover from the wiki context
			if (context != null) {
				project = ((CodeBeamerWikiContext)context).getProject();

				if (project == null) {
					// discover from request
					HttpServletRequest httpRequest = context.getHttpRequest();
					project = ControllerUtils.getCurrentProject(httpRequest);
				}
			}
		}

		// fall back to error context
		if (project == null && !optional) {
			throw new NamedPluginException(this, UNSPECIFIED_ID);
		}

		int projectIdDiscovered = project != null && project.getId() != null ? project.getId().intValue() : -1;
		isEnclosingProject = Boolean.valueOf(project != null && (projectIdPassed == -1 || projectIdPassed == projectIdDiscovered));

		return project;
	}

	/**
	 * Put all objects from Map to Velocity context.
	 */
	public static void putAll(VelocityContext context, Map map) {
		for (Iterator<Entry> iEntries = map.entrySet().iterator(); iEntries.hasNext();) {
			Entry entry = iEntries.next();
			context.put(entry.getKey().toString(), entry.getValue());
		}
	}

	public ServletContext getServletContext(WikiContext context) {
		ServletContext servletContext;
		if (context.getHttpRequest() != null) {
			servletContext = context.getHttpRequest().getSession(true).getServletContext();
		} else {
			servletContext = context.getEngine().getServletContext();
		}
		return servletContext;
	}

	/**
	 * Get Spring's ApplicationContext. First tries to resolve the context from request, and if that's not available from the WikiEngine's servletcontext.
	 */
	public ApplicationContext getApplicationContext(WikiContext ctx) {
		// use the request if available for discovering the servlet-context
		return WebApplicationContextUtils.getWebApplicationContext(getServletContext(ctx));
	}

	/**
	 * Returns the XML generated from a JDOM element recursively.
	 * @param element This is expected in escaped form, as this method does <strong>not</strong> escape.
	 */
	protected String elementToXmlString(Element element) {
		XMLOutputter xmlOutputter = new XMLOutputter();
		String xml = xmlOutputter.outputString(element);

		// undo the escaping done by XMLOutputter
		xml = StringEscapeUtils.unescapeXml(xml);

		return xml;
	}

	/**
	 * Renders texts in given any of the {@link DescriptionFormat} formats to HTML.
	 */
	protected String renderText(WikiContext context, String text, String format) { // TODO move to some parent
		HttpServletRequest httpRequest = context.getHttpRequest();
		UserDto user = getUserFromContext(context);
		WikiPageDto page = getPageFromContext(context);

		String renderedText = WikiMarkupProcessor.getInstance().transformToHtml(httpRequest, text, format, false, false, page, user);
		return renderedText;
	}

}
