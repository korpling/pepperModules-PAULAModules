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
package org.corpus_tools.peppermodules.paula;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.peppermodules.paula.readers.PAULAReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * Takes controll for reading of all paula-files. Makes sure, that file refered
 * paula-files have been read just for one time.
 * 
 * @author Florian Zipser
 * 
 */
public class PAULAFileDelegator {
	private static final Logger logger = LoggerFactory.getLogger(PAULAFileDelegator.class);
	// ================================================ start: paula-path
	/**
	 * Stores path in which all paula-files shall be
	 */
	private File paulaPath = null;

	/**
	 * @param paulaPath
	 *            the paulaPath to set
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
	// ========================== start: paulaFiles
	private List<File> paulaFiles = null;

	/**
	 * Returns a list of paula-files. Attention: If startPaulaFiles() has been
	 * called, addings to this list does have no effect.
	 * 
	 * @return the paulaFiles
	 */
	public List<File> getPaulaFiles() {
		if (paulaFiles == null)
			paulaFiles = new ArrayList<File>();
		return paulaFiles;
	}

	// ========================== end: paulaFiles
	// =================================== start: mapper for callback
	/**
	 * PAULA2SaltMapper for callback.
	 */
	private PAULA2SaltMapper mapper = null;

	/**
	 * @param mapper
	 *            the mapper to set
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

	// =================================== end: mapper for callback
	/**
	 * list of already processed paula files. Makes sure that a file will not
	 * processed two times.
	 */
	private List<File> processedPAULAFiles = null;
	/**
	 * not already processed paula-files. Makes sure, that all paula-files will
	 * be processed.
	 */
	private List<File> notProcessedPAULAFiles = null;

	/**
	 * Starts initial reading of all given PAULA-files.
	 */
	public void startPaulaFiles() {
		if ((this.getPaulaFiles() == null) || (this.getPaulaFiles().size() == 0))
			throw new PepperModuleException(getMapper(), "Cannot start reading paula-files, because no files are given.");
		if (this.getPaulaPath() == null)
			throw new PepperModuleException(getMapper(), "Cannot start reading paula-files, because paula-path is not set. Please set paula-path first.");
		this.processedPAULAFiles = new ArrayList<File>();
		this.notProcessedPAULAFiles = new ArrayList<File>();

		{// add all given files to list of not processed paula-files
			for (File paulaFile : this.getPaulaFiles())
				this.notProcessedPAULAFiles.add(paulaFile);
		}// add all given files to list of not processed paula-files
		while (this.notProcessedPAULAFiles.size() > 0) {// do until all
														// paula-files have been
														// processed
			File paulaFile = this.notProcessedPAULAFiles.get(0);
			this.startPaulaFile(paulaFile);
		}// do until all paula-files have been processed
	}

	private static volatile SAXParserFactory factory = SAXParserFactory.newInstance();

	/**
	 * Starts reading of given paula-file. If a file is given which already has
	 * been read, nothing happens.
	 * 
	 * @param paulaFile
	 */
	public void startPaulaFile(File paulaFile) {
		Long timestamp = System.nanoTime();
		if (paulaFile == null)
			throw new PepperModuleException(getMapper(), "Cannot start reading paula-file, because given file is empty.");
		if (!paulaFile.isAbsolute()) {
			paulaFile = new File(this.getPaulaPath().getAbsolutePath() + "/" + paulaFile.toString());
		}
		if (paulaFile.isDirectory())
			throw new PepperModuleException(getMapper(), "Cannot read the given paula-file ('" + paulaFile.getAbsolutePath() + "'), because it is a directory.");

		Boolean isAlreadyProcessed = false;
		for (File paulaFile2 : this.processedPAULAFiles) {
			if (paulaFile.getAbsolutePath().equals(paulaFile2.getAbsolutePath()))
				isAlreadyProcessed = true;
		}
		if (isAlreadyProcessed) {// paula-file still has been processed
		}// paula-file still has been processed
		else {// paula-file has not yet been processed
			logger.debug("[PAULAImporter] Importing paula-file: {}.", paulaFile.getAbsolutePath());
			this.notProcessedPAULAFiles.remove(paulaFile);
			this.processedPAULAFiles.add(paulaFile);
			PAULAReader paulaReader = new PAULAReader();
			paulaReader.setPaulaFileDelegator(this);

			SAXParser parser;
			XMLReader xmlReader;

			// configure mapper
			paulaReader.setMapper(this.getMapper());
			paulaReader.setPaulaFile(paulaFile);

			try {
				parser = factory.newSAXParser();
				xmlReader = parser.getXMLReader();

				// create content handler
				xmlReader.setContentHandler(paulaReader);
				// set lexical handler for validating against dtds
				xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", paulaReader);
				xmlReader.setDTDHandler(paulaReader);

				try {
					// start reading file
					InputStream inputStream = new FileInputStream(paulaFile.getAbsolutePath());
					Reader reader = new InputStreamReader(inputStream, "UTF-8");

					InputSource is = new InputSource(reader);
					// important in case of dtd's are used, the path where to
					// find them must be given
					is.setSystemId(paulaFile.getAbsolutePath());
					is.setEncoding("UTF-8");
					xmlReader.parse(is);
				} catch (SAXException e) {
					try {
						parser = factory.newSAXParser();
						xmlReader = parser.getXMLReader();
						xmlReader.setContentHandler(paulaReader);
						// set lexical handler for validating against dtds
						xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", paulaReader);
						xmlReader.setDTDHandler(paulaReader);
						xmlReader.parse(paulaFile.getAbsolutePath());
					} catch (Exception e1) {
						throw new PepperModuleException(getMapper(), "Cannot load paula file from resource '" + paulaFile.getAbsolutePath() + "'.", e1);
					}
				}
			} catch (SAXNotRecognizedException e) {
				throw new PepperModuleException(getMapper(), "Cannot read file '" + paulaFile.getAbsolutePath() + "'. Nested SAXNotSupported Exception is " + e.getLocalizedMessage());
			} catch (SAXNotSupportedException e) {
				throw new PepperModuleException(getMapper(), "Cannot read file '" + paulaFile.getAbsolutePath() + "'. Nested SAXNotSupported Exception is " + e.getLocalizedMessage());
			} catch (ParserConfigurationException e) {
				throw new PepperModuleException(getMapper(), "Cannot read file '" + paulaFile.getAbsolutePath() + "'. Nested ParserConfiguration Exception is " + e.getLocalizedMessage());
			} catch (SAXException e) {
				throw new PepperModuleException(getMapper(), "Cannot read file '" + paulaFile.getAbsolutePath() + "'. Nested SAX Exception is " + e.getLocalizedMessage());
			} catch (IOException e) {
				throw new PepperModuleException(getMapper(), "Cannot read file '" + paulaFile.getAbsolutePath() + "'. Nested IO Exception is " + e.getLocalizedMessage());
			}

			// adding progress
			this.getMapper().addProgress(1d / (this.processedPAULAFiles.size() + this.notProcessedPAULAFiles.size()));

			logger.debug("[PAULAImporter] Needed time to read document '{}':\t{}", paulaFile.getName(), ((System.nanoTime() - timestamp)) / 1000000);
		}// paula-file has not yet been processed
	}
}
