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
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.model.StyleDescription;
import org.apache.poi.hwpf.model.StyleSheet;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.ListEntry;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Section;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.wiki.CodeBeamerWikiContext;
import com.intland.codebeamer.wiki.plugins.base.AbstractArtifactAwareWikiPlugin;

/* Original Copyright
 *
 * Copyright (c) 2005, 2006 Bob Swift
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * 			 notice, this list of conditions and the following disclaimer in the
 *   		 documentation and/or other materials provided with the distribution.
 *     * The names of contributors may not be used to endorse or promote products
 *           derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Created on Jan 27, 2006 by Bob Swift
 */

/**
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 * @version $Id$
 */
public class WordPlugin extends AbstractArtifactAwareWikiPlugin {
	private final static Logger log = Logger.getLogger(WordPlugin.class);

	private boolean showInvisible = false;
	private Integer documentId = null;
	private String src;

	public String execute(WikiContext wikiContext, Map params) throws PluginException {
		autowire(wikiContext);
		CodeBeamerWikiContext context = (CodeBeamerWikiContext) wikiContext;

		processParameters(params);

		try {
			HWPFDocument doc = new HWPFDocument(getInputStream((CodeBeamerWikiContext)wikiContext));

			return wordToHtml(doc,  super.getUserFromContext(context), super.getContextPath(context));
		} catch(Throwable ex) {
			String msg = ex.toString();

			log.warn(msg, ex);

			throw new PluginException(msg);
		}
	}

	private void processParameters(Map params) {
		documentId = Integer.valueOf(NumberUtils.toInt(super.getParameter(params, "id")));
		src = super.getParameter(params, "src");
		showInvisible = Boolean.parseBoolean(super.getParameter(params, "showHidden"));
	}

	private String wordToHtml(HWPFDocument document, UserDto user, String contextPath)  throws IOException {
		Writer htmlResult = new StringWriter();

		writeOfficeEditControl(user, htmlResult, contextPath);

		processDocument(htmlResult, document);

		return htmlResult.toString();
	}

	private void processDocument(Writer htmlResult, HWPFDocument document) throws IOException {
		StyleSheet styleSheet = document.getStyleSheet();
		Range range = document.getRange();

		boolean isListStart = true;
		boolean isInList = false;
		boolean isRowStart = true;
		boolean isInTable = false;

		for (int sectionNumber = 0; sectionNumber < range.numSections(); sectionNumber++) {
			Section section = range.getSection(sectionNumber);

			for (int index = 0; index < section.numCharacterRuns(); index++) {
				Paragraph paragraph = null;
				try {
					paragraph = section.getParagraph(index);
				} catch (Throwable ex) {
					log.warn("Exception: " + ex);
				}

				if (log.isDebugEnabled()) {
					log.debug("Index: " + sectionNumber + '/' + index);
				}

				if (paragraph == null) {
					continue;
				}

				int styleIndex = paragraph.getStyleIndex();
				StyleDescription styleDescription = styleSheet.getStyleDescription(styleIndex);
				String styleDescriptionName = styleDescription.getName();
				if (log.isDebugEnabled()) {
					log.debug("Style name <" + styleDescriptionName + "> index: " + styleIndex);
				}

				boolean isHeading = styleDescriptionName.contains("Heading");

				if (!isHeading && paragraph instanceof ListEntry) {
					if (!isInList) {
						isListStart = true;
						isInList = true;
					} else {
						isListStart = false;
					}
				} else if (isInList) {
					isInList = false;
					htmlResult.append("</ul>");
				}

				if (paragraph.isInTable()) {
					if (!isInTable) {
						htmlResult.append("\n<table class=\"CBwikiTable\">\n");
						isInTable = true;
						isRowStart = true;
					}

					if (isRowStart) {
						htmlResult.append("\n<tr>\n");
						isRowStart = false;
					}

					if (paragraph.isTableRowEnd()) {
						htmlResult.append("\n</tr>\n");
						isRowStart = true;
					} else  if (!isInList) {
						htmlResult.append("\n<td class=\"textData\">");
					}
				} else {
					if (isInTable) {
						htmlResult.append("</table>");
					}
					isInTable = false;
					isRowStart = false;
				}

				if (isInList) {
					if (isListStart) {
						htmlResult.append("<ul>");
						isListStart = false;
					}
					htmlResult.append("<li>");
				}

				if (isHeading) {
					int headingLevel = 6;
					if (styleDescriptionName.length() > 8) {
						try {
							headingLevel = Integer.parseInt(styleDescriptionName.substring(8,9));
						} catch (NumberFormatException ignore) {
						}
					}
					htmlResult.append("<h" + headingLevel + ">");

					appendParagraphData(htmlResult, paragraph, styleSheet, styleIndex);

					htmlResult.append("</h" + headingLevel + ">");
				} else {
					appendParagraphData(htmlResult, paragraph, styleSheet, styleIndex);
				}

				if (isInList) {
					htmlResult.append("</li>");
				}

				if (!isInList && isInTable && !paragraph.isTableRowEnd()) {
					htmlResult.append("</td>\n");
				}
			}
		}
	}

	/* These are just experimental values. */
	private int getCharacterRunFontSize(int size) {
		if (size < 10) {
			return 1;
		}

		if (size < 24) {
			return 2;
		}

		if (size < 28) {
			return 3;
		}

		if (size < 36) {
			return 4;
		}

		if (size < 48) {
			return 5;
		}

		if (size < 64) {
			return 6;
		}
		return 7;
	}

	private String getCharacterRunFont(CharacterRun characterRun) {
		String fontName = characterRun.getFontName();
		int fontSize = getCharacterRunFontSize(characterRun.getFontSize());

//		int characterRunColor = characterRun.getColor();

		String font = "<font size=\"" + fontSize + "\" face=\"" + fontName + "\">";
		return font;
	}

	private Writer appendParagraphData(Writer htmlResult, Paragraph paragraph, StyleSheet styleSheet, int styleIndex)  throws IOException {
		int numCharacterRuns = paragraph.numCharacterRuns();
		htmlResult.append("\n<p>"); // starting paragraph

		for (int runIndex = 0; runIndex < numCharacterRuns; runIndex++) {
			CharacterRun run = paragraph.getCharacterRun(runIndex);
			appendCharacterRun(htmlResult, run);
		}
		htmlResult.append("</p>");	// closing paragraph
		return htmlResult;
	}

	private Writer appendCharacterRun(Writer htmlResult, CharacterRun characterRun) {
		try {
			short subSuperScriptIndex = characterRun.getSubSuperScriptIndex();
			boolean isDeleted = characterRun.isMarkedDeleted() || characterRun.isFldVanished() || characterRun.isStrikeThrough() || characterRun.isDoubleStrikeThrough();
			if (characterRun.isMarkedDeleted() && log.isDebugEnabled()) {
				log.debug("isDeleted: " + isDeleted);
			}

			if (log.isDebugEnabled()) {
				log.debug("CharacterRun-text: " + characterRun.text().trim());
			}

			if (showInvisible || !characterRun.isFldVanished()) {
				String text = characterRun.text();
				boolean isBlank = text.trim().length() == 0;

				String font = getCharacterRunFont(characterRun);
				htmlResult.append(font);

				if (!isBlank && characterRun.isBold()) {
					htmlResult.append("<b>");
				}

				if (!isBlank && characterRun.isItalic()) {
					htmlResult.append("<i>");
				}

				if (!isBlank && characterRun.isHighlighted()) {
					htmlResult.append("<em>");
				}

				if (!isBlank && isDeleted) {
					htmlResult.append("<del>");
				}

				if (!isBlank && characterRun.getUnderlineCode() > 0) {
					htmlResult.append("<u>");
				}

				if (!isBlank && subSuperScriptIndex >= 1 && subSuperScriptIndex <= 2) {
					htmlResult.append("<sup>");
				}

				htmlResult.append(text);

				if (!isBlank && subSuperScriptIndex >= 1 && subSuperScriptIndex <= 2) {
					htmlResult.append("</sup>");
				}

				if (!isBlank && characterRun.getUnderlineCode() > 0) {
					htmlResult.append("</u>");
				}

				if (!isBlank && isDeleted) {
					htmlResult.append("</del>");
				}

				if (!isBlank && characterRun.isHighlighted()) {
					htmlResult.append("</em>");
				}

				if (!isBlank && characterRun.isItalic()) {
					htmlResult.append("</i>");
				}

				if (!isBlank && characterRun.isBold()) {
					htmlResult.append("</b>");
				}

				if (!isBlank && font.length() > 0) {
					htmlResult.append("</font>");
				}
			}
		} catch (Throwable ex) {
			if (log.isDebugEnabled()) {
				log.debug("appendCharacterRun exception: " + ex);
			}
		}
		return htmlResult;
	}

	protected String getIconName() {
		return "mime-word.gif";
	}

	protected Integer getArtifactId() {
		return documentId;
	}

	protected String getSrc() {
		return src;
	}
}
