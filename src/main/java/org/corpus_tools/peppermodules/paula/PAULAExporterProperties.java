/**
 * Copyright 2009 Humboldt-Universität zu Berlin, INRIA.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */
package org.corpus_tools.peppermodules.paula;

import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;

/**
 * Customization properties for {@link PAULAExporter}.
 * 
 * @author Florian Zipser
 *
 */
public class PAULAExporterProperties extends PepperModuleProperties {

	public static final String PROP_HUMAN_READABLE = "humanReadable";
	public static final String PROP_ANNO_NS_PREFIX = "annoNsPrefix";
	public static final String PROP_EMPTY_NAMESPACE = "emptyNamespace";

	public PAULAExporterProperties() {
		this.addProperty(new PepperModuleProperty<Boolean>(PROP_ANNO_NS_PREFIX, Boolean.class, "Setting this property to '" + Boolean.TRUE + "' uses annotation namespaces as an annotation name prefix 'ns.' before annotation names (e.g. a POS annotation with ns salt will be called 'salt.pos')", false, false));
		this.addProperty(new PepperModuleProperty<Boolean>(PROP_HUMAN_READABLE, Boolean.class,
				"Setting this property to '" + Boolean.TRUE
						+ "' produces an output with comments containing the text, which is overlapped by a node like <struct> or <mark>.",
				true, false));
		this.addProperty(new PepperModuleProperty<String>(PROP_EMPTY_NAMESPACE, String.class,
				"The name of the default namespace when the namespace of an element is empty. If empty or not set the output will also not contain a namespace. Default is \"no_layer\"",
				"no_layer", false));
	}

	/**
	 * Returns whether the output should contain the text overlapped by nodes as
	 * comments.
	 * 
	 * @return
	 */
	public Boolean isHumanReadable() {
		Boolean retVal = false;
		PepperModuleProperty<String> prop = (PepperModuleProperty<String>) this.getProperty(PROP_HUMAN_READABLE);
		if (Boolean.TRUE.equals(prop.getValue())) {
			retVal = true;
		} else {
			retVal = false;
		}
		return (retVal);
	}
	
	
	/**
	 * Returns whether to prefix annotation names
	 * with their namespace, e.g. 'salt.pos' 
	 * in the corresponding feat file's type attribute.
         * 
	 * @return
	 */
	public Boolean useAnnoNamespacePrefix() {
		Boolean retVal = false;
		PepperModuleProperty<String> prop = (PepperModuleProperty<String>) this.getProperty(PROP_ANNO_NS_PREFIX);
		if (Boolean.TRUE.equals(prop.getValue())) {
			retVal = true;
		} else {
			retVal = false;
		}
		return (retVal);
	}


	public String getEmptyNamespace() {
		String retVal = "";
		PepperModuleProperty<String> prop = (PepperModuleProperty<String>) this.getProperty(PROP_EMPTY_NAMESPACE);
		if (prop.getValue() != null) {
			retVal = prop.getValue();
		}
		return retVal;
	}
}
