package com.intland.codebeamer.wiki.plugins;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.wiki.CodeBeamerWikiContext;
import com.intland.codebeamer.wiki.WikiInlinedResourceProvider;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;

/**
 * Shockwave Flash plugin derived from an
 * <a href="http://www.jspwiki.org/wiki/AFlashPlugin">originally JSPWiki plugin</a>.
 * <p>
 * See <a href="http://kb.adobe.com/selfservice/viewContent.do?externalId=tn_12701&sliceId=1">this</a> for Adobe Flash plugin parameters.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id$
 */
public class FlashPlugin extends AbstractCodeBeamerWikiPlugin {
	public static final String PARAM_SRC      = "src";
	public static final String PARAM_PRM      = "parameters";
	public static final String PARAM_PLAY     = "play";
	public static final String PARAM_LOOP     = "loop";
	public static final String PARAM_QUALITY  = "quality";
	public static final String PARAM_HEIGHT   = "height";
	public static final String PARAM_WIDTH    = "width";
	public static final String PARAM_CTRL     = "controls";

	/*
		here is the syntax to wrap a flash file in HTML
		from http://www.adobe.com/go/tn_4150

		<OBJECT classid="clsid:D27CDB6E-AE6D-11cf-96B8-444553540000"
				codebase="http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=6,0,40,0"
				WIDTH="550"
				HEIGHT="400"
				id="myMovieName">
		  <PARAM NAME=movie VALUE="myFlashMovie.swf">
		  <PARAM NAME=quality VALUE=high>
		  <PARAM NAME=bgcolor VALUE=#FFFFFF>
		  <EMBED src="/support/flash/ts/documents/myFlashMovie.swf"
				 quality=high
				 bgcolor=#FFFFFF
				 WIDTH="550"
				 HEIGHT="400"
				 NAME="myMovieName"
				 ALIGN=""
				 TYPE="application/x-shockwave-flash"
				 PLUGINSPAGE="http://www.macromedia.com/go/getflashplayer">
		  </EMBED>
		</OBJECT>
	 */

	public String execute(WikiContext context, Map params) throws PluginException {
		String src = getParameter(params, PARAM_SRC);
		String controls = StringUtils.lowerCase(getParameter(params, PARAM_CTRL, "true"));
		String prm = getParameter(params, PARAM_PRM);
		String ht = getParameter(params, PARAM_HEIGHT, "350");
		String wt = getParameter(params, PARAM_WIDTH, "425");
		String play = StringUtils.lowerCase(getParameter(params, PARAM_PLAY, "true"));
		String loop = StringUtils.lowerCase(getParameter(params, PARAM_LOOP, "false"));
		String quality = StringUtils.lowerCase(getParameter(params, PARAM_QUALITY, "high"));
		String menu = StringUtils.lowerCase(getParameter(params, "menu", "true"));
		String scale = StringUtils.lowerCase(getParameter(params, "scale", "default"));
		String align = StringUtils.lowerCase(getParameter(params, "align", "l"));

		if (src == null) {
			throw new PluginException("Parameter 'src' is required for Flash plugin");
		}

		if (prm != null) {
			src += "?" + prm;
		}

		try {
			src = WikiInlinedResourceProvider.getInstance().getArtifactOrAttachmentUrlByReference((CodeBeamerWikiContext)context, src);
		} catch (WikiException e) {
			// do nothing it wasn't a valid reference
		}

		StringBuffer result = new StringBuffer();
		if (controls.equals("true")) {
			// the first tag to call the javascript library
			result.append("<script language=\"javascript\" src=\"" + context.getHttpRequest().getContextPath() + "/js/Flash.js\"></script>");
			// second tag actually kicks the js in
			result.append("<script language=\"javascript\">self.onload=setupSeekBar;");
			result.append("</script>");
		}
		result.append("<table border=\"0\" bgcolor=\"#CCCCCC\" cellpadding=\"2\" align=\"center\"><tr><td bgcolor=\"#FFFFFF\">");

	result.append("<object classid=\"clsid:D27CDB6E-AE6D-11cf-96B8-444553540000\" ");
		result.append("codebase=\"http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=6,0,40,0\"\n");
		result.append("width=\"" + wt + "\" height=\"" + ht + "\" id=\"myMovieName\">");
		result.append("<param name=\"bgcolor\" value=\"#FFFFFF\">");
		result.append("<param name=\"movie\" value=\"" + src + "\">");
		result.append("<param name=\"play\" value=\"" + play + "\">");
		result.append("<param name=\"loop\" value=\"" + loop + "\">");
		result.append("<param name=\"menu\" value=\"" + menu + "\">");
		result.append("<param name=\"quality\" value=\"" + quality + "\">");
		result.append("<param name=\"scale\" value=\"" + scale + "\">");
		result.append("<param name=\"quality\" value=\"" + quality + "\">");
		result.append("<param name=\"align\" value=\"" + align + "\">");
		result.append("<embed src=\"" + src + "\" "
				+ "bgcolor=\"#FFFFFF\" "
				+ "name=\"myMovieName\" "
				+ "play=\"" + play  + "\" "
				+ "loop=\"" + loop  + "\" "
				+ "menu=\"" + menu  + "\" "
				+ "quality=\"" + quality + "\" "
				+ "scale=\"" + scale + "\" "
				+ "align=\"" + align + "\" "
				+ "width=\"" + wt + "\" "
				+ "height=\"" + ht + "\" "
				+ "type=\"application/x-shockwave-flash\" "
				+ "pluginspage=\"http://www.macromedia.com/go/getflashplayer\"> </embed></object>\n");
		result.append("</td></tr></table>");

		return result.toString();
	}
}
