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
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.corpus_tools.pepper.common.CorpusDesc;
import org.corpus_tools.pepper.common.FormatDesc;
import org.corpus_tools.pepper.testFramework.PepperImporterTest;
import org.corpus_tools.peppermodules.paula.PAULAImporter;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

public class CorpusMetaDataTest extends PepperImporterTest {

	@Before
	public void setUp() {
		super.setFixture(new PAULAImporter());

		// set formats to support
		FormatDesc formatDef = new FormatDesc();
		formatDef.setFormatName("paula");
		formatDef.setFormatVersion("1.0");
		supportedFormatsCheck.add(formatDef);
	}

	@Test
	public void testCorpusMetaData1() {
		File rootCorpus = new File(getTestResources() + "corpusMetaData/" + "rootCorpus/");

		// start: creating and setting corpus definition
		CorpusDesc corpDef = new CorpusDesc();
		FormatDesc formatDef = new FormatDesc();
		formatDef.setFormatName("xml");
		formatDef.setFormatVersion("1.0");
		corpDef.setFormatDesc(formatDef);
		corpDef.setCorpusPath(URI.createFileURI(rootCorpus.getAbsolutePath()));
		getFixture().setCorpusDesc(corpDef);
		// end: creating and setting corpus definition

		// runs the PepperModule
		this.start();

		SCorpusGraph importedSCorpusGraph = getFixture().getCorpusGraph();
		assertNotNull(importedSCorpusGraph.getCorpora());
		assertEquals(2, importedSCorpusGraph.getCorpora().size());
		assertNotNull(importedSCorpusGraph.getCorpora().get(0));
		assertNotNull(importedSCorpusGraph.getCorpora().get(0).getMetaAnnotations());
		assertEquals(2, importedSCorpusGraph.getCorpora().get(0).getMetaAnnotations().size());
		assertNotNull(importedSCorpusGraph.getCorpora().get(0).getMetaAnnotation("author"));
		assertEquals("John Doe", importedSCorpusGraph.getCorpora().get(0).getMetaAnnotation("author").getValue());
		assertNotNull(importedSCorpusGraph.getCorpora().get(0).getMetaAnnotation("lang"));
		assertEquals("eng", importedSCorpusGraph.getCorpora().get(0).getMetaAnnotation("lang").getValue());

		assertNotNull(importedSCorpusGraph.getCorpora().get(1));
		assertNotNull(importedSCorpusGraph.getCorpora().get(1).getMetaAnnotations());
		assertNotNull(importedSCorpusGraph.getCorpora().get(1).getMetaAnnotation("date"));
		assertEquals("today", importedSCorpusGraph.getCorpora().get(1).getMetaAnnotation("date").getValue());
	}

}
