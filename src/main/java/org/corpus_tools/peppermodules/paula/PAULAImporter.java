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
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.corpus_tools.pepper.impl.PepperImporterImpl;
import org.corpus_tools.pepper.modules.PepperImporter;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.graph.Identifier;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;

/**
 * This importer reads a corpus in PAULA format and maps it to a SALT corpus.
 * The mapping of each document is done in a separate thread.
 * 
 * @author Florian Zipser
 * @version 1.0
 * 
 */
@Component(name = "PAULAImporterComponent", factory = "PepperImporterComponentFactory")
public class PAULAImporter extends PepperImporterImpl implements PepperImporter {
	public PAULAImporter() {
		super();

		// setting name of module
		setName("PAULAImporter");
		setSupplierContact(URI.createURI("saltnpepper@lists.hu-berlin.de"));
		setSupplierHomepage(URI.createURI("https://github.com/korpling/pepperModules-PAULAModules"));
		setDesc("The PAULA importer imports data comming from the PAULA format to a Salt model. ");
		// set list of formats supported by this module
		this.addSupportedFormat("paula", "1.0", null);

		this.getDocumentEndings().add(ENDING_LEAF_FOLDER);
	}

	/**
	 * Stores the endings which are used for paula-files
	 */
	private String[] PAULA_FILE_ENDINGS = { "xml", "paula" };

	private Collection<File> sampleFiles(File dir, int numberOfSampledFiles, String... fileEndings) {

		Collection<File> files = FileUtils.listFiles(dir, fileEndings, true);
		File[] allFiles = new File[files.size()];
		allFiles = files.toArray(allFiles);
		Collection<File> sampledFiles = new ArrayList<>(numberOfSampledFiles);
		Random randomGenerator = new Random();
		int maxFiles= (numberOfSampledFiles > allFiles.length)? allFiles.length: numberOfSampledFiles; 
		for (int i = 0; i < maxFiles; i++) {
			sampledFiles.add(allFiles[randomGenerator.nextInt(allFiles.length)]);
		}
		return sampledFiles;
	}

	private String readLines(File file, int numOfLinesToRead) {
		StringBuilder fileContent = new StringBuilder();
		try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
			String line;
			while (((line = reader.readLine()) != null) && reader.getLineNumber() <= numOfLinesToRead) {
				fileContent.append(line);
				fileContent.append(System.lineSeparator());
			}
		} catch (IOException e) {

		}

		return fileContent.toString();
	}

	private Collection<String> readFileContents(URI corpusPath, int numberOfSampledFiles, int numberOfLines, String... fileEndings){
		File dir = new File(corpusPath.toFileString());
		Collection<File> sampledFiles = sampleFiles(dir, numberOfSampledFiles);
		Collection<String> fileContents= new ArrayList<>(sampledFiles.size());
		for (File sampleFile: sampledFiles){
			fileContents.add(readLines(sampleFile, numberOfLines));
		}
		return fileContents;
	}
	
	@Override
	public Double isImportable(URI corpusPath) {
		Double retValue= 0.0;
		
		if (corpusPath == null) {
			return retValue;
		}
		int numberOfSampledFiles = 10;
		int numberOfLines = 10;
		
		for (String content: readFileContents(corpusPath, numberOfSampledFiles, numberOfLines, PAULA_FILE_ENDINGS)){
			if ((content.contains("<?xml version=\"1.0\"")) && (content.contains("<paula version="))){
				retValue= 1.0;
				break;
			}
		}
		return retValue;
	}

	/**
	 * Creates a mapper of type {@link PAULA2SaltMapper}.
	 * {@inheritDoc PepperModule#createPepperMapper(Identifier)}
	 */
	@Override
	public PepperMapper createPepperMapper(Identifier Identifier) {
		PAULA2SaltMapper mapper = new PAULA2SaltMapper();
		mapper.setPAULA_FILE_ENDINGS(PAULA_FILE_ENDINGS);

		if (Identifier.getIdentifiableElement() instanceof SCorpus) {
			// avoid importing of SCorpus, in case of SCorpus was artificially
			// created and links to the same path as a SDocument object
			Collection<URI> pathes = this.getIdentifier2ResourceTable().values();
			int i = 0;
			for (URI path : pathes) {
				if (path.equals(this.getIdentifier2ResourceTable().get(Identifier)))
					i++;
			}
			if (i > 1)
				mapper.setIsArtificialSCorpus(true);
		}
		return (mapper);
	}
}
