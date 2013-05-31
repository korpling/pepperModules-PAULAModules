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

public class CorpusMetaDataTest extends PepperImporterTest
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
	
	public void testCorpusMetaData1()
	{
		File rootCorpus= new File(getTestFolder()+"corpusMetaData/"+"rootCorpus/");
		
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
		assertEquals(2,importedSCorpusGraph.getSCorpora().size());
		assertNotNull(importedSCorpusGraph.getSCorpora().get(0));
		assertNotNull(importedSCorpusGraph.getSCorpora().get(0).getSMetaAnnotations());
		assertEquals(2,importedSCorpusGraph.getSCorpora().get(0).getSMetaAnnotations().size());
		assertNotNull(importedSCorpusGraph.getSCorpora().get(0).getSMetaAnnotation("author"));
		assertEquals("John Doe",importedSCorpusGraph.getSCorpora().get(0).getSMetaAnnotation("author").getSValue());
		assertNotNull(importedSCorpusGraph.getSCorpora().get(0).getSMetaAnnotation("lang"));
		assertEquals("eng",importedSCorpusGraph.getSCorpora().get(0).getSMetaAnnotation("lang").getSValue());
		
		assertNotNull(importedSCorpusGraph.getSCorpora().get(1));
		assertNotNull(importedSCorpusGraph.getSCorpora().get(1).getSMetaAnnotations());
		assertNotNull(importedSCorpusGraph.getSCorpora().get(1).getSMetaAnnotation("date"));
		assertEquals("today",importedSCorpusGraph.getSCorpora().get(1).getSMetaAnnotation("date").getSValue());
	}
	
}
