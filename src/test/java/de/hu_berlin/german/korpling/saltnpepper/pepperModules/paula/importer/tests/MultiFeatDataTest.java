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

import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

import de.hu_berlin.german.korpling.saltnpepper.pepper.common.CorpusDesc;
import de.hu_berlin.german.korpling.saltnpepper.pepper.common.FormatDesc;
import de.hu_berlin.german.korpling.saltnpepper.pepper.testFramework.PepperImporterTest;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.PAULAImporter;

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
		getFixture().setCorpusDesc(corpDef);
		// end: creating and setting corpus definition

		// runs the PepperModule
		this.start();

		// SCorpusGraph importedSCorpusGraph= getFixture().getCorpusGraph();
		assertNotNull(getFixture().getCorpusGraph());
		assertNotNull(getFixture().getCorpusGraph().getCorpora());
		assertEquals(1, getFixture().getCorpusGraph().getCorpora().size());
		assertNotNull(getFixture().getCorpusGraph().getCorpora().get(0));
		assertNotNull(getFixture().getCorpusGraph().getDocuments());
		assertNotNull(getFixture().getCorpusGraph().getDocuments().get(0));

		if (getFixture().getCorpusGraph().getDocuments().get(0).getDocumentGraph() == null) {
			getFixture().getCorpusGraph().getDocuments().get(0).loadDocumentGraph();
		}

		SDocumentGraph sDocGraph = getFixture().getCorpusGraph().getDocuments().get(0).getDocumentGraph();

		assertNotNull(sDocGraph);
		assertNotNull(sDocGraph.getTextualDSs());
		assertEquals(1, sDocGraph.getTextualDSs().size());
		assertNotNull(sDocGraph.getTokens());
		assertEquals(3, sDocGraph.getTokens().size());
		assertNotNull(sDocGraph.getTokens().get(0));

		assertNotNull(sDocGraph.getTokens().get(0).getAnnotations());
		assertEquals(2, sDocGraph.getTokens().get(0).getAnnotations().size());
		assertEquals("PPER", sDocGraph.getTokens().get(0).getAnnotation("mycorpus::pos").getValue());
		assertEquals(2, sDocGraph.getTokens().get(0).getAnnotations().size());
		assertEquals("I", sDocGraph.getTokens().get(0).getAnnotation("mycorpus::lemma").getValue());

		assertNotNull(sDocGraph.getTokens().get(1));
		assertEquals(2, sDocGraph.getTokens().get(1).getAnnotations().size());
		assertEquals("VBP", sDocGraph.getTokens().get(1).getAnnotation("mycorpus::pos").getValue());
		assertEquals(2, sDocGraph.getTokens().get(1).getAnnotations().size());
		assertEquals("have", sDocGraph.getTokens().get(1).getAnnotation("mycorpus::lemma").getValue());
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
		getFixture().setCorpusDesc(corpDef);
		// end: creating and setting corpus definition

		// runs the PepperModule
		this.start();

		SCorpusGraph importedSCorpusGraph = getFixture().getSaltProject().getCorpusGraphs().get(0);
		assertNotNull(importedSCorpusGraph.getCorpora());
		assertEquals(1, importedSCorpusGraph.getCorpora().size());
		assertNotNull(importedSCorpusGraph.getCorpora().get(0));
		assertNotNull(importedSCorpusGraph.getCorpora().get(0).getMetaAnnotations());
		assertEquals(2, importedSCorpusGraph.getCorpora().get(0).getMetaAnnotations().size());
		assertNotNull(importedSCorpusGraph.getCorpora().get(0).getMetaAnnotation("author"));
		assertEquals("John Doe", importedSCorpusGraph.getCorpora().get(0).getMetaAnnotation("author").getValue());
		assertNotNull(importedSCorpusGraph.getCorpora().get(0).getMetaAnnotation("lang"));
		assertEquals("eng", importedSCorpusGraph.getCorpora().get(0).getMetaAnnotation("lang").getValue());

		assertEquals(1, importedSCorpusGraph.getDocuments().size());
		assertNotNull(importedSCorpusGraph.getDocuments().get(0));
		assertNotNull(importedSCorpusGraph.getDocuments().get(0).getMetaAnnotations());
		assertNotNull(importedSCorpusGraph.getDocuments().get(0).getMetaAnnotation("date"));
		assertEquals("today", importedSCorpusGraph.getDocuments().get(0).getMetaAnnotation("date").getValue());
	}
}
