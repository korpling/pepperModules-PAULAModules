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
package de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.osgi.service.log.LogService;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.exceptions.PAULAImporterException;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.readers.PAULAReader;

/**
 * Takes controll for reading of all paula-files. Makes sure, that file refered paula-files have been read
 * just for one time.
 * @author Florian Zipser
 *
 */
public class PAULAFileDelegator 
{
// ================================================ start: LogService	
	private LogService logService;
	
	public LogService getLogService() 
	{
		return(this.logService);
	}
	
	public void setLogService(LogService logService) 
	{
		this.logService = logService;
	}
// ================================================ end: LogService
// ================================================ start: paula-path	
	/**
	 * Stores path in which all paula-files shall be
	 */
	private File paulaPath= null;
	/**
	 * @param paulaPath the paulaPath to set
	 */
	public void setPaulaPath(File paulaPath) {
		this.paulaPath = paulaPath;
	}

	/**
	 * @return the paulaPath
	 */
	public File getPaulaPath() {
		return paulaPath;
	}
// ================================================ end: paula-path
//========================== start: paulaFiles	
	private EList<File> paulaFiles= null;

	/**
	 * Returns a list of paula-files. Attention: If startPaulaFiles() has been called, addings to this 
	 * list does have no effect.
	 * @return the paulaFiles
	 */
	public EList<File> getPaulaFiles() 
	{
		if (this.paulaFiles== null)
			this.paulaFiles= new BasicEList<File>();
		return paulaFiles;
	}
//========================== end: paulaFiles
//=================================== start: mapper for callback
	/**
	 * PAULA2SaltMapper for callback.
	 */
	private PAULA2SaltMapper mapper= null;
	
	/**
	 * @param mapper the mapper to set
	 */
	public void setMapper(PAULA2SaltMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * @return the mapper
	 */
	public PAULA2SaltMapper getMapper() {
		return mapper;
	}
//=================================== end: mapper for callback	
	/**
	 * list of already processed paula files. Makes sure that a file will not processed two times.
	 */
	private EList<File> processedPAULAFiles= null;
	/**
	 * not already processed paula-files. Makes sure, that all paula-files will be processed.
	 */
	private EList<File> notProcessedPAULAFiles= null;
	
	/**
	 * Starts initial reading of all given PAULA-files.
	 */
	public void startPaulaFiles()
	{
		if (	(this.getPaulaFiles()== null)||
				(this.getPaulaFiles().size()== 0))
			throw new PAULAImporterException("Cannot start reading paula-files, because no files are given.");
		if (this.getPaulaPath()== null)
			throw new PAULAImporterException("Cannot start reading paula-files, because paula-path is not set. Please set paula-path first.");
		this.processedPAULAFiles= new BasicEList<File>();
		this.notProcessedPAULAFiles= new BasicEList<File>();
		
		{// add all given files to list of not processed paula-files
			for (File paulaFile: this.getPaulaFiles())
				this.notProcessedPAULAFiles.add(paulaFile);
		}// add all given files to list of not processed paula-files
		while (this.notProcessedPAULAFiles.size()> 0)
		{//do until all paula-files have been processed
			File paulaFile= this.notProcessedPAULAFiles.get(0);
			this.startPaulaFile(paulaFile);
		}//do until all paula-files have been processed
	}
	
	private static volatile SAXParserFactory factory= SAXParserFactory.newInstance();
	
	/**
	 * Starts reading of given paula-file. If a file is given which already has been read, nothing happens.
	 * @param paulaFile
	 */
	public void startPaulaFile(File paulaFile)
	{
		Long timestamp= System.nanoTime();
		if (paulaFile== null)
			throw new PAULAImporterException("Cannot start reading paula-file, because given file is empty.");
		if (!paulaFile.isAbsolute())
		{
			paulaFile= new File(this.getPaulaPath().getAbsolutePath()+ "/"+paulaFile.toString());
		}
		if (paulaFile.isDirectory())
			throw new PAULAImporterException("Cannot read the given paula-file ('"+paulaFile.getAbsolutePath()+"'), because it is a directory.");
			
		Boolean isAlreadyProcessed= false;
		for (File paulaFile2: this.processedPAULAFiles)
		{
			if (paulaFile.getAbsolutePath().equals(paulaFile2.getAbsolutePath()))
				isAlreadyProcessed= true;
		}
		if (isAlreadyProcessed)
		{//paula-file still has been processed
		}//paula-file still has been processed
		else
		{//paula-file has not yet been processed
			if (this.logService!= null)
				this.logService.log(LogService.LOG_DEBUG, "Importing paula-file: "+paulaFile.getAbsolutePath()+".");
			this.notProcessedPAULAFiles.remove(paulaFile);
			this.processedPAULAFiles.add(paulaFile);
			PAULAReader paulaReader= new PAULAReader();
			paulaReader.setLogService(this.getLogService());
			paulaReader.setPaulaFileDelegator(this);
			
			SAXParser parser;
	        XMLReader xmlReader;
	        
	        {//configure mapper
				paulaReader.setMapper(this.getMapper());
				paulaReader.setPaulaFile(paulaFile);
			}//configure mapper
	        
	        try {
	        	parser= factory.newSAXParser();
		        xmlReader= parser.getXMLReader();
	
		        //create content handler
		        xmlReader.setContentHandler(paulaReader);
		       //set lexical handler for validating against dtds
				xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", paulaReader);
				xmlReader.setDTDHandler(paulaReader);
				
				try
				{
					//start reading file		        
			        InputStream inputStream= new FileInputStream(paulaFile.getAbsolutePath());
					Reader reader = new InputStreamReader(inputStream,"UTF-8");
					 
					InputSource is = new InputSource(reader);
					//important in case of dtd's are used, the path where to find them must be given
					is.setSystemId(paulaFile.getAbsolutePath());
					is.setEncoding("UTF-8");
					xmlReader.parse(is);
				} catch (SAXException e) 
				{ 
					try
		            {
						parser= factory.newSAXParser();
				        xmlReader= parser.getXMLReader();
				        xmlReader.setContentHandler(paulaReader);
						xmlReader.parse(paulaFile.getAbsolutePath());
		            }catch (Exception e1) {
//		            	e.printStackTrace();
		            	throw new PAULAImporterException("Cannot load paula file from resource '"+paulaFile.getAbsolutePath()+"'.", e1);
					}
				}
			} catch (SAXNotRecognizedException e) {
//				e.printStackTrace();
				throw new PAULAImporterException("Cannot read file '"+paulaFile.getAbsolutePath()+"'. Nested SAXNotSupported Exception is "+e.getLocalizedMessage());
			} catch (SAXNotSupportedException e) 
			{
//				e.printStackTrace();
				throw new PAULAImporterException("Cannot read file '"+paulaFile.getAbsolutePath()+"'. Nested SAXNotSupported Exception is "+e.getLocalizedMessage());
			} catch (ParserConfigurationException e) 
			{
//				e.printStackTrace();
				throw new PAULAImporterException("Cannot read file '"+paulaFile.getAbsolutePath()+"'. Nested ParserConfiguration Exception is "+e.getLocalizedMessage());
			} catch (SAXException e) 
			{
//				e.printStackTrace();
				throw new PAULAImporterException("Cannot read file '"+paulaFile.getAbsolutePath()+"'. Nested SAX Exception is "+e.getLocalizedMessage());
			} catch (IOException e) 
			{
//				e.printStackTrace();
				throw new PAULAImporterException("Cannot read file '"+paulaFile.getAbsolutePath()+"'. Nested IO Exception is "+e.getLocalizedMessage());
			}
			
			if (this.getLogService()!= null)
				this.getLogService().log(LogService.LOG_DEBUG, "Needed time to read document '"+ paulaFile.getName()+ "':\t"+ ((System.nanoTime()- timestamp))/ 1000000);
		}//paula-file has not yet been processed
	}
}
