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
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.modules.SDocumentStructureAccessor;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
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
		// create a Hashtable(SName,{PrintWriter,URI}) with initial Size equal to the number of Datasources 
		Hashtable<String,Object[]> fileTable = new Hashtable <String,Object[]>(sTextualDataSources.size());
		
		
		String documentName = sDocument.getSName();
		
		mapTextualDataSources(fileTable,sTextualDataSources,documentName,documentPath,false);
		//mapTokens(sDocument.getSDocumentGraph().getSTextualRelations(),fileTable , docID,documentPath,false);
		String oneDS = sTextualDataSources.get(0).getSId();
		//mapLayersToFiles(sDocument.getSDocumentGraph(),fileTable,docID,documentPath.toFileString(), oneDS);
		mapLayers(sDocument.getSDocumentGraph(), documentPath, documentName, fileTable,oneDS);
		
	}

	/**
	 * Writes the primary text sText to a file "documentID_text.xml" in the documentPath
	 * and returns the URI (filename).
	 * @param fileTable TODO
	 * @param sTextualDS the primary text
	 * @param documentID the document id
	 * @param documentPath the document path
	 */
	
		
	private void mapTextualDataSources( Hashtable<String,Object[]> fileTable, 
									  EList<STextualDS> sTextualDS, 
									  String documentID,
									  URI documentPath, boolean validate) {
		if (fileTable == null)
			throw new PAULAExporterException("Cannot map Data Sources because fileTable is null");

		if (sTextualDS.isEmpty())
			throw new PAULAExporterException("Cannot map Data Sources because there are none");
		if (documentID.equals(""))
			throw new PAULAExporterException("Cannot map Data Sources because documentID is empty (\"\")");
		if (documentPath == null)
			throw new PAULAExporterException("Cannot map Data Sources because documentPath is null");
		
		File textFile;
		int dsNum = sTextualDS.size();
		for (STextualDS sText : sTextualDS){
			if (dsNum == 1){
				textFile = new File(documentPath.toFileString()+ 
								File.separator + documentID+ ".merged.text.xml");
			} else {
				textFile = new File(
								documentPath.toFileString()+ 
								File.separator + documentID +".merged.text."+
								(sTextualDS.indexOf(sText)+1)+".xml");
			}
			//System.out.println("SText "+sText.getSName());
			try{
				if (! textFile.createNewFile())
					System.out.println("File: "+ textFile.getName()+ " already exists");
			
				PrintWriter output = new PrintWriter( 
										new BufferedWriter(
											new OutputStreamWriter(
												new FileOutputStream(
														textFile)
												,"UTF8")),false);
			
				fileTable.put(sText.getSId(),new Object[] {output,URI.createFileURI(textFile.getAbsolutePath())});	
				
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
				// dispose PrintWriter
				fileTable.get(sText.getSId())[0] = null;
				if (validate)
				  System.out.println( "File " + textFile.getName() + " is valid: " + this.isValidXML(textFile) );
				
				}catch (IOException e){
					System.out.println("Exception: "+ e.getMessage());
				}
			
							
		}
		
		// We don't write the output text to a variable to have less overhead
		// since java used calls by value 
		// The DTD needs a leading letter or underscore in the attribute value of
		// paula_id. Thus, I introduced a prefix (pepper)
		
	}
	
	/**
	 * Port for refactoring: map layer by layer
	 * @param sDocumentGraph
	 * @param documentPath
	 * @param documentId
	 * @param fileTable
	 * @param oneDS 
	 */
	private void mapLayers(SDocumentGraph sDocumentGraph,
						   URI documentPath,
						   String documentId,
						   Hashtable<String,Object[]> fileTable, 
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
		
		EList<SSpan> spanList = new BasicEList<SSpan>(sDocumentGraph.getSSpans());
		EList<SStructure> structList = new BasicEList<SStructure>(sDocumentGraph.getSStructures());
		
		Hashtable<String,String> tokenFileMap = null;
		EList<SSpan> layerSpanList = new BasicEList<SSpan>();
		EList<SStructure> layerStructList = new BasicEList<SStructure>();
		EList<SToken> layerTokenList = new BasicEList<SToken>();
		EList<SSpan> multiLayerSpanList = new BasicEList<SSpan>();
		EList<SStructure> multiLayerStructList = new BasicEList<SStructure>();
		EList<SToken> multiLayerTokenList = new BasicEList<SToken>();
		
		for (SLayer layer : sDocumentGraph.getSLayers()){
			layerSpanList = new BasicEList<SSpan>();
			layerStructList = new BasicEList<SStructure>();
			layerTokenList = new BasicEList<SToken>();
			
			for (SNode sNode : layer.getSNodes()){
				if (sNode instanceof SToken){
					if (sNode.getSLayers().size() > 1){
						multiLayerTokenList.add((SToken)sNode);
					} else {
						layerTokenList.add((SToken)sNode);
						
					}
				}
				
				if (sNode instanceof SSpan ){
					if (sNode.getSLayers().size() > 1){
						multiLayerSpanList.add((SSpan) sNode);
					} else {
						spanList.remove((SSpan) sNode);
						layerSpanList.add((SSpan) sNode);
					}
				}
				
				if (sNode instanceof SStructure ){
					if (sNode.getSLayers().size() > 1){
						multiLayerStructList.add((SStructure) sNode);
					} else {
						structList.remove((SStructure) sNode);
						layerStructList.add((SStructure) sNode);
					}
					
				}
					
				
			}
			
			if (! layerTokenList.isEmpty()){
				if (sDocumentGraph.getSTextualRelations().size() > layerTokenList.size())
					throw new PAULAExporterException("Salt2PAULAMapper: There are more Textual Relations then Token in layer"+ layer.getSName());
						
				tokenFileMap = mapTokens(sDocumentGraph.getSTextualRelations(),layerTokenList,fileTable,documentId,documentPath,layer.getSName());
			}
			if (! layerSpanList.isEmpty()){
				mapSpans(sDocumentGraph, layerSpanList,fileTable,documentId,documentPath, layer.getSName(), firstDSName);
			}
			if (! layerStructList.isEmpty()){
				mapStructs(layerStructList,fileTable,documentId,documentPath);
			}
		}
		if (! spanList.isEmpty()){
			
		}
		if (! structList.isEmpty()){
			
		}
		
	}
	
	
	/**
	 * Creates Annotations files for all token.
	 * TODO: Write feat file content
	 * @param tokenFileMap
	 * @param layerTokenList
	 * @param documentPath
	 */
	private void mapTokenAnnotations(Hashtable<String, String> tokenFileMap,
									EList<SToken> layerTokenList, URI documentPath) {
		if (tokenFileMap == null)
			throw new PAULAExporterException("There is no token File");
		if (layerTokenList == null)
			throw new PAULAExporterException("The token List is empty");
		
		//System.out.println("Token list has "+layerTokenList.size()+ " Tokens");
		Hashtable<String,PrintWriter> annoFileTable = new Hashtable<String,PrintWriter>();
		File f = null;
		//System.out.println("First token file: " + tokenFileMap.get(tokenFileMap.keys().nextElement()));
		for (SToken sToken : layerTokenList){
			String tokFileName = tokenFileMap.get(sToken.getSName().replace(".xml", ""));
			for (SAnnotation sAnnotation : sToken.getSAnnotations()){
				StringBuffer lineToWrite = new StringBuffer();
				String qName = tokFileName+ "_"+sAnnotation.getQName()+".xml";
				//System.out.println("Token annotation baseName: "+qName);
				if (annoFileTable.containsKey(qName)){
					annoFileTable.get(qName).println("Test");
				} else {
					f = new File(documentPath.toFileString() + File.separator+qName);
					try {
						if (!(f.createNewFile()))
							System.out.println("File: "+ f.getName()+ " already exists");
						
						annoFileTable.put(qName, 
							  new PrintWriter(
							  new BufferedWriter(	
							  new OutputStreamWriter(
							  new FileOutputStream(f),"UTF8")),
												false));
						//System.out.println("Put file "+f.getName()+ " into annoFileTable");
						annoFileTable.get(qName).write("Test");
						//System.out.println("Wrote test");
						//(new StringBuffer("Anno: "+sAnnotation.getQName())).toString());
				 
					} catch (IOException e) {
						throw new PAULAExporterException("mapTokenAnnotations: Could not write File "+f.getAbsolutePath()+": "+e.getMessage());
					}
				}
			}
		}
		
		for (PrintWriter output : annoFileTable.values()){
			System.out.println("closing file");
			output.close();
		}
	}

	
	/**
	 * Extracts the tokens including the xPointer from the STextualRelation list 
	 * and writes them to files "documentID.tokenfilenumber.tok.xml" in the documentPath.
	 * 
	 * Seems to work, but needs test and more documentation
	 * 
	 * @param sTextRels list of textual relations (tokens)pointing to a target (data source)
	 * @param layerTokenList 
	 * @param sIdMap Hashmap including the SId (String) of the data-source, the URI of the corresponding textfile and a PrintWriter for each token file
	 * @param documentID the document id of the Salt document
	 * @param documentPath the path to which the token files will be mapped
	 * @param layer Name of the layer
	 * @return Hashtable of the form (TokenName,TokenFile)
	 */
	private Hashtable<String, String> mapTokens( EList<STextualRelation> sTextRels,
									   EList<SToken> layerTokenList, Hashtable<String, Object[]> sIdMap, 
									   String documentID,  
									   URI documentPath, String layer) {
		
		if (sTextRels.isEmpty())
			throw new PAULAExporterException("Cannot create token files because there are no textual relations");
		if (documentID.equals(""))
			throw new PAULAExporterException("Cannot create token files because documentID is empty (\"\")");
		if (documentPath == null)
			throw new PAULAExporterException("Cannot create token files because documentPath is null");
		if (sIdMap == null)
			throw new PAULAExporterException("Cannot create token files because no textFileTable is defined" );
		
		Hashtable<String,String> tokenFileMap = new Hashtable<String,String>();
		String baseTextFile;
		int tokenFileIndex = 0;
		File tokenFile = null;
		
		//StringBuffer fileString = new StringBuffer();
		
		for (STextualRelation sTextualRelation : sTextRels){
			String sTextDSSid = sTextualRelation.getSTarget().getSId();
			String paulaMarkTag = new StringBuffer("\t\t<mark id=\"")
				  .append(sTextualRelation.getSToken().getSName()).append("\" ")
			      .append("xlink:href=\"#xpointer(string-range(//body,'',")
			      .append(sTextualRelation.getSStart()+1).append(",")
			      .append(sTextualRelation.getSEnd()-sTextualRelation.getSStart())
			      .append("))\" />").toString();
			
			if (sIdMap.get(sTextDSSid)[0] != null){
				((PrintWriter)(sIdMap.get(sTextDSSid)[0])).println(paulaMarkTag);
		
			} else {
				try {
				String fileName = new String(((URI)(sIdMap.get(sTextDSSid))[1]).toFileString().replace("text", "tok"));
				tokenFile = new File(fileName);
				if ( ! tokenFile.createNewFile())
					System.out.println("File: "+ tokenFile.getName()+ " already exists");
				
				tokenFileMap.put(sTextualRelation.getSToken().getSName(), tokenFile.getName());
				sIdMap.get(sTextDSSid)[0] = new PrintWriter(new BufferedWriter(	
												new OutputStreamWriter(
													new FileOutputStream(tokenFile),
													"UTF8")),
													true);
				
				baseTextFile = tokenFile.getName().replace("tok", "text");
				sIdMap.get(sTextDSSid)[1] = URI.createFileURI(tokenFile.getAbsolutePath());
				
				
				((PrintWriter) (sIdMap.get(sTextDSSid)[0])).write(
						createMarkFileBeginning(documentID+"_"+layer+"_"+tokenFileIndex+"_tok",
								"tok", 
								baseTextFile.replace(tokenFile.getPath(),""), 
								0));
				
				//fileString.delete(0, fileString.length()+1);
				} catch (IOException e){
					System.out.println("Exception: "+ e.getMessage());
				} catch (SecurityException e) {
					System.out.println("SecurityException: "+ e.getMessage());
				}
			}
		}
		/**
		 * Close all token file streams
		 */
		for (Object[] writer :  (sIdMap.values())){
			((PrintWriter) writer[0] ).write(PAULA_TOKEN_FILE_CLOSING);
			((PrintWriter) writer[0] ).close();
			//fileString.delete(0, fileString.length()+1);
		}
		mapTokenAnnotations(tokenFileMap,layerTokenList,documentPath);
		return tokenFileMap;
	}
	
	
	private void mapSpans(SDocumentGraph graph,EList<SSpan> layerSpanList, Hashtable<String, Object[]> fileTable, String documentId, URI documentPath, String layer , String firstDSName){
		if (documentPath.equals(""))
			throw new PAULAExporterException("Cannot map Layers because documentPath is empty (\"\")");
		if (documentId.equals(""))
			throw new PAULAExporterException("Cannot map Layers files because documentID is empty (\"\")");
		if (graph == null)
			throw new PAULAExporterException("Cannot map Layers files because document graph is null");
		
		SDocumentStructureAccessor accessor = new SDocumentStructureAccessor();
		accessor.setSDocumentGraph(graph);
		String baseName;
		//String markTag = "";
		Hashtable<String,PrintWriter> spanFileTable = new Hashtable<String,PrintWriter>();
		EList<SSpan> spanList = new BasicEList<SSpan>(graph.getSSpans());
		EList<String> spanFileNames = new BasicEList<String>();
		//StringBuffer lineToWrite = new StringBuffer();
		EList<SToken> overlappingTokens = null;
		
		int dsNum = graph.getSTextualDSs().size();
		String baseMarkFile = ((URI)fileTable.get(firstDSName)[1]).toFileString()
		.substring(((URI)fileTable.get(firstDSName)[1])
			.toFileString().lastIndexOf(File.separator)+1);
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
			baseName = spanFileToWrite.substring(0, spanFileToWrite.length()-4);
			output.write(
				createMarkFileBeginning(
					baseName,
					"mark",
					baseMarkFile, dsNum));
		
		} catch (IOException e) {
			throw new PAULAExporterException("mapSpans: Could not write File "+spanFileToWrite.toString()+": "+e.getMessage());
		}
			
		for (SSpan sSpan : layerSpanList){
			/**
			 * get tokens which are overlapped by this Span
			 */
			overlappingTokens = accessor.getSTextualOverlappedTokens((SStructuredNode) sSpan);
				
			spanList.remove(sSpan);
			spanFileNames.add(spanFileToWrite);
			/**
			 * Write mark tag
			 */
			output.println(
					createMarkTag(
						sSpan.getSName(),
						fileTable, overlappingTokens, 
						dsNum,
						baseMarkFile,
						firstDSName)
						);
				
		}
		output.write("\t"+MARK_LIST_CLOSE_TAG+LINE_SEPARATOR+PAULA_CLOSE_TAG);
		output.close();
		mapSpanAnnotations(layerSpanList,documentPath,baseName);
	
	}
	
	
	private void mapStructs(EList<SStructure> layerStructList, Hashtable<String, Object[]> fileTable, String documentId, URI documentPath) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Creates Annotation files for spans.
	 * TODO: Write FeatFile Content
	 * @param layerSpanList
	 * @param documentPath
	 * @param spanFileToWrite
	 */
	private void mapSpanAnnotations(EList<SSpan> layerSpanList, URI documentPath, String spanFileToWrite){
		
		Hashtable<String,PrintWriter> annoFileTable = new Hashtable<String,PrintWriter>();
		File f;
		for (SSpan sSpan : layerSpanList){
			for (SAnnotation sAnnotation : sSpan.getSAnnotations()){
				StringBuffer lineToWrite = new StringBuffer();
				String qName = spanFileToWrite + "_"+sAnnotation.getQName();
				if (annoFileTable.containsKey(qName)){
					annoFileTable.get(qName).println(lineToWrite.toString());
				} else {
					f = new File(documentPath.toFileString() + File.separator+qName+".xml");
					try {
						if (!(f.createNewFile()))
							System.out.println("File: "+ f.getName()+ " already exists");
						annoFileTable.put(f.getAbsolutePath(), 
							  new PrintWriter(
							  new BufferedWriter(	
							  new OutputStreamWriter(
							  new FileOutputStream(f.getAbsoluteFile()),"UTF8")),
												true));
						annoFileTable.get(f.getAbsolutePath()).write(
							(new StringBuffer("Anno: "+sAnnotation.getName())).toString());
				 
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						throw new PAULAExporterException("mapSpanAnnotations: Could not write File "+f.getAbsolutePath()+": "+e.getMessage());
					}
				
			/*
			 * schreibe SAnnotation in feat file fuer alle SAnnotation mit 
			 * gleichem SAnnotation.getSFullName (oder getSQName(), 
			 * hab ich vergessen)
			 * Achtung: Referenzfile muss immer das gleiche sein, 
			 * sonst neue Datei
    		 */
				}
			}
		}
		for (PrintWriter output : annoFileTable.values()){
			output.close();
		}
	}
	
	/**
	 * 
	 * @param graph
	 * @param sIdMap
	 * @param docID
	 * @param documentPath
	 */
	private void mapLayersToFiles(SDocumentGraph graph, Hashtable<String,Object[]> sIdMap ,String docID, String documentPath, String firstDSName) {
		if (documentPath.equals(""))
			throw new PAULAExporterException("Cannot map Layers because documentPath is empty (\"\")");
		if (docID.equals(""))
			throw new PAULAExporterException("Cannot map Layers files because documentID is empty (\"\")");
		if (graph == null)
			throw new PAULAExporterException("Cannot map Layers files because document graph is null");
		
		SDocumentStructureAccessor accessor = new SDocumentStructureAccessor();
		accessor.setSDocumentGraph(graph);
		
		String markTag = "";
		Hashtable<String,PrintWriter> fileTable = new Hashtable<String,PrintWriter>();
		File f;
		EList<SSpan> spanList = new BasicEList<SSpan>(graph.getSSpans());
		EList<SStructure> structList = new BasicEList<SStructure>(graph.getSStructures());
		EList<String> spanFileNames = new BasicEList<String>();
		EList<String> structFileNames = new BasicEList<String>();
		StringBuffer lineToWrite = new StringBuffer();
		EList<SToken> overlappingTokens = null;
		
		int dsNum = graph.getSTextualDSs().size();
		String baseMarkFile = ((URI)sIdMap.get(firstDSName)[1]).toFileString()
		.substring(((URI)sIdMap.get(firstDSName)[1])
			.toFileString().lastIndexOf(File.separator)+1);
		
		
		for (SLayer layer : graph.getSLayers()){
			String fileToWrite="";
			String spanFileToWrite = docID +"."+layer.getSName() +".mark.xml";
			String structFileToWrite = docID +"."+ layer.getSName() +".struct.xml";
			String sTextName = "";
			for (SNode sNode : layer.getSNodes() ){
				if (sNode instanceof SSpan){
					try{
					overlappingTokens = accessor.getSTextualOverlappedTokens((SStructuredNode) sNode);
					markTag = createMarkTag(
							sNode.getSName(),
							sIdMap, overlappingTokens, 
							dsNum,
							baseMarkFile,
							firstDSName
							)
							+ LINE_SEPARATOR;
					if (fileTable.get(spanFileToWrite) == null){
						if (!(f = new File(documentPath + File.separator + spanFileToWrite))
								.createNewFile())
							System.out.println("File: "+ f.getName()+ " already exists");
					
						fileTable.put(spanFileToWrite, new PrintWriter(
							new BufferedWriter(	
									new OutputStreamWriter(
									new FileOutputStream(f.getAbsoluteFile())
									,"UTF8")),
											false));
					
						fileTable.get(spanFileToWrite).write(
							createMarkFileBeginning(
									spanFileToWrite.substring(0, spanFileToWrite.length()-4),
									"mark",
									baseMarkFile, dsNum));
					
						spanList.remove((SSpan)sNode);
						spanFileNames.add(spanFileToWrite);
						fileTable.get(spanFileToWrite)
							.write(
								createMarkTag(
									sNode.getSName(),
									sIdMap, overlappingTokens, 
									dsNum,
									baseMarkFile,
									firstDSName)
									+ LINE_SEPARATOR);
					
						fileToWrite = docID +"."+ layer.getSName()+ ".mark";
					} else {
						fileTable.get(spanFileToWrite)
						.write(
							createMarkTag(sNode.getSName(),
									sIdMap, overlappingTokens,dsNum, 
									baseMarkFile,firstDSName )
									+ LINE_SEPARATOR);
					}
					}catch(IOException ioe){
						throw new PAULAExporterException("mapLayersToFiles: Could not write File "+spanFileToWrite.toString()+": "+ioe.getMessage());
					}
				} else {
					if (sNode instanceof SStructure){
						structList.remove((SStructure) sNode);	
						
						try {
						if (fileTable.get(structFileToWrite) == null){	
							if (! (f = new File(documentPath + File.separator +structFileToWrite)).createNewFile())
								System.out.println("File: "+ f.getName()+ " already exists");
						
							fileTable.put(structFileToWrite, new PrintWriter(
								new BufferedWriter(	
										new OutputStreamWriter(
										new FileOutputStream(f.getAbsoluteFile())
										,"UTF8")),
												false));
							structFileNames.add(structFileToWrite);
							fileTable.get(structFileToWrite).write(createStructFileBeginning(
								structFileToWrite.replace(".xml", ""),
								"struct","struct"));
						} else {
							fileTable.get(structFileToWrite).println(sNode.getSName());
						}
						} catch(IOException ioe){
							throw new PAULAExporterException("mapLayersToFiles: Could not write File "+structFileToWrite.toString()+": "+ioe.getMessage());
						}
						
						
						fileToWrite = docID +"."+ layer.getSName()+ ".struct";
					} else {
						if (sNode instanceof SToken)
							sTextName = ((SToken)sNode).getSDocumentGraph()
												.getSTextualRelations().get(
												((SToken)sNode).getSDocumentGraph().getSTextualRelations()
												.indexOf(sNode)).getSTarget().getSName();
							fileToWrite = ((URI)sIdMap.get( sTextName)[1]).toFileString()
										  .substring(((URI)sIdMap.get( sTextName)[1]).toFileString().lastIndexOf(File.separator + 1));
										
						
					}
				}
				
				for (SAnnotation sAnnotation : sNode.getSAnnotations()){
					String qName = fileToWrite + "_"+sAnnotation.getQName();
					if (fileTable.containsKey(qName)){
						fileTable.get(qName).println(lineToWrite.toString());
					} else {
						f = new File(documentPath + File.separator+qName+".xml");
						try {
							fileTable.put(f.getAbsolutePath(), 
									  new PrintWriter(
									  new BufferedWriter(	
									  new OutputStreamWriter(
									  new FileOutputStream(f.getAbsoluteFile()),"UTF8")),
														true));
							fileTable.get(f.getAbsolutePath()).write(
									(new StringBuffer("Anno: "+sAnnotation.getName())).toString());
						 
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					/*
					 * schreibe SAnnotation in feat file fuer alle SAnnotation mit 
					 * gleichem SAnnotation.getSFullName (oder getSQName(), 
					 * hab ich vergessen)
					 * Achtung: Referenzfile muss immer das gleiche sein, 
					 * sonst neue Datei
            		 */
				}
				
			}
		
		}
		for (String spanFileName : spanFileNames)
			fileTable.get(spanFileName).write("\t"+MARK_LIST_CLOSE_TAG+LINE_SEPARATOR+PAULA_CLOSE_TAG);
			
		
		for (PrintWriter out : fileTable.values()){
			
			out.close();
		}
		/**
		 * Map spans and structures without Layer to files
		 */
		if (! spanList.isEmpty()){
			
		}
		if (! structList.isEmpty()){
			
		}
		
		/*
		 * 
    	   

			wenn SDocumentGrpah.getSSPans().size() != numOfSSPan
    			suche alle SSPans ohne Layer und wieder hole obiges
			wenn SDocumentGrpah.getSStructure().size() != numOfSStructure
    			suche alle SSPans ohne Layer und wieder hole obiges 
		 */
	}
	
	
	
	
	/**
	 * TO DO: Wenn das Token nicht auf die Base zeigt, Base davorhaengen!!!!
	 * @param sName
	 * @param eList
	 * @param dataSourceCount
	 * @param base
	 * @param firstDSName 
	 * @return
	 */

	
	private String createMarkTag(String sName, Hashtable<String,Object[]> sIdMap, EList<SToken> eList,int dataSourceCount, String base, String firstDSName) {
		StringBuffer buffer = new StringBuffer();
		EList<STextualRelation> rel = eList.get(0).getSDocumentGraph().getSTextualRelations();
		String sTextualDSName;
		String tokenFile;
		URI tokenPath;
		buffer.append("\t\t<mark ").append(ATT_MARK_MARK_ID).append("=\"").append(sName)
			.append("\" ").append(ATT_MARK_MARK_HREF).append("=\"(");
		if (dataSourceCount == 1){
			for (SToken token : eList){
				if (eList.indexOf(token) < eList.size()-1){
					buffer.append("#").append(token.getSName()).append(",");
				} else {
				buffer.append("#").append(token.getSName());
				}
			}
		} else {
			for (SToken token : eList){
				sTextualDSName = rel.get(rel.indexOf(token)).getSTarget().getSName();
				tokenPath = (URI)sIdMap.get(sTextualDSName)[1];
				tokenFile = tokenPath.toFileString().substring(tokenPath.toFileString().lastIndexOf(File.separator+1));
				if (eList.indexOf(token) < eList.size()-1){
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

	private String createStructFileBeginning(String replace, String string,
			String string2) {
		// TODO Auto-generated method stub
		return "";
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
	

