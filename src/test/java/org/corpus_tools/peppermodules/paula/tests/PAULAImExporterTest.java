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


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.corpus_tools.pepper.common.CorpusDesc;
import org.corpus_tools.pepper.modules.PepperExporter;
import org.corpus_tools.pepper.modules.PepperImporter;
import org.corpus_tools.pepper.modules.PepperModule;
import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;
import org.corpus_tools.pepper.testFramework.PepperTestUtil;
import org.corpus_tools.peppermodules.paula.PAULAExporter;
import org.corpus_tools.peppermodules.paula.PAULAExporterProperties;
import org.corpus_tools.peppermodules.paula.PAULAImporter;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.ElementNameQualifier;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.eclipse.emf.common.util.URI;
import org.junit.Test;
import org.junit.Before;
import org.xml.sax.SAXException;

public class PAULAImExporterTest {
	URI tmpFolderURI = URI.createFileURI(PepperTestUtil.getTempPath_static("imExportTest").getAbsolutePath());

	/**
	 * Clean tmp folder.
	 * 
	 * @throws IOException
	 */
	@Before
	public void tearDown() throws IOException {
		FileUtils.deleteDirectory(new File(tmpFolderURI.toFileString()));
	}

	/**
	 * Imports a corpus and exports it, then checks if bother paula corpora are
	 * equal.
	 * 
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	@Test
	public void testImExport() throws IOException, ParserConfigurationException, SAXException {
		File testFolder = new File(PepperTestUtil.getTestResources() + "imExporterTest1/");
		URI testFolderURI = URI.createFileURI(testFolder.getAbsolutePath());

		CorpusDesc corpDef = null;

		/** instantiate importer **/
		PepperImporter importer = new PAULAImporter();
		// creating and setting corpus definition
		corpDef = new CorpusDesc();
		corpDef.setCorpusPath(testFolderURI).getFormatDesc().setFormatName("xml").setFormatVersion("1.0");
		importer.setCorpusDesc(corpDef);

		/** instantiate exporter **/
		PepperExporter exporter = new PAULAExporter();
		((PepperModuleProperty<Boolean>)exporter.getProperties().getProperty(PAULAExporterProperties.PROP_HUMAN_READABLE)).setValue(false);
		// creating and setting corpus definition
		corpDef = new CorpusDesc();
		corpDef.setCorpusPath(tmpFolderURI).getFormatDesc().setFormatName("xml").setFormatVersion("1.0");
		exporter.setCorpusDesc(corpDef);

		Collection<PepperModule> fixtures = new ArrayList<PepperModule>();
		fixtures.add(importer);
		fixtures.add(exporter);

		PepperTestUtil.start(fixtures);

		tmpFolderURI = tmpFolderURI.appendSegment("imExporterTest1").appendSegment("myCorpus").appendSegment("myDocument");
		testFolderURI = testFolderURI.appendSegment("myCorpus").appendSegment("myDocument");

		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		
		XMLAssert.assertXMLEqual(XMLUnit.compareXML(docBuilder.parse(new File(testFolderURI.appendSegment("myDocument.text.xml").toFileString())), docBuilder.parse(new File(tmpFolderURI.appendSegment("myDocument.text.xml").toFileString()))),true);;
		XMLAssert.assertXMLEqual(XMLUnit.compareXML(docBuilder.parse(new File(testFolderURI.appendSegment("myDocument.tok.xml").toFileString())), docBuilder.parse(new File(tmpFolderURI.appendSegment("myDocument.tok.xml").toFileString()))),true);
		XMLAssert.assertXMLEqual(XMLUnit.compareXML(docBuilder.parse(new File(testFolderURI.appendSegment("syntax.myDocument.struct.xml").toFileString())), docBuilder.parse(new File(tmpFolderURI.appendSegment("syntax.myDocument.struct.xml").toFileString()))),true);
		XMLAssert.assertXMLEqual(XMLUnit.compareXML(docBuilder.parse(new File(testFolderURI.appendSegment("syntax.myDocument.struct_const.xml").toFileString())), docBuilder.parse(new File(tmpFolderURI.appendSegment("syntax.myDocument.struct_const.xml").toFileString()))),true);
	}
}
