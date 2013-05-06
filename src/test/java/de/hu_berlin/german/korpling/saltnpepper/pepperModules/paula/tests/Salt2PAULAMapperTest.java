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

import de.hu_berlin.german.korpling.saltnpepper.pepper.testSuite.moduleTests.util.FileComparator;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.PAULAExporter;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.PAULAExporterProperties;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.Salt2PAULAMapper;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.exceptions.PAULAExporterException;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SElementId;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltSample.SaltSample;

import junit.framework.TestCase;
import org.eclipse.emf.common.util.URI;
import org.xml.sax.InputSource;

 
 
public class Salt2PAULAMapperTest extends TestCase implements FilenameFilter{
	private Salt2PAULAMapper fixture = null;
	private SaltSample saltSample = null;
	
	String resourcePath = "file://"+(new File("_TMP"+File.separator+"test"+File.separator).getAbsolutePath());
	
	
	String outputDirectory1 = resourcePath+File.separator+"SampleExport1"+File.separator;
	String outputDirectory2 = resourcePath+File.separator+"SampleExport2"+File.separator;
	
	public boolean accept( File f, String s )
	{
		return s.toLowerCase().endsWith( ".xml" ) 
				& s.toLowerCase().indexOf("anno")==-1 ;
	}

	
	public Salt2PAULAMapper getFixture() {
		return this.fixture;
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
		this.getFixture().setProperties(new PAULAExporterProperties());
		this.setSaltSample(new SaltSample());
		SDocument doc = this.saltSample.getCorpus().getSDocuments().get(0);
		
		
		//this.saltSample.createToken(0, 2, doc.getSDocumentGraph().getSTextualDSs().get(0), doc, null);
		
		SToken sToken= SaltFactory.eINSTANCE.createSToken();
		doc.getSDocumentGraph().addSNode(sToken);
		//layer.getSNodes().add(sToken);
		STextualRelation sTextRel= SaltFactory.eINSTANCE.createSTextualRelation();
		sTextRel.setSToken(sToken);
		sTextRel.setSTextualDS(doc.getSDocumentGraph().getSTextualDSs().get(0));
		sTextRel.setSStart(0);
		sTextRel.setSEnd(2);
		doc.getSDocumentGraph().addSRelation(sTextRel);
		
		
		if (! new File(resourcePath).exists()){
			new File(resourcePath).mkdir();
//			System.out.println("Creating Resource Path :"+ resourcePath);
		}
	}
	
	
	
	public void testMapSDocumentStructure() throws ClassNotFoundException{
		System.out.println("Cleaning up before testing.");
		this.cleanUp();
		/*
		 * testing with null reference to Document Path and SDocument
		 */
		try {
			this.getFixture().setSDocument(null);
			this.getFixture().setResourceURI(null);
			this.getFixture().mapSDocument();
//			this.getFixture().mapSDocumentStructure(null, null);
			fail("Document Path and SDocument are not referenced");
		} catch (PAULAExporterException e){
		}	
		/*
		 * testing with null reference to Document Path
		 */
		try {
			this.getFixture().setSDocument(SaltFactory.eINSTANCE.createSDocument());
			this.getFixture().setResourceURI(null);
			this.getFixture().mapSDocument();
//			this.getFixture().mapSDocumentStructure(SaltFactory.eINSTANCE.createSDocument(), null);
			fail("There is no reference to Document Path");
		} catch (PAULAExporterException e){
		}
		/*
		 * testing with null reference to SDocument
		 */
		try {
			this.getFixture().setSDocument(null);
			this.getFixture().setResourceURI(URI.createURI(outputDirectory1));
			this.getFixture().mapSDocument();
//			this.getFixture().mapSDocumentStructure(null, URI.createURI(outputDirectory1));
			fail("There is no reference to SDocument");
		} catch (PAULAExporterException e){
		}
	}
	
	
	private void cleanUp() {
		File resourceDir = new File(resourcePath).getParentFile();
		//System.out.println(resourceDir.toString());
		if (deleteDirectory(resourceDir))
			System.out.println("Deleted temporary directory "+resourceDir.getAbsolutePath());
		
		
	}
	
	public void testMapCorpusStructure2()
	{
		/*
		 * testing with salt sample graph. Export twice and compare
		 */
		PAULAExporter exporter= new PAULAExporter();
		Hashtable<SElementId, URI> documentPaths1 = 
			exporter.mapCorpusStructure(saltSample.getCorpus(), URI.createURI(outputDirectory1));
		Hashtable<SElementId, URI> documentPaths2 =
				exporter.mapCorpusStructure(saltSample.getCorpus(), URI.createURI(outputDirectory2));
		
		// XML-file comparision
		for (SDocument sDocument : saltSample.getCorpus().getSDocuments()){
			
			this.getFixture().setSDocument(sDocument);
			this.getFixture().setResourceURI(documentPaths1.get(sDocument.getSElementId()));
			this.getFixture().mapSDocument();
		}
		for (SDocument sDocument : saltSample.getCorpus().getSDocuments()){
			this.getFixture().setSDocument(sDocument);
			this.getFixture().setResourceURI(documentPaths2.get(sDocument.getSElementId()));
			this.getFixture().mapSDocument();
		}
		for (SDocument sDocument : saltSample.getCorpus().getSDocuments()){
			this.compareDocuments(documentPaths1.get(sDocument.getSElementId()),documentPaths2.get(sDocument.getSElementId()));
		}
	}
	
	/**
	 * Deletes the directory with all contained directories/files
	 * @param fileToDelete
	 */
	private boolean deleteDirectory(File fileToDelete) {
		//System.out.println("Deleting "+fileToDelete.getAbsolutePath());
		if (fileToDelete.isDirectory()) {
	        String[] directoryContent = fileToDelete.list();
	        for (int i=0; i < directoryContent.length; i++) {
	            if (! (deleteDirectory(new File(fileToDelete, directoryContent[i])))) {
	                return false;
	            }
	        }
	    }

	    return fileToDelete.delete();
	}


	/**
	 * Method for compating xml-documents. <br/>
	 * This method checks whether input and output files 
	 * are identical which should <br/>
	 * be the case since the 
	 * test works with SaltSample. 
	 * 
	 * @param uri input path
	 * @param uri2 output path
	 */
	private void compareDocuments(URI uri, URI uri2) {
		File fileToCheck = null;
		InputSource gold = null;
		InputSource toCheck = null;
		FileComparator fileComparator = new FileComparator();
		for (File in : new File(uri.toFileString()).listFiles(this)){
			fileToCheck = new File(uri2.toFileString()+File.separator+in.getName());
			try {
				toCheck = new InputSource(new FileInputStream(fileToCheck));
				gold = new InputSource(new FileInputStream(in));
				
				
				if (! (fileComparator.compareFiles(in, fileToCheck))){
					System.out.println("WARNING: File "+in.getAbsolutePath()+" and "+ fileToCheck.getAbsolutePath()+" are not equal!");
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
}
