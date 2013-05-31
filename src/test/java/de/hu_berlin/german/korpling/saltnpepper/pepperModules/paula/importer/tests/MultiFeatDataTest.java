package de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.importer.tests;

import java.io.File;

import org.eclipse.emf.common.util.URI;

import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.CorpusDefinition;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.FormatDefinition;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.PepperModulesFactory;
import de.hu_berlin.german.korpling.saltnpepper.pepper.testSuite.moduleTests.PepperImporterTest;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.PAULAImporter;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;

public class MultiFeatDataTest extends PepperImporterTest
{
	URI resourceURI= URI.createFileURI(new File(".").getAbsolutePath());
	URI temproraryURI= URI.createFileURI(System.getProperty("java.io.tmpdir"));
	
	public void setUp()
	{
		super.setFixture(new PAULAImporter());
		
		super.getFixture().setSaltProject(SaltFactory.eINSTANCE.createSaltProject());
		super.setResourcesURI(resourceURI);
		super.setTemprorariesURI(temproraryURI);
		
		//setting temproraries and resources
		this.getFixture().setTemproraries(temproraryURI);
		this.getFixture().setResources(resourceURI);
		
		//set formats to support
		FormatDefinition formatDef= PepperModulesFactory.eINSTANCE.createFormatDefinition();
		formatDef.setFormatName("paula");
		formatDef.setFormatVersion("1.0");
		this.supportedFormatsCheck.add(formatDef);
	}
	
	public static String getTestFolder()
	{
		return("./src/test/resources/");
	}
	
	public void testMultiFeatData()
	{
		File rootCorpus= new File(getTestFolder()+"MultiFeatData/"+"myCorpus/");
		
		//start: creating and setting corpus definition
			CorpusDefinition corpDef= PepperModulesFactory.eINSTANCE.createCorpusDefinition();
			FormatDefinition formatDef= PepperModulesFactory.eINSTANCE.createFormatDefinition();
			formatDef.setFormatName("xml");
			formatDef.setFormatVersion("1.0");
			corpDef.setFormatDefinition(formatDef);
			corpDef.setCorpusPath(URI.createFileURI(rootCorpus.getAbsolutePath()));
			this.getFixture().setCorpusDefinition(corpDef);
		//end: creating and setting corpus definition
		
		SCorpusGraph importedSCorpusGraph= SaltFactory.eINSTANCE.createSCorpusGraph();
		this.getFixture().getSaltProject().getSCorpusGraphs().add(importedSCorpusGraph);
		
		//runs the PepperModule
		this.start();
		
		assertNotNull(importedSCorpusGraph);
		assertNotNull(importedSCorpusGraph.getSCorpora());
		assertNotNull(importedSCorpusGraph.getSCorpora().get(0));
		assertNotNull(importedSCorpusGraph.getSDocuments());
		assertNotNull(importedSCorpusGraph.getSDocuments().get(0));
		assertNotNull(importedSCorpusGraph.getSDocuments().get(0).getSDocumentGraph());
		SDocumentGraph sDocGraph= importedSCorpusGraph.getSDocuments().get(0).getSDocumentGraph();
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
	
	public void testMultiFeat_MetaData()
	{
		File rootCorpus= new File(getTestFolder()+"multiFeat_MetaData/"+"myCorpus/");
		
		//start: creating and setting corpus definition
			CorpusDefinition corpDef= PepperModulesFactory.eINSTANCE.createCorpusDefinition();
			FormatDefinition formatDef= PepperModulesFactory.eINSTANCE.createFormatDefinition();
			formatDef.setFormatName("xml");
			formatDef.setFormatVersion("1.0");
			corpDef.setFormatDefinition(formatDef);
			corpDef.setCorpusPath(URI.createFileURI(rootCorpus.getAbsolutePath()));
			this.getFixture().setCorpusDefinition(corpDef);
		//end: creating and setting corpus definition
		
		SCorpusGraph importedSCorpusGraph= SaltFactory.eINSTANCE.createSCorpusGraph();
		this.getFixture().getSaltProject().getSCorpusGraphs().add(importedSCorpusGraph);
		
		//runs the PepperModule
		this.start();
		
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
