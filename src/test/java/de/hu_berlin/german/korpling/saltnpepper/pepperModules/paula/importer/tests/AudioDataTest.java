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

import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SMedialDS;
import org.corpus_tools.salt.common.SMedialRelation;
import org.corpus_tools.salt.common.SSpan;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

import de.hu_berlin.german.korpling.saltnpepper.pepper.common.CorpusDesc;
import de.hu_berlin.german.korpling.saltnpepper.pepper.common.FormatDesc;
import de.hu_berlin.german.korpling.saltnpepper.pepper.testFramework.PepperImporterTest;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.PAULAImporter;

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
	 * Tests that a {@link SMedialDS} and corresponding {@link SMedialRelation}
	 * are created. Further a {@link SSpan} is created, which is in PAULA the
	 * anchor for an audio annotation. This span is not necessary, but it it is
	 * not decidable whether this span is necessary or not.
	 */
	@Test
	public void testAudioData() {
		File testFolder = new File(getTestResources() + "audioData3/");
		URI testFolderURI = URI.createFileURI(testFolder.getAbsolutePath());

		// creating and setting corpus definition
		CorpusDesc corpDef = new CorpusDesc();
		corpDef.setCorpusPath(testFolderURI).getFormatDesc().setFormatName("xml").setFormatVersion("1.0");
		getFixture().setCorpusDesc(corpDef);

		// runs the PepperModule
		this.start();
		SDocumentGraph graph = getFixture().getCorpusGraph().getDocuments().get(0).getDocumentGraph();
		assertEquals(1, graph.getMedialDSs().size());
		assertNotNull(graph.getMedialDSs());
		URI audioURI = testFolderURI.appendSegment("audio").appendSegment("sample.mp3");
		assertEquals(audioURI, graph.getMedialDSs().get(0).getMediaReference());
		assertEquals(1, graph.getSpans().size());
		assertEquals(6, graph.getMedialRelations().size());
		for (SMedialRelation rel : graph.getMedialRelations()) {
			assertEquals(graph.getMedialDSs().get(0), rel.getSource());
		}
	}
}