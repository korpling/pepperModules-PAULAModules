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
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.log.LogService;

import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.exceptions.PAULAImporterException;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.readers.PAULASpecificReader;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.readers.PAULAStructReader;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.util.xPointer.XPtrInterpreter;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.util.xPointer.XPtrRef;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDominanceRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SPointingRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SSpan;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SSpanningRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SStructure;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SElementId;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SLayer;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SMetaAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SNode;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SRelation;

public class PAULA2SaltMapper
{	
	public PAULA2SaltMapper()
	{
		this.init();
	}
	
	private void init()
	{
		this.elementNamingTable= new Hashtable<String, String>();
		this.elementOrderTable= new Hashtable<String, Collection<String>>();
		this.stagingArea= new Hashtable<String, SElementId>();
	}
// ================================================ start: LogService	
	private LogService logService;

	public void setLogService(LogService logService) 
	{
		this.logService = logService;
	}
	
	public LogService getLogService() 
	{
		return(this.logService);
	}
// ================================================ end: LogService
// ================================================ start: physical path of the paula-documents
	/**
	 * Stores the physical path of the paula-documents.
	 */
	private URI currentPAULADocument= null;
	/**
	 * @param currentPAULADocument the currentPAULADocument to set
	 */
	public void setCurrentPAULADocument(URI currentPAULADocument) {
		this.currentPAULADocument = currentPAULADocument;
	}

	/**
	 * @return the currentPAULADocument
	 */
	public URI getCurrentPAULADocument() {
		return currentPAULADocument;
	}
	
// ================================================ end: physical path of the paula-documents
// ================================================ start: current SDocument	
	private SDocument currentSDocument= null;
	/**
	 * @param currentSDocument the currentSDocument to set
	 */
	public void setCurrentSDocument(SDocument currentSDocument) {
		this.currentSDocument = currentSDocument;
	}

	/**
	 * @return the currentSDocument
	 */
	public SDocument getCurrentSDocument() {
		return this.currentSDocument;
	}
// ================================================ end: current SDocument
// ================================================ start: handling PAULA-file endings
	/**
	 * Stores the endings which are used for paula-files
	 */
	private String[] PAULA_FILE_ENDINGS= null;
	/**
	 * @param pAULA_FILE_ENDINGS the pAULA_FILE_ENDINGS to set
	 */
	public void setPAULA_FILE_ENDINGS(String[] pAULA_FILE_ENDINGS) 
	{
		this.PAULA_FILE_ENDINGS = pAULA_FILE_ENDINGS;
	}

	/**
	 * @return the pAULA_FILE_ENDINGS
	 */
	public String[] getPAULA_FILE_ENDINGS() {
		return this.PAULA_FILE_ENDINGS;
	}
// ================================================ end: handling PAULA-file endings
	/**
	 * Maps the set current paulaDocument given by an URI, to the set current SDocument. 
	 */
	public void mapPAULADocument2SDocument()
	{
		if (this.getCurrentPAULADocument()== null)
			throw new PAULAImporterException("Cannot map a paula-document to SDocument, because the path for paula-document is empty.");
		if (this.getCurrentSDocument()== null)
			throw new PAULAImporterException("Cannot map a paula-document to SDocument, because the SDocument is empty.");
		if (	(this.getPAULA_FILE_ENDINGS()== null) ||
				(this.getPAULA_FILE_ENDINGS().length==0))
			throw new PAULAImporterException("Cannot map a paula-document to SDocument, no paula-xml-document endings are given.");
		
		{//create SDocumentGraph
			SDocumentGraph sDocGraph= SaltFactory.eINSTANCE.createSDocumentGraph();
			sDocGraph.setSName(this.currentSDocument.getSName()+"_graph");
			this.getCurrentSDocument().setSDocumentGraph(sDocGraph);
		}//create SDocumentGraph
		
		PAULAFileDelegator paulaFileDelegator= new PAULAFileDelegator();
		paulaFileDelegator.setLogService(this.getLogService());
		paulaFileDelegator.setMapper(this);
		File paulaPath= new File(this.getCurrentPAULADocument().toFileString());
		paulaFileDelegator.setPaulaPath(paulaPath);
		{//map all xml-documents
			for (File paulaFile: paulaPath.listFiles())
			{
				String[] parts= paulaFile.getName().split("[.]");
				if (parts.length> 1)
				{
					for (String ending: this.getPAULA_FILE_ENDINGS()) 
					{
						if (parts[parts.length-1].equalsIgnoreCase(ending))
						{
							paulaFileDelegator.getPaulaFiles().add(paulaFile);
						}
					}
				}
			}	
			paulaFileDelegator.startPaulaFiles();
		}//map all xml-documents
	}
// ======================================= start: staging area
	/**
	 * Stores the nodes and edges which has not been seen, but were refenced from other nodes.
	 * These nodes and edges are already in the SDocument-graph.
	 */
	private Hashtable<String, SElementId> stagingArea= null;
// ======================================= end: staging area
	/**
	 * global naming table for all elements stores paulaId of one element and corresponding salt id
	 * PAULAId, SaltId
	 */
	private Map<String, String> elementNamingTable= null;
	
	/**
	 * Seperator, to be used in uniquenames.
	 */
	private static final String KW_NAME_SEP="#";
	
	/**
	 * stores paula-document-names and corresponding paula-elements in readed order
	 * importent for interpreting xpointer (ranges)
	 */
	private Map<String, Collection<String>> elementOrderTable= null;
	
	/**
	 * Extracts the namespace of the paula file name and returns it.
	 * @param paulaFile the name of the paula file
	 * @return the namespace of the file.
	 */
	private String extractNSFromPAULAFile(File paulaFile)
	{
		String retVal= null;
		if (paulaFile!= null)
		{
			String[] parts= paulaFile.getName().split("[.]");
			if (parts[0]!= null)
				retVal= parts[0];
		}
		return(retVal);
	}
	
//	/**
//	 * Stores names of layers corresponding to the layer elements.
//	 */
//	private Hashtable<String, SLayer> layerName2SLayer= null;
	
	/**
	 * Attaches the given sNode to the sLayer, corresponding to the given layer name. If no Layer for this
	 * name exists, a new one will be created.
	 * @param sNode node which shall be attached
	 * @param sLayerName name of the SLayer 
	 */
	private SLayer attachSNode2SLayer(SNode sNode, String sLayerName)
	{
		SLayer retVal= null;
		
		{//search if layer already exists
			for (SLayer sLayer: this.getCurrentSDocument().getSDocumentGraph().getSLayers())
			{
				if (sLayer.getSName().equalsIgnoreCase(sLayerName))
				{
					retVal= sLayer;
					break;
				}
			}
		}//search if layer already exists
		
		if (retVal== null)
		{//create new layer if not exists
			retVal= SaltFactory.eINSTANCE.createSLayer();
			retVal.setSName(sLayerName);
			this.currentSDocument.getSDocumentGraph().getSLayers().add(retVal);
		}//create new layer if not exists
		
		//add sNode to sLayer
		retVal.getSNodes().add(sNode);
		
		return(retVal);
	}
	
	/**
	 * Attaches the given sRelation to the sLayer, corresponding to the given layer name. If no Layer for this
	 * name exists, a new one will be created.
	 * @param sNode node which shall be attached
	 * @param sLayerName name of the SLayer 
	 */
	private SLayer attachSRelation2SLayer(SRelation sRel, String sLayerName)
	{
		SLayer retVal= null;
		
		{//search if layer already exists
			for (SLayer sLayer: this.getCurrentSDocument().getSDocumentGraph().getSLayers())
			{
				if (sLayer.getSName().equalsIgnoreCase(sLayerName))
				{
					retVal= sLayer;
					break;
				}
			}
		}//search if layer already exists
		
		if (retVal== null)
		{//create new layer if not exists
			retVal= SaltFactory.eINSTANCE.createSLayer();
			retVal.setSName(sLayerName);
			this.currentSDocument.getSDocumentGraph().getSLayers().add(retVal);
		}//create new layer if not exists
		
		//add sNode to sLayer
		retVal.getSRelations().add(sRel);
		
		return(retVal);
	}
	
//=============================================== start: PAULA-connectors	
	/**
	 * Recieves data from PAULATextReader and maps them to Salt.
	 * @param corpusPath
	 * @param paulaFile
	 * @param paulaId
	 * @param text
	 * @throws Exception
	 */
	public void paulaTEXTConnector(	File paulaFile,
									String paulaId, 
									String text) 
	{
		if (this.getCurrentSDocument()== null)
			throw new PAULAImporterException("Cannot map primary data to salt document, because no salt document is given.");
		if (this.getCurrentSDocument().getSDocumentGraph()== null)
			throw new PAULAImporterException("Cannot map primary data to salt document, because no salt document-graph is given.");
		
		//create uniqueName
		String uniqueName= paulaFile.getName();
		STextualDS sTextualDS= null;
		{//check staging area
			if (this.stagingArea.containsKey(uniqueName))
			{//take node which already exists in SDocumentGraph
				sTextualDS=(STextualDS) this.getCurrentSDocument().getSDocumentGraph().getSNode(this.stagingArea.get(uniqueName).getSId());
			}//take node which already exists in SDocumentGraph
			else
			{//create new node for SDocument-graph
				//create element
				sTextualDS= SaltFactory.eINSTANCE.createSTextualDS();
				sTextualDS.setSName(uniqueName);
				this.getCurrentSDocument().getSDocumentGraph().addSNode(sTextualDS);
			}//create new node for SDocument-graph
		}//check staging area
		
		sTextualDS.setSText(text);
		//create entry in naming table
		this.elementNamingTable.put(uniqueName, sTextualDS.getSId());	
	}
	
	/**
	 * Recieves data from PAULAMarkReader in case of paula-type= tok and maps them to Salt.
	 * @param corpusPath
	 * @param paulaFile
	 * @param paulaId
	 * @param text
	 * @throws Exception
	 */
	public void paulaMARK_TOKConnector(	File paulaFile,
										String paulaId, 
										String paulaType,
										String xmlBase, 
										String markID, 
										String href, 
										String markType)
	{
//		System.out.println("tokDataConnector with data:\t"+
//							"paulaFile: "+ paulaFile.getName()+
//							", paulaID: " + paulaId + ", paulaType: " + paulaType +
//							", xmlBase: " + xmlBase + ", markID: "+ markID+
//							", href: "+ href + ", markType: "+ markType);
		//create unique name for element
		
		String uniqueName= paulaFile.getName() +KW_NAME_SEP + markID;
		{
			//create entry in element order table (file: elements)
			if (this.elementOrderTable.get(paulaFile.getName())== null)
			{
				Collection<String> orderedElementSlot= new Vector<String>();
				this.elementOrderTable.put(paulaFile.getName(), orderedElementSlot);
			}	
			Collection<String> orderedElementSlot= this.elementOrderTable.get(paulaFile.getName());
			orderedElementSlot.add(uniqueName);
		}
		
		//Objekt zum Interpretieren des XLinks in mark.href initialisieren
		Vector<XPtrRef> xPtrRefs= null;
		{//extract 
			XPtrInterpreter xPtrInter= new XPtrInterpreter();
			xPtrInter.setInterpreter(xmlBase, href);
			try
			{
				xPtrRefs=  xPtrInter.getResult();
			}
			catch (Exception e) 
			{
				throw new PAULAImporterException("Cannot read href ("+href+") in file "+this.getCurrentPAULADocument()+".");
			}
		}
		int runs= 0;
		//search for STextualDS
		STextualDS sTextDS= null;
		Long left= null;	//linke Textgrenze
		Long right= null;	//rechte Textgrenze
		for (XPtrRef xPtrRef: xPtrRefs)
		{
			runs++;
			//if there is more than one reference
			if (runs > 1) 
				throw new PAULAImporterException("There are too many references for a token node element: " + href);
			//when XPointer refers to a text 
			else if (xPtrRef.getType()== XPtrRef.POINTERTYPE.TEXT)
			{
				String textNodeName= this.elementNamingTable.get(xPtrRef.getDoc());
				sTextDS= (STextualDS) this.getCurrentSDocument().getSDocumentGraph().getSNode(textNodeName);
				try
				{
					left= new Long (xPtrRef.getLeft());
					right= new Long (xPtrRef.getRight());
					//arrange left and right value
					left= left-1;
					right= left + right;
					if (left > right)
						throw new PAULAImporterException("Cannot create token, because its left value is higher than its right value. Error in document "+ paulaFile.getName()+ ".");
					if (left < 0)
						throw new PAULAImporterException("Cannot create token, because its left value is smaller than 0. Error in document "+ paulaFile.getName()+ ".");
					if (right > sTextDS.getSText().length())
						throw new PAULAImporterException("Cannot create token, because its right value is higher than the size of the text. Error in document "+ paulaFile.getName()+ ".");
				}
				catch (Exception e)
				{throw new PAULAImporterException("The left or right border of XPointer is not set in a correct way: " + href, e);}
			}
			//when XPointer does not refer to a text 
			else 
				throw new PAULAImporterException("An XPointer of the parsed document does not refer to a xml-textelement. Incorrect pointer: " + "base: "+xPtrRef.getDoc() + ", element: " + href + ", type: "+ xPtrRef.getType());
		}
		//if no sTextDS exists-> error
		if (sTextDS == null) 
			throw new PAULAImporterException("No primary data node found for token element: " + paulaFile.getName() + KW_NAME_SEP + markID );
		
		//create SToken object
		SToken sToken= SaltFactory.eINSTANCE.createSToken();
		//sToken.setSName(markID);
		sToken.setSName(markID);
		this.currentSDocument.getSDocumentGraph().addSNode(sToken);
		
		{//adding sToken to layer
			String sLayerName= this.extractNSFromPAULAFile(paulaFile);
			this.attachSNode2SLayer(sToken, sLayerName);
		}//adding sToken to layer
		
		//create entry in naming table
		this.elementNamingTable.put(uniqueName, sToken.getSId());
		
		//create relation
		STextualRelation textRel= SaltFactory.eINSTANCE.createSTextualRelation();
		textRel.setSSource(sToken);
		textRel.setSTarget(sTextDS);
		textRel.setSStart(left.intValue());
		textRel.setSEnd(right.intValue());
		this.currentSDocument.getSDocumentGraph().addSRelation(textRel);
	}
	
	/**
	 * Returns a list of all paula-element-ids refered by the given xpointer-expression.
	 * @param xmlBase
	 * @param href
	 */
	private Collection<String> getPAULAElementIds(String xmlBase, String href)
	{
		Collection<String> refPaulaIds= null;
		try 
		{
			refPaulaIds= new Vector<String>();
			XPtrInterpreter xPtrInter= new XPtrInterpreter();
			xPtrInter.setInterpreter(xmlBase, href);
			//gehe durch alle Knoten, auf die sich dieses Element bezieht
			Vector<XPtrRef> xPtrRefs=  xPtrInter.getResult();
			for (XPtrRef xPtrRef: xPtrRefs)
			{
				//Fehler, wenn XPointer-Reference vom falschen Typ
				if (xPtrRef.getType()!= XPtrRef.POINTERTYPE.ELEMENT)
					throw new PAULAImporterException("The XPointer references in current file are incorrect. There only have to be element pointers and the following is not one of them: " + href+ ". Error in file: "+xmlBase);
				
				//wenn XPointer-Bezugsknoten einen Bereich umfasst
				if (xPtrRef.isRange())
				{
					//erzeuge den Namen des linken Bezugsknotens
					String leftName= xPtrInter.getDoc() +KW_NAME_SEP + xPtrRef.getLeft();
					//erzeuge den Namen des rechten Bezugsknotens
					String rightName= xPtrInter.getDoc() +KW_NAME_SEP + xPtrRef.getRight();
					//extract all paula elements which are refered by this pointer
					{
						
						boolean start= false;
						for (String paulaElementId : this.elementOrderTable.get(xPtrInter.getDoc()))
						{
							//if true, first element was found
							if (paulaElementId.equalsIgnoreCase(leftName))
								start= true;
							//if start of range is reached
							if (start)
							{
								refPaulaIds.add(paulaElementId);
							}
							//if last element was found, break
							if (paulaElementId.equalsIgnoreCase(rightName))
								break;
						}
					}
				}
				//wenn XPointer-Bezugsknoten einen einzelnen Knoten referenziert
				else
				{
					String paulaElementId= xPtrRef.getDoc() +KW_NAME_SEP + xPtrRef.getID();
					refPaulaIds.add(paulaElementId);
				}	
			}
		} catch (Exception e) 
		{
			e.printStackTrace();
			throw new PAULAImporterException("Cannot compute paula-ids corresponding to xmlBase '"+xmlBase+"' and href '"+href+"'.",e);
		}
		
		return(refPaulaIds);
	}
	
	/**
	 * Recieves data from PAULAMarkReader and maps them to Salt.
	 * @param corpusPath
	 * @param paulaFile
	 * @param paulaId
	 * @param text
	 * @throws Exception
	 */
	public void paulaMARKConnector(	File paulaFile,
									String paulaId, 
									String paulaType,
									String xmlBase, 
									String markID, 
									String href, 
									String markType)
	{
//		if (this.getLogService()!= null) 
//			this.getLogService().log(LogService.LOG_DEBUG,
//					MSG_STD +"markableDataConnector with data:\n"+MSG_STD +
//						"\tcorpusPath: "+ corpusPath+ ", paulaFile: "+ paulaFile.getName()+
//						", paulaID: " + paulaId + ", paulaType: " + paulaType +
//						", xmlBase: " + xmlBase + ", markID: "+ markID+
//						", href: "+ href +", markType: "+ markType);
		
		//create unique name for current node
		String uniqueName= paulaFile.getName() +KW_NAME_SEP + markID;
		{
			//create entry in element order table (file: elements)
			if (this.elementOrderTable.get(paulaFile.getName())== null)
			{
				Collection<String> orderedElementSlot= new Vector<String>();
				this.elementOrderTable.put(paulaFile.getName(), orderedElementSlot);
			}	
			Collection<String> orderedElementSlot= this.elementOrderTable.get(paulaFile.getName());
			orderedElementSlot.add(uniqueName);
		}
		//create list of all refered elements
		Collection<String> refPAULAElementIds= this.getPAULAElementIds(xmlBase, href);
		
		EList<SNode> referedElements= new BasicEList<SNode>();
		for (String refPAULAId: refPAULAElementIds)
		{
			String paulaIdEntry= this.elementNamingTable.get(refPAULAId);
			if (paulaIdEntry== null)
				throw new PAULAImporterException("Cannot map the markable '"+markID+"' of file '"+paulaId+"', because the reference '"+refPAULAId+"'does not exist.");
			SNode dstElement= this.getCurrentSDocument().getSDocumentGraph().getSNode(paulaIdEntry);
			if (dstElement== null)
			{
				if (this.getLogService()!= null) 
					this.getLogService().log(LogService.LOG_WARNING, "Cannot create span, because destination does not exist in graph: "+ refPAULAId+ ". Error in file: "+this.getCurrentPAULADocument().toFileString());
			}
			else referedElements.add(dstElement);
		}
		//if list of refered elements is empty, don�t put relation or referncing element in graph
		if (referedElements.size()== 0)
		{
			if (this.getLogService()!= null) 
				this.getLogService().log(LogService.LOG_WARNING, "Cannot create span, because it has no destination elements: "+ uniqueName+ ". Error in file: "+this.getCurrentPAULADocument().toFileString());
		}
		else
		{

			//create span element
			SSpan sSpan= SaltFactory.eINSTANCE.createSSpan();
//			sSpan.setSName(markID);
			this.getCurrentSDocument().getSDocumentGraph().addSNode(sSpan);
			
			{//adding sSpan to layer
				String sLayerName= this.extractNSFromPAULAFile(paulaFile);
				this.attachSNode2SLayer(sSpan, sLayerName);
			}//adding sSpan to layer
			
			//create entry in naming table
			this.elementNamingTable.put(uniqueName, sSpan.getSId());
			
			//create relations for all referenced tokens
			SSpanningRelation sSpanRel= null;
			for (String refPAULAId: refPAULAElementIds)
			{
				SNode dstNode= this.getCurrentSDocument().getSDocumentGraph().getSNode(this.elementNamingTable.get(refPAULAId));
				if (dstNode== null)
				{
					if (this.getLogService()!= null)
						this.getLogService().log(LogService.LOG_WARNING, "Cannot create span, because destination does not exist in graph: "+ refPAULAId+ ". Error in file: "+this.getCurrentPAULADocument().toFileString());
				}
				else
				{	if (!(dstNode instanceof SToken))
						throw new PAULAImporterException("The referred Target Node '"+refPAULAId+"' in document '"+xmlBase+"'is not of type SToken.");
					sSpanRel= SaltFactory.eINSTANCE.createSSpanningRelation();
					sSpanRel.setSSource(sSpan);
					sSpanRel.setSTarget(dstNode);
					this.getCurrentSDocument().getSDocumentGraph().addSRelation(sSpanRel);
					{//adding sSpanRel to layer
						String sLayerName= this.extractNSFromPAULAFile(paulaFile);
						this.attachSRelation2SLayer(sSpanRel, sLayerName);
					}//adding sSpanRel to layer
				}
			}
		}
	}
	
	private static final String KW_FILE_VAL="file:/";
	/**
	 * Recieves data from PAULAFeatReader and maps them to Salt.
	 * @param corpusPath
	 * @param paulaFile
	 * @param paulaId
	 * @param text
	 * @throws Exception
	 */
	public void paulaFEATConnector(	File 	paulaFile,
									String 	paulaId, 
									String 	paulaType,
									String 	xmlBase,
									String 	featID,
									String 	featHref,
									String 	featTar,
									String 	featVal,
									String 	featDesc,
									String 	featExp)
	{
		//if (DEBUG_ANNO_DATA)
//		System.out.println("annoDataConnector with data:\n"+
//				"paulaFile: "+ paulaFile.getName()+
//				", paulaID: " + paulaId + ", paulaType: " + paulaType +
//				", xmlBase: " + xmlBase + ", featID: "+ featID+
//				", featHref: "+ featHref +", featTar: "+ featTar+
//				", featVal: "+ featVal + ", featDesc: "+ featDesc+
//				", featExp: " +featExp);
		
		if ((paulaType== null)|| (paulaType.isEmpty()))
		{
			if (this.getLogService()!= null)	
				this.getLogService().log(LogService.LOG_WARNING, "Cannot work with the given annotation of element: "+paulaId+", because the type-value is empty. Error in file: "+paulaFile+".");			
		}
		else
		{	
			if ((featVal== null) || (featVal.isEmpty())) 
			{	
				if (this.getLogService()!= null)	
						this.getLogService().log(LogService.LOG_WARNING, "The value-value of element: "+paulaId+" is empty. Error in file: "+paulaFile+".");
			}
			
			Collection<String> paulaElementIds= this.getPAULAElementIds(xmlBase, featHref);
			SAnnotation sAnno= SaltFactory.eINSTANCE.createSAnnotation();
      
			if ((paulaType!= null) && (!paulaType.isEmpty()))
			{
				//extract type name and namespace
				String[] parts= paulaType.split("[.]");
				if (	(parts!= null)&&
						(parts.length>0))
					sAnno.setSName(parts[parts.length-1]);
				if (	(parts!= null)&&
						(parts.length>1))
				{//namespace exists
					String namespace= "";
					for (int i=0; i<parts.length-1; i++)
					{
						if (i==0)
							namespace= parts[0];
						else namespace= namespace +"."+parts[i];
						i++;
					}
					sAnno.setSNS(namespace);
				}//namespace exists
				else
				{//compute namespace from file name
					String annoNamespace = this.extractNSFromPAULAFile(paulaFile);
					if(annoNamespace != null && !annoNamespace.isEmpty())
					{
						sAnno.setSNS(annoNamespace);
					}
				}//compute namespace from file name
								
				//check wether annotation value is string or URI
				if (	(featVal.length() >= KW_FILE_VAL.length()) &&
						(featVal.substring(0, KW_FILE_VAL.length()) .equalsIgnoreCase(KW_FILE_VAL)))
				{//featVal starts with file:/, it is an reference to an external file
					URI uri= URI.createURI(featVal);
					File file= new File(paulaFile.getParentFile()+"/"+uri.path());
					sAnno.setSValue(URI.createFileURI(file.getAbsolutePath()));
				}//featVal starts with file:/, it is an reference to an external file
				else
				{	
					sAnno.setSValue(featVal);
				}
			}
			for (String paulaElementId: paulaElementIds)
			{
				if ((paulaElementId== null) || (paulaElementId.isEmpty()))
					throw new PAULAImporterException("No element with xml-id:"+ paulaElementId+ " was found.");
				String sElementName= this.elementNamingTable.get(paulaElementId);
			 	if (sElementName== null)
				{
					this.getLogService().log(LogService.LOG_WARNING,"An element was reffered by an annotation, which does not exist in paula file. The missing element is '"+paulaElementId+"' and it was refferd in file'"+paulaFile.getAbsolutePath()+"'.");
				}
				else
				{	
					SNode refElement= this.getCurrentSDocument().getSDocumentGraph().getSNode(sElementName);
					SRelation refRelation= this.getCurrentSDocument().getSDocumentGraph().getSRelation(sElementName);
					if (refElement!= null)
					{
						try 
						{
							refElement.addSAnnotation(sAnno);
						} catch (Exception e) 
						{
							if (this.getLogService()!= null)	
								this.getLogService().log(LogService.LOG_WARNING, "Exception in paula file: "+this.getCurrentPAULADocument().toFileString()+" at element: "+featHref+". Original message is: "+e.getMessage());
						}
					}	
					else if(refRelation!= null)
					{
						refRelation.addSAnnotation(sAnno);
					}
					else
						{throw new PAULAImporterException("No element with xml-id:"+ paulaElementId+ " was found.");}
				}
			}
		}
	}
	
	/**
	 * Recieves data from PAULARelReader and maps them to Salt.
	 * @param corpusPath
	 * @param paulaFile
	 * @param paulaId
	 * @param text
	 * @throws Exception
	 */
	public void paulaRELConnector(	File 	paulaFile,
									String 	paulaId, 
									String 	paulaType,
									String 	xmlBase,
									String	relId,
									String 	srcHref,
									String 	dstHref)
	{
//		System.out.println(	"pointingRelDataConnector with data:\n"+
//		", paulaFile: "+ paulaFile.getName()+
//		", paulaID: " + paulaId + ", paulaType: " + paulaType +
//		", xmlBase: " + xmlBase + ", relId: "+ relId+
//		", srcHref: "+ srcHref + ", dstHref: "+ dstHref);
		
		if (	(	(srcHref== null) || (srcHref.isEmpty())) ||
				(	(dstHref== null) || (dstHref.isEmpty())) ||
				(	(srcHref.equalsIgnoreCase("empty") || (dstHref.equalsIgnoreCase("empty")))))
		{
			if (this.getLogService()!= null) 
				this.getLogService().log(LogService.LOG_WARNING, "Cannot create pointing relation of file ("+paulaFile.getName()+"), because source or destination is empty (see element '"+relId+"').");
		}
		else
		{	
			if (srcHref.equalsIgnoreCase(dstHref))
				this.getLogService().log(LogService.LOG_WARNING, "Cannot create the pointing relation '"+srcHref+"' to '"+dstHref+"' in document '"+currentSDocument.getSId()+"', because it is a cycle. The cycle was found in file ("+paulaFile.getName()+").");
			else
			{	
				Collection<String> paulaSrcElementIds= this.getPAULAElementIds(xmlBase, srcHref);
				Collection<String> paulaDstElementIds= this.getPAULAElementIds(xmlBase, dstHref);
				if ((paulaSrcElementIds== null) || (paulaSrcElementIds.size()== 0))
					throw new PAULAImporterException("The source of pointing relation in file: "+paulaFile.getName() +" is not set.");
				if ((paulaDstElementIds== null) || (paulaDstElementIds.size()== 0))
					throw new PAULAImporterException("The destination of pointing relation in file: "+paulaFile.getName() +" is not set.");
				//if there are more than one sources or destinations create cross product
				for (String paulaSrcElementId: paulaSrcElementIds)
				{
					for (String paulaDstElementId: paulaDstElementIds)
					{
						String saltSrcName= this.elementNamingTable.get(paulaSrcElementId);
						String saltDstName= this.elementNamingTable.get(paulaDstElementId);
						if ((saltSrcName== null) || (saltSrcName.isEmpty()))
						{
							if (this.getLogService()!= null)
								this.getLogService().log(LogService.LOG_WARNING, "The requested source of relation (xml-id: "+paulaSrcElementId+") of file '"+paulaFile.getName()+"' does not exist.");
							return;
						}
						SPointingRelation pRel= SaltFactory.eINSTANCE.createSPointingRelation();
						//SDominanceRelation pRel= SaltFactory.eINSTANCE.createSDominanceRelation();
						if ((saltDstName== null) || (saltDstName.isEmpty()))
						{
							if (this.getLogService()!= null)
								this.getLogService().log(LogService.LOG_WARNING, "The requested destination of relation (xml-id: "+paulaDstElementId+") of file '"+paulaFile.getName()+"' does not exist.");
							return;
						}
						pRel.addSType(paulaType);
						pRel.setSSource(this.getCurrentSDocument().getSDocumentGraph().getSNode(saltSrcName));
						pRel.setSTarget(this.getCurrentSDocument().getSDocumentGraph().getSNode(saltDstName));
						this.getCurrentSDocument().getSDocumentGraph().addSRelation(pRel);
						//adding sSpanRel to layer
							String sLayerName= this.extractNSFromPAULAFile(paulaFile);
							this.attachSRelation2SLayer(pRel, sLayerName);
						//adding sSpanRel to layer
	
						//write SPointingRelation in elementNamingTable, to map it with its paula id
							String uniqueName= paulaFile.getName() +KW_NAME_SEP + relId; 
							this.elementNamingTable.put(uniqueName, pRel.getSElementId().getSId());
						//write SPointingRelation in elementNamingTable, to map it with its paula id
					}
				}
			}
		}
	}
	
	/**
	 * Recieves data from PAULAFeatReader and maps them to Salt. To call in case of feats for corpus 
	 * or document.
	 * @param corpusPath
	 * @param paulaFile
	 * @param paulaId
	 * @param text
	 * @throws Exception
	 */
	public void paulaFEAT_METAConnector(	File 	paulaFile,
											String 	paulaId, 
											String 	paulaType,
											String 	xmlBase,
											String 	featID,
											String 	featHref,
											String 	featTar,
											String 	featVal,
											String 	featDesc,
											String 	featExp)
	{
//		System.out.println(	"metaAnnoDataConnector with data:\t"+
//								"paulaFile: "+ paulaFile.getName()+
//								", paulaID: " + paulaId + ", paulaType: " + paulaType +
//								", xmlBase: " + xmlBase + ", featHref: "+ featHref+
//								", featVal: "+ featVal);
		
		if ((paulaType== null) || (paulaType.isEmpty()))
		{
			if (this.getLogService()!= null) 
				this.getLogService().log(LogService.LOG_WARNING, "Cannot add the given meta-annotation, because no annotation name is given.");
			return;
		}
		//creates a fullName for this meta annotation
		String fullName= paulaType;
		if (this.getCurrentSDocument().getSMetaAnnotation(fullName)== null)
		{
			SMetaAnnotation anno= null;
			anno= SaltFactory.eINSTANCE.createSMetaAnnotation();
			anno.setSName(fullName);
			anno.setSValue(featVal);
			this.getCurrentSDocument().addSMetaAnnotation(anno);
		}
	}
	
	/**
	 * Needed for storing dominance relations out of paula-struct-documents. 
	 * Pre-Storing is necassary, because of struct-elements can refer to other struct-elements
	 * which aren�t read at this time. Therefore the relations can be stored after reading all 
	 * elements.
	 * @author Florian Zipser
	 *
	 */
	private static class DominanceRelationContainer
	{
		public String paulaId= null;
		public SDominanceRelation relation= null;
		public String xmlBase= null;
		public String href= null;
	}
	
	private Hashtable<File, EList<DominanceRelationContainer>> dominanceRelationContainers= null;
	
	/**
	 * Recieves data from PAULAStrcutReader and maps them to Salt. T
	 * @param corpusPath
	 * @param paulaFile
	 * @param paulaId
	 * @param text
	 * @throws Exception
	 */
	public void paulaSTRUCTConnector(	File 	paulaFile,
										String 	paulaId, 
										String 	paulaType,
										String 	xmlBase,
										String 	structID,
										String	relID,
										String	relHref,
										String	relType)
	{
//		System.out.println("structEdgeDataConnector with data:\n"+
//								"paulaFile: "+ paulaFile.getName()+
//								", paulaID: " + paulaId + ", paulaType: " + paulaType +
//								", xmlBase: " + xmlBase + ", structID: "+ structID+
//								", relID: "+ relID +", relHref: "+ relHref + ", relType: "+ relType);
		//create unique name for element
		String uniqueNameStruct= paulaFile.getName() +KW_NAME_SEP + structID;
		String uniqueNameRel= paulaFile.getName() +KW_NAME_SEP + relID;
		{//compute xml-base if given is empty
			if (	(xmlBase== null)||
					(xmlBase.isEmpty()))
			{
				//if xml-base is empty, than set xml-base to current processed paula-file
				xmlBase= paulaFile.getName();
			}
		}//compute xml-base if given is empty
		{
			//create entry in element order table (file: elements)
			if (this.elementOrderTable.get(paulaFile.getName())== null)
			{
				Collection<String> orderedElementSlot= new Vector<String>();
				this.elementOrderTable.put(paulaFile.getName(), orderedElementSlot);
			}	
			//check if struct is already inserted
			Collection<String> orderedElementSlot= this.elementOrderTable.get(paulaFile.getName());
			if (!orderedElementSlot.contains(uniqueNameStruct))
			{	
				orderedElementSlot.add(uniqueNameStruct);
			}
			if (!orderedElementSlot.contains(uniqueNameRel))
			{	
				orderedElementSlot.add(uniqueNameRel);
			}
		}
		
		if (this.elementNamingTable.get(uniqueNameStruct)== null)
		{	
			//create struct element
			SStructure sStruct= SaltFactory.eINSTANCE.createSStructure();
			sStruct.setSName(structID);
			
			//sStruct.setId(structID); //not possible, because these id�s are not unique for one document file+id is unique but long
			this.getCurrentSDocument().getSDocumentGraph().addSNode(sStruct);
			
			{//adding sStruct to layer
				String sLayerName= this.extractNSFromPAULAFile(paulaFile);
				this.attachSNode2SLayer(sStruct, sLayerName);
			}//adding sStruct to layer
			
			//create entry in naming table for struct		
			this.elementNamingTable.put(uniqueNameStruct, sStruct.getSId());
		}
		
		//pre creating relation
		SDominanceRelation domRel= SaltFactory.eINSTANCE.createSDominanceRelation();
		String saltDstName= this.elementNamingTable.get(uniqueNameStruct);
		domRel.setSSource(this.getCurrentSDocument().getSDocumentGraph().getSNode(saltDstName));
		if ((relType!= null) && (!relType.isEmpty()))
		{
			domRel.addSType(relType);
		}
		
		//creating new container list
		if (dominanceRelationContainers== null)
			dominanceRelationContainers= new Hashtable<File, EList<DominanceRelationContainer>>();
		
		EList<DominanceRelationContainer> domRelSlot= null;
		domRelSlot= dominanceRelationContainers.get(paulaFile);
		if (domRelSlot== null)
		{
			domRelSlot= new BasicEList<DominanceRelationContainer>();
			this.dominanceRelationContainers.put(paulaFile, domRelSlot);
		}
		
		//creating dominance relation container
		DominanceRelationContainer domCon= new DominanceRelationContainer();
		domCon.paulaId= uniqueNameRel;
		domCon.relation= domRel;
		domCon.xmlBase= xmlBase;
		domCon.href= relHref;
		domRelSlot.add(domCon);
	}
	
	/**
	 * Will be called at the end of xml-document processing. In case of paula file was of type struct.dtd,
	 * all nodes and relations will now be added to graph.
	 */
	public void endDocument(	PAULASpecificReader paulaReader,
								File paulaFile)
	{
		if (paulaReader instanceof PAULAStructReader)
		{//if PAULAReader is PAULAStructReader	
			//storing dominance relations in graph
			if (dominanceRelationContainers!= null)
			{
				for (DominanceRelationContainer domCon: dominanceRelationContainers.get(paulaFile))
				{
					Collection<String> refPAULAElementIds= this.getPAULAElementIds(domCon.xmlBase, domCon.href);
					for (String refPAULAId: refPAULAElementIds)
					{
						String sNodeName= this.elementNamingTable.get(refPAULAId);
						if (sNodeName== null)
							throw new PAULAImporterException("An element is referred, which was not already read. The reffered element is '"+refPAULAId+"' and it was reffered in file '"+paulaFile+"'.");
						SNode dstNode= this.getCurrentSDocument().getSDocumentGraph().getSNode(sNodeName);
						if (dstNode== null)
							throw new PAULAImporterException("No paula element with name: "+ refPAULAId + " was found.");
						domCon.relation.setSTarget(dstNode);
						this.getCurrentSDocument().getSDocumentGraph().addSRelation(domCon.relation);
						{//adding sSpanRel to layer
							String sLayerName= this.extractNSFromPAULAFile(paulaFile);
							this.attachSRelation2SLayer(domCon.relation, sLayerName);
						}//adding sSpanRel to layer
						//create entry in naming table for struct
						if (this.elementNamingTable.get(domCon.paulaId)== null)
						{
							this.elementNamingTable.put(domCon.paulaId, domCon.relation.getSId());
						}
					}
				}
				dominanceRelationContainers= null;
			}
		}//if PAULAReader is PAULAStructReader
	}
//=============================================== end: PAULA-connectors
}
