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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

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
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SPointingRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SSpan;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SStructure;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SStructuredNode;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SElementId;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SLayer;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SMetaAnnotation;
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
	
	private static boolean validate = true;
	
	
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
		Hashtable <String,String> nodeFileMap = mapLayers(sDocument.getSDocumentGraph(), documentPath, documentName, dataSourceFileTable,oneDS);
		System.out.println("Node file map contains "+ nodeFileMap.size()+" nodes");
		
		
	}

	

	

	/**
	 * Writes the primary text sText to a file "documentID_text.xml" in the documentPath
	 * and returns the URI (filename).
	 * @param sTextualDS the primary text
	 * @param documentID the document id
	 * @param documentPath the document path
	 * @return Hashtable&lt;STextualDS SName,TextFileName&gt;
	 */
	private Hashtable<String,String> mapTextualDataSources( 
									  EList<STextualDS> sTextualDS, 
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
								File.separator +"merged."+ documentID+ ".text.xml");
			} else {
				textFile = new File(
								documentPath.toFileString()+ 
								File.separator +"merged."+ documentID +".text."+
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
					if (dsNum == 1){
						output.println( new StringBuffer("\t<header paula_id=\"merged.")
									.append(documentID)
									.append(".text\" type=\"text\"/>").toString());
					} else {
						output.println( new StringBuffer("\t<header paula_id=\"merged.")
							.append(documentID)
							.append(".text"+(sTextualDS.indexOf(sText)+1)+"\" type=\"text\"/>").toString());
					}
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
		if (validate){
			for (String fileName : sTextualDSFileTable.values()){
				System.out.println("XML-Validation: "+fileName+ " is valid: "+ 
						isValidXML(new File(documentPath.toFileString()+File.separator+fileName)));
			}
		}
		return sTextualDSFileTable;
		
	}
	
	/**
	 * 
	 * Map the layers of the document graph including token, spans and structs to files.
	 * @param sDocumentGraph
	 * @param documentPath
	 * @param documentId
	 * @param fileTable the data source file table
	 * @param oneDS the first data source
	 * @return 
	 */
	private Hashtable<String, String> mapLayers(
			               SDocumentGraph sDocumentGraph,
						   URI documentPath,
						   String documentId,
						   Hashtable<String, String> fileTable, 
						   String firstDSName){
		
		if (sDocumentGraph == null)
			throw new PAULAExporterException("Cannot map Layers because document graph is null");
		if (documentPath.equals(""))
			throw new PAULAExporterException("Cannot map Layers because documentPath is empty (\"\")");
		if (documentId.equals(""))
			throw new PAULAExporterException("Cannot map Layers because documentID is empty (\"\")");
		if (fileTable == null)
			throw new PAULAExporterException("Cannot map Layers because fileTable is null");
		if (firstDSName.equals(""))
			throw new PAULAExporterException("Cannot map Layers because no first Data source name is specified");
		
		
		SDocumentStructureAccessor accessor = new SDocumentStructureAccessor();
		accessor.setSDocumentGraph(sDocumentGraph);
		
		/**
		 * Copy the spans and structs.
		 * By doing this, we can assure later that we found all spans/structs
		 */
		EList<SSpan> spanList = new BasicEList<SSpan>(sDocumentGraph.getSSpans());
		EList<SStructure> structList = new BasicEList<SStructure>(sDocumentGraph.getSStructures());
		
		/**
		 * Hashtables containing tok/span/struct names and the including filename
		 */
		Hashtable<String,String> nodeFileMap = new Hashtable<String, String>();
		Set<String> layerNodeFileNames = Collections.synchronizedSet(new HashSet<String>());
		
		/**
		 * Lists for all constructs we may find in one layer 
		 */
		EList<SSpan> layerSpanList ;
		EList<SStructure> layerStructList ;
		EList<SToken> layerTokenList ;
		EList<SPointingRelation> layerPointingRelationList;
		/**
		 * Port lists for later paula versions allowing to handle
		 * structured elements belonging to multiple layers
		 */
		EList<SSpan> multiLayerSpanList = new BasicEList<SSpan>();
		EList<SStructure> multiLayerStructList = new BasicEList<SStructure>();
		EList<SToken> multiLayerTokenList = new BasicEList<SToken>();
		
		File annoSetFile = new File(documentPath.toFileString()+File.separator+"merged."+documentId+".anno.xml");
		File annoFeatFile = new File(documentPath.toFileString()+File.separator+"merged."+documentId+".anno_feat.xml");
		PrintWriter annoSetOutput = null;
		PrintWriter annoFeatOutput = null;
		try{
			if (! annoSetFile.createNewFile())
				System.out.println("File: "+ annoSetFile.getName()+ " already exists");
			
			if (! annoFeatFile.createNewFile())
				System.out.println("File: "+ annoFeatFile.getName()+ " already exists");
			
			
			annoSetOutput = new PrintWriter(new BufferedWriter(	
					new OutputStreamWriter(new FileOutputStream(annoSetFile),"UTF8")),
							true);
			annoFeatOutput = new PrintWriter(new BufferedWriter(	
					new OutputStreamWriter(new FileOutputStream(annoFeatFile),"UTF8")),
							true);
		}catch(IOException e){
			
		}
		
		annoSetOutput.write(createStructFileBeginning("merged."+documentId+".anno", "annoSet"));
		annoSetOutput.println(new StringBuffer().append("\t\t<")
				.append(TAG_STRUCT_STRUCT).append(" ").append(ATT_STRUCT_STRUCT_ID)
				.append("=\"").append(documentId).append("\">").toString());
		int i = 1;
		for (String textFile : fileTable.values()){
			annoSetOutput.println(new StringBuffer().append("\t\t\t<")
					.append(TAG_STRUCT_REL).append(" ").append(ATT_STRUCT_REL_ID)
					.append("=\"").append("rel_"+i).append("\" ").append(ATT_STRUCT_REL_HREF)
					.append("=\"").append(textFile).append("\" />").toString());
			i++;
		}
		annoSetOutput.println("\t\t</"+TAG_STRUCT_STRUCT+">");
		
		annoFeatOutput.write(createFeatFileBeginning("merged."+documentId+".anno_feat", "annoFeat", annoSetFile.getName()));
		int j = 1;
		annoFeatOutput.println(new StringBuffer().append("\t\t<").append(TAG_FEAT_FEAT)
							.append(" ").append(ATT_FEAT_FEAT_HREF).append("=\"#")
							.append("anno_").append(j).append("\" ").append(ATT_FEAT_FEAT_VAL)
							.append("=\"").append(documentId).append("\" />").toString());
		
		j++;
		
		/**
		 * iterate over all layers
		 */
		for (SLayer layer : sDocumentGraph.getSLayers()){
			layerSpanList = new BasicEList<SSpan>();
			layerStructList = new BasicEList<SStructure>();
			layerTokenList = new BasicEList<SToken>();
			layerPointingRelationList = new BasicEList<SPointingRelation>();
			
			layerNodeFileNames.clear();
			
			annoSetOutput.println(new StringBuffer().append("\t\t<")
					.append(TAG_STRUCT_STRUCT).append(" ").append(ATT_STRUCT_STRUCT_ID)
					.append("=\"").append(layer.getSName()).append("\">").toString());
			
			annoFeatOutput.println(new StringBuffer().append("\t\t<").append(TAG_FEAT_FEAT)
					.append(" ").append(ATT_FEAT_FEAT_HREF).append("=\"#")
					.append("anno_").append(j).append("\" ").append(ATT_FEAT_FEAT_VAL)
					.append("=\"").append(layer.getSName()).append("\" />").toString());

			j++;

			
			
			/**
			 * iterate over all nodes.
			 * put the nodes in the right lists, according to their type
			 */
			for (SNode sNode : layer.getSNodes()){
				/**
				 * fetch Pointing Relations for this layer
				 */
				for (Edge edge : sDocumentGraph.getOutEdges(sNode.getSId())){
					if (edge instanceof SPointingRelation)
						layerPointingRelationList.add((SPointingRelation)edge);
				}
				
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
				mapTokens(sDocumentGraph.getSTextualRelations(),layerTokenList,fileTable,documentId,documentPath,layer.getSName(), nodeFileMap,layerNodeFileNames);
			}
			
			if (! layerSpanList.isEmpty()){
				mapSpans(sDocumentGraph, layerSpanList,nodeFileMap,fileTable,documentId,documentPath, layer.getSName(),layerNodeFileNames, firstDSName);
			}
			if (! layerStructList.isEmpty()){
				mapStructs(layerStructList,nodeFileMap,layer.getSName(),documentId, documentPath,layerNodeFileNames);
			}
			// Map pointing relations between nodes
			if (! layerPointingRelationList.isEmpty()){
				mapPointingRelations(sDocumentGraph,documentPath,documentId,layer.getSName(), nodeFileMap, layerPointingRelationList,layerNodeFileNames);
			}
			
			for (String nodeFile : layerNodeFileNames){
				annoSetOutput.println(new StringBuffer().append("\t\t\t<")
						.append(TAG_STRUCT_REL).append(" ").append(ATT_STRUCT_REL_ID)
						.append("=\"").append("rel_"+i).append("\" ").append(ATT_STRUCT_REL_HREF)
						.append("=\"").append(nodeFile).append("\" />").toString());
				i++;
				if (validate){
						System.out.println("XML-Validation: "+nodeFile+ " is valid: "+ 
								isValidXML(new File(documentPath.toFileString()+File.separator+nodeFile)));
					
				}
			}
						
			annoSetOutput.println("\t\t</"+TAG_STRUCT_STRUCT+">");
		}
		/**
		 * If we did not find all spans/structs in the layers we take the remaining
		 * spans/structs and map them in extra files
		 */
		
		layerNodeFileNames = Collections.synchronizedSet(new HashSet<String>());
		
		boolean nolayerNodesExist = false;
		
		if (! spanList.isEmpty()){
			nolayerNodesExist = true;
			System.out.println("There are Spans which are not in one Layer. Mapping into nolayer span file");
			mapSpans(sDocumentGraph, spanList,nodeFileMap,fileTable,documentId,documentPath, "nolayer", layerNodeFileNames ,firstDSName);
		}
		if (! structList.isEmpty()){
			nolayerNodesExist = true;
			System.out.println("There are Structs which are not in one Layer. Mapping into nolayer struct file.");
			mapStructs(structList,nodeFileMap,"nolayer",documentId, documentPath, layerNodeFileNames);

		}
		
		if (nolayerNodesExist){
			annoSetOutput.println(new StringBuffer().append("\t\t<")
				.append(TAG_STRUCT_STRUCT).append(" ").append(ATT_STRUCT_STRUCT_ID)
				.append("=\"").append("nolayer").append("\">").toString());
		
			for (String nodeFile : layerNodeFileNames){
				annoSetOutput.println(new StringBuffer().append("\t\t<")
					.append(TAG_STRUCT_REL).append(" ").append(ATT_STRUCT_REL_ID)
					.append("=\"").append("rel_"+i).append("\" ").append(ATT_STRUCT_REL_HREF)
					.append("=\"").append(nodeFile).append("\"")
					.append("\" />").toString());
				i++;
				if (validate){
					System.out.println("XML-Validation: "+nodeFile+ " is valid: "+ 
							isValidXML(new File(documentPath.toFileString()+File.separator+nodeFile)));
				
			}
			}
					
			annoSetOutput.println("\t\t</"+TAG_STRUCT_STRUCT+">");
			
			annoFeatOutput.println(new StringBuffer().append("\t\t<").append(TAG_FEAT_FEAT)
					.append(" ").append(ATT_FEAT_FEAT_HREF).append("=\"#")
					.append("anno_").append(j).append("\" ").append(ATT_FEAT_FEAT_VAL)
					.append("=\"").append("nolayer").append("\" />").toString());

			j++;

		}
		if (validate){
			System.out.println("XML-Validation: "+annoSetFile.getName()+ " is valid: "+ 
					isValidXML(new File(documentPath.toFileString()+File.separator+annoSetFile.getName())));
			System.out.println("XML-Validation: "+annoFeatFile.getName()+ " is valid: "+ 
					isValidXML(new File(documentPath.toFileString()+File.separator+annoFeatFile.getName())));
		
		}
		
		annoSetOutput.println("\t</"+TAG_STRUCT_STRUCTLIST+">");
		annoSetOutput.println(PAULA_CLOSE_TAG);
		annoSetOutput.close();
		annoFeatOutput.println("\t</"+TAG_FEAT_FEATLIST+">");
		annoFeatOutput.println(PAULA_CLOSE_TAG);
		annoFeatOutput.close();
		mapMetaAnnotations(sDocumentGraph,documentPath,documentId);
		return nodeFileMap;
		
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
	 * @param nodeFileMap 
	 * @param layerNodeFileNames 
	 * @return 
	 * @return Hashtable of the form (TokenName,TokenFile)
	 */
	private void mapTokens( 
					EList<STextualRelation> sTextRels,
					EList<SToken> layerTokenList, 
					Hashtable<String, String> fileTable, 
					String documentID,  
					URI documentPath, 
					String layer, 
					Hashtable<String, String> nodeFileMap, 
					Set<String> layerNodeFileNames) {
		
		if (sTextRels.isEmpty())
			throw new PAULAExporterException("Cannot create token files because there are no textual relations");
		if (layerTokenList == null)
			throw new PAULAExporterException("Cannot create token files because there are no tokens in this layer");
		if (fileTable == null)
			throw new PAULAExporterException("Cannot create token files because no textFileTable is defined" );
		if (documentID.equals(""))
			throw new PAULAExporterException("Cannot create token files because documentID is empty (\"\")");
		if (documentPath == null)
			throw new PAULAExporterException("Cannot create token files because documentPath is null");
		if (layer.equals(""))
			throw new PAULAExporterException("Cannot create token files because no layer was specified");
		if (nodeFileMap == null)
			throw new PAULAExporterException("Cannot create token files because there is no node file map to save the filenames to");
		if (layerNodeFileNames == null)
			throw new PAULAExporterException("Cannot create token files because there is no Set to save the token file names to");
		
		/**
		 * Create one Hashmap for returning and
		 * one Hashmap for the PrintWriter 
		 */
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
					tokenFileName = new String(documentPath.toFileString()+File.separator+layer+"."+documentID+".tok"+tokenFileIndex+".xml");
				}else{
					tokenFileName = new String(documentPath.toFileString()+File.separator+layer+"."+documentID+".tok.xml");
				}
				baseTextFile = new String(fileTable.get(sTextDSSid));
				tokenFile = new File(tokenFileName);
				try {
					if ( ! tokenFile.createNewFile())
						System.out.println("File: "+ tokenFile.getName()+ " already exists");
					layerNodeFileNames.add(tokenFile.getName());
					output = new PrintWriter(new BufferedWriter(	
							new OutputStreamWriter(
									new FileOutputStream(tokenFile),
									"UTF8")),
									true);
					/**
					 * Write preamble and the first mark tag to file
					 */
					if (fileTable.size()>1){
						output.write(createMarkFileBeginning(layer+"."+documentID+"."+tokenFileIndex+".tok",
							"tok", 
							baseTextFile.replace(tokenFile.getPath(),"")));
					}else{
						output.write(createMarkFileBeginning(layer+"."+documentID+".tok",
								"tok", 
								baseTextFile.replace(tokenFile.getPath(),"")));
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
			nodeFileMap.put(sTextualRelation.getSToken().getSName(), tokenFile.getName());
			
			
		}
		/**
		 * Close all token file streams
		 */
		for (PrintWriter writer :  (tokenWriteMap.values())){
			writer.write(PAULA_TOKEN_FILE_CLOSING);
			writer.close();
			//System.out.println("Wrote token File");
		}
		/**
		 * dispose all Writers since we are finished with the tokens
		 * map token annotations
		 * return the token file map
		 */
		tokenWriteMap = null;
		mapTokenAnnotations(nodeFileMap,layerTokenList,documentPath,documentID,layerNodeFileNames);
		
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
	 * @param layerNodeFileNames 
	 * @param firstDSName
	 * @param spanFileMap 
	 * @return 
	 */
	private void mapSpans(
				SDocumentGraph graph,
				EList<SSpan> layerSpanList, 
				Hashtable<String, String> nodeFileMap, 
				Hashtable<String, String> dSFileTable, 
				String documentId, 
				URI documentPath, 
				String layer , 
				Set<String> layerNodeFileNames, 
				String firstDSName){
		
		if (graph == null)
			throw new PAULAExporterException("Cannot map span files because document graph is null");
		if (layerSpanList == null)
			throw new PAULAExporterException("Cannot map span files because layerSpanList is null");
		if (nodeFileMap == null)
			throw new PAULAExporterException("Cannot map span files because token File Table is null");
		if (dSFileTable == null)
			throw new PAULAExporterException("Cannot map span files because there is no data source file table");
		if (documentId.equals(""))
			throw new PAULAExporterException("Cannot map span files because documentID is empty (\"\")");
		if (documentPath == null)
			throw new PAULAExporterException("Cannot map span because documentPath is not specified");
		if (layer.equals(""))
			throw new PAULAExporterException("Cannot map span files because layer name is empty (\"\")");
		if (layerNodeFileNames == null)
			throw new PAULAExporterException("Cannot map span files because there is no set to save the file names to");
		if (firstDSName.equals(""))
			throw new PAULAExporterException("Cannot map span files because first DS Name is empty (\"\")");
		
		
		SDocumentStructureAccessor accessor = new SDocumentStructureAccessor();
		accessor.setSDocumentGraph(graph);
		String paulaID;
		
		
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
		String spanFileToWrite = layer+"."+documentId +".mark.xml";
		PrintWriter output = null;
		/**
		 * Create span File
		 */
		File spanFile = new File(documentPath.toFileString() + File.separator + spanFileToWrite);
		try {
			if (!(spanFile.createNewFile()))
				System.out.println("File: "+ spanFile.getName()+ " already exists");
			
			layerNodeFileNames.add(spanFile.getName());
			
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
					baseMarkFile));
		
		} catch (IOException e) {
			throw new PAULAExporterException("mapSpans: Could not write File "+spanFileToWrite.toString()+": "+e.getMessage());
		}
			
		for (SSpan sSpan : layerSpanList){
			/**
			 * get tokens which are overlapped by this Span
			 */
			overlappingTokens = accessor.getSTextualOverlappedTokens((SStructuredNode) sSpan);
			nodeFileMap.put(sSpan.getSName(), spanFileToWrite);	
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
						firstDSName)
						);
				
		}
		output.write("\t"+MARK_LIST_CLOSE_TAG+LINE_SEPARATOR+PAULA_CLOSE_TAG);
		output.close();
		mapSpanAnnotations(layerSpanList,documentPath,paulaID,layerNodeFileNames);
		
	}
	
	/**
	 * Maps all Structs to paula-format.
	 * Maps also Dominance relations and the dominance relation annotations to paula-format.
	 * TODO:  NEED MORE JAVADOC!!!!
	 * @param layerStructList
	 * @param fileTable
	 * @param spanFileMap 
	 * @param layer
	 * @param documentId
	 * @param documentPath
	 * @param layerNodeFileNames 
	 * @return 
	 */
	private void mapStructs(
			EList<SStructure> layerStructList, 
			Hashtable<String, String> nodeFileMap,
			String layer, 
			String documentId, 
			URI documentPath, 
			Set<String> layerNodeFileNames) {
		
		if (layerStructList == null)
			throw new PAULAExporterException("Cannot map struct files because layerSpanList is null");
		if (nodeFileMap == null)
			throw new PAULAExporterException("Cannot map struct files because node file map is null");
		if (layer.equals(""))
			throw new PAULAExporterException("Cannot map struct files because layer name is empty (\"\")");
		if (documentId.equals(""))
			throw new PAULAExporterException("Cannot map struct files because documentID is empty (\"\")");
		if (documentPath.equals(""))
			throw new PAULAExporterException("Cannot map struct because documentPath is empty (\"\")");
		if (layerNodeFileNames == null)
			throw new PAULAExporterException("Cannot map struct files because there is no set to save the file names to");
		
		
		
		Hashtable<String,PrintWriter> domRelAnnotationWriterTable = new Hashtable<String,PrintWriter>();
		Hashtable<String,String> structFileMap = new Hashtable<String,String>();
		
		SDocumentStructureAccessor accessor = new SDocumentStructureAccessor();
		accessor.setSDocumentGraph(layerStructList.get(0).getSDocumentGraph());
		
		String paulaID = layer+"."+documentId+".struct";
		
		File structFile = new File(documentPath.toFileString()+File.separator+paulaID+".xml");
		PrintWriter output = null;
		try {
			if (!(structFile.createNewFile()))
				System.out.println("File: "+ structFile.getName()+ " already exists");
		
			layerNodeFileNames.add(structFile.getName());

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
		//EList<SDominanceRelation> domRels = layerStructList.get(0).getSDocumentGraph().getSDominanceRelations();
		
		for (SStructure struct : layerStructList){
			output.println(new StringBuffer("struct id=\"").insert(0, "\t\t<")
					.append(struct.getSName()).append("\">").toString());
			/**
			 * Save the struct name in the struct file map
			 */
			structFileMap.put(struct.getSName(), structFile.getName());
			
			for (Edge edge : struct.getSDocumentGraph().getOutEdges(((SNode)struct).getSId())){
				String baseFile;
				if (edge instanceof SDominanceRelation){
					
					if (((SDominanceRelation)edge).getSTarget() instanceof SSpan){
						baseFile = nodeFileMap.get(((SDominanceRelation) edge).getSTarget().getSName());
					} else {
						if (((SDominanceRelation)edge).getSTarget() instanceof SToken){
							baseFile = nodeFileMap.get(((SDominanceRelation)edge).getSTarget().getSName());
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
				
				
					/**
					 * Map dominance relation Annotations
					 */
					for (SAnnotation sAnnotation: ((SDominanceRelation)edge).getSAnnotations()){
						String annoType = sAnnotation.getQName();
						String annoPaulaId = paulaID+"_"+annoType;
						String domRelAnnoFileName = annoPaulaId+".xml";
						
						StringBuffer featTag = new StringBuffer("\t\t")
						.append("<").append(TAG_FEAT_FEAT)
						.append(" ").append(ATT_FEAT_FEAT_HREF)
						.append("=\"#").append(((SDominanceRelation)edge).getSName())
						.append("\" ").append(ATT_FEAT_FEAT_VAL)
						.append("=\"").append(sAnnotation.getSValue())
						.append("\"/>");
						
						PrintWriter annoOutput = domRelAnnotationWriterTable.get(domRelAnnoFileName);
						
						if (annoOutput == null){
							File domRelAnnoFile = 
								new File(documentPath.toFileString()+File.separator+domRelAnnoFileName);
							try{
								if (!(domRelAnnoFile.createNewFile()))
									System.out.println("File: "+ domRelAnnoFile.getName()+ " already exists");
								
								layerNodeFileNames.add(domRelAnnoFile.getName());
								
								annoOutput = new PrintWriter(
										new BufferedWriter(	
												new OutputStreamWriter(
														new FileOutputStream(domRelAnnoFile.getAbsoluteFile())
																,"UTF8")),
																false);
								
							} catch (IOException e) {
								throw new PAULAExporterException("mapStructs: Could not write File "+domRelAnnoFile.getName()+": "+e.getMessage());
							}
							annoOutput.write(createFeatFileBeginning(annoPaulaId, annoType, structFile.getName()));
							
							annoOutput.println(featTag);
							domRelAnnotationWriterTable.put(domRelAnnoFile.getName(), annoOutput);
						} else {
							annoOutput.println(featTag);
						}
						
					}
				}
			}
			
			
			output.println("\t\t</struct>");
		}
		
		for (PrintWriter annoOutput : domRelAnnotationWriterTable.values()){
			annoOutput.println("\t</"+TAG_FEAT_FEATLIST+">");
			annoOutput.println(PAULA_CLOSE_TAG);
			annoOutput.close();
		}
		domRelAnnotationWriterTable = null;
		
		output.println("\t"+STRUCT_LIST_CLOSE_TAG);
		output.println(PAULA_CLOSE_TAG);
		output.close();
		mapStructAnnotations(layerStructList,documentPath,structFile.getName(),layerNodeFileNames);
		
	}

	
	/**
	 * TODO: DOCUMENTATION!
	 * @param sDocumentGraph
	 * @param documentPath
	 * @param documentId
	 * @param layer
	 * @param nodeFileMap
	 * @param layerPointingRelationList
	 * @param layerNodeFileNames 
	 */
	private void mapPointingRelations(
			SDocumentGraph sDocumentGraph,
			URI documentPath, 
			String documentId, 
			String layer, 
			Hashtable<String, String> nodeFileMap, 
			EList<SPointingRelation> layerPointingRelationList, 
			Set<String> layerNodeFileNames) {
		
		if (sDocumentGraph == null)
			throw new PAULAExporterException("Cannot map pointing relations because document graph is null");
		if (documentPath.equals(""))
			throw new PAULAExporterException("Cannot map pointing relations because documentPath is empty (\"\")");
		if (documentId.equals(""))
			throw new PAULAExporterException("Cannot map pointing relations because documentID is empty (\"\")");
		if (layer.equals(""))
			throw new PAULAExporterException("Cannot map pointing relation files because layer name is empty (\"\")");
		if (nodeFileMap == null)
			throw new PAULAExporterException("Cannot map pointing relation files because node file map is null");
		if (layerPointingRelationList == null)
			throw new PAULAExporterException("Cannot map pointing relation files because there are no pointing relations in this layer");
		if (layerNodeFileNames == null)
			throw new PAULAExporterException("Cannot map pointing relation files because there is no set to save the file names to");
		
		
		
		Hashtable<String,PrintWriter> relWriterTable = new Hashtable<String,PrintWriter>();
		Hashtable<String,String> relFileTable = new Hashtable<String,String>();
			
		for (SPointingRelation pointRel : layerPointingRelationList){
			
			String type = pointRel.getSTypes().get(0);
			String paulaID = layer+"."+documentId+".pointRel"+"_"+type;
			
			File pointingRelFile = new File(documentPath.toFileString()+File.separator+paulaID+".xml");
			
			PrintWriter output = relWriterTable.get(pointingRelFile.getName());
			
			relFileTable.put(pointRel.getSName(), pointingRelFile.getName());
			
			String relTag = new StringBuffer("\t\t<")
					.append(TAG_REL_REL).append(" ").append(ATT_REL_REL_ID)
					.append("=\"").append(pointRel.getSName()).append("\" ")
					.append(ATT_REL_REL_HREF).append("=\"")
					.append(nodeFileMap.get(pointRel.getSSource().getSName()))
					.append("#")
					.append(pointRel.getSSource().getSName()).append("\" ")
					.append(ATT_REL_REL_TARGET).append("=\"")
					.append(nodeFileMap.get(pointRel.getSTarget().getSName()))
					.append("#").append(pointRel.getSTarget().getSName())
					.append("\"/>").toString();
			
			if (output == null){
				try{
					if ( ! pointingRelFile.createNewFile())
						System.out.println("File: "+ pointingRelFile.getName()+ " already exists");
			
					layerNodeFileNames.add(pointingRelFile.getName());
						
					output = new PrintWriter(
							new BufferedWriter(	
									new OutputStreamWriter(
										new FileOutputStream(pointingRelFile.getAbsoluteFile())
												,"UTF8")),
												false);
				} catch (IOException e) {
					throw new PAULAExporterException("mapPointingRelations: Could not write File "+pointingRelFile.getName()+": "+e.getMessage());
				}
				output.write(createRelFileBeginning(paulaID, type));
				output.println(relTag);
				relWriterTable.put(pointingRelFile.getName(), output);
			}else{
				output.println(relTag);
			}
			
		}		
		
		for (PrintWriter output : relWriterTable.values()){
			output.println("\t</"+TAG_REL_RELLIST+">");
			output.println(PAULA_CLOSE_TAG);
			output.close();
		}
		mapPointingRelationAnnotations(documentPath, layerPointingRelationList,relFileTable,layerNodeFileNames);
	}
	

	/**
	 * Creates Annotations files for all token.
	 * TODO: NEED MORE JAVADOC
	 * @param tokenFileMap
	 * @param layerTokenList
	 * @param documentPath
	 * @param documentID 
	 * @param layerNodeFileNames 
	 */
	private void mapTokenAnnotations(
			Hashtable<String, String> tokenFileMap,
			EList<SToken> layerTokenList, 
			URI documentPath, 
			String documentID, 
			Set<String> layerNodeFileNames) {
		
		if (tokenFileMap == null)
			throw new PAULAExporterException("Cannot map token annotations: There is no token File");
		if (layerTokenList == null)
			throw new PAULAExporterException("Cannot map token annotations: The token List is empty for this layer");
		if (documentPath == null)
			throw new PAULAExporterException("Cannot map token annotations: The documentPath is null");
		if (documentID.equals(""))
			throw new PAULAExporterException("Cannot map token annotations: The documentID is not specified");
		if (layerNodeFileNames == null)
			throw new PAULAExporterException("Cannot map token annotations: There is no Set to save the filenames to");
		
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
				
				/*
				 * mask special signs:
				 * < -> &lt;
 					> -> &gt;
 					& -> &amp;
 					" -> &quot;
 					' -> &apos;
				 */
				
				String annoString = sAnnotation.getSValue().toString();
				if (annoString.equals("\""))
					annoString = "&quot;";
				if (annoString.equals("<"))
					annoString = "&lt;";
				if (annoString.equals(">"))
					annoString = "&gt;";
				if (annoString.equals("&"))
					annoString = "&amp;";
				if (annoString.equals("'"))
					annoString = "&apos;";
				
				StringBuffer featTag = new StringBuffer("\t\t")
					.append("<").append(TAG_FEAT_FEAT)
					.append(" ").append(ATT_FEAT_FEAT_HREF)
					.append("=\"#").append(sToken.getSName())
					.append("\" ").append(ATT_FEAT_FEAT_VAL)
					.append("=\"").append(annoString)
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
						
						layerNodeFileNames.add(annoFile.getName());
						/**
						 * Write Preamble and Tag
						 */
						output = new PrintWriter(
							  new BufferedWriter(	
									  new OutputStreamWriter(
									  new FileOutputStream(annoFile),"UTF8")),
														false);
						output.write(createFeatFileBeginning(paulaID, type, base));
						
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
	 * @param baseSpanFile The filename of the Span file without ".xml"
	 * @param layerNodeFileNames 
	 */
	private void mapSpanAnnotations(
			EList<SSpan> layerSpanList, 
			URI documentPath, 
			String baseSpanFile, 
			Set<String> layerNodeFileNames){
		
		if (layerSpanList == null)
			throw new PAULAExporterException("Cannot map span annotations: There are no spans in this layer");
		if (documentPath == null)
			throw new PAULAExporterException("Cannot map span annotations: No document path was specified");
		if (baseSpanFile.equals(""))
			throw new PAULAExporterException("Cannot map span annotations: No base span file paula id was specified");
		if (layerNodeFileNames == null)
			throw new PAULAExporterException("Cannot map span annotations: There is no Set fo save the file names to");
		
		Hashtable<String,PrintWriter> annoFileTable = new Hashtable<String,PrintWriter>();
		File annoFile;
		for (SSpan sSpan : layerSpanList){
			for (SAnnotation sAnnotation : sSpan.getSAnnotations()){
				String type = sAnnotation.getQName();
				String qName = baseSpanFile + "_"+type;
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
					annoFile = new File(documentPath.toFileString() + File.separator+qName+".xml");
					try {
						if (!(annoFile.createNewFile()))
							System.out.println("File: "+ annoFile.getName()+ " already exists");
						
						layerNodeFileNames.add(annoFile.getName());
						
						output = new PrintWriter(
							  new BufferedWriter(	
							  new OutputStreamWriter(
							  new FileOutputStream(annoFile.getAbsoluteFile()),"UTF8")),
												true);
						
						/**
						 * Write the feat file beginning and the first feat tag
						 * to the file
						 */
						output.println(createFeatFileBeginning(qName, type, baseSpanFile+".xml"));
						output.println(featTag.toString());
						
						/**
						 * put the PrintWriter into the Hashtable for later access
						 */
						annoFileTable.put(qName, output);
						
					} catch (IOException e) {
						throw new PAULAExporterException("mapSpanAnnotations: Could not write File "+annoFile.getAbsolutePath()+": "+e.getMessage());
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
	 * TODO: DOCUMENTATION!
	 * @param layerStructList
	 * @param documentPath
	 * @param baseStructFile
	 * @param layerNodeFileNames 
	 */
	private void mapStructAnnotations(
			EList<SStructure> layerStructList,
			URI documentPath, 
			String baseStructFile, 
			Set<String> layerNodeFileNames) {

		if (layerStructList == null)
			throw new PAULAExporterException("Cannot map struct annotations: There are no spans in this layer");
		if (documentPath == null)
			throw new PAULAExporterException("Cannot map struct annotations: No document path was specified");
		if (baseStructFile.equals(""))
			throw new PAULAExporterException("Cannot map struct annotations: No base span file paula id was specified");
		if (layerNodeFileNames == null)
			throw new PAULAExporterException("Cannot map struct annotations: There is no Set fo save the file names to");
		
		
		Hashtable<String,PrintWriter> annoFileTable = new Hashtable<String,PrintWriter>();
		File f;
		for (SStructure sSpan : layerStructList){
			for (SAnnotation sAnnotation : sSpan.getSAnnotations()){
				String type = sAnnotation.getQName();
				String qName = baseStructFile.replace(".xml", "_"+type+".xml") ;
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
						
						layerNodeFileNames.add(f.getName());
							
						output = new PrintWriter(
							  new BufferedWriter(	
							  new OutputStreamWriter(
							  new FileOutputStream(f.getAbsoluteFile()),"UTF8")),
												true);
						
						/**
						 * Write the feat file beginning and the first feat tag
						 * to the file
						 */
						output.println(createFeatFileBeginning(qName, type, baseStructFile));
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
	 * TODO: DOCUMENTATION
	 * @param sDocumentGraph
	 * @param documentPath
	 * @param documentId
	 */
	private void mapMetaAnnotations(
			SDocumentGraph sDocumentGraph,
			URI documentPath, 
			String documentId) {
		
		if (sDocumentGraph == null)
			throw new PAULAExporterException("Cannot map Meta annotations: There is no reference to the document graph");
		if (documentPath == null)
			throw new PAULAExporterException("Cannot map Meta annotations: No document path was specified");
		if (documentId.equals(""))
			throw new PAULAExporterException("Cannot map Meta annotations: The document ID was not specified");
		
		
		Hashtable<String,PrintWriter> annoFileTable = new Hashtable<String, PrintWriter>();
		
		String base = "merged."+documentId+".anno.xml";
		
		for (SMetaAnnotation anno : sDocumentGraph.getSDocument().getSMetaAnnotations()){
			
			
			StringBuffer featTag = new StringBuffer("\t\t")
			.append("<").append(TAG_FEAT_FEAT)
			.append(" ").append(ATT_FEAT_FEAT_HREF)
			.append("=\"#").append(anno.getSName())
			.append("\" ").append(ATT_FEAT_FEAT_VAL)
			.append("=\"").append(anno.getSValue())
			.append("\"/>");
		
			String type = anno.getQName();
			String paulaID = "merged."+documentId+".anno_"+type;
			/**
			 * Create the anno file name (paulaId + .xml)
			 */
			String annoFileName = paulaID+".xml";
		
		/**
		 * Reference one PrintWriter
		 */
		PrintWriter output = annoFileTable.get(annoFileName);
		
		/**
		 * If Reference is null, we have to create a anno file
		 */
		if (output != null){
			output.println(featTag.toString());
					
		}else{
			File annoFile = new File(documentPath.toFileString() + File.separator+annoFileName);
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
				output.write(createFeatFileBeginning(paulaID, type, base));
				
				output.println(featTag.toString());
						
				/**
				 * Put file (Writer) in FileTable for further access 
				 */
				annoFileTable.put(annoFileName, output);
				
		
			} catch (IOException e) {
				throw new PAULAExporterException("mapTokenAnnotations: Could not write File "+annoFile.getAbsolutePath()+": "+e.getMessage());
			}
		}
		}
		for (PrintWriter output : annoFileTable.values()){
			output.println("\t</"+TAG_FEAT_FEATLIST+">");
			output.println(PAULA_CLOSE_TAG);
			output.close();
		}
	}

	
	
	/**
	 * TODO: DOCUMENTATION
	 * @param documentPath
	 * @param layerPointingRelationList
	 * @param relFileTable
	 * @param layerNodeFileNames 
	 */
	private void mapPointingRelationAnnotations(
			URI documentPath,
			EList<SPointingRelation> layerPointingRelationList,
			Hashtable<String, String> relFileTable, 
			Set<String> layerNodeFileNames) {

		if (documentPath == null)
			throw new PAULAExporterException("Cannot map pointing relation annotations: No document path was specified");
		if (layerPointingRelationList == null)
			throw new PAULAExporterException("Cannot map pointing relation annotations: There are no pointing relations in this layer");
		if (relFileTable == null)
			throw new PAULAExporterException("Cannot map pointing relation annotations: There are no pointing relations files");
		if (layerNodeFileNames == null)
			throw new PAULAExporterException("Cannot map pointing relation annotations: There is no Set fo save the file names to");
		
		
		
		Hashtable<String,PrintWriter> annoFileTable = new Hashtable<String, PrintWriter>();
		for (SPointingRelation rel : layerPointingRelationList){
			String relationFile = relFileTable.get(rel.getSName()).replace(".xml", "");
			String base = relFileTable.get(rel.getSName());
			for (SAnnotation anno : rel.getSAnnotations()){
				StringBuffer featTag = new StringBuffer("\t\t")
				.append("<").append(TAG_FEAT_FEAT)
				.append(" ").append(ATT_FEAT_FEAT_HREF)
				.append("=\"#").append(rel.getSName())
				.append("\" ").append(ATT_FEAT_FEAT_VAL)
				.append("=\"").append(anno.getSValue())
				.append("\"/>");
			
				String type = anno.getQName();
				String paulaID = relationFile	+"_"+type;
				String annoFileName = paulaID+".xml";
				
				PrintWriter output = annoFileTable.get(annoFileName);
				
				/**
				 * If Reference is null, we have to create a anno file
				 */
				if (output != null){
					output.println(featTag.toString());
							
				}else{
					File annoFile = new File(documentPath.toFileString() + File.separator+annoFileName);
					try{
						if (!(annoFile.createNewFile()))
							System.out.println("File: "+ annoFile.getName()+ " already exists");
						
						layerNodeFileNames.add(annoFile.getName());
						/**
						 * Write Preamble and Tag
						 */
						output = new PrintWriter(
							  new BufferedWriter(	
									  new OutputStreamWriter(
									  new FileOutputStream(annoFile),"UTF8")),
														false);
						output.write(createFeatFileBeginning(paulaID, type, base));
						
						output.println(featTag.toString());
								
						/**
						 * Put file (Writer) in FileTable for further access 
						 */
						annoFileTable.put(annoFileName, output);
						
				
					} catch (IOException e) {
						throw new PAULAExporterException("mapRelAnnotations: Could not write File "+annoFile.getAbsolutePath()+": "+e.getMessage());
					}
				}
			}
		}
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
	 * @param firstDSName 
	 * @return
	 */
	private String createSpanFileMarkTag(
			String sName, 
			Hashtable<String, String> dSFileMap, 
			EList<SToken> overlappedTokenList,
			int dataSourceCount, 
			String firstDSName) {
		
		if (sName.equals(""))
			throw new PAULAExporterException("Cannot create span file mark tag: No span name was specified");
		if (dSFileMap == null)
			throw new PAULAExporterException("Cannot create span file mark tag: There is no token--DS file map");
		if (overlappedTokenList.isEmpty())
			throw new PAULAExporterException("Cannot create span file mark tag: There are no overlapped tokens");
		if (dataSourceCount == 0)
			throw new PAULAExporterException("Cannot create span file mark tag: There are no data sources");
		if (firstDSName.equals(""))
			throw new PAULAExporterException("Cannot create span file mark tag: No first DS name was specified");
		
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
	private String createStructFileBeginning(
			String paulaID,
			String type) {
		
		if (paulaID.equals(""))
			throw new PAULAExporterException("Cannot create struct file beginning: No Paula ID was specified");
		if (type.equals(""))
			throw new PAULAExporterException("Cannot create struct file beginning: No type was specified");
		
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
	 * @return String representation of the Preamble
	 */
	private String createMarkFileBeginning(
			String paulaID,
			String type, 
			String base) {
		
		if (paulaID.equals(""))
			throw new PAULAExporterException("Cannot create mark file beginning: No Paula ID was specified");
		if (type.equals(""))
			throw new PAULAExporterException("Cannot create mark file beginning: No type was specified");
		if (base.equals(""))
			throw new PAULAExporterException("Cannot create mark file beginning: No base file was specified");
		
		
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
	 * @return
	 */
	private String createFeatFileBeginning(
			String paulaID,
			String type, 
			String base){
		
		if (paulaID.equals(""))
			throw new PAULAExporterException("Cannot create feat file beginning: No Paula ID was specified");
		if (type.equals(""))
			throw new PAULAExporterException("Cannot create feat file beginning: No type was specified");
		if (base.equals(""))
			throw new PAULAExporterException("Cannot create feat file beginning: No base file was specified");
		
		
		
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
	 * 
	 * @param paulaID
	 * @param type
	 * @return
	 */
	private String createRelFileBeginning(
			String paulaID,
			String type){
		
		if (paulaID.equals(""))
			throw new PAULAExporterException("Cannot create rel file beginning: No Paula ID was specified");
		if (type.equals(""))
			throw new PAULAExporterException("Cannot create rel file beginning: No type was specified");
		
		StringBuffer buffer = new StringBuffer(TAG_HEADER_XML);
		buffer.append(LINE_SEPARATOR).append(PAULA_REL_DOCTYPE_TAG)
			  .append(LINE_SEPARATOR).append(TAG_PAULA_OPEN)
			  .append(LINE_SEPARATOR).append("\t")
			  .append("<"+TAG_HEADER+" "+ATT_HEADER_PAULA_ID[0]+"=\""+paulaID+"\"/>")
			  .append(LINE_SEPARATOR).append("\t")
			  .append("<"+TAG_REL_RELLIST+" xmlns:xlink=\"http://www.w3.org/1999/xlink\" "+
					  ATT_REL_RELLIST_TYPE+"=\""+type+"\">")
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
	

