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
package org.corpus_tools.peppermodules.paula.importer;

import static org.junit.Assert.assertEquals;

import org.corpus_tools.pepper.testFramework.PepperTestUtil;
import org.corpus_tools.peppermodules.paula.PAULAImporter;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

public class IsImportableTest {

	private PAULAImporter fixture;

	public PAULAImporter getFixture() {
		return fixture;
	}

	public void setFixture(PAULAImporter fixture) {
		this.fixture = fixture;
	}

	@Before
	public void beforeEach() {
		setFixture(new PAULAImporter());
	}

	public static String getTestResources() {
		return (PepperTestUtil.getTestResources() + "isImportable/");
	}

	@Test
	public void whenCorpusPathContainsNoPAULAFiles_thenReturn0() {
		URI corpusPath = URI.createFileURI(getTestResources() + "noPaula/");
		assertEquals(Double.valueOf(0.0), getFixture().isImportable(corpusPath));
	}

	@Test
	public void whenCorpusPathContainsNoFilesWithPaulaEnding_thenReturn0() {
		URI corpusPath = URI.createFileURI(getTestResources() + "fakePaula/");
		assertEquals(Double.valueOf(0.0), getFixture().isImportable(corpusPath));
	}

	@Test
	public void whenCorpusPathContainsOnlyPaulaFiles_thenReturn1() {
		URI corpusPath = URI.createFileURI(getTestResources() + "onlyPaula/");
		assertEquals(Double.valueOf(1.0), getFixture().isImportable(corpusPath));
	}

	@Test
	public void whenCorpusPathContainsPaulaAndNonePaulaFiles_thenReturn1() {
		URI corpusPath = URI.createFileURI(getTestResources() + "mixedContent/");
		assertEquals(Double.valueOf(1.0), getFixture().isImportable(corpusPath));
	}
}
