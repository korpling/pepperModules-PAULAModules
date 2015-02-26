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
package de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula;

import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.PepperModuleProperties;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.PepperModuleProperty;

/**
 * Customization properties for {@link PAULAExporter}.
 * 
 * @author Florian Zipser
 *
 */
public class PAULAExporterProperties extends PepperModuleProperties {
	
	public static final String PROP_HUMAN_READABLE = "humanReadable";

	public PAULAExporterProperties() {
		this.addProperty(new PepperModuleProperty<Boolean>(PROP_HUMAN_READABLE, Boolean.class, "Setting this property to '"+Boolean.TRUE+"' produces an output with comments containing the text, wich is overlapped ba a node like <struct> or <mark>.",false, false));
	}

	/**
	 * Returns whether the output should contain the text overlapped by nodes as comments.
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
}
