/**
 * Copyright 2009 Humboldt University of Berlin, INRIA.
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
	public static final String PREFIX = "paulaExporter.";
	public static final String PROP_VALIDATE_OUTPUT = PREFIX + "validateOutput";

	public static final String KEY_YES = "yes";
	public static final String KEY_NO = "no";

	public PAULAExporterProperties() {
		this.addProperty(new PepperModuleProperty<String>(PROP_VALIDATE_OUTPUT, String.class, "The PAULAExporter supports the validation of the output files against the PAULA DTDs by parsing the created PAULA documents. To enable this function, the property file needs to have a property'" + PROP_VALIDATE_OUTPUT + "' with the value '" + KEY_YES + "'. The default value is '" + KEY_NO + "'.", KEY_NO, false));
	}

	/**
	 * Returns if the output shall be validated against the paula dtds.
	 * 
	 * @return
	 */
	public Boolean getIsValidate() {
		Boolean retVal = false;
		PepperModuleProperty<String> prop = (PepperModuleProperty<String>) this.getProperty(PROP_VALIDATE_OUTPUT);
		if (KEY_YES.equals(prop.getValue())) {
			retVal = true;
		} else {
			retVal = false;
		}
		return (retVal);
	}
}
