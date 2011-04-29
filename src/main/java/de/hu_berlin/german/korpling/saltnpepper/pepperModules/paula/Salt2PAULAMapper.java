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
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.log.LogService;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;

import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.exceptions.PAULAExporterException;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualRelation;
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
	
	private final String xmlHead = "<?xml version=\"1.0\" standalone=\"no\"?>";
	private final String paulaOpenTag = "<paula version=\"1.0\">" ;
	private final String paulaCloseTag = "</paula>";
	private final String paulaMark = "<!DOCTYPE paula SYSTEM \"paula_mark.dtd\">";
	private final String paulaText = "<!DOCTYPE paula SYSTEM \"paula_text.dtd\">";
	private final String bodyOpen = "<body>";
	private final String bodyClose = "</body>";
	private final String lineSeparator = System.getProperty("line.separator");
	
	
	
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
		
		EList<STextualDS> sTextualDataSource = sDocument.getSDocumentGraph().getSTextualDSs();
		EList<STextualRelation> sTextRels = sDocument.getSDocumentGraph().getSTextualRelations();
		Hashtable<String,URI> textFileTable = new Hashtable<String,URI>();
		
		
		
		String docID = sDocument.getSName();
		int dsNumber = 1;
		for (STextualDS sText : sTextualDataSource){
			textFileTable.put(sText.getSId(), createPAULATextFile(sText.getSText(),docID+"."+dsNumber,documentPath,false));
		}
		
		createPAULATokenFile(sTextRels,textFileTable , docID,documentPath,false);
		
		//TODO:!done! read primary text from Salt file
		//TODO !done! check that parameters are not null and raise an exception if necessary
		//TODO map sDocument to PAULA and write files to documentPath
	}

	/**
	 * Writes the primary text sText to a file "documentID_text.xml" in the documentPath.
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
			output.println(xmlHead);
			output.println(paulaText);
			output.println(paulaOpenTag);
			output.println( new StringBuffer("\t<header paula_id=\"pepper.")
							.append(documentID)
							.append("_text\" type=\"text\"/>").toString());
			output.println("\t\t"+bodyOpen);
			output.println("\t\t\t" + sText);
			output.println("\t\t"+bodyClose);
			output.println(paulaCloseTag);			
		}	
		output.close();
		if (validate)
		  System.out.println( "File " + textFile.getCanonicalPath() + " is " + this.validateXMLOutput(textFile) );
		
		}catch (IOException e){
			System.out.println("Exception: "+ e.getMessage());
		}
		return(org.eclipse.emf.common.util.URI.createFileURI(textFile.getName()));		
	}
	
	
	/**
	 * Extracts the tokens including the xPointer from the STextualRelation list 
	 * and writes them to a file "documentID_tok.xml" in the documentPath.
	 * 
	 * UNDER HARD CONSTRUCTION!!!!
	 * 
	 * @param sTextRels 
	 * @param textFileTable 
	 * @param documentID 
	 * @param documentPath 
	 * @param validate
	 */
	private void createPAULATokenFile( EList<STextualRelation> sTextRels,
									   Hashtable<String, URI> textFileTable, 
									   String documentID,  
									   URI documentPath,
									   boolean validate) {
		
		if (sTextRels.isEmpty())
			throw new PAULAExporterException("Cannot create token files because there are no textual relations");
		if (documentID.equals(""))
			throw new PAULAExporterException("Cannot create token files because documentID is empty (\"\")");
		if (documentPath == null)
			throw new PAULAExporterException("Cannot create token files because documentPath is null");
		if (textFileTable == null)
			throw new PAULAExporterException("Cannot create token files because no textFileTable is defined" );
		
		//String markListOpenTag = "<markList xmlns:xlink=\"http://www.w3.org/1999/xlink\" type=\"tok\" xml:base=\""+documentID+".text.xml\">" ;
		String markListCloseTag = "</markList>";
		String markID,markTag,xPointerDef, markListOpenTag;
		String baseTextFile;
		String tokenHeader = "<header paula_id=\"pepper."+ documentID+"_tok\"/>";
		Hashtable<String,PrintWriter> tokenFileTable = new Hashtable<String,PrintWriter>(); 
		int tokenFileIndex = 0;
		File tokenFile;
		StringBuffer fileString = new StringBuffer();
		for (STextualRelation sTextualRelation : sTextRels){
			if (tokenFileTable.containsKey(sTextualRelation.getSTarget().getSId())){
				markID = "id=\""+sTextualRelation.getSToken().getSName()+"\"";
				xPointerDef = "xlink:href=\"#xpointer(string-range(//body,'',"
							+ (sTextualRelation.getSStart()+1) + "," 
							+ (sTextualRelation.getSEnd()-sTextualRelation.getSStart())
							+ "))\"";
				markTag = "\t\t<mark " + markID + " " + xPointerDef + " />";
				tokenFileTable.get(sTextualRelation.getSTarget().getSId()).
						println(markTag);
			} else {
				try {
				tokenFileIndex++;
				tokenFile = new File(documentPath.toFileString()+"/" + documentID + "."+ tokenFileIndex +".tok.xml");
				tokenFile.createNewFile();
				tokenFileTable.put(sTextualRelation.getSTarget().getSId(),
						new PrintWriter(new BufferedWriter(	new OutputStreamWriter(
										new FileOutputStream((tokenFile)),"UTF8")),
												true));
				tokenHeader = "<header paula_id=\"pepper."+ documentID+"."+tokenFileIndex+"_tok\"/>";
				
				baseTextFile = textFileTable.get(sTextualRelation.getSTarget().getSId()).toFileString();
				//baseTextFile = baseTextFile.substring(baseTextFile.lastIndexOf(File.pathSeparator)+1);
				markListOpenTag = "<markList xmlns:xlink=\"http://www.w3.org/1999/xlink\" type=\"tok\" xml:base=\""+baseTextFile.replace(tokenFile.getPath(),"")+"\">" ;
				fileString.append(xmlHead).append(lineSeparator)
				  .append(paulaMark).append(lineSeparator)
				  .append(paulaOpenTag).append(lineSeparator)
				  .append("\t").append(tokenHeader).append(lineSeparator)
				  .append("\t").append(markListOpenTag).append(lineSeparator);
				tokenFileTable.get(sTextualRelation.getSTarget().getSId()).write(fileString.toString());
				fileString.delete(0, fileString.length()+1);
				markID = "id=\""+sTextualRelation.getSToken().getSName()+"\"";
				xPointerDef = "xlink:href=\"#xpointer(string-range(//body,'',"
							+ (sTextualRelation.getSStart()+1) + "," 
							+ (sTextualRelation.getSEnd()-sTextualRelation.getSStart())
							+ "))\"";
				markTag = "\t\t<mark " + markID + " " + xPointerDef + " />";
				tokenFileTable.get(sTextualRelation.getSTarget().getSId()).
						println(markTag);
				
				} catch (IOException e){
					System.out.println("Exception: "+ e.getMessage());
				} 
			}
		}
		
		for (PrintWriter writer : tokenFileTable.values()){
			fileString.append("\t")
			  .append(markListCloseTag).append(lineSeparator)
			  .append(paulaCloseTag).append(lineSeparator);
			writer.write(fileString.toString());
			writer.close();
			//if (validate)
			//	  System.out.println( "File is " + this.validateXMLOutput(tokenFile) );
			fileString.delete(0, fileString.length()+1);
		}
		
		/*
		File tokenFile = new File(documentPath.toFileString()+"/" + documentID + ".tok.xml");
		StringBuffer fileString = new StringBuffer();
		try{
			tokenFile.createNewFile();
			PrintWriter output = new PrintWriter(
					new BufferedWriter(	new OutputStreamWriter(
									new FileOutputStream(tokenFile),"UTF8")),
									true);
			
			fileString.append(xmlHead).append(lineSeparator)
					  .append(paulaMark).append(lineSeparator)
					  .append(paulaOpenTag).append(lineSeparator)
					  .append("\t").append(tokenHeader).append(lineSeparator)
					  .append("\t").append(markListOpenTag).append(lineSeparator);
			
			output.write(fileString.toString());
			fileString.delete(0, fileString.length()+1);
			
			for (STextualRelation sTextualRelation : sTextRels){	
				//System.out.println(sTextualRelation.getSTarget().getSName());
				markID = "id=\""+sTextualRelation.getSToken().getSName()+"\"";
				xPointerDef = "xlink:href=\"#xpointer(string-range(//body,'',"
							+ (sTextualRelation.getSStart()+1) + "," 
							+ (sTextualRelation.getSEnd()-sTextualRelation.getSStart())
							+ "))\"";
				markTag = "\t\t<mark " + markID + " " + xPointerDef + " />";
				output.println(markTag);
			}
			
			 
			fileString.append("\t")
					  .append(markListCloseTag).append(lineSeparator)
					  .append(paulaCloseTag).append(lineSeparator);
			
			output.write(fileString.toString());
			
			output.close();
			if (validate)
			  System.out.println( "File " + tokenFile.getCanonicalPath() + " is " + this.validateXMLOutput(tokenFile) );
		}catch (IOException e){
			System.out.println("Exception: "+ e.getMessage());
		} finally {
			fileString = null;
		}
		*/
	}
	
	
	/**
	 * Checks whether the file is valid by the DTD which is noted in the file
	 * 
	 * @param fileToValidate
	 * @return "Valid!" if the fileToValidate matches the specified DTD
	 * 			"INVALID!" else
	 */
	private String validateXMLOutput(File fileToValidate) {
				
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
	         return "INVALID!";
	      } catch (SAXException e) {
	         System.out.println(e.getMessage());
	         return "INVALID!";
	      } catch (IOException e) {
	         System.out.println(e.getMessage());
	      }
	      return "Valid!";	 

		
	}
	
	
	
	  
}	
	

