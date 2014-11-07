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
 *
 * Created: 02.08.2007
 */

package com.intland.codebeamer.wiki.plugins.base;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.controller.ControllerUtils;
import com.intland.codebeamer.manager.ArtifactManager;
import com.intland.codebeamer.manager.UrlShortenerManager;
import com.intland.codebeamer.manager.UserManager;
import com.intland.codebeamer.manager.util.ActionData;
import com.intland.codebeamer.persistence.dto.ArtifactDto;
import com.intland.codebeamer.persistence.dto.BinaryStreamDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.util.ArtifactRevision;
import com.intland.codebeamer.utils.OfficeUtils;
import com.intland.codebeamer.wiki.CodeBeamerWikiContext;
import com.intland.codebeamer.wiki.WikiInlinedResourceProvider;

/**
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 * @version $Id: KlausMehling 2009-09-08 09:50 +0000 22554:61759bf5389c  $
 */
public abstract class AbstractArtifactAwareWikiPlugin extends AbstractCodeBeamerWikiPlugin {
	abstract protected Integer getArtifactId();
	abstract protected String getSrc();
	abstract protected String getIconName();

	private final static ArtifactManager artifactManager = ArtifactManager.getInstance();
	private ArtifactDto document;
	@Autowired
	private UrlShortenerManager urlShortenerManager;
	@Autowired
	private MessageSource messageSource;

	private boolean autowired = false;

	protected ArtifactDto getArtifact() {
		return document;
	}

	/**
	 * autowire the urlShortenerManager
	 */
	public void autowire(WikiContext wikiContext) {
		if (!autowired) {
			ControllerUtils.autoWire(this, getApplicationContext(wikiContext));
			autowired = true;
		}
	}

	public InputStream getInputStream(CodeBeamerWikiContext context) throws Exception {
		URL url = null;
		InputStream retval = null;

		UserDto user = context.getUser();

		Integer docId = getArtifactId();

		if (docId != null) {
			ArtifactRevision<ArtifactDto> revision = new ArtifactRevision<ArtifactDto>(docId, null, null);
			document = artifactManager.findById(user, docId);

			BinaryStreamDto content = artifactManager.getContent(user, revision, new ActionData(context.getHttpRequest()));
			if (content != null) {
				retval = content.getInputStream();
			} else {
				Integer userId = user == null ? Integer.valueOf(-1) : user.getId();
				String msg = "Document (id: " + docId + ") not accessible by user (id: " + userId + ")";
				throw new PluginException(msg);
			}
		} else {
			String src = getSrc();
			if (src != null) {
				// first, assume that src is a URL
				try {
					url = new URL(src);
					retval = url.openStream();
				} catch(MalformedURLException urlex) {
					// Trying from classpath
					try {
						retval = getClass().getResourceAsStream(src);
					} catch (Throwable ex) {
					}

					if (retval == null) {
						// if it wasn't a valid URL, try it as a file reference
						// this would work, for instance for "C:\foo.xls" or "\\myserver\share\foo.xls"
						try {
							retval = new FileInputStream(src);
						} catch(FileNotFoundException ex) {
							// fall back to using it as attachment reference
							retval = WikiInlinedResourceProvider.getInstance().getArtifactOrAttachmentInputStreamByReference(context, src);
						}
					}
				}
			} else {
				throw new PluginException("Either 'src' or 'id' parameter must be used");
			}
		}

		if (retval != null) {
			retval = new BufferedInputStream(retval);
		}
		return retval;
	}

	protected void writeOfficeEditControl(UserDto user, Writer bw, String contextPath) throws IOException {
		if (document != null) {
			Date lastModifiedAt = document.getLastModifiedAt() == null ? document.getCreatedAt() : document.getLastModifiedAt();
			String lastModifiedAtString = null;
			String lastModifiedByString = null;
			if (user != null) {
				FastDateFormat dateFormat = user.getDateTimeFormat();
				lastModifiedAtString = dateFormat.format(lastModifiedAt);
				lastModifiedByString = UserManager.getInstance().getAliasName(user);
			} else {
				lastModifiedAtString = lastModifiedAt.toString();
			}

			String title = "[DOC: " + document.getId() + "] Project: " + document.getProject().getName()
					+ "; Document: " + document.getPath() + "; Last modified: " + lastModifiedAtString;
			if (lastModifiedByString != null) {
				title += " (by " + lastModifiedByString + ")";
			}

			bw.write(showIcon(getIconName(), contextPath)
					+ "<a href=\"" + contextPath + document.getUrlLink() + "\" "
					+ "title=\"" + StringEscapeUtils.escapeHtml(title) + "\">"
					+ StringEscapeUtils.escapeHtml(document.getName()) + "</a>");

			if (document.isWritable()) {
				if (urlShortenerManager != null) {
					String fullpath = this.urlShortenerManager.createLiveEditURI(OfficeUtils.OFFICE_EDIT_DOCUMENT,
							document.getName(), user, document.getId());
					String onclick = "OfficeEdit.doOfficeEditing(" + document.getId() + ",'" + fullpath + "', '" + contextPath
							+ "'); return false;";
					Locale loc = user.getLocale();
					Object[] args = new Object[] {};
					String editViaOffice = messageSource.getMessage("document.liveEdit.label", args, loc);
					bw.write("<a href=\"#\" onclick=\"" + onclick + "\" " + "title=\"" + editViaOffice + "\">"
							+ showIcon(getOfficeEditIcon(), contextPath) + "</a>");
				}
			}
		}
	}

	protected String getOfficeEditIcon(){
		return "newskin/action/icon_wedit.png";
	}

	protected String showIcon(String iconName, String contextPath) {
		return "<img src=\"" + contextPath + "/images/" + iconName + "\" border=\"0\" width=\"16\" height=\"16\" />";
	}
}
