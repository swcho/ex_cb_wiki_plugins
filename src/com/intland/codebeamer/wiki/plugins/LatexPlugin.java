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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteWatchdog;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.plugin.PluginManager;
import com.intland.codebeamer.config.ExePaths;
import com.intland.codebeamer.controller.tempstorage.TempStorageController;
import com.intland.codebeamer.controller.tempstorage.TempStorageService;
import com.intland.codebeamer.controller.tempstorage.TempStorageService.StoreData;
import com.intland.codebeamer.utils.Common;
import com.intland.codebeamer.utils.SafeStopWatch;
import com.intland.codebeamer.utils.StreamDigester;
import com.intland.codebeamer.wiki.CodeBeamerWikiContext;
import com.intland.codebeamer.wiki.plugins.base.AbstractArtifactAwareWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin;
import com.intland.codebeamer.wiki.refs.ImageReference;
import com.sun.star.uno.RuntimeException;

/**
 * Plugin renders LaTeX text/document to an image and puts that on the page.
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class LatexPlugin extends AbstractCommandWikiPlugin<LatexPluginCommand> {

	private final static Logger logger = Logger.getLogger(LatexPlugin.class);

	@Autowired
	private TempStorageService tempStorageService;
	@Autowired(required=false) @Qualifier("latexPluginCache")
	private Cache cache;

	public LatexPlugin() {
		setIgnoreUnknownFields(true); // for backwards compatibility ignore the unbound/unknown fields
	}

	@Override
	public LatexPluginCommand createCommand() throws PluginException {
		return new LatexPluginCommand();
	}

	@Override
	protected String getTemplateFilename() {
		return "latex-plugin.vm";
	}

	protected String getLatexText(final LatexPluginCommand command, Map params) throws Exception {
		if (StringUtils.isNotEmpty(command.getText())) {
			return command.getText();
		}
		String text = (String) params.get(PluginManager.PARAM_BODY);
		if (StringUtils.isNotEmpty(text)) {
			return text;
		}

		if (command.getId() != null || StringUtils.isEmpty(command.getSrc()) == false) {
			// using the AbstractArtifactAwareWikiPlugin for loading the text by using
			// the doc_id or src from the wiki plugin parameters
			AbstractArtifactAwareWikiPlugin delegate = new AbstractArtifactAwareWikiPlugin() {

				public String execute(WikiContext context, Map _params) throws PluginException {
					return null;
				}

				@Override
				protected String getSrc() {
					return command.getSrc();
				}

				@Override
				protected String getIconName() {
					return null;
				}

				@Override
				protected Integer getArtifactId() {
					return command.getId();
				}
			};

			InputStream inputStream = delegate.getInputStream((CodeBeamerWikiContext) getWikiContext());
			text = Common.readFileToString(inputStream, command.getEncoding());
			text = StringUtils.remove(text, (char)0);
			text = StringUtils.trimToEmpty(text);
		}
		return text;
	}

	@Override
	protected Map populateModel(DataBinder binder, LatexPluginCommand command, Map params) throws PluginException {
		String latexText;
		try {
			latexText = getLatexText(command, params);
		} catch (Exception ex) {
			String msg = getSimpleMessageResolver().getMessage("latex.plugin.empty.content");
			throw new PluginException(msg, ex);
		}
		if (StringUtils.isEmpty(latexText)) {
			String msg = getSimpleMessageResolver().getMessage("latex.plugin.empty.content");
			throw new PluginException(msg);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Command:" + command);
			logger.debug("Rendering text:<" + latexText +">");
		}

		String imgSrcPath = ImageReference.EXTERNAL_URL;
		String contextPath = getContextPath();
		if (contextPath != null) {
			String format = command.isUsePng() ? "png" : "gif";

			String cacheKey = createImageAndStore(command, latexText, format);

			imgSrcPath = contextPath + TempStorageController.getUrl(cacheKey);
		}

		Map model = new HashMap<String, String>();
		model.put("command", command);
		model.put("imgSrc", imgSrcPath);
		String latexTextShort = StringUtils.abbreviate(StringUtils.trimToEmpty(latexText), 100);
		model.put("latexTextShort", latexTextShort);
		if (StringUtils.isEmpty(command.getTitle())) {
			command.setTitle(latexTextShort);
		}

		return model;
	}

	/**
	 * @return The temp-storage key for the image
	 */
	private String createImageAndStore(LatexPluginCommand command, String latexText, final String format) throws PluginException {
		String storageKey = null;
		String cacheKey = null;
		if (cache != null && command.isCache()) {
			cacheKey = StreamDigester.getSHA1Checksum(latexText) + "-" + command.getDensity() +"-" + format;
			Element cached = cache.get(cacheKey);
			if (cached != null) {
				storageKey = (String) cached.getValue();
				// check if the image is still available in temp-storage
				if (storageKey != null && tempStorageService.getData(storageKey) != null) {
					if (logger.isDebugEnabled()) {
						logger.debug("Latex plugin result found in cache for <" + latexText +">");
					}
					return storageKey;
				}
			}
		}

		SafeStopWatch sw = new SafeStopWatch();
		File image = null;
		sw.start();
		try {
			image = createImage(latexText, format, command);
			sw.stop();
		} catch (Exception ex) {
			String msg;
			if (ex instanceof IOException && ex.getMessage() != null && ex.getMessage().toLowerCase().contains("no such file")) {
				msg = getSimpleMessageResolver().getMessage("latex.plugin.not.available", ex.getMessage());
			} else {
				msg = getSimpleMessageResolver().getMessage("latex.plugin.failed.rendering", latexText);
			}

			throw new PluginException(msg, ex);
		}
		if (image == null || !image.exists()) {
			String msg = getSimpleMessageResolver().getMessage("latex.plugin.failed.rendering", latexText);
			throw new PluginException(msg);
		}

		StoreData imageData;
		try {
			imageData = new StoreData(image, "image/" + format);
		} catch (IOException ex) {
			throw new PluginException(ex.getMessage(), ex);
		}
		storageKey = tempStorageService.storeData(imageData);
		image.delete();

		logger.info("Created image file " + image.getAbsolutePath() + " length: " + image.length() + ", time:" + sw.getLastTaskTimeMillis());
		if (cache != null && command.isCache() && cacheKey != null) {
			cache.put(new Element(cacheKey, storageKey));
		}
		return storageKey;
	}

	protected String decorateLatexTextForRendering(LatexPluginCommand command, String latexText) {
		return latexText;
	}

	protected File createImage(String latexText, String imageformat, LatexPluginCommand command) throws Exception {
		latexText= decorateLatexTextForRendering(command, latexText);

		File latexInputFile = File.createTempFile("cbmt", ".tex");
		latexInputFile.delete();

		final File outputFile = getCompanionFile(latexInputFile, imageformat);

		Writer writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(latexInputFile));
			writer.write(latexText);
		} finally {
			if (writer != null) {
				writer.close();
			}
		}

		// The files below will be created by executed commands.
		File dviFile = getCompanionFile(latexInputFile, "dvi");
		File psFile = getCompanionFile(latexInputFile, "ps");

		try {
			// latex --interaction=nonstopmode math.tex
			List<String> args = new ArrayList<String>();
			args.add("latex");
			args.add("--interaction=nonstopmode");
			args.add(latexInputFile.getAbsolutePath());

			Executor latexExecutor = execCommand(args, latexInputFile.getParentFile(), command.getTimeout());
			if (!dviFile.exists()) {
				String msg = getSimpleMessageResolver().getMessage("latex.plugin.latex.conversion.failed.output",latexExecutor.getOut());
				throw new PluginException(msg);
			}

			if (dviFile.exists()) {
				// dvips
				args.clear();
				args.add("dvips");
				if (!logger.isInfoEnabled()) {
					args.add("-q");		// quiet
				}
				args.add("-E");
				args.add(dviFile.getAbsolutePath());
				args.add("-o");
				args.add(psFile.getAbsolutePath());

				execCommand(args, latexInputFile.getParentFile(), command.getTimeout());
			}

			if (psFile.exists()) {
				// convert
				args.clear();
				args.add("convert");
				if (logger.isDebugEnabled()) {
					args.add("-verbose");
				}
				args.add("-density");
				args.add(command.getDensity());

				if ("png".equals(imageformat)) {
					args.add("-trim");
					args.add("-transparent");
					args.add("#FFFFFF");
				} else {
					args.add("-background");
					args.add("white");
					args.add("-flatten");
				}
				args.add(psFile.getAbsolutePath());
				args.add(outputFile.getAbsolutePath());

				execCommand(args, latexInputFile.getParentFile(), command.getTimeout());
			}
		} finally {
			FileUtils.deleteQuietly(latexInputFile);
			FileUtils.deleteQuietly(dviFile);
			FileUtils.deleteQuietly(psFile);

			File auxFile = getCompanionFile(latexInputFile,"aux");
			FileUtils.deleteQuietly(auxFile);

			File logFile = getCompanionFile(latexInputFile, "log");
			FileUtils.deleteQuietly(logFile);
		}

		return outputFile;
	}

	/**
	 * Generate similar file-name as the latex file, but with different extension
	 */
	private File getCompanionFile(File latexInputFile, String extension) {
		String baseName = StringUtils.substringBeforeLast(latexInputFile.getName(), ".");
		File f = new File(latexInputFile.getParent(), baseName + "." + extension);
		return f;
	}

	class Executor {

		private ByteArrayOutputStream out;
		private ByteArrayOutputStream err;
		private int exitCode;

		public Executor execCommand(List<String> args, File workingDir, int timeout) throws Exception {
			// the first arg is the executable name, fix its path from the Environment
			String exeName = args.get(0);
			exeName = ExePaths.search(exeName);
			args.set(0, exeName);

			logger.info("Exec:" + args);

			out = new ByteArrayOutputStream(256);
			err = new ByteArrayOutputStream(128);
			PumpStreamHandler handler = new PumpStreamHandler(out, err);
			Execute exec = new Execute(handler, new ExecuteWatchdog(timeout*1000l));
			exec.setWorkingDirectory(workingDir);
			exec.setCommandline(args.toArray(new String[0]));
			exitCode = exec.execute();

			Level level = (exitCode == 0) ? Level.DEBUG : Level.INFO;
			if (logger.isEnabledFor(level)) {
				logger.log(level, "Exit: " + exitCode);
				logger.log(level, "output:<" + getOut() +">");
				logger.log(level, "error :<" + getErr() +">");
			}
			return this;
		}

		public int getExitCode() {
			return exitCode;
		}

		public String getOut() {
			return getString(out);
		}

		public String getErr() {
			return getString(err);
		}

		private String getString(ByteArrayOutputStream stream) {
			if (stream == null) {
				return "";
			}
			try {
				return new String(stream.toByteArray(), "UTF-8");
			} catch (UnsupportedEncodingException ex) {
				throw new RuntimeException(ex.getMessage(), ex);
			}
		}

	}

	/**
	 * @return Returns the out/err streams
	 */
	protected Executor execCommand(List<String> args, File workingDir, int timeout) throws Exception {
		return new Executor().execCommand(args, workingDir, timeout);
	}

}
