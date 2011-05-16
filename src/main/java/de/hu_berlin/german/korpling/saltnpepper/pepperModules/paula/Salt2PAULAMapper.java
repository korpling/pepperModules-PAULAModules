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
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SSpan;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SStructure;
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
		
		EList<STextualDS> sTextualDataSource = sDocument.getSDocumentGraph().getSTextualDSs();
		
		// create a Hashtable(Sid,{URI,PrintWriter}) with initial Size equal to the number of Datasources 
		Hashtable<String,Object[]> sIdMap = new Hashtable <String,Object[]>(sTextualDataSource.size());
		
		
		String docID = sDocument.getSName();
		int dsNumber = 1;
		for (STextualDS sText : sTextualDataSource){
			sIdMap.put(sText.getSId(),new Object[] {createPAULATextFile(sText.getSText(),docID+"."+dsNumber,documentPath,false),null});
		}
		
		createPAULATokenFile(sDocument.getSDocumentGraph().getSTextualRelations(),sIdMap , docID,documentPath,false);
		mapLayersToFiles(sDocument.getSDocumentGraph(),docID,documentPath);
		//TODO:!done! read primary text from Salt file
		//TODO !done! check that parameters are not null and raise an exception if necessary
		//TODO map sDocument to PAULA and write files to documentPath
	}

	/**
	 * Writes the primary text sText to a file "documentID_text.xml" in the documentPath
	 * and returns the URI (filename).
	 *   
	 * @param sText the primary text
	 * @param documentID the document id
	 * @param documentPath the document path
	 * @return URI of the written text file
	 */
	
		
	private URI createPAULATextFile( String sText, 
									  String documentID, 
									  URI documentPath,
									  boolean validate) {
		if (sText.equals(""))
			throw new PAULAExporterException("Warning: Primary text is empty");
		if (documentID.equals(""))
			throw new PAULAExporterException("Cannot create text file because documentID is empty (\"\")");
		if (documentPath == null)
			throw new PAULAExporterException("Cannot create text file because documentPath is null");
		
		File textFile = new File(documentPath.toFileString()+"/" + documentID + ".text.xml");
		
		try{
		textFile.createNewFile();
		PrintWriter output = new PrintWriter( new BufferedWriter(
				new OutputStreamWriter(
						new FileOutputStream(textFile),"UTF8")),true);
		
		// We don't write the output text to a variable to have less overhead
		// since java used calls by value 
		// The DTD needs a leading letter or underscore in the attribute value of
		// paula_id. Thus, I introduced a prefix (pepper)
		{
			output.println(TAG_HEADER_XML);
			output.println(PAULA_TEXT_DOCTYPE_TAG);
			output.println(TAG_PAULA_OPEN);
			output.println( new StringBuffer("\t<header paula_id=\"pepper.")
							.append(documentID)
							.append("_text\" type=\"text\"/>").toString());
			output.println("\t\t"+TAG_TEXT_BODY_OPEN);
			output.println("\t\t\t" + sText);
			output.println("\t\t"+TAG_TEXT_BODY_CLOSE);
			output.println(PAULA_CLOSE_TAG);			
		}	
		output.close();
		if (validate)
		  System.out.println( "File " + textFile.getName() + " is valid: " + this.isValidXML(textFile) );
		
		}catch (IOException e){
			System.out.println("Exception: "+ e.getMessage());
		}
		return(org.eclipse.emf.common.util.URI.createFileURI(textFile.getName()));		
	}
	
	
	/**
	 * Extracts the tokens including the xPointer from the STextualRelation list 
	 * and writes them to files "documentID.tokenfilenumber.tok.xml" in the documentPath.
	 * 
	 * Seems to work, but needs test and more documentation
	 * 
	 * @param sTextRels list of testual relations (tokens)pointing to a target (data source)
	 * @param sIdMap Hashmap including the SId (String) of the datasource, the URI of the corresponding textfile and a PrintWriter for each token file
	 * @param documentID the document id of the Salt document
	 * @param documentPath the path to which the token files will be mapped
	 * @param validate states whether the output shall be validated according to the mark.dtd
	 */
	private void createPAULATokenFile( EList<STextualRelation> sTextRels,
									   Hashtable<String, Object[]> sIdMap, 
									   String documentID,  
									   URI documentPath,
									   boolean validate) {
		
		if (sTextRels.isEmpty())
			throw new PAULAExporterException("Cannot create token files because there are no textual relations");
		if (documentID.equals(""))
			throw new PAULAExporterException("Cannot create token files because documentID is empty (\"\")");
		if (documentPath == null)
			throw new PAULAExporterException("Cannot create token files because documentPath is null");
		if (sIdMap == null)
			throw new PAULAExporterException("Cannot create token files because no textFileTable is defined" );
		
		String baseTextFile;
		int tokenFileIndex = 0;
		File tokenFile = null;
		ArrayList<File> tokenFileList = null;
		if (validate){
			tokenFileList = new ArrayList<File>();
		}
		StringBuffer fileString = new StringBuffer();
		for (STextualRelation sTextualRelation : sTextRels){
			String sTextDSSid = sTextualRelation.getSTarget().getSId();
			String paulaMarkTag = new StringBuffer("\t\t<mark id=\"")
				  .append(sTextualRelation.getSToken().getSName()).append("\" ")
			      .append("xlink:href=\"#xpointer(string-range(//body,'',")
			      .append(sTextualRelation.getSStart()+1).append(",")
			      .append(sTextualRelation.getSEnd()-sTextualRelation.getSStart())
			      .append("))\" />").toString();
			
			if (sIdMap.get(sTextDSSid)[1] != null){
				((PrintWriter)(sIdMap.get(sTextDSSid)[1])).println(paulaMarkTag);
		
			} else {
				try {
				tokenFileIndex++;
				tokenFile = new File(documentPath.toFileString()+"/" + documentID + "."+ tokenFileIndex +".tok.xml");
				tokenFile.createNewFile();
				if (validate){
					tokenFileList.add(tokenFile);
				}
				sIdMap.get(sTextDSSid)[1] = new PrintWriter(new BufferedWriter(	
												new OutputStreamWriter(
													new FileOutputStream(tokenFile),
													"UTF8")),
													true);
				
				baseTextFile = ((URI) sIdMap.get(sTextDSSid)[0]).toFileString();
				
				fileString.append(TAG_HEADER_XML).append(LINE_SEPARATOR)
				  .append(PAULA_MARK_DOCTYPE_TAG).append(LINE_SEPARATOR)
				  .append(TAG_PAULA_OPEN).append(LINE_SEPARATOR)
				  // appending token header
				  .append("\t<header paula_id=\"pepper.").append(documentID)
				  .append(".").append(tokenFileIndex).append("_tok\"/>").append(LINE_SEPARATOR)
				  // append markList open Tag
				  .append("\t<markList xmlns:xlink=\"http://www.w3.org/1999/xlink\" type=\"tok\" xml:base=\"")
				  .append(baseTextFile.replace(tokenFile.getPath(),""))
				  .append("\">").append(LINE_SEPARATOR)
				  .append(paulaMarkTag).append(LINE_SEPARATOR);
				
				((PrintWriter) (sIdMap.get(sTextDSSid)[1])).write(fileString.toString());
				
				fileString.delete(0, fileString.length()+1);
				} catch (IOException e){
					System.out.println("Exception: "+ e.getMessage());
				} 
			}
		}
		
		for (Object[] writer :  (sIdMap.values())){
			((PrintWriter) writer[1] ).write(PAULA_TOKEN_FILE_CLOSING);
			((PrintWriter) writer[1] ).close();
			fileString.delete(0, fileString.length()+1);
			
		}
		
		if (validate){
			for (File file : tokenFileList){
				System.out.println( "File "+file.getName()+" is valid: " + this.isValidXML(file) );
			}
		}				
	}
	
	/**
	 * 
	 * @param graph
	 * @param docID
	 * @param documentPath
	 */
	private void mapLayersToFiles(SDocumentGraph graph, String docID, URI documentPath) {
		if (documentPath.toFileString().equals(""))
			throw new PAULAExporterException("Cannot map Layers because documentPath is empty (\"\")");
		if (docID.equals(""))
			throw new PAULAExporterException("Cannot map Layers files because documentID is empty (\"\")");
		if (graph == null)
			throw new PAULAExporterException("Cannot map Layers files because document graph is null");
		
		Hashtable<String,PrintWriter> fileTable = new Hashtable<String,PrintWriter>();
		PrintWriter output;
		EList<SSpan> spanList = new BasicEList<SSpan>(graph.getSSpans());
		EList<SStructure> structList = new BasicEList<SStructure>(graph.getSStructures());
		
		StringBuffer lineToWrite = new StringBuffer();
		for (SLayer layer : graph.getSLayers()){
			String fileToWrite = "";
			System.out.println(layer.getSName());
			/**
			 * Iterate over all layers and map the nodes to mark/struct
			 */
			for (SNode sNode : layer.getSNodes() ){
				/**
				 * Map Spans to File per layer
				 */
				if (sNode instanceof SSpan){
					fileToWrite = docID +"."+layer.getSName() +".mark.xml";
					//System.out.println("File: "+fileToWrite);
					// write to File a
					lineToWrite.append(((SSpan)sNode));
					spanList.remove((SSpan)sNode);
				}
				/**
				 * Map Structures to File per layer
				 */
				if (sNode instanceof SStructure){
					fileToWrite = docID +"."+ layer.getSName() +".struct.xml";
					//System.out.println("File: "+fileToWrite);
					// write to file b
					structList.remove((SStructure) sNode);
				}
				if (sNode instanceof SToken){
					/**
					 * Layer 1 is token Layer
					 */
					System.out.println("Token: Layer SId : "+layer.getSId()+" SName: "+layer.getSName());
					continue;
				}
				
				//System.out.println("Spanlist size:"+ spanList.size()+ " Structlist size:"+structList.size());
				if ((output = fileTable.get(fileToWrite)) != null){
					output.println(lineToWrite.toString());
				} else {
					try{
					//System.out.println(documentPath.toFileString() + File.separator +fileToWrite);
					
					File f = new File(documentPath.toFileString() + File.separator +fileToWrite);
					f.createNewFile();
					
					fileTable.put(fileToWrite, new PrintWriter(
												new BufferedWriter(	
												new OutputStreamWriter(
												new FileOutputStream(f.getAbsoluteFile())
												,"UTF8")),
														true));
					fileTable.get(fileToWrite).println(lineToWrite.toString());
					} catch(IOException ioe){
						throw new PAULAExporterException("mapLayersToFiles: Could not write File "+fileToWrite.toString()+": "+ioe.getMessage());
					}
				}
				
				
				for (SAnnotation sAnnotation : sNode.getSAnnotations()){
					if (sNode instanceof SSpan)
						fileToWrite = docID +"."+ layer.getSName()+ ".mark";
						//System.out.println("Span Annotation name: "+ sAnnotation.getQName());
					if (sNode instanceof SStructure)
						fileToWrite = docID +"."+ layer.getSName()+ ".struct";
						//System.out.println("Structure Annotation name: "+ sAnnotation.getQName());
					String qName = fileToWrite + "_"+sAnnotation.getQName();
					if (fileTable.containsKey(qName)){
						fileTable.get(qName).println(lineToWrite.toString());
					} else {
						File f = new File(documentPath.toFileString() + File.separator+qName+".xml");
						try {
							fileTable.put(qName, 
									  new PrintWriter(
									  new BufferedWriter(	
									  new OutputStreamWriter(
									  new FileOutputStream(f.getAbsoluteFile()),"UTF8")),
														true));
							fileTable.get(qName).write(
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
				for (PrintWriter out : fileTable.values()){
					out.close();
				}
			}
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
	

