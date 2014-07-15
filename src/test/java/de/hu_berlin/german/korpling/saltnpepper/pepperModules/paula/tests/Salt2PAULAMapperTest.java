/**
 * Copyright 2009 Humboldt University of Berlin, INRIA.
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
package de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.tests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.hu_berlin.german.korpling.saltnpepper.pepper.testFramework.PepperModuleTest;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.PAULAExporterProperties;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.Salt2PAULAMapper;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDominanceRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltSample.SaltSample;

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
		getFixture().setSDocument(SaltFactory.eINSTANCE.createSDocument());
		getFixture().getSDocument().setSName("doc1");
		getFixture().getSDocument().setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());

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
		SaltSample.createPrimaryData(getFixture().getSDocument());
		getFixture().setResourceURI(URI.createFileURI(PepperModuleTest.getTempPath_static("paulaExporter/" + testName).getAbsolutePath()));
		getFixture().mapSDocument();

		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/no_layer.doc1.text.xml", getFixture().getResourceURI().toFileString() + "/no_layer.doc1.text.xml"));
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

		SaltSample.createPrimaryData(getFixture().getSDocument());
		SaltSample.createPrimaryData(getFixture().getSDocument(), "de");
		getFixture().setResourceURI(URI.createFileURI(PepperModuleTest.getTempPath_static("paulaExporter/" + testName).getAbsolutePath()));
		getFixture().mapSDocument();

		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/no_layer.doc1.text1.xml", getFixture().getResourceURI().toFileString() + "/no_layer.doc1.text1.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/no_layer.doc1.text2.xml", getFixture().getResourceURI().toFileString() + "/no_layer.doc1.text2.xml"));
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
		SaltSample.createPrimaryData(getFixture().getSDocument());
		SaltSample.createTokens(getFixture().getSDocument());
		SaltSample.createMorphologyAnnotations(getFixture().getSDocument());
		getFixture().setResourceURI(URI.createFileURI(PepperModuleTest.getTempPath_static("paulaExporter/" + testName).getAbsolutePath()));
		getFixture().mapSDocument();

		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/no_layer.doc1.text.xml", getFixture().getResourceURI().toFileString() + "/no_layer.doc1.text.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/morphology.doc1.tok.xml", getFixture().getResourceURI().toFileString() + "/morphology.doc1.tok.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/morphology.doc1.tok_LEMMA.xml", getFixture().getResourceURI().toFileString() + "/morphology.doc1.tok_LEMMA.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/morphology.doc1.tok_POS.xml", getFixture().getResourceURI().toFileString() + "/morphology.doc1.tok_POS.xml"));
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
		SaltSample.createPrimaryData(getFixture().getSDocument());
		SaltSample.createTokens(getFixture().getSDocument());
		SaltSample.createInformationStructureSpan(getFixture().getSDocument());
		SaltSample.createInformationStructureAnnotations(getFixture().getSDocument());
		getFixture().setResourceURI(URI.createFileURI(PepperModuleTest.getTempPath_static("paulaExporter/" + testName).getAbsolutePath()));
		getFixture().mapSDocument();

		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/no_layer.doc1.text.xml", getFixture().getResourceURI().toFileString() + "/no_layer.doc1.text.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/morphology.doc1.tok.xml", getFixture().getResourceURI().toFileString() + "/morphology.doc1.tok.xml"));
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
		SaltSample.createPrimaryData(getFixture().getSDocument());
		SaltSample.createTokens(getFixture().getSDocument());
		SaltSample.createSyntaxStructure(getFixture().getSDocument());
		SaltSample.createSyntaxAnnotations(getFixture().getSDocument());
		getFixture().setResourceURI(URI.createFileURI(PepperModuleTest.getTempPath_static("paulaExporter/" + testName).getAbsolutePath()));
		
		SDominanceRelation domRel= SaltFactory.eINSTANCE.createSDominanceRelation();
		getFixture().getSDocument().getSDocumentGraph().addSRelation(domRel);
		
		getFixture().mapSDocument();

		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/no_layer.doc1.text.xml", getFixture().getResourceURI().toFileString() + "/no_layer.doc1.text.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/morphology.doc1.tok.xml", getFixture().getResourceURI().toFileString() + "/morphology.doc1.tok.xml"));
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
		SaltSample.createPrimaryData(getFixture().getSDocument());
		SaltSample.createTokens(getFixture().getSDocument());
		SaltSample.createAnaphoricAnnotations(getFixture().getSDocument());
		getFixture().setResourceURI(URI.createFileURI(PepperModuleTest.getTempPath_static("paulaExporter/" + testName).getAbsolutePath()));
		
		SDominanceRelation domRel= SaltFactory.eINSTANCE.createSDominanceRelation();
		getFixture().getSDocument().getSDocumentGraph().addSRelation(domRel);
		
		getFixture().mapSDocument();

		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/no_layer.doc1.text.xml", getFixture().getResourceURI().toFileString() + "/no_layer.doc1.text.xml"));
		assertTrue(compareXMLFiles(PepperModuleTest.getTestResources() + "/" + testName + "/morphology.doc1.tok.xml", getFixture().getResourceURI().toFileString() + "/morphology.doc1.tok.xml"));
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
}
