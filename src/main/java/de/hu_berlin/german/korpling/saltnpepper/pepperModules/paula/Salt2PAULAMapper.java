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
import java.util.Collections;
import java.util.Hashtable;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.log.LogService;

import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.exceptions.PAULAExporterException;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SElementId;

/**
 * Maps SCorpusGraph objects to a folder structure and maps a SDocumentStructure to the necessary files containing the document data in PAULA notation.
 * @author Mario Frank
 *
 */
public class Salt2PAULAMapper 
{
	/**
	 * OSGI-log service
	 */
	private LogService logService= null;
	public void setLogService(LogService logService) {
		this.logService = logService;
	}

	public LogService getLogService() {
		return logService;
	}
	
	/**
	 * 	Maps the SCorpusStructure to a folder structure on disk relative to the given corpusPath.
	 * @param sCorpusGraph
	 * @param corpusPath
	 * @return
	 */
	public Hashtable<SElementId, URI> mapCorpusStructure(SCorpusGraph sCorpusGraph, URI corpusPath)
	{   
		if (sCorpusGraph== null)
			throw new PAULAExporterException("Cannot export corpus structure, because sCorpusGraph is null.");
		if (corpusPath== null)
			throw new PAULAExporterException("Cannot export corpus structure, because the path to export to is null.");
		Hashtable<SElementId, URI> retVal= null;
		int numberOfCreatedDirectories = 0;
		
		EList<SDocument> sDocumentList = (EList<SDocument>) Collections.synchronizedList(sCorpusGraph.getSDocuments());
		
		
		// Check whether corpus path ends with Path separator. If not, hang it on, else convert it to String as it is
		String corpusPathString = "";
		if (! corpusPath.toFileString().endsWith(File.pathSeparator)){
			corpusPathString = corpusPath.toFileString()+File.pathSeparator;
		} else {
			corpusPathString = corpusPath.toFileString();
		}
		for (SDocument sDocument : sDocumentList) {
			String completeDocumentPath = corpusPathString;
			String relativeDocumentPath;
			// Check whether sDocumentPath begins with a salt:/. If it does, remove it and save the remainder. else just save the complete String
			relativeDocumentPath = sDocument.getSElementId().toString().replace("salt:/", "");
			
			// remove leading path separator, if existent
			if (relativeDocumentPath.substring(0, 1).equals(File.pathSeparator))
				completeDocumentPath += relativeDocumentPath.substring(1);
			
			// Check whether directory exists and throw an exception if it does. Else create it
			if (new File(completeDocumentPath).isDirectory()){
				throw new PAULAExporterException("Directory "+ completeDocumentPath + " already exists");
			} else {
				
				if (!( (new File(completeDocumentPath)).mkdirs() )){ 
					throw new PAULAExporterException("Cannot create directory"+completeDocumentPath);
				} else {
					numberOfCreatedDirectories++;
				}
			}
					
		}
		if (numberOfCreatedDirectories > 0){
			retVal = new Hashtable<SElementId,URI>();
			for (SDocument sDocument : sDocumentList){
				retVal.put(sDocument.getSElementId(), sDocument.getSElementPath());
			}
		}
		
		//TODO !done! for each SDocument in sCorpusGraph.getSDocuments() create a directory relative to corpusPath. for instance if corpusPath= c:/corpusPath and sDocument.getSElementId()= corpus1/corpus2/document1, than the directory has to be c:/corpusPath/corpus1/corpus2/document1
		//TODO !done! check, that a directory is created only once, else an exception has to be raised
		//TODO !done! check that the directory has been created successfully
		//TODO !done?! for each SDocument object create an entry in retVal, but note initialize retVal first, but only if at minimum one folder has been created 
		
		
		return(retVal);
	}

	/**
	 * 	Maps the SCorpusStructure to a folder structure on disk relative to the given corpusPath.
	 * @param sCorpusGraph
	 * @param corpusPath
	 * @return
	 */
	public void mapSDocumentStructure(SDocument sDocument, URI documentPath)
	{
		//TODO check that parameters are not null and raise an exception if necessary
		//TODO map sDocument to PAULA and write files to documentPath
	}
}
