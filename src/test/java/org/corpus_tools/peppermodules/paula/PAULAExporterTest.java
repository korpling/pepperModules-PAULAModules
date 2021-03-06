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

import org.corpus_tools.pepper.common.FormatDesc;
import org.corpus_tools.pepper.testFramework.PepperExporterTest;
import org.junit.Before;

public class PAULAExporterTest extends PepperExporterTest {

	@Before
	public void beforeEach() {
		super.setFixture(new PAULAExporter());
		addFormatWhichShouldBeSupported(new FormatDesc.FormatDescBuilder().withName(PAULAExporter.FORMAT_NAME)
				.withVersion(PAULAExporter.FORMAT_VERSION).build());
	}
}
