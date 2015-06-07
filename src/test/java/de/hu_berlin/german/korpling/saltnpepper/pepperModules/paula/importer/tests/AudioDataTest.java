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
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SAudioDSRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SAudioDataSource;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SSpan;

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

	/**
	 * Tests that a {@link SAudioDataSource} and corresponding {@link SAudioDSRelation} are created. Further a {@link SSpan}
	 * is created, which is in PAULA the anchor for an audio annotation. This span is not necessary, but it it is not
	 * decidable whether this span is necessary or not.
	 */
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
		assertEquals(1, graph.getSAudioDataSources().size());
		assertNotNull(graph.getSAudioDataSources());
		URI audioURI= testFolderURI.appendSegment("audio").appendSegment("sample.mp3");
		assertEquals(audioURI, graph.getSAudioDataSources().get(0).getSAudioReference());
		assertEquals(1, graph.getSSpans().size());
		assertEquals(6, graph.getSAudioDSRelations().size());
		for (SAudioDSRelation rel: graph.getSAudioDSRelations()){
			assertEquals(graph.getSAudioDataSources().get(0), rel.getSAudioDS());
		}
	}
}