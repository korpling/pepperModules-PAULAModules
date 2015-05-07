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
package de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.importer.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

import de.hu_berlin.german.korpling.saltnpepper.pepper.common.CorpusDesc;
import de.hu_berlin.german.korpling.saltnpepper.pepper.common.FormatDesc;
import de.hu_berlin.german.korpling.saltnpepper.pepper.testFramework.PepperImporterTest;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.PAULAImporter;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;

public class MultiFeatDataTest extends PepperImporterTest {
	@Before
	public void setUp() {
		super.setFixture(new PAULAImporter());

		// set formats to support
		FormatDesc formatDef = new FormatDesc();
		formatDef.setFormatName("paula");
		formatDef.setFormatVersion("1.0");
		this.supportedFormatsCheck.add(formatDef);
	}

	@Test
	public void testMultiFeatData() {
		File rootCorpus = new File(getTestResources() + "multiFeatData/" + "myCorpus/");
		assertTrue(rootCorpus.getAbsolutePath() + " does not exist", rootCorpus.exists());
		assertTrue(rootCorpus.getAbsolutePath() + " is not a directory", rootCorpus.isDirectory());

		// start: creating and setting corpus definition
		CorpusDesc corpDef = new CorpusDesc();
		FormatDesc formatDef = new FormatDesc();
		formatDef.setFormatName("xml");
		formatDef.setFormatVersion("1.0");
		corpDef.setFormatDesc(formatDef);
		corpDef.setCorpusPath(URI.createFileURI(rootCorpus.getAbsolutePath()));
		this.getFixture().setCorpusDesc(corpDef);
		// end: creating and setting corpus definition

		// runs the PepperModule
		this.start();

		// SCorpusGraph importedSCorpusGraph= getFixture().getSCorpusGraph();
		assertNotNull(getFixture().getSCorpusGraph());
		assertNotNull(getFixture().getSCorpusGraph().getSCorpora());
		assertEquals(1, getFixture().getSCorpusGraph().getSCorpora().size());
		assertNotNull(getFixture().getSCorpusGraph().getSCorpora().get(0));
		assertNotNull(getFixture().getSCorpusGraph().getSDocuments());
		assertNotNull(getFixture().getSCorpusGraph().getSDocuments().get(0));

		if (getFixture().getSCorpusGraph().getSDocuments().get(0).getSDocumentGraph() == null) {
			getFixture().getSCorpusGraph().getSDocuments().get(0).loadSDocumentGraph();
		}

		SDocumentGraph sDocGraph = getFixture().getSCorpusGraph().getSDocuments().get(0).getSDocumentGraph();

		assertNotNull(sDocGraph);
		assertNotNull(sDocGraph.getSTextualDSs());
		assertEquals(1, sDocGraph.getSTextualDSs().size());
		assertNotNull(sDocGraph.getSTokens());
		assertEquals(3, sDocGraph.getSTokens().size());
		assertNotNull(sDocGraph.getSTokens().get(0));

		assertNotNull(sDocGraph.getSTokens().get(0).getSAnnotations());
		assertEquals(2, sDocGraph.getSTokens().get(0).getSAnnotations().size());
		assertEquals("PPER", sDocGraph.getSTokens().get(0).getSAnnotation("mycorpus::pos").getSValue());
		assertEquals(2, sDocGraph.getSTokens().get(0).getSAnnotations().size());
		assertEquals("I", sDocGraph.getSTokens().get(0).getSAnnotation("mycorpus::lemma").getSValue());

		assertNotNull(sDocGraph.getSTokens().get(1));
		assertEquals(2, sDocGraph.getSTokens().get(1).getSAnnotations().size());
		assertEquals("VBP", sDocGraph.getSTokens().get(1).getSAnnotation("mycorpus::pos").getSValue());
		assertEquals(2, sDocGraph.getSTokens().get(1).getSAnnotations().size());
		assertEquals("have", sDocGraph.getSTokens().get(1).getSAnnotation("mycorpus::lemma").getSValue());
	}

	@Test
	public void testMultiFeat_MetaData() {
		File rootCorpus = new File(getTestResources() + "multiFeat_MetaData/" + "myCorpus/");

		// start: creating and setting corpus definition
		CorpusDesc corpDef = new CorpusDesc();
		FormatDesc formatDef = new FormatDesc();
		formatDef.setFormatName("xml");
		formatDef.setFormatVersion("1.0");
		corpDef.setFormatDesc(formatDef);
		corpDef.setCorpusPath(URI.createFileURI(rootCorpus.getAbsolutePath()));
		this.getFixture().setCorpusDesc(corpDef);
		// end: creating and setting corpus definition

		// runs the PepperModule
		this.start();

		SCorpusGraph importedSCorpusGraph = getFixture().getSaltProject().getSCorpusGraphs().get(0);
		assertNotNull(importedSCorpusGraph.getSCorpora());
		assertEquals(1, importedSCorpusGraph.getSCorpora().size());
		assertNotNull(importedSCorpusGraph.getSCorpora().get(0));
		assertNotNull(importedSCorpusGraph.getSCorpora().get(0).getSMetaAnnotations());
		assertEquals(2, importedSCorpusGraph.getSCorpora().get(0).getSMetaAnnotations().size());
		assertNotNull(importedSCorpusGraph.getSCorpora().get(0).getSMetaAnnotation("author"));
		assertEquals("John Doe", importedSCorpusGraph.getSCorpora().get(0).getSMetaAnnotation("author").getSValue());
		assertNotNull(importedSCorpusGraph.getSCorpora().get(0).getSMetaAnnotation("lang"));
		assertEquals("eng", importedSCorpusGraph.getSCorpora().get(0).getSMetaAnnotation("lang").getSValue());

		assertEquals(1, importedSCorpusGraph.getSDocuments().size());
		assertNotNull(importedSCorpusGraph.getSDocuments().get(0));
		assertNotNull(importedSCorpusGraph.getSDocuments().get(0).getSMetaAnnotations());
		assertNotNull(importedSCorpusGraph.getSDocuments().get(0).getSMetaAnnotation("date"));
		assertEquals("today", importedSCorpusGraph.getSDocuments().get(0).getSMetaAnnotation("date").getSValue());
	}
}
