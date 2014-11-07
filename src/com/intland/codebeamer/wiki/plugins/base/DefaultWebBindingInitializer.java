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

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.request.WebRequest;
import org.springmodules.validation.validator.CompoundValidator;

import com.intland.codebeamer.utils.AnchoredPeriod;
import com.intland.codebeamer.utils.AnchoredPeriodPropertyEditor;

/**
 * Default binder which does <code>javax.validation</code> (JSR-303) validation.
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class DefaultWebBindingInitializer extends ConfigurableWebBindingInitializer {

	public DefaultWebBindingInitializer() {
		setValidator(new CompoundValidator());
		configureBeanValidation();
		addPropertyEditorRegistrar(new PropertyEditorRegistrar() {

			public void registerCustomEditors(PropertyEditorRegistry registry) {
				registry.registerCustomEditor(AnchoredPeriod.class, new AnchoredPeriodPropertyEditor());
			}
		});
	}

	/**
	 * Ensure that the binder contains compound-validator, if not then wrap existing validator in a compound
	 * @return The compound validator
	 */
	protected CompoundValidator getCompoundValidator() {
		if (getValidator() instanceof CompoundValidator) {
			return (CompoundValidator) getValidator();
		}
		// wrap the existing validator to a compound one
		CompoundValidator compo = new CompoundValidator();
		if (getValidator() != null) {
			compo.addValidator(getValidator());
		}
		setValidator(compo);
		return compo;
	}

	protected void configureBeanValidation() {
		// This does the <code>javax.validation</code> (JSR-303) validation
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		addValidator(validator);
	}

	/**
	 * Add a new PropertyEditorRegistar and keep all the previous ones
	 * @param registar The new registar
	 */
	protected void addPropertyEditorRegistrar(PropertyEditorRegistrar registar) {
		PropertyEditorRegistrar[] oldregs = getPropertyEditorRegistrars();
		int oldlen = (oldregs == null ? 0 : oldregs.length);
		PropertyEditorRegistrar[] newregs = new PropertyEditorRegistrar[oldlen + 1];
		if (oldregs != null) {
			System.arraycopy(oldregs, 0, newregs, 0, oldregs.length);
		}
		newregs[oldlen] = registar;
		setPropertyEditorRegistrars(newregs);
	}

	/**
	 * Add a new validator, and keep existing validators too
	 * @param validator The new validator
	 */
	protected void addValidator(Validator validator) {
		if (validator == null) {
			return;
		}
		getCompoundValidator().addValidator(validator);
	}

	/**
	 * If target object can register their own bindings if they implement the {@link PropertyEditorRegistrar} interface
	 */
	protected boolean registerTargetBindings = true;

	@Override
	public void initBinder(WebDataBinder binder, WebRequest request) {
		super.initBinder(binder, request);

		if (registerTargetBindings && binder.getTarget() instanceof PropertyEditorRegistrar) {
			((PropertyEditorRegistrar) binder.getTarget()).registerCustomEditors(binder);
		}
	}

}