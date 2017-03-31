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
 */
public class PAULAImporterProperties extends PepperModuleProperties {

	public static final String PROP_EMPTY_NAMESPACE = "emptyNamespace";
	public static final String PROP_ANNO_NAMESPACE_FROM_FILE = "annoNamespaceFromFile";

	public PAULAImporterProperties() {
		this.addProperty(new PepperModuleProperty<String>(PROP_EMPTY_NAMESPACE, String.class,
				"The name of the default namespace which should be treated as if the namespace of an element is empty. Default is \"no_layer\"",
				"no_layer", false));
		this.addProperty(new PepperModuleProperty<Boolean>(PROP_ANNO_NAMESPACE_FROM_FILE, Boolean.class,
				"If 'true' inherit the annotation namespace from the namespace part of the file name when no explicit namespace is given in dot notation (\"namespace.name\") in the name itself. Default is 'true'.",
				true, false));
	}

	public String getEmptyNamespace() {
		String retVal = "";
		PepperModuleProperty<String> prop = (PepperModuleProperty<String>) this.getProperty(PROP_EMPTY_NAMESPACE);
		if (prop.getValue() != null) {
			retVal = prop.getValue();
		}
		return retVal;
	}

	public boolean getAnnoNamespaceFromFile() {
		PepperModuleProperty<Boolean> prop = (PepperModuleProperty<Boolean>) this
				.getProperty(PROP_ANNO_NAMESPACE_FROM_FILE);
		return prop.getValue();
	}
}
