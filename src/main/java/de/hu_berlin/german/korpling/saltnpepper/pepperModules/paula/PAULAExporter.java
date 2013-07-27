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
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;

import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperExceptions.PepperFWException;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.PepperExporter;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.PepperMapper;
import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.impl.PepperExporterImpl;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.exceptions.PAULAExporterException;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SElementId;

/**
 * This class exports data from Salt to the PAULA 1.0 format.
 * @author Mario Frank
 * @author Florian Zipser
 *
 */
@Component(name="PAULAExporterComponent", factory="PepperExporterComponentFactory")
public class PAULAExporter extends PepperExporterImpl implements PepperExporter
{
	public PAULAExporter()
	{
		super();
		
		//setting name of module
		this.name= "PAULAExporter";
				
		//set list of formats supported by this module
		this.addSupportedFormat("paula", "1.0", null);
		
		this.setProperties(new PAULAExporterProperties());
	}	
	
	/**
	 * Maps all sElementIds corresponding to SDocument object to the URI path were they shall be stored. 
	 */
	private Hashtable<SElementId, URI> sDocumentResourceTable= null;
	
	@Override
	public void exportCorpusStructure(SCorpusGraph sCorpusGraph)
	{
		if (sCorpusGraph== null)
			throw new PepperFWException("No SCorpusGraph was passed for exportCorpusStructure(SCorpusGraph corpusGraph). This might be a bug of the pepper framework.");
		else 
		{
			Salt2PAULAMapper mapper= new Salt2PAULAMapper();
			Salt2PAULAMapper.setResourcePath(this.getResources());
			mapper.setPAULAExporter(this);
			mapper.setLogService(this.getLogService());
			sDocumentResourceTable= mapCorpusStructure(sCorpusGraph, this.getCorpusDefinition().getCorpusPath());
			if (sDocumentResourceTable== null)
				throw new PepperFWException("mapCorpusStructure() returned an empty table. This might be a bug of pepper module.");
			if (	(sDocumentResourceTable== null)||
					(sDocumentResourceTable.size()== 0))
			{
				throw new PAULAExporterException("Cannot export SCorpusGraph '"+sCorpusGraph.getSName()+"', because of an unknown reason.");
			}
		}
	}
	
	/**
	 * 	Maps the SCorpusStructure to a folder structure on disk relative to <br/>
	 * the given corpusPath.
	 * @param sCorpusGraph
	 * @param corpusPath
	 * @return null, if no document directory could be created <br>
	 * 		   HashTable&lt;SElementId,URI&gt; else.<br>
	 * 			Comment: URI is the complete document path
	 */
	public Hashtable<SElementId, URI> mapCorpusStructure(SCorpusGraph sCorpusGraph, 
														URI corpusPath)
	{   
		if (sCorpusGraph== null)
			throw new PAULAExporterException("Cannot export corpus structure, because sCorpusGraph is null.");
		if (corpusPath== null)
			throw new PAULAExporterException("Cannot export corpus structure, because the path to export to is null.");
		Hashtable<SElementId, URI> retVal= null;
		int numberOfCreatedDirectories = 0;
		
		List<SDocument> sDocumentList =  Collections.synchronizedList(sCorpusGraph.getSDocuments());
		
		Hashtable<SElementId,URI> tempRetVal = new Hashtable<SElementId,URI>();
		
		// Check whether corpus path ends with Path separator. If not, hang it on, else convert it to String as it is
		String corpusPathString = corpusPath.toFileString().replace("//", "/");
		if (! corpusPathString.endsWith("/")){
			corpusPathString = corpusPathString.concat("/");
		} else {
			corpusPathString = corpusPath.toFileString();
		}
		for (SDocument sDocument : sDocumentList) {
			String completeDocumentPath = corpusPathString;
			String relativeDocumentPath;
			// Check whether sDocumentPath begins with a salt:/. If it does, remove it and save the remainder. else just save the complete String
			//relativeDocumentPath = sDocument.getSName().replace("salt:/", "");
			
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
		if (numberOfCreatedDirectories > 0){
			retVal = tempRetVal;
		}
		tempRetVal = null;
		
		return(retVal);
	}
	
	@Override
	public PepperMapper createPepperMapper(SElementId sElementId)
	{
		Salt2PAULAMapper mapper= new Salt2PAULAMapper();
		if (this.sDocumentResourceTable== null)
			throw new PepperFWException("this.sDocumentResourceTable() is not initialized. This might be a bug of pepper module '"+this.getName()+"'.");
		
		URI resource= this.sDocumentResourceTable.get(sElementId);
		mapper.setResourceURI(resource);
		return(mapper);
	}
}
