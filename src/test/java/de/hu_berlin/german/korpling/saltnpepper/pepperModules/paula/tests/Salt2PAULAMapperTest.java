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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Hashtable;

import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;

import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.Salt2PAULAMapper;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.exceptions.PAULAExporterException;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SElementId;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltSample.SaltSample;
import de.hu_berlin.german.korpling.saltnpepper.devTools.generalModuleTests.util.FileComparator;

import junit.framework.TestCase;
import org.custommonkey.xmlunit.*;
import org.eclipse.emf.common.util.URI;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

 
 

public class Salt2PAULAMapperTest extends TestCase implements FilenameFilter{
	
	String inputDirectory = "/home/eladrion/Desktop/MarioTask/PAULAExporter/pcc2/paula_Export/pcc2/11299/";
	String outputDirectory = "/home/eladrion/Desktop/MarioTask/PAULAExporter/pcc2/paula_ExportCompare/pcc2/11299/";
	
	String outputDirectory1 = "/home/eladrion/Desktop/MarioTask/PAULAExporter/SampleExport1/";
	String outputDirectory2 = "/home/eladrion/Desktop/MarioTask/PAULAExporter/SampleExport2/";
	
	private Salt2PAULAMapper fixture = null;
	private SaltSample saltSample = null;

	public boolean accept( File f, String s )
	  {
	    return s.toLowerCase().endsWith( ".xml" ) 
	    	 & s.toLowerCase().indexOf("anno")==-1 ;
			
		
	  }

	
	public Salt2PAULAMapper getFixture() {
		return fixture;
	}

	public void setFixture(Salt2PAULAMapper fixture) {
		this.fixture = fixture;
	}
	
	public void setSaltSample(SaltSample saltSample){
		this.saltSample = saltSample;
	}
	
	@Override	
	public void setUp(){
		this.setFixture(new Salt2PAULAMapper());
		this.setSaltSample(new SaltSample());
		//XMLUnit.setControlEntityResolver(new CatalogResolver());
		//XMLUnit.setTestEntityResolver(new CatalogResolver());
	}
	
	public void testMapCorpusStructure(){
		try {
		this.getFixture().mapCorpusStructure(null, null);
		fail("Null corpus Graph");
		} catch (PAULAExporterException e){
			//System.out.println(e.getMessage());
			//fail(e.getMessage());
		}	
		
	}
	
	//TODO @Mario please delete comments and fix the test 
	public void testMapSDocumentStructure() throws ClassNotFoundException{
		/*
		 * testing with null reference to Document Path and SDocument
		 */
		try {
			this.getFixture().mapSDocumentStructure(null, null);
			fail("Document Path and SDocument are not referenced");
		} catch (PAULAExporterException e){
				//System.out.println(e.getMessage());
				//fail(e.getMessage());
		}	
		/*
		 * testing with null reference to Document Path
		 */
		try {
			this.getFixture().mapSDocumentStructure(SaltFactory.eINSTANCE.createSDocument(), null);
			fail("There is no reference to Document Path");
		} catch (PAULAExporterException e){
			
		}
		/*
		 * testing with null reference to SDocument
		 */
		try {
			this.getFixture().mapSDocumentStructure(null, URI.createURI(outputDirectory));
			fail("There is no reference to Document Path");
		} catch (PAULAExporterException e){
			
		}
		/*
		 * testing with salt sample graph. Export twice and compare
		 */
		try{
			Hashtable<SElementId, URI> documentPaths1 = 
				this.getFixture().mapCorpusStructure(saltSample.getCorpus(), URI.createURI(outputDirectory1));
			Hashtable<SElementId, URI> documentPaths2 =
				this.getFixture().mapCorpusStructure(saltSample.getCorpus(), URI.createURI(outputDirectory2));
			//this.XMLUnitComparision();
			for (SDocument sDocument : saltSample.getCorpus().getSDocuments()){
				this.getFixture().mapSDocumentStructure(sDocument, documentPaths1.get(sDocument.getSElementId()));
			}
			for (SDocument sDocument : saltSample.getCorpus().getSDocuments()){
				this.getFixture().mapSDocumentStructure(sDocument, documentPaths2.get(sDocument.getSElementId()));
			}
			for (SDocument sDocument : saltSample.getCorpus().getSDocuments()){
				this.compareDocuments(documentPaths1.get(sDocument.getSElementId()),documentPaths2.get(sDocument.getSElementId()));
			}
			
		}catch(PAULAExporterException e){
			
		}
		
	}


	private void compareDocuments(URI uri, URI uri2) {
		File fileToCheck = null;
		Diff difference = null;
		InputSource gold = null;
		InputSource toCheck = null;
		FileComparator fileComparator = new FileComparator();
		for (File in : new File(uri.toFileString()).listFiles(this)){
			fileToCheck = new File(uri2.toFileString()+File.separator+in.getName());
			try {
				toCheck = new InputSource(new FileInputStream(fileToCheck));
				gold = new InputSource(new FileInputStream(in));
				
				System.out.print("File "+in.getAbsolutePath()+" and "+ fileToCheck.getAbsolutePath()+" are");
				if (fileComparator.compareFiles(in, fileToCheck)){
					System.out.println(" equal!");
				} else {
					System.out.println(" not equal!");
				}
				//difference = XMLUnit.compareXML(gold, toCheck);
				//difference = new Diff(gold, toCheck);
				
				//XMLAssert.assertXMLEqual("not equal!",difference,true);
				//assertTrue("not equal!", difference.similar());
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			//} catch (SAXException e) {
				// TODO Auto-generated catch block
			//	e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
}
