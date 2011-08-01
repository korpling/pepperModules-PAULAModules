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

import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.Salt2PAULAMapper;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.exceptions.PAULAExporterException;
import junit.framework.TestCase;
import org.custommonkey.xmlunit.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

 
 

public class Salt2PAULAMapperTest extends TestCase implements FilenameFilter{
	
	String inputDirectory = "/home/eladrion/Desktop/MarioTask/PAULAExporter/pcc2/paula_Export/pcc2/11299/";
	String outputDirectory = "/home/eladrion/Desktop/MarioTask/PAULAExporter/pcc2/paula_ExportCompare/pcc2/11299/";
	
	private Salt2PAULAMapper fixture = null;

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
	
	@Override	
	public void setUp(){
		this.setFixture(new Salt2PAULAMapper());
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
	public void testMapSDocumentStructure(){
		File inputDir = new File(inputDirectory);
		//File outputDir = new File(outputDirectory);
		File fileToCheck = null;
		Diff difference = null;
		for (File in : inputDir.listFiles(this)){
			fileToCheck = new File(outputDirectory+in.getName());
			try {
				System.out.println("File "+in.getAbsolutePath()+" and "+ fileToCheck.getAbsolutePath()+" are");
				difference = new Diff(new InputSource(new FileInputStream(in)),new InputSource(new FileInputStream(fileToCheck)));
				//difference.
				XMLAssert.assertXMLEqual("not equal!",difference,true);
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			this.getFixture().mapSDocumentStructure(null, null);
			fail("Document Path is null");
			} catch (PAULAExporterException e){
				//System.out.println(e.getMessage());
				//fail(e.getMessage());
			}	
			
	}
	
	public void compareXMLFiles(){
		File inputDir = new File(inputDirectory);
		//File outputDir = new File(outputDirectory);
		File fileToCheck = null;
		for (File in : inputDir.listFiles(this)){
			fileToCheck = new File(outputDirectory+in.getName());
			try {
				XMLAssert.assertXMLEqual("Not equal", new InputSource(new FileInputStream(in)), new InputSource(new FileInputStream(fileToCheck)));
				System.out.println("File "+in.getAbsolutePath()+" and "+ fileToCheck.getAbsolutePath()+" are equal");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
}
