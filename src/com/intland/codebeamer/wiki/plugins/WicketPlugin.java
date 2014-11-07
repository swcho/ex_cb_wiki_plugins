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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.protocol.http.WebApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.Config;
import com.intland.codebeamer.controller.ControllerUtils;
import com.intland.codebeamer.text.html.HtmlCleaner;
import com.intland.codebeamer.wicket.CBWicketTester;
import com.intland.codebeamer.wicket.pages.WicketPluginTestPage;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;

/**
 * Experimental plugin for embedding wicket pages on wiki pages as plugins. It does render the wicket output,
 * but during the form and ajax submit the page will loose its state.
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 *
 */
public class WicketPlugin extends AbstractCodeBeamerWikiPlugin {

	private final static Logger logger = Logger.getLogger(WicketPlugin.class);

	@Autowired
	private WebApplication wicketApp;

	@Autowired
	private ApplicationContext applicationContext;

	/**
	 * TODO: incomplete
	 * see also:
	 * http://www.danwalmsley.com/2008/10/23/sending-html-email-with-wicket-part-ii-converting-links/
	 * http://apache-wicket.1842946.n4.nabble.com/embedding-Wicket-into-JSP-td1867715.html
	 * http://apache-wicket.1842946.n4.nabble.com/embedding-Wicket-into-JSP-td1867715.html
	 *
	 */
	protected String renderPage(WebApplication application, Class<? extends Page> pageClass, PageParameters pageParameters) {
		CBWicketTester tester = CBWicketTester.create(applicationContext);
		//tester.setCreateAjaxRequest(true);
		tester.startPage(pageClass, pageParameters);
		String rendered = tester.getServletResponse().getDocument();

		// keep the <head> and <body> parts
		HtmlCleaner htmlParts = new HtmlCleaner(rendered);
		String clean = htmlParts.getHead() + htmlParts.getBody();
		if (logger.isDebugEnabled()) {
			logger.debug("Wicket page " + pageClass + "(" + pageParameters +") is rendered to <" + clean +">");
		}
		return clean;
	}

	public String execute(WikiContext context, Map params) throws PluginException {
		if (!Config.isDevelopmentMode()) {
			throw new PluginException("Experimental plugin, not yet available.");
		}

		HttpServletRequest req = context.getHttpRequest();
		ControllerUtils.autoWire(this, req);

		String rendered = renderPage(wicketApp, WicketPluginTestPage.class, new PageParameters());
		return rendered;
	}

}
