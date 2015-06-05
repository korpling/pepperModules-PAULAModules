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

import java.io.File;

import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

import de.hu_berlin.german.korpling.saltnpepper.pepper.common.CorpusDesc;
import de.hu_berlin.german.korpling.saltnpepper.pepper.common.FormatDesc;
import de.hu_berlin.german.korpling.saltnpepper.pepper.testFramework.PepperImporterTest;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.PAULAImporter;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.Label;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;

public class AudioDataTest extends PepperImporterTest {

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
	public void testAudioData() {
		File testFolder = new File(getTestResources() + "audioData3/");
		URI testFolderURI= URI.createFileURI(testFolder.getAbsolutePath());
		
		// creating and setting corpus definition
		CorpusDesc corpDef = new CorpusDesc();
		corpDef.setCorpusPath(testFolderURI).getFormatDesc().setFormatName("xml").setFormatVersion("1.0");
		getFixture().setCorpusDesc(corpDef);
		
		// runs the PepperModule
		this.start();
		
		SDocumentGraph graph= getFixture().getSCorpusGraph().getSDocuments().get(0).getSDocumentGraph();
		assertEquals(1, graph.getSSpans().size());
		assertEquals("This is a sample text.", graph.getSText(graph.getSSpans().get(0)));
		assertEquals(1, graph.getSSpans().get(0).getSAnnotations().size());
		
		assertNotNull(graph.getSSpans().get(0).getSAnnotation(SaltFactory.eINSTANCE.createQName("mark_audio", "audio")));
		assertNotNull(graph.getSSpans().get(0).getSAnnotation(SaltFactory.eINSTANCE.createQName("mark_audio", "audio")).getSValueSURI());
		URI audioURI= testFolderURI.appendSegment("audio").appendSegment("sample.mp3");
		assertEquals(audioURI, graph.getSSpans().get(0).getSAnnotation(SaltFactory.eINSTANCE.createQName("mark_audio", "audio")).getSValueSURI());
	}
}