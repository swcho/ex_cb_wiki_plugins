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

import java.beans.PropertyEditor;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;

import com.intland.codebeamer.utils.URLCoder;

/**
 * Convert a bean to an query-parameters. Useful for passing a command-bean's properties via the url parameters
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class BeanToQueryParametersConverter<T> {

	private final static Logger logger = Logger.getLogger(BeanToQueryParametersConverter.class);

	/**
	 * Annotation used to mark a field as NOT to appear in the query parameters.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target( {/* ElementType.METHOD, */ElementType.FIELD })
	@Documented
	public @interface Ignored {
	}

	private final static ToStringStyle style = new ToStringStyle() {

		{
			setUseClassName(false);
			setUseFieldNames(true);
			setUseIdentityHashCode(false);
			setUseShortClassName(false);
			setContentEnd("");
			setContentStart("");
			setFieldSeparator("&");
			setArraySeparator(",");
			setArrayStart("");
			setArrayEnd("");
			setNullText("");
		}

	};

	/**
	 * If transients are added to the Query string
	 */
	private boolean appendTransients = false;

	/**
	 * If null fields are added to the Query string
	 */
	private boolean appendNullFields = false;

	/**
	 * If the @Ignored annotation is used
	 */
	private boolean useIgnoreAnnotation = true;
	
	// the names of the excluded fields
	private String[] excludedFieldNames = new String[0];
	
	/**
	 * Use a ConfigurableWebBindingInitializer to set up the property-editors for conversion.
	 */
	@Autowired
	protected ConfigurableWebBindingInitializer webBindingInitializer;
	
	private PropertyEditorRegistry propertyEditors;

	/**
	 * If transients are added to the Query string
	 */
	public boolean isAppendTransients() {
		return appendTransients;
	}
	public void setAppendTransients(boolean appendTransients) {
		this.appendTransients = appendTransients;
	}

	/**
	 * If null fields are added to the Query string
	 */
	public boolean isAppendNullFields() {
		return appendNullFields;
	}
	public void setAppendNullFields(boolean appendNullFields) {
		this.appendNullFields = appendNullFields;
	}

	/**
	 * If the @Ignored annotation is used. Defaults to true.
	 */
	public boolean isUseIgnoreAnnotation() {
		return useIgnoreAnnotation;
	}
	public void setUseIgnoreAnnotation(boolean useIgnoreAnnotation) {
		this.useIgnoreAnnotation = useIgnoreAnnotation;
	}

	/**
	 * Init property editors from the bean using the BindingInitializer
	 * @param bean
	 */
	protected void initPropertyEditors(T bean) {
		if (propertyEditors == null && webBindingInitializer != null) {
			propertyEditors = createPropertyEditorRegistry(bean, webBindingInitializer);
		}
	}
	
	/**
	 * Convert a bean to query parameters
	 * @param bean
	 * @return
	 */
	public String convertToQueryParameters(final T bean) {
		initPropertyEditors(bean);
		
		ReflectionToStringBuilder reflect = new ReflectionToStringBuilder(bean, style) {

			/* (non-Javadoc)
			 * @see org.apache.commons.lang.builder.ReflectionToStringBuilder#getValue(java.lang.reflect.Field)
			 */
			@Override
			protected Object getValue(Field field) throws IllegalArgumentException, IllegalAccessException {
				Object value = super.getValue(field);
				if (value != null && propertyEditors != null) {
					PropertyEditor customEditor = propertyEditors.findCustomEditor(value.getClass(), field.getName());
					try {
						if (customEditor != null) {
							customEditor.setValue(value);
							String text = customEditor.getAsText();
							return text;
						}
					} catch (Throwable th) {
						logger.debug("Ignoring converter of field" + field +" on " + bean, th);
					}
				}
				return value;
			}

			@Override
			protected boolean accept(Field field) {
				boolean accept = super.accept(field);
				if (accept && useIgnoreAnnotation) {
					if (field.getAnnotation(Ignored.class) != null) {
						return false;
					}
				}
				if (accept && appendNullFields == false) {
					Object value;
					try {
						value = getValue(field);
						accept = (value != null);
					} catch (IllegalArgumentException e) {
						logger.trace("Can not determine if field is null, skipping", e);
					} catch (IllegalAccessException e) {
						logger.trace("Can not determine if field is null, skipping", e);
					}
				}
				return accept;
			}

		};
		reflect.setExcludeFieldNames(excludedFieldNames);
		reflect.setAppendTransients(appendTransients); // one can mark fields not to get to url as transient
		reflect.setAppendStatics(false);
		String result = reflect.toString();
		result = encodeURLParams(result);
		return result;
	}

	private String encodeURLParams(String params) {
		String[] pars = params.split("\\&");
		StringBuilder result = new StringBuilder(params.length() * 2);
		for (String par: pars) {
			if (result.length() != 0) {
				result.append("&");
			}
			int i = par.indexOf("=");
			if (i != -1) {
				String name = par.substring(0, i+1);
				String value = par.substring(i+1);
				value = URLCoder.encode(value);	// UTF8 encoding
				result.append(name).append(value);
			} else {
				// can not happen, but handling anyway
				result.append(par);
			}
		}
		return result.toString();
	}
	
	public String[] getExcludedFieldNames() {
		return excludedFieldNames;
	}
	
	public ConfigurableWebBindingInitializer getWebBindingInitializer() {
		return webBindingInitializer;
	}
	public BeanToQueryParametersConverter setWebBindingInitializer(ConfigurableWebBindingInitializer webBindingInitializer) {
		this.webBindingInitializer = webBindingInitializer;
		return this;
	}
	
	/**
	 * @param excludedFieldNames the field names to exclude
	 * @return 
	 */
	public BeanToQueryParametersConverter setExcludedFieldNames(String... excludedFieldNames) {
		this.excludedFieldNames = excludedFieldNames;
		return this;
	}

	/**
	 * Create a PropertyEditorRegistry where a BindingInitializer configures the property editors.
	 * @param bean The bean to bind for
	 * @param bindingInitializer The binding initializer
	 * @return The property editors
	 */
	public static PropertyEditorRegistry createPropertyEditorRegistry(Object bean, ConfigurableWebBindingInitializer bindingInitializer) {
		if (bindingInitializer == null) {
			return null;
		}
		WebDataBinder binder = new WebDataBinder(bean);
		bindingInitializer.initBinder(binder, null);
		return binder;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}

}
