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
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;

public class CorpusMetaDataTest extends PepperImporterTest
{
	
	@Before
	public void setUp()
	{
		super.setFixture(new PAULAImporter());
		
		//set formats to support
		FormatDesc formatDef= new FormatDesc();
		formatDef.setFormatName("paula");
		formatDef.setFormatVersion("1.0");
		this.supportedFormatsCheck.add(formatDef);
	}
	
	@Test
	public void testCorpusMetaData1()
	{
		File rootCorpus= new File(getTestResources()+"corpusMetaData/"+"rootCorpus/");
		
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
		
		SCorpusGraph importedSCorpusGraph= getFixture().getSCorpusGraph();
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
