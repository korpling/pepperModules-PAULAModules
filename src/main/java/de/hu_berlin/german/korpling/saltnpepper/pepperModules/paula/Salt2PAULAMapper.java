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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.log.LogService;

import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.exceptions.PAULAExporterException;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.Node;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.SaltCommonFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
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
	 * @return null, if no document directory could be created <br>
	 * 		   HashTable&lt;SElementId,URI&gt; else.<br>
	 * 			Comment: URI is the complete document path
	 */
	public Hashtable<SElementId, URI> mapCorpusStructure(SCorpusGraph sCorpusGraph, URI corpusPath)
	{   
		if (sCorpusGraph== null)
			throw new PAULAExporterException("Cannot export corpus structure, because sCorpusGraph is null.");
		if (corpusPath== null)
			throw new PAULAExporterException("Cannot export corpus structure, because the path to export to is null.");
		Hashtable<SElementId, URI> retVal= null;
		int numberOfCreatedDirectories = 0;
		//System.out.println(corpusPath.toFileString());
		
		List<SDocument> sDocumentList =  Collections.synchronizedList(sCorpusGraph.getSDocuments());
		
		Hashtable<SElementId,URI> tempRetVal = new Hashtable<SElementId,URI>();
		
		// Check whether corpus path ends with Path separator. If not, hang it on, else convert it to String as it is
		String corpusPathString = corpusPath.toFileString().replace("//", "/");
		if (! corpusPathString.endsWith(File.pathSeparator)){
			corpusPathString.concat(File.pathSeparator);
		} else {
			corpusPathString = corpusPath.toFileString();
		}
		for (SDocument sDocument : sDocumentList) {
			String completeDocumentPath = corpusPathString;
			String relativeDocumentPath;
			// Check whether sDocumentPath begins with a salt:/. If it does, remove it and save the remainder. else just save the complete String
			relativeDocumentPath = sDocument.getSElementId().getValueString().replace("salt:/", "");
			// remove leading path separator, if existent
			if (relativeDocumentPath.substring(0, 1).equals(File.pathSeparator)){
				completeDocumentPath = completeDocumentPath.concat(relativeDocumentPath.substring(1));
			} else {
				completeDocumentPath = completeDocumentPath.concat(relativeDocumentPath);
			}
				
			// Check whether directory exists and throw an exception if it does. Else create it
			// We don't need this... we just overwrite the document
			
			if ((new File(completeDocumentPath).isDirectory())){
				numberOfCreatedDirectories++;
				tempRetVal.put(sDocument.getSElementId(),org.eclipse.emf.common.util.URI.createFileURI(completeDocumentPath));
			} else {
				if (!( (new File(completeDocumentPath)).mkdirs() )){ 
					throw new PAULAExporterException("Cannot create directory "+completeDocumentPath);
				} else {
					numberOfCreatedDirectories++;
					tempRetVal.put(sDocument.getSElementId(),org.eclipse.emf.common.util.URI.createFileURI(completeDocumentPath));
				}
			}
		}
		//System.out.println("Created directories (number): "+ numberOfCreatedDirectories);
		if (numberOfCreatedDirectories > 0){
			retVal = tempRetVal;
		}
		tempRetVal = null;
		//TODO !done! for each SDocument in sCorpusGraph.getSDocuments() create a directory relative to corpusPath. for instance if corpusPath= c:/corpusPath and sDocument.getSElementId()= corpus1/corpus2/document1, than the directory has to be c:/corpusPath/corpus1/corpus2/document1
		//TODO !done! check, that a directory is created only once, else an exception has to be raised
		//TODO !done! check that the directory has been created successfully
		//TODO !done! for each SDocument object create an entry in retVal, but note initialize retVal first, but only if at minimum one folder has been created 
				
		return(retVal);
	}

	/**
	 * 	Maps the SDocument to PAULA format and writes files to documentPath.
	 * @param sDocument
	 * @param documentPath
	 * @return nothing
	 */
	public void mapSDocumentStructure(SDocument sDocument, URI documentPath)
	{
		if (sDocument == null)
			throw new PAULAExporterException("Cannot export document structure because sDocument is null");
		
		if (documentPath == null)
			throw new PAULAExporterException("Cannot export document structure because documentPath is null");
		
		EList<Node> nodeList = (EList<Node>) sDocument.getSDocumentGraph().getNodes();
		EList<STextualDS> sTextualDataSource = sDocument.getSDocumentGraph().getSTextualDSs();
		for (STextualDS sText : sTextualDataSource){
			String docID = sDocument.getSElementId().getValueString().substring(sDocument.getSElementId().getValueString().indexOf("/"));
			createPAULATextFile(sText.getSText(),docID,documentPath);
		}
		
		
		
		//TODO: read primary text from Salt file
		//TODO !done! check that parameters are not null and raise an exception if necessary
		//TODO map sDocument to PAULA and write files to documentPath
	}

	/**
	 * 
	 * @param sText
	 * @param documentID 
	 * @param documentPath 
	 */
	private void createPAULATextFile(String sText, String documentID, URI documentPath) {
		
		File textFile = new File(documentPath.toFileString() + documentID + "_text.xml");
		System.out.println("Filename: " + textFile.getAbsolutePath() + "\n DocID: " + documentID );
		String fileText = new StringBuffer()
		.append("<?xml version=\"1.0\" standalone=\"no\"?>\n")
		.append("<!DOCTYPE paula SYSTEM \"paula_text.dtd\">\n")
		.append("<paula version=\"1.0\">\n")
		.append("<header paula_id=\"")
		.append(documentID.replace("/", ""))
		.append("_text\" type=\"text\"/>\n")
		.append("<body>\n")
		.append(sText+ "\n")
		.append("</body>\n")
		.append("</paula>")
		.toString();
		
		try{
		textFile.createNewFile();
		Writer output = new BufferedWriter(
				new OutputStreamWriter(
						new FileOutputStream(textFile),"UTF8"));
		
		for (int i = 0;i < fileText.length();i++){
			output.write(fileText.charAt(i));
		}
		output.close();
		}catch (IOException e){
			System.out.println("Exception: "+ e.getMessage());
		}
				
	}
	
	
}
