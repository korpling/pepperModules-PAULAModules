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
package org.corpus_tools.peppermodules.paula.tests;

import static org.junit.Assert.fail;

import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.peppermodules.paula.PAULAExporter;
import org.junit.Before;
import org.junit.Test;

public class PAULAExporterTest {
	private PAULAExporter fixture = null;

	public PAULAExporter getFixture() {
		return fixture;
	}

	public void setFixture(PAULAExporter fixture) {
		this.fixture = fixture;
	}

	@Before
	public void setUp() {
		this.setFixture(new PAULAExporter());
	}

	@Test
	public void testMapCorpusStructure() {
		try {
			getFixture().mapCorpusStructure(null, null);
			fail("Null corpus Graph");
		} catch (PepperModuleException e) {

		}

	}
}
