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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import net.sf.ehcache.Cache;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.google.common.base.Charsets;
import com.google.common.base.Supplier;
import com.intland.codebeamer.utils.ehcache.WithCache;
import com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin;
import com.sun.star.uno.RuntimeException;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;

/**
 * Feed inclusion plugin.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public class FeedPlugin extends AbstractCommandWikiPlugin<FeedPluginCommand> {
	final static private Logger logger = Logger.getLogger(FeedPlugin.class);

	@Autowired(required=false)
	@Qualifier("feedPluginCache")
	protected Cache cache;

	@Override
	public FeedPluginCommand createCommand() throws PluginException {
		return new FeedPluginCommand();
	}

	@Override
	protected String getTemplateFilename() {
		return "feed-plugin.vm";
	}

	@Override
	protected Map populateModel(DataBinder binder, FeedPluginCommand command, Map params) throws PluginException {
		Map model = new HashMap();

		logger.info("Rendering:<" + command +">");

		try {
			String feedData = fetchFeedDataAndCache(command);

			// fetch feed
			SyndFeedInput in = new SyndFeedInput();

			SyndFeed feed = in.build(new StringReader(feedData));

			// trim entries to max size
			while (feed.getEntries().size() > command.getMax()) {
				feed.getEntries().remove(feed.getEntries().size()-1);
			}

			model.put("feed", feed);
			model.put("feedDetails", Boolean.valueOf(command.isFeedDetails()));
			model.put("entryDetails", Boolean.valueOf(command.isEntryDetails()));
			model.put("contextPath", getApplicationContextPath(getWikiContext()));
		} catch (Throwable ex) {
			String msg = "Failed to fetch feed from " + StringEscapeUtils.escapeHtml(command.getUrl());

			throw new PluginException(msg, ex);
		}

		return model;
	}

	protected String fetchFeedDataAndCache(final FeedPluginCommand command) {
		String cacheKey = command.getUrl();
		WithCache<String, String> withCache = new WithCache<String, String>(cache, cacheKey).setTimeToLive(command.getCacheTime());
		String cached = withCache.call(new Supplier<String>() {

			@Override
			public String get() {
				try {
					InputStream is = fetchFeedData(command);
					try {
						String data = IOUtils.toString(is, Charsets.UTF_8);
						return data;
					} finally {
						IOUtils.closeQuietly(is);
					}
				} catch (IOException ex) {
					throw new RuntimeException(ex.getMessage(), ex);
				}
			}
		});
		return cached;
	}

	protected InputStream fetchFeedData(FeedPluginCommand command) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("Fetching feed <" + command.getUrl() +">");
		}

		URL feedURL = new URL(command.getUrl());
		URLConnection openConnection = feedURL.openConnection();
		try {
			// convert timeout to milliseconds
			int iTimeout = command.getTimeout() * 1000;
			openConnection.setConnectTimeout(iTimeout);
			openConnection.setReadTimeout(iTimeout);
		} catch (Exception ex) {
			logger.warn("Can not set timeout values on connection", ex);
		}
		return openConnection.getInputStream();
	}

}
