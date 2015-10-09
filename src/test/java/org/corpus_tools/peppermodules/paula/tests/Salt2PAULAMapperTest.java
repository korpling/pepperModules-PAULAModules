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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.corpus_tools.peppermodules.paula.PAULAExporterProperties;
import org.corpus_tools.peppermodules.paula.Salt2PAULAMapper;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDominanceRelation;
import org.corpus_tools.salt.common.SMedialDS;
import org.corpus_tools.salt.common.SMedialRelation;
import org.corpus_tools.salt.samples.SampleGenerator;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.hu_berlin.german.korpling.saltnpepper.pepper.testFramework.PepperModuleTest;

public class Salt2PAULAMapperTest {

	private Salt2PAULAMapper fixture = null;

	public Salt2PAULAMapper getFixture() {
		return fixture;
	}

	public void setFixture(Salt2PAULAMapper fixture) {
		this.fixture = fixture;
	}

	@Before
	public void setUp() throws IOException {
		setFixture(new Salt2PAULAMapper());
		getFixture().setDocument(SaltFactory.createSDocument());
		getFixture().getDocument().setName("doc1");
		getFixture().getDocument().setDocumentGraph(SaltFactory.createSDocumentGraph());

		getFixture().setProperties(new PAULAExporterProperties());
		getFixture().setResourcePath(URI.createFileURI(new File("src/main/resources/").getAbsolutePath()));
	}

	@BeforeClass
	public static void setUpAll() {
		deleteDirectory(PepperModuleTest.getTempPath_static("paulaExporter/"));
	}

	public static boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

	/**
	 * Tests the export of one primary text.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 */
	@Test
	public void testPrimaryText() throws IOException, SAXException {
		String testName = "primText";
		SampleGenerator.createPrimaryData(getFixture().getDocument());
		getFixture().setResourceURI(URI.createFileURI(PepperModuleTest.getTempPath_static("paulaExporter/" + testName).getAbsolutePath()));
		getFixture().mapSDocument();

		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/doc1.text.xml", getFixture().getResourceURI().toFileString() + "/doc1.text.xml"));
	}

	/**
	 * Tests the export of two primary text.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 */
	@Test
	public void testPrimaryText2() throws IOException, SAXException {
		String testName = "primText2";

		SampleGenerator.createPrimaryData(getFixture().getDocument());
		SampleGenerator.createPrimaryData(getFixture().getDocument(), "de");
		getFixture().setResourceURI(URI.createFileURI(PepperModuleTest.getTempPath_static("paulaExporter/" + testName).getAbsolutePath()));
		getFixture().mapSDocument();

		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/doc1.text1.xml", getFixture().getResourceURI().toFileString() + "/doc1.text1.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/doc1.text2.xml", getFixture().getResourceURI().toFileString() + "/doc1.text2.xml"));
	}

	/**
	 * Tests the export of a tokenization.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 */
	@Test
	public void testTokenization() throws IOException, SAXException {
		String testName = "tokenization";
		SampleGenerator.createPrimaryData(getFixture().getDocument());
		SampleGenerator.createTokens(getFixture().getDocument());
		SampleGenerator.createMorphologyAnnotations(getFixture().getDocument());
		getFixture().setResourceURI(URI.createFileURI(PepperModuleTest.getTempPath_static("paulaExporter/" + testName).getAbsolutePath()));
		
		getFixture().mapSDocument();

		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/doc1.text.xml", getFixture().getResourceURI().toFileString() + "/doc1.text.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/doc1.tok.xml", getFixture().getResourceURI().toFileString() + "/doc1.tok.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/doc1.tok_lemma.xml", getFixture().getResourceURI().toFileString() + "/doc1.tok_lemma.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/doc1.tok_pos.xml", getFixture().getResourceURI().toFileString() + "/doc1.tok_pos.xml"));
	}

	/**
	 * Tests the export of a spans and annotations.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 */
	@Test
	public void testSpans() throws IOException, SAXException {
		String testName = "spans";
		SampleGenerator.createPrimaryData(getFixture().getDocument());
		SampleGenerator.createTokens(getFixture().getDocument());
		SampleGenerator.createInformationStructureSpan(getFixture().getDocument());
		SampleGenerator.createInformationStructureAnnotations(getFixture().getDocument());
		getFixture().setResourceURI(URI.createFileURI(PepperModuleTest.getTempPath_static("paulaExporter/" + testName).getAbsolutePath()));
		getFixture().mapSDocument();
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/doc1.text.xml", getFixture().getResourceURI().toFileString() + "/doc1.text.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/doc1.tok.xml", getFixture().getResourceURI().toFileString() + "/doc1.tok.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/no_layer.doc1.mark.xml", getFixture().getResourceURI().toFileString() + "/no_layer.doc1.mark.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/no_layer.doc1.mark_Inf-Struct.xml", getFixture().getResourceURI().toFileString() + "/no_layer.doc1.mark_Inf-Struct.xml"));
	}

	/**
	 * Tests the export of a hierarchie and annotations.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 */
	@Test
	public void testHierarchies() throws IOException, SAXException {
		String testName = "hierarchies";
		SampleGenerator.createPrimaryData(getFixture().getDocument());
		SampleGenerator.createTokens(getFixture().getDocument());
		SampleGenerator.createSyntaxStructure(getFixture().getDocument());
		SampleGenerator.createSyntaxAnnotations(getFixture().getDocument());
		getFixture().setResourceURI(URI.createFileURI(PepperModuleTest.getTempPath_static("paulaExporter/" + testName).getAbsolutePath()));

		getFixture().mapSDocument();

		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/doc1.text.xml", getFixture().getResourceURI().toFileString() + "/doc1.text.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/doc1.tok.xml", getFixture().getResourceURI().toFileString() + "/doc1.tok.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/syntax.doc1.struct.xml", getFixture().getResourceURI().toFileString() + "/syntax.doc1.struct.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/syntax.doc1.struct_const.xml", getFixture().getResourceURI().toFileString() + "/syntax.doc1.struct_const.xml"));
	}

	/**
	 * Tests the export of pointing relations
	 * 
	 * @throws IOException
	 * @throws SAXException
	 */
	@Test
	public void testPointingRelations() throws IOException, SAXException {
		String testName = "pointingRelations";
		SampleGenerator.createPrimaryData(getFixture().getDocument());
		SampleGenerator.createTokens(getFixture().getDocument());
		SampleGenerator.createAnaphoricAnnotations(getFixture().getDocument());
		getFixture().setResourceURI(URI.createFileURI(PepperModuleTest.getTempPath_static("paulaExporter/" + testName).getAbsolutePath()));

		getFixture().mapSDocument();

		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/doc1.text.xml", getFixture().getResourceURI().toFileString() + "/doc1.text.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/doc1.tok.xml", getFixture().getResourceURI().toFileString() + "/doc1.tok.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/no_layer.doc1.anaphoric.xml", getFixture().getResourceURI().toFileString() + "/no_layer.doc1.anaphoric.xml"));
	}

	public boolean compareXMLFiles(String goldName, String fixtureName) throws SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(false);
		dbf.setValidating(false);
		try {
			dbf.setFeature("http://xml.org/sax/features/namespaces", false);
			dbf.setFeature("http://xml.org/sax/features/validation", false);
			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		XMLUnit.setTestDocumentBuilderFactory(dbf);
		XMLUnit.setControlDocumentBuilderFactory(dbf);

		Reader goldReader = new InputStreamReader(new FileInputStream(goldName), "UTF-8");
		Reader fixtureReader = new InputStreamReader(new FileInputStream(fixtureName), "UTF-8");
		Diff diff = XMLUnit.compareXML(goldReader, fixtureReader);
		if (!diff.identical()) {
			System.out.println(goldName + " <> " + fixtureName);
			System.out.println(diff);
		}

		return (diff.identical());
	}

	@Test
	public void testMetaAnnotationExport() throws SAXException, IOException {
		String testName = "metaAnnotation";
		getFixture().getDocument().createMetaAnnotation(null, "annotator", "Homer Simpson");
		getFixture().getDocument().createMetaAnnotation(null, "genre", "Sports");

		getFixture().setResourceURI(URI.createFileURI(PepperModuleTest.getTempPath_static("paulaExporter/" + testName).getAbsolutePath()));

		getFixture().mapSDocument();

		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/anno.xml", getFixture().getResourceURI().toFileString() + "/anno.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/anno_annotator.xml", getFixture().getResourceURI().toFileString() + "/anno_annotator.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/anno_genre.xml", getFixture().getResourceURI().toFileString() + "/anno_genre.xml"));
	}

	/**
	 * Tests the export of audio files when one token is connected to audio
	 * file.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 */
	@Test
	public void testAudioData() throws SAXException, IOException {
		String testName = "audioData";

		getFixture().getDocument().getDocumentGraph().createTextualDS("This is a sample text.");
		getFixture().getDocument().getDocumentGraph().tokenize();
		SMedialDS audio = SaltFactory.createSMedialDS();
		audio.setMediaReference(URI.createFileURI(PepperModuleTest.getTestResources() + "/audioData/sample.mp3"));
		getFixture().getDocument().getDocumentGraph().addNode(audio);
		SMedialRelation rel = SaltFactory.createSMedialRelation();
		rel.setTarget(audio);
		rel.setSource(getFixture().getDocument().getDocumentGraph().getTokens().get(0));
		getFixture().getDocument().getDocumentGraph().addRelation(rel);
		getFixture().setResourceURI(URI.createFileURI(PepperModuleTest.getTempPath_static("paulaExporter/" + testName).getAbsolutePath()));

		getFixture().mapSDocument();

		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/doc1.mark.audio.xml", getFixture().getResourceURI().toFileString() + "/doc1.mark.audio.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/doc1.mark.audio_feat.xml", getFixture().getResourceURI().toFileString() + "/doc1.mark.audio_feat.xml"));
		assertTrue(new File(PepperModuleTest.getTestResources() + "/" + testName + "/sample.mp3").exists());
	}

	/**
	 * Tests the export of audio files when no token is connected to audio file.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 */
	@Test
	public void testAudioData2() throws SAXException, IOException {
		String testName = "audioData2";
		getFixture().getDocument().getDocumentGraph().createTextualDS("This is a sample text.");
		getFixture().getDocument().getDocumentGraph().tokenize();
		SMedialDS audio = SaltFactory.createSMedialDS();
		audio.setMediaReference(URI.createFileURI(PepperModuleTest.getTestResources() + "/audioData/sample.mp3"));
		getFixture().getDocument().getDocumentGraph().addNode(audio);
		getFixture().setResourceURI(URI.createFileURI(PepperModuleTest.getTempPath_static("paulaExporter/" + testName).getAbsolutePath()));

		getFixture().mapSDocument();

		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/doc1.mark.audio.xml", getFixture().getResourceURI().toFileString() + "/doc1.mark.audio.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/doc1.mark.audio_feat.xml", getFixture().getResourceURI().toFileString() + "/doc1.mark.audio_feat.xml"));
		assertTrue(new File(PepperModuleTest.getTestResources() + "/" + testName + "/sample.mp3").exists());
	}
}
