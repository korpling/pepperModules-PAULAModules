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
package de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.importer.tests;

import java.io.File;



import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.*;
import static org.junit.Assert.*;

import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.PAULAImporter;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.pepper.common.CorpusDesc;
import de.hu_berlin.german.korpling.saltnpepper.pepper.common.FormatDesc;
import de.hu_berlin.german.korpling.saltnpepper.pepper.testFramework.PepperImporterTest;

public class MultiFeatDataTest extends PepperImporterTest
{
	URI resourceURI= URI.createFileURI(new File(".").getAbsolutePath());
	URI temproraryURI= URI.createFileURI(System.getProperty("java.io.tmpdir"));
	
	@Before
	public void setUp()
	{
		super.setFixture(new PAULAImporter());
		
		super.getFixture().setSaltProject(SaltFactory.eINSTANCE.createSaltProject());
		super.setResourcesURI(resourceURI);
		
		//setting temproraries and resources
		this.getFixture().setTemproraries(temproraryURI);
		this.getFixture().setResources(resourceURI);
		
		//set formats to support
		FormatDesc formatDef= new FormatDesc();
		formatDef.setFormatName("paula");
		formatDef.setFormatVersion("1.0");
		this.supportedFormatsCheck.add(formatDef);
	}
	
	public static String getTestFolder()
	{
		return("src/test/resources/");
	}
	@Test
	public void testMultiFeatData()
	{
		File rootCorpus= new File(getTestFolder()+"multiFeatData/"+"myCorpus/");
		assertTrue(rootCorpus.getAbsolutePath()+" does not exist",rootCorpus.exists());
		assertTrue(rootCorpus.getAbsolutePath()+" is not a directory",rootCorpus.isDirectory());
		
		//start: creating and setting corpus definition
			CorpusDesc corpDef= new CorpusDesc();
			FormatDesc formatDef= new FormatDesc();
			formatDef.setFormatName("xml");
			formatDef.setFormatVersion("1.0");
			corpDef.setFormatDesc(formatDef);
			corpDef.setCorpusPath(URI.createFileURI(rootCorpus.getAbsolutePath()));
			this.getFixture().setCorpusDesc(corpDef);
		//end: creating and setting corpus definition
		
		//runs the PepperModule
		this.start();
		
		SCorpusGraph importedSCorpusGraph= getFixture().getSaltProject().getSCorpusGraphs().get(0);
		assertNotNull(importedSCorpusGraph);
		assertNotNull(importedSCorpusGraph.getSCorpora());
		assertEquals(1, importedSCorpusGraph.getSCorpora().size());
		assertNotNull(importedSCorpusGraph.getSCorpora().get(0));
		assertNotNull(importedSCorpusGraph.getSDocuments());
		assertNotNull(importedSCorpusGraph.getSDocuments().get(0));
		
		SDocumentGraph sDocGraph= importedSCorpusGraph.getSDocuments().get(0).getSDocumentGraph();
		
		System.out.println("docs: "+ importedSCorpusGraph.getSDocuments());
		
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
	public void testMultiFeat_MetaData()
	{
		File rootCorpus= new File(getTestFolder()+"multiFeat_MetaData/"+"myCorpus/");
		
		//start: creating and setting corpus definition
			CorpusDesc corpDef= new CorpusDesc();
			FormatDesc formatDef= new FormatDesc();
			formatDef.setFormatName("xml");
			formatDef.setFormatVersion("1.0");
			corpDef.setFormatDesc(formatDef);
			corpDef.setCorpusPath(URI.createFileURI(rootCorpus.getAbsolutePath()));
			this.getFixture().setCorpusDesc(corpDef);
		//end: creating and setting corpus definition
		
		//runs the PepperModule
		this.start();
		
		SCorpusGraph importedSCorpusGraph= getFixture().getSaltProject().getSCorpusGraphs().get(0);
		assertNotNull(importedSCorpusGraph.getSCorpora());
		assertEquals(1,importedSCorpusGraph.getSCorpora().size());
		assertNotNull(importedSCorpusGraph.getSCorpora().get(0));
		assertNotNull(importedSCorpusGraph.getSCorpora().get(0).getSMetaAnnotations());
		assertEquals(2,importedSCorpusGraph.getSCorpora().get(0).getSMetaAnnotations().size());
		assertNotNull(importedSCorpusGraph.getSCorpora().get(0).getSMetaAnnotation("author"));
		assertEquals("John Doe",importedSCorpusGraph.getSCorpora().get(0).getSMetaAnnotation("author").getSValue());
		assertNotNull(importedSCorpusGraph.getSCorpora().get(0).getSMetaAnnotation("lang"));
		assertEquals("eng",importedSCorpusGraph.getSCorpora().get(0).getSMetaAnnotation("lang").getSValue());
		
		assertEquals(1,importedSCorpusGraph.getSDocuments().size());
		assertNotNull(importedSCorpusGraph.getSDocuments().get(0));
		assertNotNull(importedSCorpusGraph.getSDocuments().get(0).getSMetaAnnotations());
		assertNotNull(importedSCorpusGraph.getSDocuments().get(0).getSMetaAnnotation("date"));
		assertEquals("today",importedSCorpusGraph.getSDocuments().get(0).getSMetaAnnotation("date").getSValue());
	}
}
