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

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.request.ServletWebRequest;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.plugin.PluginManager;
import com.intland.codebeamer.controller.support.SimpleMessageResolver;
import com.intland.codebeamer.persistence.util.exception.CodebeamerRuntimeException;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractWikiPluginCommand;

/**
 * Simple wiki plugin base class that uses Spring data-binding to bind wiki plugin
 * parameters to a command object. A bit modelled after the Spring's Abstract/BaseCommandController,
 * but not exactly.
 *
 * @param <Command> Generic parameter, the type for the command bean.
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 * $Id$
 */
public abstract class AbstractCommandWikiPlugin<Command> extends AutoWiringCodeBeamerPlugin {

	private final static Logger logger =  Logger.getLogger(AbstractCommandWikiPlugin.class);

	/** Name for the command during binding. */
	protected String commandName = "";

	protected ConfigurableWebBindingInitializer webBindingInitializer;

	/**
	 * If the unknown fields are ignored. When false then an exception will be thrown if an invalid parameter
	 * arrives, which can not be bound.
	 */
	protected boolean ignoreUnknownFields = false;

	public boolean isIgnoreUnknownFields() {
		return ignoreUnknownFields;
	}

	public void setIgnoreUnknownFields(boolean ignoreUnknownFields) {
		this.ignoreUnknownFields = ignoreUnknownFields;
	}

	/**
	 * Populate the velocity context, before the template gets rendered.
	 */
	public final void populateContext(VelocityContext velocityContext, Map params) throws PluginException {
		Command command = createCommand();

		DataBinder binder = bindAndValidate(command, getWikiContext().getHttpRequest(), params);

		if (binder.getBindingResult().hasErrors()) {
			String bindingErrorString = bindingResultToErrorString(binder.getBindingResult());
			throw new PluginException(bindingErrorString);
		}

		// populate the model
		Map model = populateModel(binder, command, params);
		if (model != null) {
			putAll(velocityContext, model);
		}
		velocityContext.put(StringUtils.isEmpty(commandName) ? "command" : commandName, command);
	}

	/**
	 * Bind and validate the command.
	 */
	public DataBinder bindAndValidate(Command command, HttpServletRequest request, Map params) {
		// bind
		WebDataBinder binder = createBinder(command);
		binder.setIgnoreUnknownFields(ignoreUnknownFields);
		if (webBindingInitializer != null) {
			// use a mock request in notification mails
			if(request == null) {
				request = new MockHttpServletRequest();
			}
			webBindingInitializer.initBinder(binder, new ServletWebRequest(request));
		}
		initBinder(binder, command, params);

		binder.bind(new MutablePropertyValues(params));
		// TODO: manually bind the JSP's body, would be nice to use some binding config for this
		if (command instanceof AbstractWikiPluginCommand) {
			 ((AbstractWikiPluginCommand) command).setPluginBody((String) params.get(PluginManager.PARAM_BODY));
		}
		
		onBind(binder, command, params);
		if (binder.getBindingResult().hasErrors()) {
			onBindingErrors(binder, command, params);
		} else {
			if (binder.getValidator() != null) {
				binder.validate();
			}

			try {
				validate(binder, command, params);
			} catch (NamedPluginException ex) {
				ObjectError err = new ObjectError("command", new String[] {ex.getMessage()} , null, ex.getMessage());

				binder.getBindingResult().addError(err);
			}
		}
		return binder;
	}

	/**
	 * Add some error message about a property.
	 */
	protected void addError(DataBinder binder, String objectName, String msg) {
		binder.getBindingResult().addError(new ObjectError(objectName, msg));
	}

	/**
	 * Create a new command where the parameters will be bound to.
	 */
	public abstract Command createCommand() throws PluginException;

	/**
	 * Create the data binder for the command.
	 */
	protected WebDataBinder createBinder(Command command) {
		WebDataBinder binder = new WebDataBinder(command, commandName);
		return binder;
	}

	/**
	 * Override this method to initialize the binder.
	 */
	protected void initBinder(DataBinder binder, Command command, Map params) {
	}

	/**
	 * Custom post-processing after the binder finished allows custom binding.
	 */
	protected void onBind(DataBinder binder, Command command, Map params) {
	}

	/**
	 * Called after the binding done, if the binding has errors. Override to handle them
	 */
	protected void onBindingErrors(DataBinder binder, Command command, Map params) {
	}

	/**
	 * Validate called when binding is ok, to validate the bound object.
	 * @throws NamedPluginException
	 */
	protected void validate(DataBinder binder, Command command, Map params) throws NamedPluginException {
	}

	/**
	 * Returns the URL query string formed of request parameters as the properties of
	 * the passed command object.
	 */
	protected String getRequestParamsByCommand(Command command) {
		return getRequestParamsByCommand(command, false);
	}

	/**
	 * Returns the URL query string formed of request parameters as the properties of
	 * the passed command object.
	 *
	 * @param command The bean to convert to request parameters
	 * @param full If all parameters (even @Ignored) are put on the url
	 */
	public String getRequestParamsByCommand(Command command, boolean full) {
		BeanToQueryParametersConverter converter = getBeanToQueryParametersConverter();
		converter.setUseIgnoreAnnotation(!full);
		return converter.convertToQueryParameters(command);
	}
	
	/**
	 * Get the conveter from bean to query parameters
	 * @return
	 */
	protected BeanToQueryParametersConverter getBeanToQueryParametersConverter() {
		BeanToQueryParametersConverter converter = new BeanToQueryParametersConverter();
		converter.setWebBindingInitializer(getWebBindingInitializer());
		return converter;
	}
	
	/**
	 * Populate the model to be used with Velocity context
	 * @param params contains contains the original wiki plugin parameters.
	 * @return the model object.
	 */
	protected abstract Map populateModel(DataBinder binder, Command command, Map params) throws PluginException;

	/**
	 * Transforms binding result to a human-readable HTML error message.
	 * If the message is wrapped inside "{code}" that "code" will be resolved using MessageSource
	 */
	private String bindingResultToErrorString(BindingResult bindingResult) {
		StringBuilder buffer = new StringBuilder();

		SimpleMessageResolver resolver = getSimpleMessageResolver();

		for(Iterator<ObjectError> it = bindingResult.getAllErrors().iterator(); it.hasNext();) {
			ObjectError error = it.next();

			String message = null;
			if (error instanceof FieldError) {
				FieldError fe = (FieldError) error;
				String field = fe.getField();
				buffer.append('"').append(field).append("\" ");
			}

			message = resolver.getMessage(error);
			// backwards compatibility: if the defaultMessage is surrounded by "{...}"-s treat it as a message code
			if (message.equals(error.getDefaultMessage()) && StringUtils.startsWith(message, "{") && StringUtils.endsWith(message, "}")) {
				message = message.substring(1, message.length() - 1);
				message = resolver.getMessage(message, error.getArguments(), message);
			}

			buffer.append(message);
			if(it.hasNext()) {
				buffer.append("<br>");
			}
		}

		return buffer.toString();
	}

	public ConfigurableWebBindingInitializer getWebBindingInitializer() {
		return webBindingInitializer;
	}

	public void setWebBindingInitializer(ConfigurableWebBindingInitializer webBindingInitializer) {
		this.webBindingInitializer = webBindingInitializer;
	}

	/**
	 * Helper binds and validates the command using the plugin's binding mechanism, and throws an exception if binding fails
	 * It throws exceptions if the binding or validation fails
	 * @return The newly created/bound command
	 *
	 * @throws RuntimeException if the bind/validation failed
	 */
	public Command bindAndValidateFromRequest(HttpServletRequest request) {
		setIgnoreUnknownFields(true);

		Command command;
		try {
			command = createCommand();
		} catch (PluginException ex) {
			throw new CodebeamerRuntimeException("Command can not be created", ex);
		}

		Map params = request.getParameterMap();
		DataBinder binder = bindAndValidate(command, request, params);
		if (binder.getBindingResult().hasErrors()) {
			throw new CodebeamerRuntimeException("Binding and validation failed:" + binder.getBindingResult());
		}
		logger.debug("Bound command:" + command);
		return command;
	}
}
