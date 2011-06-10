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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.log.LogService;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;

import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.exceptions.PAULAExporterException;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.PAULAXMLStructure;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.Edge;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.Node;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.modules.SDocumentStructureAccessor;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDominanceRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SSpan;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SStructure;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SStructuredNode;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SElementId;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SLayer;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SNode;

/**
 * Maps SCorpusGraph objects to a folder structure and maps a SDocumentStructure to the necessary files containing the document data in PAULA notation.
 * @author Mario Frank
 *
 */
public class Salt2PAULAMapper implements PAULAXMLStructure
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
	public Hashtable<SElementId, URI> mapCorpusStructure(SCorpusGraph sCorpusGraph, 
														URI corpusPath)
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
		if (! corpusPathString.endsWith(File.separator)){
			corpusPathString = corpusPathString.concat(File.separator);
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
		return(retVal);
	}

	/**
	 * 	Maps the SDocument to PAULA format and writes files to documentPath.
	 * @param sDocument the Salt document that has to be mapped
	 * @param documentPath the output document path to map to
	 * @return nothing
	 */
	public void mapSDocumentStructure(SDocument sDocument, URI documentPath)
	{
		if (sDocument == null)
			throw new PAULAExporterException("Cannot export document structure because sDocument is null");
		
		if (documentPath == null)
			throw new PAULAExporterException("Cannot export document structure because documentPath is null");
		
		EList<STextualDS> sTextualDataSources = sDocument.getSDocumentGraph().getSTextualDSs();
		// create a Hashtable(SName,FileName) with initial Size equal to the number of Datasources 
		Hashtable<String,String> dataSourceFileTable = new Hashtable <String,String>(sTextualDataSources.size());
		
		
		String documentName = sDocument.getSName();
		// map textual data sources
		dataSourceFileTable = mapTextualDataSources(sTextualDataSources,documentName,documentPath);
		// name of the first data source
		String oneDS = sTextualDataSources.get(0).getSName();
		// map all layers
		mapLayers(sDocument.getSDocumentGraph(), documentPath, documentName, dataSourceFileTable,oneDS);
		
	}

	/**
	 * Writes the primary text sText to a file "documentID_text.xml" in the documentPath
	 * and returns the URI (filename).
	 * @param sTextualDS the primary text
	 * @param documentID the document id
	 * @param documentPath the document path
	 * @return Hashtable&lt;STextualDS SName,TextFileName&gt;
	 */
	
		
	private Hashtable<String,String> mapTextualDataSources( EList<STextualDS> sTextualDS, 
									  String documentID,
									  URI documentPath) {
		
		if (sTextualDS.isEmpty())
			throw new PAULAExporterException("Cannot map Data Sources because there are none");
		if (documentID.equals(""))
			throw new PAULAExporterException("Cannot map Data Sources because documentID is empty (\"\")");
		if (documentPath == null)
			throw new PAULAExporterException("Cannot map Data Sources because documentPath is null");
		
		File textFile;
		int dsNum = sTextualDS.size();
		// Hashtable <DataSourceSName,PrintWriter>
		Hashtable<String,PrintWriter> sTextualDSWriterTable = new Hashtable<String,PrintWriter>(dsNum);
		// Hashtable <DataSourceSName,fileName>
		Hashtable<String,String> sTextualDSFileTable = new Hashtable<String,String>(dsNum);
		
		/**
		 * Iterate over all Textual Data Sources
		 */
		for (STextualDS sText : sTextualDS){
			/**
			 * If there is one DS, create one non-numerated text file, else numerate
			 */
			if (dsNum == 1){
				textFile = new File(documentPath.toFileString()+ 
								File.separator + documentID+ ".merged.text.xml");
			} else {
				textFile = new File(
								documentPath.toFileString()+ 
								File.separator + documentID +".merged.text."+
								(sTextualDS.indexOf(sText)+1)+".xml");
			}
			
			try{
				if (! textFile.createNewFile())
					System.out.println("File: "+ textFile.getName()+ " already exists");
			
				PrintWriter output = new PrintWriter( 
										new BufferedWriter(
											new OutputStreamWriter(
												new FileOutputStream(
														textFile)
												,"UTF8")),false);
			
				/**
				 * put the PrintWriter into WriterTable for further access
				 * put the SName and FileName into FileTable for Token file construction
				 */
				sTextualDSWriterTable.put(sText.getSName(), output);
				sTextualDSFileTable.put(sText.getSName(), textFile.getName());
				
				/**
				 * Write the Text file content 
				 */
				{
					output.println(TAG_HEADER_XML);
					output.println(PAULA_TEXT_DOCTYPE_TAG);
					output.println(TAG_PAULA_OPEN);
					output.println( new StringBuffer("\t<header paula_id=\"pepper.")
									.append(documentID)
									.append("_text\" type=\"text\"/>").toString());
					output.println("\t"+TAG_TEXT_BODY_OPEN);
					output.println("\t\t" + sText.getSText());
					output.println("\t"+TAG_TEXT_BODY_CLOSE);
					output.println(PAULA_CLOSE_TAG);			
				}	
				output.close();
				
				}catch (IOException e){
					System.out.println("mapTextualDataSources: could not map to file "+textFile.getName()+" . Cause: "+ e.getMessage());
				}
			
							
		}
		// dispose PrintWriter table
		sTextualDSWriterTable = null;
		
		return sTextualDSFileTable;
		
	}
	
	/**
	 * Map the layers of the document graph including token, spans and structs to files.
	 * @param sDocumentGraph
	 * @param documentPath
	 * @param documentId
	 * @param fileTable the data source file table
	 * @param oneDS the first data source
	 */
	private void mapLayers(SDocumentGraph sDocumentGraph,
						   URI documentPath,
						   String documentId,
						   Hashtable<String, String> fileTable, 
						   String firstDSName){
		
		if (sDocumentGraph == null)
			throw new PAULAExporterException("Cannot map Layers files because document graph is null");
		if (documentPath.equals(""))
			throw new PAULAExporterException("Cannot map Layers because documentPath is empty (\"\")");
		if (documentId.equals(""))
			throw new PAULAExporterException("Cannot map Layers files because documentID is empty (\"\")");
		if (fileTable == null)
			throw new PAULAExporterException("Cannot map Layers files because fileTable is null");
	
		SDocumentStructureAccessor accessor = new SDocumentStructureAccessor();
		accessor.setSDocumentGraph(sDocumentGraph);
		
		/**
		 * Copy the spans ant structs.
		 * By doing this, we can assure later that we found all spans/structs
		 */
		EList<SSpan> spanList = new BasicEList<SSpan>(sDocumentGraph.getSSpans());
		EList<SStructure> structList = new BasicEList<SStructure>(sDocumentGraph.getSStructures());
		
		Hashtable<String,String> tokenFileMap = null;
		Hashtable<String,String> spanFileMap = null;
		/**
		 * Lists for all constructs we may find in one layer 
		 */
		EList<SSpan> layerSpanList ;
		EList<SStructure> layerStructList ;
		EList<SToken> layerTokenList ;
		/**
		 * Port lists for later paula versions allowing to handle
		 * structured elements belonging to multiple layers
		 */
		EList<SSpan> multiLayerSpanList = new BasicEList<SSpan>();
		EList<SStructure> multiLayerStructList = new BasicEList<SStructure>();
		EList<SToken> multiLayerTokenList = new BasicEList<SToken>();
		
		EList<STextualRelation> textualRelations ;
		/**
		 * iterate over all layers
		 */
		for (SLayer layer : sDocumentGraph.getSLayers()){
			layerSpanList = new BasicEList<SSpan>();
			layerStructList = new BasicEList<SStructure>();
			layerTokenList = new BasicEList<SToken>();
			
			/**
			 * iterate over all nodes.
			 * put the nodes in the right lists, according to their type
			 */
			for (SNode sNode : layer.getSNodes()){
				/**
				 * Token
				 */
				if (sNode instanceof SToken){
					if (sNode.getSLayers().size() > 1){
						multiLayerTokenList.add((SToken)sNode);
					} else {
						layerTokenList.add((SToken)sNode);
						
					}
				}
				/**
				 * Spans
				 */
				if (sNode instanceof SSpan ){
					if (sNode.getSLayers().size() > 1){
						multiLayerSpanList.add((SSpan) sNode);
					} else {
						spanList.remove((SSpan) sNode);
						layerSpanList.add((SSpan) sNode);
					}
				}
				
				/**
				 * Structs
				 */
				if (sNode instanceof SStructure ){
					if (sNode.getSLayers().size() > 1){
						multiLayerStructList.add((SStructure) sNode);
					} else {
						structList.remove((SStructure) sNode);
						layerStructList.add((SStructure) sNode);
					}
					
				}
					
				
			}
			
			/**
			 * We searched the layer completly
			 * now we have to map the token/spans/structs
			 */
			
			if (! layerTokenList.isEmpty()){
				/**
				 * We did not find all token (should not happen!)
				 */
				if (sDocumentGraph.getSTextualRelations().size() > layerTokenList.size())
					throw new PAULAExporterException("Salt2PAULAMapper: There are more Textual Relations then Token in layer"+ layer.getSName());
				/**
				 * map token		
				 */
				tokenFileMap = mapTokens(sDocumentGraph.getSTextualRelations(),layerTokenList,fileTable,documentId,documentPath,layer.getSName());
			}
			
			if (! layerSpanList.isEmpty()){
				spanFileMap = mapSpans(sDocumentGraph, layerSpanList,tokenFileMap,fileTable,documentId,documentPath, layer.getSName(), firstDSName);
			}
			if (! layerStructList.isEmpty()){
				mapStructs(layerStructList,tokenFileMap,spanFileMap,layer.getSName(),documentId, documentPath);
			}
		}
		/**
		 * If we did not find all spans/structs in the layers we take the remaining
		 * spans/structs and map them in extra files
		 */
		if (! spanList.isEmpty()){
			System.out.println("There are Spans which are not in one Layer");
		}
		if (! structList.isEmpty()){
			System.out.println("There are Structs which are not in one Layer");
		}
		
	}
	
	
	
	/**
	 * Extracts the tokens including the xPointer from the STextualRelation list 
	 * and writes them to files "documentID.tokenfilenumber.tok.xml" in the 
	 * documentPath.
	 * 
	 * If there is only one textual data source, the tokenfile number is omitted.
	 * 
	 * 
	 * @param sTextRels list of textual relations (tokens)pointing to a target (data source)
	 * @param layerTokenList 
	 * @param fileTable Hashmap including the SId (String) of the data-source, the URI of the corresponding textfile and a PrintWriter for each token file
	 * @param documentID the document id of the Salt document
	 * @param documentPath the path to which the token files will be mapped
	 * @param layer Name of the layer
	 * @return Hashtable of the form (TokenName,TokenFile)
	 */
	private Hashtable<String, String> mapTokens( EList<STextualRelation> sTextRels,
									   EList<SToken> layerTokenList, 
									   Hashtable<String, String> fileTable, 
									   String documentID,  
									   URI documentPath, String layer) {
		
		if (sTextRels.isEmpty())
			throw new PAULAExporterException("Cannot create token files because there are no textual relations");
		if (documentID.equals(""))
			throw new PAULAExporterException("Cannot create token files because documentID is empty (\"\")");
		if (documentPath == null)
			throw new PAULAExporterException("Cannot create token files because documentPath is null");
		if (fileTable == null)
			throw new PAULAExporterException("Cannot create token files because no textFileTable is defined" );
		
		/**
		 * Create one Hashmap for returning and
		 * one Hashmap for the PrintWriter 
		 */
		Hashtable<String,String> tokenFileMap = new Hashtable<String,String>();
		Hashtable<String,PrintWriter> tokenWriteMap = new Hashtable<String,PrintWriter>();
		String baseTextFile;
		int tokenFileIndex = 0;
		File tokenFile = null;
		
		//StringBuffer fileString = new StringBuffer();
		/**
		 * iterate over all textual relations
		 */
		for (STextualRelation sTextualRelation : sTextRels){
			/**
			 * Get one PrintWriter
			 */
			PrintWriter output = tokenWriteMap.get(sTextualRelation.getSTarget().getSName());
			/**
			 * get the target of the current textual Relation
			 */
			String sTextDSSid = sTextualRelation.getSTarget().getSName();
			
			/**
			 * Set the tokenFileIndex
			 * Split the file name by dots and take the string before xml
			 * This will be a number if there are at least 2 data sources
			 */
			if (fileTable.size() > 1){
				String[] textFileParts = fileTable.get(sTextDSSid).split(".");
				tokenFileIndex = Integer.parseInt(textFileParts[textFileParts.length-2]);
			
			}
			/**
			 * Prepare the mark tag
			 */
			String tokenMarkTag = new StringBuffer("\t\t<mark id=\"")
				  .append(sTextualRelation.getSToken().getSName()).append("\" ")
			      .append("xlink:href=\"#xpointer(string-range(//body,'',")
			      .append(sTextualRelation.getSStart()+1).append(",")
			      .append(sTextualRelation.getSEnd()-sTextualRelation.getSStart())
			      .append("))\" />").toString();
			
			
			
			/**
			 * If output is null, we first have to create one token file,
			 * write the preamble and the mark tag
			 * Else we can just write the mark tag
			 */
			if (output != null){
				output.println(tokenMarkTag);
			}else{
				/**
				 * Create the token file name (Path + filename of DS with text replaced by tok)
				 * get the base text file (is contained in the fileTable)
				 */
				String tokenFileName;
				if (fileTable.size()>1){
					tokenFileName = new String(documentPath.toFileString()+File.separator+documentID+"."+layer+"."+"tok"+tokenFileIndex+".xml");
				}else{
					tokenFileName = new String(documentPath.toFileString()+File.separator+documentID+"."+layer+"."+"tok.xml");
				}
				baseTextFile = new String(fileTable.get(sTextDSSid));
				tokenFile = new File(tokenFileName);
				try {
					if ( ! tokenFile.createNewFile())
						System.out.println("File: "+ tokenFile.getName()+ " already exists");
					
					output = new PrintWriter(new BufferedWriter(	
							new OutputStreamWriter(
									new FileOutputStream(tokenFile),
									"UTF8")),
									true);
					/**
					 * Write preamble and the first mark tag to file
					 */
					if (fileTable.size()>1){
						output.write(createMarkFileBeginning(documentID+"_"+layer+"_"+tokenFileIndex+"_tok",
							"tok", 
							baseTextFile.replace(tokenFile.getPath(),""), 
							0));
					}else{
						output.write(createMarkFileBeginning(documentID+"_"+layer+"_tok",
								"tok", 
								baseTextFile.replace(tokenFile.getPath(),""), 
								0));
					}
						
					output.println(tokenMarkTag);
					
					/**
					 * Put PrintWriter into the tokenWriteMap for further access
					 * 
					 */
					tokenWriteMap.put(sTextualRelation.getSTarget().getSName(), output);
					//tokenFileMap.put(sTextualRelation.getSToken().getSName(), tokenFile.getName());
				
				} catch (IOException e) {
					System.out.println("Exception: "+ e.getMessage());
				}
				
				 
			}
			/**
			 * Put <TokenName,TokenFileName> into tokenFileMap
			 */
			tokenFileMap.put(sTextualRelation.getSToken().getSName(), tokenFile.getName());
			
			
		}
		/**
		 * Close all token file streams
		 */
		for (PrintWriter writer :  (tokenWriteMap.values())){
			writer.write(PAULA_TOKEN_FILE_CLOSING);
			writer.close();
			System.out.println("Wrote token File");
		}
		/**
		 * dispose all Writers since we are finished with the tokens
		 * map token annotations
		 * return the token file map
		 */
		tokenWriteMap = null;
		mapTokenAnnotations(tokenFileMap,layerTokenList,documentPath,documentID);
		return tokenFileMap;
	}
	
	/**
	 * Writes all span files for one specific layer.
	 * TODO: Need more JAVADOC!!!
	 * @param graph
	 * @param layerSpanList
	 * @param tokenFileTable
	 * @param dSFileTable
	 * @param documentId
	 * @param documentPath
	 * @param layer
	 * @param firstDSName
	 * @return 
	 */
	private Hashtable<String, String> mapSpans(SDocumentGraph graph,EList<SSpan> layerSpanList, Hashtable<String, String> tokenFileTable, Hashtable<String, String> dSFileTable, String documentId, URI documentPath, String layer , String firstDSName){
		
		if (documentPath.equals(""))
			throw new PAULAExporterException("Cannot map Layers because documentPath is empty (\"\")");
		if (documentId.equals(""))
			throw new PAULAExporterException("Cannot map Layers files because documentID is empty (\"\")");
		if (graph == null)
			throw new PAULAExporterException("Cannot map Layers files because document graph is null");
		if (layerSpanList == null)
			throw new PAULAExporterException("Cannot map Layers files because layerSpanList is null");
		if (tokenFileTable == null)
			throw new PAULAExporterException("Cannot map Layers files because token File Table is null");
		if (dSFileTable == null)
			throw new PAULAExporterException("Cannot map Layers files because dataSource File Table is null");
		if (layer.equals(""))
			throw new PAULAExporterException("Cannot map Layers files because layer name is empty (\"\")");
		if (firstDSName.equals(""))
			throw new PAULAExporterException("Cannot map Layers files because first DS Name is empty (\"\")");
		
		
		SDocumentStructureAccessor accessor = new SDocumentStructureAccessor();
		accessor.setSDocumentGraph(graph);
		String paulaID;
		
		/**
		 * Create PrintWriter Table
		 */
		Hashtable<String,String> spanFileTable = new Hashtable<String,String>();
		/**
		 * get all spans
		 */
		EList<SSpan> spanList = new BasicEList<SSpan>(graph.getSSpans());
		EList<String> spanFileNames = new BasicEList<String>();
		EList<SToken> overlappingTokens = null;
		
		int dsNum = graph.getSTextualDSs().size();
		/**
		 * Create the base for the markList tag
		 * This is the name of the first token file
		 * which is the name of the first DS File with text replaced by tok
		 */
		String baseMarkFile = dSFileTable.get(firstDSName).replace("text", "tok");
		String spanFileToWrite = documentId +"."+layer +".mark.xml";
		PrintWriter output = null;
		/**
		 * Create span File
		 */
		File spanFile = new File(documentPath.toFileString() + File.separator + spanFileToWrite);
		try {
			if (!(spanFile.createNewFile()))
				System.out.println("File: "+ spanFile.getName()+ " already exists");
			
			output = new PrintWriter(
				new BufferedWriter(	
					new OutputStreamWriter(
							new FileOutputStream(spanFile.getAbsoluteFile())
									,"UTF8")),
									false);
			
			/**
			 * Write markfile-preamble to file
			 * 
			 */
			paulaID = spanFileToWrite.substring(0, spanFileToWrite.length()-4);
			output.write(
				createMarkFileBeginning(
					paulaID,
					"mark",
					baseMarkFile, 
					dsNum));
		
		} catch (IOException e) {
			throw new PAULAExporterException("mapSpans: Could not write File "+spanFileToWrite.toString()+": "+e.getMessage());
		}
			
		for (SSpan sSpan : layerSpanList){
			/**
			 * get tokens which are overlapped by this Span
			 */
			overlappingTokens = accessor.getSTextualOverlappedTokens((SStructuredNode) sSpan);
			spanFileTable.put(sSpan.getSName(), spanFileToWrite);	
			spanList.remove(sSpan);
			spanFileNames.add(spanFileToWrite);
			/**
			 * Write mark tag
			 */
			output.println(
					createSpanFileMarkTag(
						sSpan.getSName(),
						dSFileTable, overlappingTokens, 
						dsNum,
						baseMarkFile,
						firstDSName)
						);
				
		}
		output.write("\t"+MARK_LIST_CLOSE_TAG+LINE_SEPARATOR+PAULA_CLOSE_TAG);
		output.close();
		mapSpanAnnotations(layerSpanList,documentPath,paulaID);
		return spanFileTable;
	}
	
	/**
	 * TODO: IMPLEMENT, NEED MORE JAVADOC!!!!
	 * @param layerStructList
	 * @param fileTable
	 * @param spanFileMap 
	 * @param layer
	 * @param documentId
	 * @param documentPath
	 */
	private void mapStructs(EList<SStructure> layerStructList, Hashtable<String, String> tokenFileTable,Hashtable<String, String> spanFileTable, String layer, String documentId, URI documentPath) {
		if (documentPath.equals(""))
			throw new PAULAExporterException("Cannot map Layers because documentPath is empty (\"\")");
		if (documentId.equals(""))
			throw new PAULAExporterException("Cannot map Layers files because documentID is empty (\"\")");
		if (layerStructList == null)
			throw new PAULAExporterException("Cannot map Layers files because layerSpanList is null");
		if (tokenFileTable == null)
			throw new PAULAExporterException("Cannot map Layers files because token File Table is null");
		if (layer.equals(""))
			throw new PAULAExporterException("Cannot map Layers files because layer name is empty (\"\")");
		
		Hashtable<String,PrintWriter> structWriterTable = new Hashtable<String,PrintWriter>();
		SDocumentStructureAccessor accessor = new SDocumentStructureAccessor();
		accessor.setSDocumentGraph(layerStructList.get(0).getSDocumentGraph());
		
		String paulaID = documentId+"."+layer+"_struct";
		
		File structFile = new File(documentPath.toFileString()+File.separator+documentId+"."+layer+".struct.xml");
		PrintWriter output = null;
		try {
			if (!(structFile.createNewFile()))
				System.out.println("File: "+ structFile.getName()+ " already exists");
		
		output = new PrintWriter(
				new BufferedWriter(	
						new OutputStreamWriter(
								new FileOutputStream(structFile.getAbsoluteFile())
										,"UTF8")),
										false);
		
		} catch (IOException e) {
			throw new PAULAExporterException("mapStructs: Could not write File "+structFile.getName()+": "+e.getMessage());
		}
		
		output.write(
				createStructFileBeginning(
					paulaID,
					"struct"));
		EList<SDominanceRelation> domRels = layerStructList.get(0).getSDocumentGraph().getSDominanceRelations();
		
		for (SStructure struct : layerStructList){
			output.println(new StringBuffer("struct id=\"").insert(0, "\t\t<")
					.append(struct.getSName()).append("\">").toString());
			
			
			for (Edge edge : struct.getSDocumentGraph().getOutEdges(((SNode)struct).getSId())){
				String baseFile;
				if (edge instanceof SDominanceRelation){
					if (((SDominanceRelation)edge).getSTarget() instanceof SSpan){
						baseFile = spanFileTable.get(((SDominanceRelation) edge).getSTarget().getSName());
					} else {
						if (((SDominanceRelation)edge).getSTarget() instanceof SToken){
							baseFile = tokenFileTable.get(((SDominanceRelation)edge).getSTarget().getSName());
						}else{
							baseFile = "";
						}
					}
					output.println(new StringBuffer("\t\t\t<rel id=\"")
						.append(((SDominanceRelation)edge).getSName()).append("\" type=\"")
						.append(((SDominanceRelation)edge).getSTypes().get(0)).append("\" xlink:href=\"")
						.append(baseFile).append("#")
						.append(((SDominanceRelation)edge).getSTarget().getSName())
						.append("\"/>").toString());
				}
			}
			
			/*
			for (SDominanceRelation rel : domRels){
				if (rel.getSSource() == struct){
					String baseFile;
					if (rel.getSTarget() instanceof SSpan){
						baseFile = spanFileTable.get(rel.getSTarget().getSName());
					} else {
						if (rel.getSTarget() instanceof SToken){
							baseFile = tokenFileTable.get(rel.getSTarget().getSName());
						}else{
							baseFile = "";
						}
					}
					output.println(new StringBuffer("\t\t\t<rel id=\"")
						.append(rel.getSName()).append("\" type=\"")
						.append(rel.getSTypes().get(0)).append("\" xlink:href=\"")
						.append(baseFile).append("#")
						.append(rel.getSTarget().getSName())
						.append("\"/>").toString());
				}
			}
			*/
			output.println("\t\t</struct>");
		}
		
		output.println("\t"+STRUCT_LIST_CLOSE_TAG);
		output.println(PAULA_CLOSE_TAG);
		output.close();
		mapStructAnnotations(layerStructList,documentPath,structFile.getName());
	}

	
	

	/**
	 * Creates Annotations files for all token.
	 * TODO: NEED MORE JAVADOC
	 * @param tokenFileMap
	 * @param layerTokenList
	 * @param documentPath
	 * @param documentID 
	 */
	private void mapTokenAnnotations(Hashtable<String, String> tokenFileMap,
									EList<SToken> layerTokenList, 
									URI documentPath, String documentID) {
		if (tokenFileMap == null)
			throw new PAULAExporterException("There is no token File");
		if (layerTokenList == null)
			throw new PAULAExporterException("The token List is empty");
		if (documentPath == null)
			throw new PAULAExporterException("The documentPath is null");
		
		/**
		 * Create a File Table for annotation writers
		 */
		Hashtable<String,PrintWriter> annoFileTable = new Hashtable<String,PrintWriter>();
		
		/**
		 * Iterate over all tokens
		 */
		for (SToken sToken : layerTokenList){
			
			String base = tokenFileMap.get(sToken.getSName());
			/**
			 * get the base token file name (without .xml)
			 */
			String baseTokenFileName = base.replace(".xml", "");
			
			/**
			 * Iterate over all annotations of this token
			 */
			for (SAnnotation sAnnotation : sToken.getSAnnotations()){
				StringBuffer featTag = new StringBuffer("\t\t")
					.append("<").append(TAG_FEAT_FEAT)
					.append(" ").append(ATT_FEAT_FEAT_HREF)
					.append("=\"#").append(sToken.getSName())
					.append("\" ").append(ATT_FEAT_FEAT_VAL)
					.append("=\"").append(sAnnotation.getSValue())
					.append("\"/>");
				
				String type = sAnnotation.getQName();
				String paulaID = baseTokenFileName+"_"+type;
				/**
				 * Create the token file name (baseName + AnnoName + .xml)
				 */
				String tokenFileName = paulaID +".xml";
				
				/**
				 * Reference one PrintWriter
				 */
				PrintWriter output = annoFileTable.get(tokenFileName);
				
				/**
				 * If Reference is null, we have to create a anno file
				 */
				if (output != null){
					output.println(featTag.toString());
							
				}else{
					File annoFile = new File(documentPath.toFileString() + File.separator+tokenFileName);
					try{
						if (!(annoFile.createNewFile()))
							System.out.println("File: "+ annoFile.getName()+ " already exists");
						
						/**
						 * Write Preamble and Tag
						 */
						output = new PrintWriter(
							  new BufferedWriter(	
									  new OutputStreamWriter(
									  new FileOutputStream(annoFile),"UTF8")),
														false);
						output.write(createFeatFileBeginning(paulaID, type, base, 0));
						
						output.println(featTag.toString());
								
						/**
						 * Put file (Writer) in FileTable for further access 
						 */
						annoFileTable.put(tokenFileName, output);
						
				
					} catch (IOException e) {
						throw new PAULAExporterException("mapTokenAnnotations: Could not write File "+annoFile.getAbsolutePath()+": "+e.getMessage());
					}
				}
			}
		}
		/**
		 * Close all Writers
		 */
		for (PrintWriter output : annoFileTable.values()){
			output.println("\t</"+TAG_FEAT_FEATLIST+">");
			output.println(PAULA_CLOSE_TAG);
			output.close();
		}
		annoFileTable = null;
	}

	
	
	/**
	 * Creates Annotation files for spans.
	 * 
	 * @param layerSpanList a list with all Spans, found in a specific layer
	 * @param documentPath The Document Path 
	 * @param spanPaulaId The filename of the Span file without ".xml"
	 */
	private void mapSpanAnnotations(EList<SSpan> layerSpanList, URI documentPath, String spanPaulaId){
		
		Hashtable<String,PrintWriter> annoFileTable = new Hashtable<String,PrintWriter>();
		File f;
		for (SSpan sSpan : layerSpanList){
			for (SAnnotation sAnnotation : sSpan.getSAnnotations()){
				String type = sAnnotation.getQName();
				String qName = spanPaulaId + "_"+type;
				/**
				 * create the feat tag
				 */
				StringBuffer featTag = new StringBuffer("\t\t")
				.append("<").append(TAG_FEAT_FEAT)
				.append(" ").append(ATT_FEAT_FEAT_HREF)
				.append("=\"#").append(sSpan.getSName())
				.append("\" ").append(ATT_FEAT_FEAT_VAL)
				.append("=\"").append(sAnnotation.getSValue())
				.append("\"/>");
				
				
				/**
				 * reference one PrintWriter from the annotation file Table
				 */
				PrintWriter output = annoFileTable.get(qName);
				
				
				/**
				 * If there is a PrintWriter to an annotation file, then
				 * write the feat tag
				 */
				if (output != null){
					output.println(featTag.toString());
				} else {
					f = new File(documentPath.toFileString() + File.separator+qName+".xml");
					try {
						if (!(f.createNewFile()))
							System.out.println("File: "+ f.getName()+ " already exists");
						
						output = new PrintWriter(
							  new BufferedWriter(	
							  new OutputStreamWriter(
							  new FileOutputStream(f.getAbsoluteFile()),"UTF8")),
												true);
						
						/**
						 * Write the feat file beginning and the first feat tag
						 * to the file
						 */
						output.println(createFeatFileBeginning(qName, type, spanPaulaId+".xml", 0));
						output.println(featTag.toString());
						
						/**
						 * put the PrintWriter into the Hashtable for later access
						 */
						annoFileTable.put(qName, output);
						
					} catch (IOException e) {
						throw new PAULAExporterException("mapSpanAnnotations: Could not write File "+f.getAbsolutePath()+": "+e.getMessage());
					}
				
			
				}
			}
		}
		/**
		 * Write the closing tags, close all streams and
		 * dereference the annotation file Table
		 */
		for (PrintWriter output : annoFileTable.values()){
			output.println("\t</"+TAG_FEAT_FEATLIST+">");
			output.println(PAULA_CLOSE_TAG);
			output.close();
		}
		annoFileTable = null;
	}
	
	
	private void mapStructAnnotations(EList<SStructure> layerStructList,
			URI documentPath, String base) {

		Hashtable<String,PrintWriter> annoFileTable = new Hashtable<String,PrintWriter>();
		File f;
		for (SStructure sSpan : layerStructList){
			for (SAnnotation sAnnotation : sSpan.getSAnnotations()){
				String type = sAnnotation.getQName();
				String qName = base.replace(".xml", "_"+type+".xml") ;
				/**
				 * create the feat tag
				 */
				StringBuffer featTag = new StringBuffer("\t\t")
				.append("<").append(TAG_FEAT_FEAT)
				.append(" ").append(ATT_FEAT_FEAT_HREF)
				.append("=\"#").append(sSpan.getSName())
				.append("\" ").append(ATT_FEAT_FEAT_VAL)
				.append("=\"").append(sAnnotation.getSValue())
				.append("\"/>");
				
				
				/**
				 * reference one PrintWriter from the annotation file Table
				 */
				PrintWriter output = annoFileTable.get(qName);
				
				
				/**
				 * If there is a PrintWriter to an annotation file, then
				 * write the feat tag
				 */
				if (output != null){
					output.println(featTag.toString());
				} else {
					f = new File(documentPath.toFileString() + File.separator+qName);
					try {
						if (!(f.createNewFile()))
							System.out.println("File: "+ f.getName()+ " already exists");
						
						output = new PrintWriter(
							  new BufferedWriter(	
							  new OutputStreamWriter(
							  new FileOutputStream(f.getAbsoluteFile()),"UTF8")),
												true);
						
						/**
						 * Write the feat file beginning and the first feat tag
						 * to the file
						 */
						output.println(createFeatFileBeginning(qName, type, base, 0));
						output.println(featTag.toString());
						
						/**
						 * put the PrintWriter into the Hashtable for later access
						 */
						annoFileTable.put(qName, output);
						
					} catch (IOException e) {
						throw new PAULAExporterException("mapStructAnnotations: Could not write File "+f.getAbsolutePath()+": "+e.getMessage());
					}
				
			
				}
			}
		}
		/**
		 * Write the closing tags, close all streams and
		 * dereference the annotation file Table
		 */
		for (PrintWriter output : annoFileTable.values()){
			output.println("\t</"+TAG_FEAT_FEATLIST+">");
			output.println(PAULA_CLOSE_TAG);
			output.close();
		}
		annoFileTable = null;
		
	}
	
	
	/**
	 * Creates the mark tag , containing the overlapped tokens
	 * TODO: NEED MORE JAVADOC!!!
	 * @param sName
	 * @param overlappedTokenList
	 * @param dataSourceCount Number of textual data sources
	 * @param base 
	 * @param firstDSName 
	 * @return
	 */
	private String createSpanFileMarkTag(String sName, 
										 Hashtable<String, String> dSFileMap, 
										 EList<SToken> overlappedTokenList,
										 int dataSourceCount, 
										 String base, 
										 String firstDSName) {
		
		StringBuffer buffer = new StringBuffer();
		EList<STextualRelation> rel = overlappedTokenList.get(0).getSDocumentGraph().getSTextualRelations();
		String sTextualDSName;
		String tokenFile;
		String tokenPath;
		buffer.append("\t\t<mark ").append(ATT_MARK_MARK_ID).append("=\"").append(sName)
			.append("\" ").append(ATT_MARK_MARK_HREF).append("=\"(");
		if (dataSourceCount == 1){
			for (SToken token : overlappedTokenList){
				if (overlappedTokenList.indexOf(token) < overlappedTokenList.size()-1){
					buffer.append("#").append(token.getSName()).append(",");
				} else {
				buffer.append("#").append(token.getSName());
				}
			}
		} else {
			for (SToken token : overlappedTokenList){
				sTextualDSName = rel.get(rel.indexOf(token)).getSTarget().getSName();
				tokenPath = dSFileMap.get(sTextualDSName);
				tokenFile = tokenPath.substring(tokenPath.lastIndexOf(File.separator+1));
				if (overlappedTokenList.indexOf(token) < overlappedTokenList.size()-1){
					if (sTextualDSName.equals(firstDSName)){
						buffer.append("#").append(token.getSName()).append(",");
					} else {
						buffer.append(tokenFile).append("#").append(token.getSName()).append(",");
					}
				} else {
					if (sTextualDSName.equals(firstDSName)){
						buffer.append("#").append(token.getSName()).append(",");
					} else {
						buffer.append(tokenFile).append("#").append(token.getSName());
					}
				}
			}
		}
		buffer.append(")\"/>");
		return buffer.toString();
	}

	
	
	/**
	 *  METHODS FOR CREATING FILE BEGINNINGS
	 */
	
	/**
	 * TODO:JAVADOC
	 * @param paulaID
	 * @param type
	 * @return
	 */
	private String createStructFileBeginning(String paulaID,String type) {
		StringBuffer buffer = new StringBuffer(TAG_HEADER_XML);
		buffer.append(LINE_SEPARATOR).append(PAULA_STRUCT_DOCTYPE_TAG)
			  .append(LINE_SEPARATOR).append(TAG_PAULA_OPEN)
			  .append(LINE_SEPARATOR).append("\t")
			  .append("<"+TAG_HEADER+" "+ATT_HEADER_PAULA_ID[0]+"=\""+paulaID+"\"/>")
			  .append(LINE_SEPARATOR).append("\t")
			  .append("<"+TAG_STRUCT_STRUCTLIST+" xmlns:xlink=\"http://www.w3.org/1999/xlink\" "+
					  ATT_MARK_MARKLIST_TYPE+"=\""+type+"\">")
			  .append(LINE_SEPARATOR);
		return buffer.toString();
	}

	/**
	 * Method for construction of the Mark file preamble (Headers and MarkList Tag)
	 * 
	 * @param paulaID
	 * @param type 
	 * @param base base token file (the first if there is more then one)
	 * @param dsNum Count of textual datasources
	 * @return String representation of the Preamble
	 */
	private String createMarkFileBeginning(String paulaID,String type, String base, int dsNum) {
		
		StringBuffer buffer = new StringBuffer(TAG_HEADER_XML);
		buffer.append(LINE_SEPARATOR).append(PAULA_MARK_DOCTYPE_TAG)
			  .append(LINE_SEPARATOR).append(TAG_PAULA_OPEN)
			  .append(LINE_SEPARATOR).append("\t")
			  .append("<"+TAG_HEADER+" "+ATT_HEADER_PAULA_ID[0]+"=\""+paulaID+"\"/>")
			  .append(LINE_SEPARATOR).append("\t")
			  .append("<"+TAG_MARK_MARKLIST+" xmlns:xlink=\"http://www.w3.org/1999/xlink\" "+
					  ATT_MARK_MARKLIST_TYPE+"=\""+type+"\" "+ATT_MARK_MARKLIST_BASE+"=\""+base+"\" >")
			  .append(LINE_SEPARATOR);
		return buffer.toString();
	}

	/**
	 * TODO: JAVADOC
	 * @param paulaID
	 * @param type
	 * @param base
	 * @param dsNum
	 * @return
	 */
	private String createFeatFileBeginning(String paulaID,String type, String base, int dsNum){
		StringBuffer buffer = new StringBuffer(TAG_HEADER_XML);
		buffer.append(LINE_SEPARATOR).append(PAULA_FEAT_DOCTYPE_TAG)
			  .append(LINE_SEPARATOR).append(TAG_PAULA_OPEN)
			  .append(LINE_SEPARATOR).append("\t")
			  .append("<"+TAG_HEADER+" "+ATT_HEADER_PAULA_ID[0]+"=\""+paulaID+"\"/>")
			  .append(LINE_SEPARATOR).append("\t")
			  .append("<"+TAG_FEAT_FEATLIST+" xmlns:xlink=\"http://www.w3.org/1999/xlink\" "+
					  ATT_FEAT_FEATLIST_TYPE+"=\""+type+"\" "+ATT_FEAT_FEATLIST_BASE+"=\""+base+"\" >")
			  .append(LINE_SEPARATOR);
		return buffer.toString();
	}
	
	
	/**
	 * Checks whether the file is valid by the DTD which is noted in the file
	 * 
	 * @param fileToValidate
	 * @return true if the fileToValidate matches the specified DTD
	 * 			false else
	 */
	private boolean isValidXML(File fileToValidate) {
				
		try {
	      	 File XMLFile = fileToValidate;
	      	 
	         DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
	         documentBuilderFactory.setValidating(true); 
	         DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
	         ErrorHandler errorHandler = new DefaultHandler();
	         documentBuilder.setErrorHandler(errorHandler);
	         Document document = documentBuilder.parse(XMLFile);
	         
		  } catch (ParserConfigurationException e) {
	         System.out.println(e.getMessage()); 
	         return false;
	      } catch (SAXException e) {
	         System.out.println(e.getMessage());
	         return false;
	      } catch (IOException e) {
	         System.out.println(e.getMessage());
	      }
	      return true;	 

		
	}
	
	  
}	
	

