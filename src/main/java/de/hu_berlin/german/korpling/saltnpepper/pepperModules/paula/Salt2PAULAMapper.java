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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.hu_berlin.german.korpling.saltnpepper.pepper.common.DOCUMENT_STATUS;
import de.hu_berlin.german.korpling.saltnpepper.pepper.exceptions.PepperFWException;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.PepperImporter;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.exceptions.PepperModuleException;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.impl.PepperMapperImpl;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.Edge;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.modules.SDocumentStructureAccessor;
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
 * Maps SCorpusGraph objects to a folder structure and maps a SDocumentStructure
 * to the necessary files containing the document data in PAULA notation.
 * 
 * @author Mario Frank
 * 
 */

public class Salt2PAULAMapper extends PepperMapperImpl implements PAULAXMLDictionary, FilenameFilter {
	private static final Logger logger = LoggerFactory.getLogger(Salt2PAULAMapper.class);

	/**
	 * String to be used for namespaces having no layer.
	 */
	public static final String NO_LAYER = "nolayer";

	/**
	 * infix for paula files to determine that file is a text file.
	 */
	public static final String PAULA_INFIX_TEXT = "text";

	private static URI resourcePath = null;

	private PAULAExporter exporter = null;

	public void setPAULAExporter(PAULAExporter ex) {
		this.exporter = ex;
	}

	public PAULAExporter getPAULAExporter() {
		return this.exporter;
	}

	/**
	 * {@inheritDoc PepperMapper#setSDocument(SDocument)}
	 * 
	 * OVERRIDE THIS METHOD FOR CUSTOMIZED MAPPING.
	 */
	@Override
	public DOCUMENT_STATUS mapSDocument() {
		if (getSDocument() == null)
			throw new PepperModuleException(this, "Cannot export document structure because sDocument is null");

		if (this.getResourceURI() == null)
			throw new PepperModuleException(this, "Cannot export document structure because documentPath is null for '" + this.getSDocument().getSElementId() + "'.");

		// copy DTD-files to output-path
		if (resourcePath != null) {
			File DTDDirectory = new File(resourcePath.toFileString() + "/" + "dtd_09/");
			if ((DTDDirectory.exists()) && (DTDDirectory.listFiles(this) != null)) {
				for (File DTDFile : DTDDirectory.listFiles(this)) {
					copyFile(URI.createFileURI(DTDFile.getAbsolutePath()), this.getResourceURI().toFileString());
				}
			} else {
				logger.warn("Cannot copy dtds fom resource directory, because resource directory '" + DTDDirectory.getAbsolutePath() + "' does not exist.");
			}
		} else {
			logger.warn("There is no reference to a resource path!");
		}
		if (getSDocument().getSDocumentGraph() == null) {
			getSDocument().setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		}
		EList<STextualDS> sTextualDataSources = getSDocument().getSDocumentGraph().getSTextualDSs();
		// create a Hashtable(SName,FileName) with initial Size equal to the
		// number of Datasources
		Hashtable<String, String> dataSourceFileTable = new Hashtable<String, String>(sTextualDataSources.size());

		String documentName = getSDocument().getSName();
		// map textual data sources
		dataSourceFileTable = mapTextualDataSources(sTextualDataSources, documentName, this.getResourceURI());
		// name of the first data source
		String oneDS = sTextualDataSources.get(0).getSName();
		// map all layers
		try {
			mapLayers(getSDocument().getSDocumentGraph(), this.getResourceURI(), documentName, dataSourceFileTable, oneDS);
		} catch (XMLStreamException e) {
			throw new PepperModuleException(this,"A problem occured while writing to xml file. ",e);
		}
		return (DOCUMENT_STATUS.COMPLETED);
	}
	/**
	 * A factory to create {@link XMLStreamWriter} objects.
	 */
	private XMLOutputFactory xmlFactory = XMLOutputFactory.newFactory();
	/**
	 * Writes the primary text sText to a file "documentID_text.xml" in the
	 * documentPath and returns the URI (filename).
	 * 
	 * @param sTextualDSs
	 *            the primary text
	 * @param documentID
	 *            the document id
	 * @param documentPath
	 *            the document path
	 * @return Hashtable&lt;STextualDS SName,TextFileName&gt;
	 */
	private Hashtable<String, String> mapTextualDataSources(EList<STextualDS> sTextualDSs, String documentID, URI documentPath) {

		if (sTextualDSs.isEmpty())
			throw new PepperModuleException(this, "Cannot map Data Sources because there are none");
		if (documentID.isEmpty())
			throw new PepperModuleException(this, "Cannot map Data Sources because documentID is empty (\"\")");
		if (documentPath == null)
			throw new PepperModuleException(this, "Cannot map Data Sources because documentPath is null");

		File textFile;
		int dsNum = sTextualDSs.size();
		// Hashtable <DataSourceSName,PrintWriter>
		Hashtable<String, PrintWriter> sTextualDSWriterTable = new Hashtable<String, PrintWriter>(dsNum);
		// Hashtable <DataSourceSName,fileName>
		Hashtable<String, String> sTextualDSFileTable = new Hashtable<String, String>(dsNum);
		String layer = "";
		int i = 0;
		/**
		 * Iterate over all Textual Data Sources
		 */
		for (STextualDS sTextualDS : sTextualDSs) {
			if (sTextualDS.getSLayers() != null && sTextualDS.getSLayers().size() != 0) {
				layer = sTextualDS.getSLayers().get(0).getSName() + ".";
			}
			/**
			 * If there is one DS, create one non-numerated text file, else
			 * numerate
			 */
			if (dsNum == 1) {
				textFile = new File(documentPath.toFileString() + "/" + layer + documentID + "." + PAULA_INFIX_TEXT + "." + PepperImporter.ENDING_XML);
			} else {
				textFile = new File(documentPath.toFileString() + "/" + layer + documentID + "." + PAULA_INFIX_TEXT + "." + (i + 1) + "." + PepperImporter.ENDING_XML);
			}
			PrintWriter output = null;
			try {
				if (!textFile.createNewFile()) {
					logger.warn("File: " + textFile.getName() + " already exists");
				}
				output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(textFile), "UTF8")), false);
				XMLStreamWriter xmlWriter = xmlFactory.createXMLStreamWriter(output);
				/**
				 * put the PrintWriter into WriterTable for further access put
				 * the SName and FileName into FileTable for Token file
				 * construction
				 */
				sTextualDSWriterTable.put(sTextualDS.getSName(), output);
				sTextualDSFileTable.put(sTextualDS.getSName(), textFile.getName());

				//Write the Text file content
				xmlWriter.writeStartDocument();
				xmlWriter.writeDTD(PAULA_TEXT_DOCTYPE_TAG);
				xmlWriter.writeStartElement(TAG_PAULA);
					xmlWriter.writeAttribute(ATT_VERSION, VERSION);
					xmlWriter.writeStartElement(TAG_HEADER);
						if (dsNum == 1) {
							xmlWriter.writeAttribute(ATT_PAULA_ID, layer+documentID+"."+PAULA_INFIX_TEXT);
						} else {
							xmlWriter.writeAttribute(ATT_PAULA_ID, "merged."+documentID+"." + PAULA_INFIX_TEXT);
						}
						xmlWriter.writeAttribute(ATT_TYPE, PAULA_INFIX_TEXT);
					xmlWriter.writeEndElement();
					xmlWriter.writeStartElement(TAG_TEXT_BODY);
						xmlWriter.writeCharacters(StringEscapeUtils.escapeXml(sTextualDS.getSText()));
					xmlWriter.writeEndElement();
				xmlWriter.writeEndElement();
				
				i++;
			} catch (IOException e) {
				throw new PepperModuleException(this, "MapTextualDataSources: could not map to file " + textFile.getName() + " . Cause: ", e);
			} catch (XMLStreamException e) {
				throw new PepperModuleException(this, "Cannot create output stream for primary text '"+sTextualDS+"'. ",e);
			}
			finally {
				if (output != null)
					output.close();
			}
		}
		// dispose PrintWriter table
		sTextualDSWriterTable = null;

		if (this.getProperties() == null)
			throw new PepperFWException("No customization property object was given. This might be a bug in pepper module.");
		if (((PAULAExporterProperties) this.getProperties()).getIsValidate()) {
			for (String fileName : sTextualDSFileTable.values()) {
				logger.warn("XML-Validation: " + fileName + " is valid: " + isValidXML(new File(documentPath.toFileString() + "/" + fileName)));
			}
		}
		return sTextualDSFileTable;
	}

	/**
	 *  Hashtables containing a mapping, between node ids of nodes: tok/span/struct and
	 *  the corresponding filename, where they are stored in.
	 */
	private Map<SElementId, String> nodeFileMap = new Hashtable<SElementId, String>();
	
	/**
	 * 
	 * Map the layers of the document graph including token, spans and structs
	 * to files. Invokes methods for mapping spans, structs, spans, pointing
	 * relations, and meta annotations.
	 * 
	 * @param sDocumentGraph
	 *            the document graph
	 * @param documentPath
	 *            the base document path
	 * @param documentId
	 *            the document name
	 * @param fileTable
	 *            the data source file table &lt;DSName, containingFile&gt;
	 * @param firstDSName
	 *            the first data source
	 * @return a table with all nodes (structs/spans/tokens) and the containing
	 *         files
	 * @throws XMLStreamException 
	 */
	private void mapLayers(SDocumentGraph sDocumentGraph, URI documentPath, String documentId, Hashtable<String, String> fileTable, String firstDSName) throws XMLStreamException {

		if (sDocumentGraph == null)
			throw new PepperModuleException(this, "Cannot map Layers because document graph is null");
		if (documentPath.isEmpty())
			throw new PepperModuleException(this, "Cannot map Layers because documentPath is empty (\"\")");
		if (documentId.isEmpty())
			throw new PepperModuleException(this, "Cannot map Layers because documentID is empty (\"\")");
		if (fileTable == null)
			throw new PepperModuleException(this, "Cannot map Layers because fileTable is null");
		if (firstDSName.isEmpty())
			throw new PepperModuleException(this, "Cannot map Layers because no first Data source name is specified");

//		SDocumentStructureAccessor accessor = new SDocumentStructureAccessor();
//		accessor.setSDocumentGraph(sDocumentGraph);

		// Copy the spans and structs. By doing this, we can assure later that we found all spans/structs
		EList<SSpan> spanList = new BasicEList<SSpan>(sDocumentGraph.getSSpans());
		EList<SStructure> structList = new BasicEList<SStructure>(sDocumentGraph.getSStructures());

		
		Set<String> layerNodeFileNames = Collections.synchronizedSet(new HashSet<String>());

		// Lists for all constructs we may find in one layer
		EList<SSpan> layerSpanList;
		EList<SStructure> layerStructList;
		EList<SToken> layerTokenList;
		EList<SPointingRelation> layerPointingRelationList;
		EList<SPointingRelation> pointingRelationList = new BasicEList<SPointingRelation>(sDocumentGraph.getSPointingRelations());
		EList<STextualRelation> layerTextualRelationList;
		EList<STextualDS> layerTextualDS;
		EList<STextualRelation> textualRelationList = sDocumentGraph.getSTextualRelations();
		// Port lists for later paula versions allowing to handle structured elements belonging to multiple layers
		EList<SSpan> multiLayerSpanList = new BasicEList<SSpan>();
		EList<SStructure> multiLayerStructList = new BasicEList<SStructure>();
		EList<SToken> multiLayerTokenList = new BasicEList<SToken>();
		//create files and PrintWriters for annoSet and annoFeat
		File annoSetFile = new File(documentPath.toFileString() + "/" + documentId + ".anno.xml");
		File annoFeatFile = new File(documentPath.toFileString() + "/" + documentId + ".anno_feat.xml");
		XMLStreamWriter annoSetWriter=null;
		XMLStreamWriter annoFeatWriter= null;
		PrintWriter annoSetOutput = null;
		PrintWriter annoFeatOutput = null;
		
		try {
			if (!annoSetFile.exists()) {
				if (!(annoSetFile.createNewFile()))
					logger.warn("Cannot create file '" + annoSetFile.getName() + "', because it already exists.");
			}
			if (!annoFeatFile.exists()) {
				if (!(annoFeatFile.createNewFile()))
					logger.warn("Cannot create file '" + annoFeatFile.getName() + "', because it already exists.");
			}
			annoSetOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(annoSetFile), "UTF8")), true);
			annoSetWriter= xmlFactory.createXMLStreamWriter(annoSetOutput);
			annoFeatOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(annoFeatFile), "UTF8")), true);
			annoFeatWriter= xmlFactory.createXMLStreamWriter(annoFeatOutput);
		} catch (IOException e) {
			throw new PepperModuleException(this,"Cannot create an output stream for annoSet or annoFeat file", e);
		} catch (XMLStreamException e) {
			throw new PepperModuleException(this,"Cannot create an output stream for annoSet or annoFeat file", e);
		}
		// Write the annoSet file beginning
		int j = 0;
		createFileBeginning(PAULA_TYPE.STRUCT, documentId + ".anno", "annoSet", null, annoSetWriter);
		
		int i = 1;

		EList<STextualDS> nolayerSTextualDS = null;
		//map the datasource filenames to rel tags (anno_0) for all datasources which are not in one layer
		for (STextualDS sTextualDS : sDocumentGraph.getSTextualDSs()) {
			if (sTextualDS.getSLayers() == null || sTextualDS.getLayers().size() == 0) {
				if (nolayerSTextualDS == null)
					nolayerSTextualDS = new BasicEList<STextualDS>();

				nolayerSTextualDS.add(sTextualDS);

			}
		}
		if (nolayerSTextualDS != null) {
			annoSetWriter.writeStartElement(TAG_STRUCT_STRUCT);
				annoSetWriter.writeAttribute(ATT_ID, "anno_"+j);

				for (STextualDS sTextualDS : nolayerSTextualDS) {
					annoSetWriter.writeStartElement(TAG_STRUCT_REL);
						annoSetWriter.writeAttribute(ATT_ID, "rel_" + i);
						annoSetWriter.writeAttribute(ATT_HREF, fileTable.get(escapeNCName(sTextualDS.getSName())));
					annoSetWriter.writeEndElement();
					i++;
				}
			annoSetWriter.writeEndElement();
		}

		/**
		 * Create annoFeat file beginning and set the feat value to the document
		 * ID (name)
		 */
		createFileBeginning(PAULA_TYPE.FEAT, documentId + ".anno_feat", "annoFeat", null, annoFeatWriter);
		if (nolayerSTextualDS != null) {
			annoFeatWriter.writeStartElement(TAG_FEAT_FEAT);
				annoFeatWriter.writeAttribute(ATT_HREF, "#anno_"+j);
				annoFeatWriter.writeAttribute(ATT_FEAT_FEAT_VAL, documentId);
			annoFeatWriter.writeEndElement();
			j++;
		}

		/**
		 * iterate over all layers
		 */
		for (SLayer layer : sDocumentGraph.getSLayers()) {
			layerSpanList = new BasicEList<SSpan>();
			layerStructList = new BasicEList<SStructure>();
			layerTokenList = new BasicEList<SToken>();
			layerPointingRelationList = new BasicEList<SPointingRelation>();
			layerTextualRelationList = new BasicEList<STextualRelation>();
			layerTextualDS = new BasicEList<STextualDS>();

			layerNodeFileNames.clear();
			// Add a struct anno_i to annoSet and set the value in annoFeat to the layer name
			annoSetWriter.writeStartElement(TAG_STRUCT_STRUCT);
				annoSetWriter.writeAttribute(ATT_ID, "anno_"+j);
			annoSetWriter.writeEndElement();
			annoFeatWriter.writeStartElement(TAG_FEAT_FEAT);
				annoFeatWriter.writeAttribute(ATT_HREF, "#anno_"+j);
				annoFeatWriter.writeAttribute(ATT_FEAT_FEAT_VAL, layer.getSName());
			annoFeatWriter.writeEndElement();

			j++;

			/**
			 * fetch Pointing Relations for this layer
			 */
			for (Edge edge : layer.getEdges()) {
				if (edge instanceof SPointingRelation) {
					layerPointingRelationList.add((SPointingRelation) edge);
					if (pointingRelationList != null) {
						pointingRelationList.remove((SPointingRelation) edge);
					}
				}
			}

			/**
			 * iterate over all nodes. put the nodes in the right lists,
			 * according to their type
			 */
			for (SNode sNode : layer.getSNodes()) {
				if (sNode instanceof STextualDS) {
					layerTextualDS.add((STextualDS) sNode);
					layerNodeFileNames.add(fileTable.get(((STextualDS) sNode).getSName()));
				}
				/**
				 * Token
				 */
				if (sNode instanceof SToken) {

					for (STextualRelation relation : textualRelationList) {
						if (relation.getSToken().equals(sNode))
							layerTextualRelationList.add(relation);
					}
					if (sNode.getSLayers().size() > 1) {
						multiLayerTokenList.add((SToken) sNode);
					} else {
						layerTokenList.add((SToken) sNode);

					}
				}
				/**
				 * Spans
				 */
				if (sNode instanceof SSpan) {

					if (sNode.getSLayers().size() > 1) {
						multiLayerSpanList.add((SSpan) sNode);
					} else {
						spanList.remove((SSpan) sNode);
						layerSpanList.add((SSpan) sNode);
					}
				}

				//Structs
				if (sNode instanceof SStructure) {
					if (sNode.getSLayers().size() > 1) {
						multiLayerStructList.add((SStructure) sNode);
					} else {
						structList.remove((SStructure) sNode);
						layerStructList.add((SStructure) sNode);
					}

				}
			}

			/**
			 * We searched the layer completly now we have to map the
			 * token/spans/structs
			 */

			if (!layerTokenList.isEmpty()) {
				/**
				 * We did not find all token (should not happen!)
				 */
				if (layerTextualRelationList.size() > layerTokenList.size())
					throw new PepperModuleException(this, "There are more Textual Relations then Token in layer" + layer.getSName());
				/**
				 * map token
				 */
				mapTokens(layerTextualRelationList, layerTokenList, fileTable, documentId, documentPath, layer.getSName(), layerNodeFileNames);
			}

			if (!layerSpanList.isEmpty()) {
				mapSpans(sDocumentGraph, layerSpanList, fileTable, documentId, documentPath, layer.getSName(), layerNodeFileNames, firstDSName);
			}
			if (!layerStructList.isEmpty()) {
				mapStructs(layerStructList, layer.getSName(), documentId, documentPath, layerNodeFileNames);
			}
			// Map pointing relations between nodes
			if (!layerPointingRelationList.isEmpty()) {
				mapPointingRelations(sDocumentGraph, documentPath, documentId, layer.getSName(), layerPointingRelationList, layerNodeFileNames);
			}

			/**
			 * create rel tags for each created file in this layer and close the
			 * annoSet struct tag
			 */
			for (String nodeFile : layerNodeFileNames) {
				annoSetWriter.writeStartElement(TAG_STRUCT_REL);
					annoSetWriter.writeAttribute(ATT_ID, "rel_" + i);
					annoSetWriter.writeAttribute(ATT_HREF, nodeFile);
				annoSetWriter.writeEndElement();
				i++;
			}
		}
		/**
		 * If we did not find all spans/structs in the layers we take the
		 * remaining spans/structs and map them in extra files
		 */

		layerNodeFileNames = Collections.synchronizedSet(new HashSet<String>());

		boolean nolayerNodesExist = false;

		/**
		 * when there are spans and structs, which are not in one layer, create
		 * #NO_LAYER files
		 */
		EList<STextualRelation> noLayerSTextRels = new BasicEList<STextualRelation>();
		EList<SToken> noLayerTokens = new BasicEList<SToken>();
		for (STextualRelation rel : sDocumentGraph.getSTextualRelations()) {

			if (rel.getLayers() == null || rel.getLayers().size() == 0 || rel.getSToken().getSLayers() == null || rel.getSToken().getSLayers().size() == 0) {
				// logger.debug("Token "+ rel.getSToken().getSId() +
				// " is not in a layer");
				noLayerSTextRels.add(rel);
				if (rel.getSToken().getSLayers() == null || rel.getSToken().getSLayers().size() == 0) {
					noLayerTokens.add(rel.getSToken());
				}
			}
		}
		// there exist tokens which are not in a layer
		if (noLayerSTextRels.size() != 0 && noLayerTokens.size() != 0) {
			mapTokens(noLayerSTextRels, noLayerTokens, fileTable, documentId, documentPath, NO_LAYER, layerNodeFileNames);
		}

		if (spanList != null && !spanList.isEmpty()) {
			nolayerNodesExist = true;
			mapSpans(sDocumentGraph, spanList, fileTable, documentId, documentPath, NO_LAYER, layerNodeFileNames, firstDSName);
		}
		if (structList != null && !structList.isEmpty()) {
			nolayerNodesExist = true;
			mapStructs(structList, NO_LAYER, documentId, documentPath, layerNodeFileNames);

		}
		if (pointingRelationList != null && !pointingRelationList.isEmpty()) {
			nolayerNodesExist = true;
			mapPointingRelations(sDocumentGraph, documentPath, documentId, NO_LAYER, pointingRelationList, layerNodeFileNames);
		}

		/**
		 * Create entries in annoSet and annoFeat for nolayer if we had
		 * spans/structs which are not in one layer
		 */
		if (nolayerNodesExist) {
			annoSetWriter.writeStartElement(TAG_STRUCT_STRUCT);
				annoSetWriter.writeAttribute(ATT_ID, "anno_"+j);			
				for (String nodeFile : layerNodeFileNames) {
					annoSetWriter.writeStartElement(TAG_STRUCT_REL);
						annoSetWriter.writeAttribute(ATT_ID, "rel_" + i);
						annoSetWriter.writeAttribute(ATT_HREF, nodeFile);
					annoSetWriter.writeEndElement();
					i++;
				}

			annoSetWriter.writeEndElement();
			
			annoFeatWriter.writeStartElement(TAG_FEAT_FEAT);
				annoFeatWriter.writeAttribute(ATT_HREF, "#anno_"+j);
				annoFeatWriter.writeAttribute(ATT_FEAT_FEAT_VAL, NO_LAYER);
			annoFeatWriter.writeEndElement();
			
			j++;

		}
		layerNodeFileNames.add(annoSetFile.getName());
		layerNodeFileNames.add(annoFeatFile.getName());
		
		//close element TAG_STRUCT_LIST
		annoSetWriter.writeEndElement();
		//close TAG_PAULA
		annoSetWriter.writeEndElement();
		annoSetOutput.close();
		
		//close TAG_FEATLIST
		annoFeatWriter.writeEndElement();
		//close TAG_PAULA
		annoFeatWriter.writeEndElement();
		annoFeatOutput.close();

		// map all meta annotations
		mapMetaAnnotations(sDocumentGraph, documentPath, documentId, layerNodeFileNames);

		//validate all created files
		if (((PAULAExporterProperties) this.getProperties()).getIsValidate()) {
			for (String filename : layerNodeFileNames) {
				logger.debug("XML-Validation: " + filename + " is valid: " + isValidXML(new File(documentPath.toFileString() + "/" + filename)));

			}
		}
	}

	/**
	 * Checks if given ncname starts with a numeric. If true, it will be prefixed with the string 'id'. 
	 * @return
	 */
	public String escapeNCName(String ncName){
		if (Character.isDigit(ncName.charAt(0))){
			return("id"+ncName);
		}else{
			return(ncName);
		}
	}
	
	/**
	 * Extracts the tokens including the xPointer from the STextualRelation list
	 * and writes them to files "documentID.tokenfilenumber.tok.xml" in the
	 * documentPath.
	 * 
	 * If there is only one textual data source, the tokenfile number is
	 * omitted.
	 * 
	 * 
	 * @param sTextRels
	 *            list of textual relations (tokens)pointing to a target (data
	 *            source)
	 * @param layerTokenList
	 *            list of tokens contained in the current layer
	 * @param fileTable
	 *            Hashmap including the SId (String) of the data-source, the URI
	 *            of the corresponding textfile and a PrintWriter for each token
	 *            file
	 * @param documentID
	 *            the document id of the Salt document
	 * @param documentPath
	 *            the path to which the token files will be mapped
	 * @param layer
	 *            Name of the layer
	 * @param layerNodeFileNames
	 *            set where all created files (name) are stored
	 * @return
	 */
	private void mapTokens(EList<STextualRelation> sTextRels, EList<SToken> layerTokenList, Hashtable<String, String> fileTable, String documentID, URI documentPath, String layer, Set<String> layerNodeFileNames) {

		if (sTextRels.isEmpty())
			throw new PepperModuleException(this, "Cannot create token files because there are no textual relations");
		if (layerTokenList == null)
			throw new PepperModuleException(this, "Cannot create token files because there are no tokens in this layer");
		if (fileTable == null)
			throw new PepperModuleException(this, "Cannot create token files because no textFileTable is defined");
		if (documentID.isEmpty())
			throw new PepperModuleException(this, "Cannot create token files because documentID is empty (\"\")");
		if (documentPath == null)
			throw new PepperModuleException(this, "Cannot create token files because documentPath is null");
		if (layer.isEmpty())
			throw new PepperModuleException(this, "Cannot create token files because no layer was specified");
		if (layerNodeFileNames == null)
			throw new PepperModuleException(this, "Cannot create token files because there is no Set to save the token file names to");

		/**
		 * Create one Hashmap for returning and one Hashmap for the PrintWriter
		 */
		Hashtable<String, PrintWriter> tokenWriteMap = new Hashtable<String, PrintWriter>();
		String baseTextFile;
		int tokenFileIndex = 0;
		File tokenFile = null;

		// StringBuffer fileString = new StringBuffer();

		/**
		 * iterate over all textual relations
		 */
		for (STextualRelation sTextualRelation : sTextRels) {
			if (!layerTokenList.contains(sTextualRelation.getSToken())) {
				/**
				 * if layerTokenList does not contain the token, do nothing for
				 * this STextualRelation
				 */
			} else {
				/**
				 * Get one PrintWriter
				 */
				PrintWriter output = tokenWriteMap.get(sTextualRelation.getSTarget().getSName());
				/**
				 * get the target of the current textual Relation
				 */
				String sTextDSSid = sTextualRelation.getSTarget().getSName();

				/**
				 * Set the tokenFileIndex Split the file name by dots and take
				 * the string before xml This will be a number if there are at
				 * least 2 data sources
				 */
				if (fileTable.size() > 1) {
					String[] textFileParts = (fileTable.get(sTextDSSid)).split("\\.");
					tokenFileIndex = Integer.parseInt(textFileParts[textFileParts.length - 2]);

				}
				/**
				 * Prepare the mark tag
				 */
				String tokenMarkTag = new StringBuffer("\t\t<").append(TAG_MARK_MARK).append(" ").append(ATT_ID).append("=\"").append(escapeNCName(sTextualRelation.getSToken().getSName())).append("\" ").append(ATT_HREF).append("=\"#xpointer(string-range(//body,'',").append(sTextualRelation.getSStart() + 1).append(",").append(sTextualRelation.getSEnd() - sTextualRelation.getSStart()).append("))\" />").toString();

				/**
				 * If output is null, we first have to create one token file,
				 * write the preamble and the mark tag Else we can just write
				 * the mark tag
				 */
				if (output != null) {
					output.println(tokenMarkTag);
				} else {
					/**
					 * Create the token file name (Path + filename of DS with
					 * text replaced by tok) get the base text file (is
					 * contained in the fileTable)
					 */
					String tokenFileName;
					if (fileTable.size() > 1) {
						tokenFileName = new String(documentPath.toFileString() + "/" + layer + "." + documentID + ".tok." + tokenFileIndex + ".xml");
					} else {
						tokenFileName = new String(documentPath.toFileString() + "/" + layer + "." + documentID + ".tok.xml");
					}
					baseTextFile = new String(fileTable.get(sTextDSSid));
					tokenFile = new File(tokenFileName);
					try {
						if (!tokenFile.exists()) {
							if (!(tokenFile.createNewFile()))
								logger.warn("Cannot create file '" + tokenFile.getName() + "', because it already exists.");
						}

						layerNodeFileNames.add(tokenFile.getName());
						output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tokenFile), "UTF8")), true);
						/**
						 * Write preamble and the first mark tag to file
						 */
						if (fileTable.size() > 1) {
							output.write(createFileBeginning(PAULA_TYPE.MARK, layer + "." + documentID + "." + tokenFileIndex + ".tok", "tok", baseTextFile.replace(tokenFile.getPath(), "")));
						} else {
							output.write(createFileBeginning(PAULA_TYPE.MARK, layer + "." + documentID + ".tok", "tok", baseTextFile.replace(tokenFile.getPath(), "")));
						}

						output.println(tokenMarkTag);

						/**
						 * Put PrintWriter into the tokenWriteMap for further
						 * access
						 * 
						 */
						tokenWriteMap.put(sTextualRelation.getSTarget().getSName(), output);

					} catch (IOException e) {
						throw new PepperModuleException(this, "", e);
					}

				}
				/**
				 * Put <TokenName,TokenFileName> into tokenFileMap
				 */
				nodeFileMap.put(sTextualRelation.getSToken().getSElementId(), tokenFile.getName());

			}
		}
		/**
		 * Close all token file streams
		 */
		for (PrintWriter writer : (tokenWriteMap.values())) {
			writer.write(PAULA_TOKEN_FILE_CLOSING);
			writer.close();
		}
		/**
		 * dispose all Writers since we are finished with the tokens map token
		 * annotations return the token file map
		 */
		tokenWriteMap = null;
		mapTokenAnnotations(layerTokenList, documentPath, documentID, layerNodeFileNames);

	}

	/**
	 * Writes all span files for one specific layer to file and calls the Span
	 * annotation map routine.
	 * 
	 * @param graph
	 *            the document graph
	 * @param layerSpanList
	 *            list of spans in the current layer
	 * @param nodeFileMap
	 *            table containing all token names and the containing files
	 * @param dSFileTable
	 *            table containing all sTextual datasources and the containing
	 *            files
	 * @param documentId
	 *            the document name
	 * @param documentPath
	 *            the base document path
	 * @param layer
	 *            name of the current layer
	 * @param layerNodeFileNames
	 *            set where all created files (name) are stored
	 * @param firstDSName
	 *            name of the first datasource
	 * @return
	 */
	private void mapSpans(SDocumentGraph graph, EList<SSpan> layerSpanList, Hashtable<String, String> dSFileTable, String documentId, URI documentPath, String layer, Set<String> layerNodeFileNames, String firstDSName) {

		if (graph == null)
			throw new PepperModuleException(this, "Cannot map span files because document graph is null");
		if (layerSpanList == null)
			throw new PepperModuleException(this, "Cannot map span files because layerSpanList is null");
		if (dSFileTable == null)
			throw new PepperModuleException(this, "Cannot map span files because there is no data source file table");
		if (documentId.isEmpty())
			throw new PepperModuleException(this, "Cannot map span files because documentID is empty (\"\")");
		if (documentPath == null)
			throw new PepperModuleException(this, "Cannot map span because documentPath is not specified");
		if (layer.isEmpty())
			throw new PepperModuleException(this, "Cannot map span files because layer name is empty (\"\")");
		if (layerNodeFileNames == null)
			throw new PepperModuleException(this, "Cannot map span files because there is no set to save the file names to");
		if (firstDSName.isEmpty())
			throw new PepperModuleException(this, "Cannot map span files because first DS Name is empty (\"\")");

		/**
		 * create SDocumentStructureAccessor in order to have access to
		 * overlapped tokens
		 */
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
		 * Create the base for the markList tag This is the name of the first
		 * token file which is the name of the first DS File with text replaced
		 * by tok
		 */
		// TODO Hack hacky hack hack
		String baseMarkFile = nodeFileMap.get(accessor.getSTextualOverlappedTokens((SStructuredNode) layerSpanList.get(0)).get(0).getSElementId());
		String spanFileToWrite = layer + "." + documentId + ".mark.xml";
		PrintWriter output = null;
		paulaID = spanFileToWrite.substring(0, spanFileToWrite.length() - 4);
		/**
		 * Create span File
		 */
		File spanFile = new File(documentPath.toFileString() + "/" + spanFileToWrite);
		try {
			if (!spanFile.exists()) {
				if (!(spanFile.createNewFile()))
					logger.warn("Cannot create file '" + spanFile.getName() + "', because it already exists.");
			}

			layerNodeFileNames.add(spanFile.getName());

			output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(spanFile.getAbsoluteFile()), "UTF8")), false);

			/**
			 * Write markfile-preamble to file
			 * 
			 */
			output.write(createFileBeginning(PAULA_TYPE.MARK, paulaID, "mark", baseMarkFile));
		} catch (IOException e) {
			throw new PepperModuleException(this, "mapSpans: Could not write File " + spanFileToWrite.toString() + ": " + e.getMessage());
		}

		for (SSpan sSpan : layerSpanList) {
			/**
			 * get tokens which are overlapped by this Span
			 */
			overlappingTokens = accessor.getSTextualOverlappedTokens((SStructuredNode) sSpan);
			nodeFileMap.put(sSpan.getSElementId(), spanFileToWrite);
			spanList.remove(sSpan);
			spanFileNames.add(spanFileToWrite);
			/**
			 * Write mark tag
			 */
			output.println(createSpanFileMarkTag(sSpan.getSName(), dSFileTable, overlappingTokens, dsNum, firstDSName));

		}
		output.write("\t" + MARK_LIST_CLOSE_TAG + LINE_SEPARATOR + PAULA_CLOSE_TAG);
		output.close();
		mapSpanAnnotations(layerSpanList, documentPath, paulaID, layerNodeFileNames);

	}

	/**
	 * 
	 * Maps all Structs to PAULA format. Maps also Dominance relations and the
	 * dominance relation annotations to PAULA format. Calls the mapping routine
	 * for Struct annotations.
	 * 
	 * @param layerStructList
	 *            list of Structs in the current layer
	 * @param nodeFileMap
	 *            Table of node names and the containing file (name)
	 * @param layer
	 *            name of the current layer
	 * @param documentId
	 *            the document name
	 * @param documentPath
	 *            the base document path
	 * @param layerNodeFileNames
	 *            set where all created files (name) are stored
	 * @return
	 */
	private void mapStructs(EList<SStructure> layerStructList, String layer, String documentId, URI documentPath, Set<String> layerNodeFileNames) {

		if (layerStructList == null)
			throw new PepperModuleException(this, "Cannot map struct files because layerSpanList is null");
		if (layer.isEmpty())
			throw new PepperModuleException(this, "Cannot map struct files because layer name is empty (\"\")");
		if (documentId.isEmpty())
			throw new PepperModuleException(this, "Cannot map struct files because documentID is empty (\"\")");
		if (documentPath.isEmpty())
			throw new PepperModuleException(this, "Cannot map struct because documentPath is empty (\"\")");
		if (layerNodeFileNames == null)
			throw new PepperModuleException(this, "Cannot map struct files because there is no set to save the file names to");

		/**
		 * Hashtables for all PrintWriters for DominanceRelation annotations and
		 * struct names and the including files (name)
		 */
		Hashtable<String, PrintWriter> domRelAnnotationWriterTable = new Hashtable<String, PrintWriter>();
	
		/**
		 * accessor for extracting the overlapped Spans
		 */
		SDocumentStructureAccessor accessor = new SDocumentStructureAccessor();
		accessor.setSDocumentGraph(layerStructList.get(0).getSDocumentGraph());

		String paulaID = layer + "." + documentId + ".struct";

		File structFile = new File(documentPath.toFileString() + "/" + paulaID + ".xml");
		PrintWriter output = null;
		try {
			if (!structFile.exists()) {
				if (!(structFile.createNewFile()))
					logger.warn("Cannot create file '" + structFile.getName() + "', because it already exists.");
			}
			layerNodeFileNames.add(structFile.getName());

			output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(structFile.getAbsoluteFile()), "UTF8")), false);

		} catch (IOException e) {
			throw new PepperModuleException(this, "mapStructs: Could not write File " + structFile.getName() + ": " + e.getMessage());
		}

		output.write(createFileBeginning(PAULA_TYPE.STRUCT, paulaID, "struct", null));

		/**
		 * iterate over all structures, map the struct and all included
		 * edges(domRels) to other nodes
		 */
		for (SStructure struct : layerStructList) {
			output.println(new StringBuffer("\t\t<").append(TAG_STRUCT_STRUCT).append(" ").append(ATT_ID).append("=\"").append(escapeNCName(struct.getSName())).append("\">").toString());
			/**
			 * Save the struct name in the struct file map
			 */
			nodeFileMap.put(struct.getSElementId(), structFile.getName());

			for (Edge edge : struct.getSDocumentGraph().getOutEdges(((SNode) struct).getSId())) {
				String baseFile;
				if (edge instanceof SDominanceRelation) {

					/**
					 * set the base file according to the type of the target of
					 * the domRel: type = span --> base file is a span file type
					 * = token --> base file is a token file type = struct -->
					 * base file is a struct file
					 */
					SNode targetNode = ((SDominanceRelation) edge).getSTarget();
					if (targetNode instanceof SSpan || targetNode instanceof SToken || targetNode instanceof SStructure) {

						baseFile = nodeFileMap.get(targetNode.getSElementId());
					} else {
						baseFile = "";
					}
					/**
					 * output rel tag. If the edge has no sType, ommit the type
					 * attribute
					 */
					StringBuffer out = new StringBuffer("\t\t\t<");
					out.append(TAG_STRUCT_REL);
					out.append(" ").append(ATT_ID).append("=\"");
					out.append(escapeNCName(((SDominanceRelation) edge).getSName())).append("\" ");

					if ((((SDominanceRelation) edge).getSTypes() != null) && (!((SDominanceRelation) edge).getSTypes().isEmpty())) {
						out.append(ATT_STRUCT_REL_TYPE).append("=\"");
						out.append(((SDominanceRelation) edge).getSTypes().get(0));
						out.append("\" ");
					}

					out.append(ATT_HREF).append("=\"");
					if (baseFile != null)
						out.append(baseFile);
					out.append("#");
					out.append(escapeNCName(((SDominanceRelation) edge).getSTarget().getSName()));
					out.append("\"/>");
					output.println(out.toString());

					/**
					 * Map dominance relation Annotations
					 */
					for (SAnnotation sAnnotation : ((SDominanceRelation) edge).getSAnnotations()) {
						String annoType = sAnnotation.getQName().replace("::", ".");
						String annoPaulaId = paulaID + "_" + annoType;
						String domRelAnnoFileName = annoPaulaId + ".xml";

						String annoString = null;
						// copy referenced files
						if (sAnnotation.getSValueSURI() != null) {
							annoString = copyFile(sAnnotation.getSValueSURI(), documentPath.toFileString());
						} else {
							annoString = sAnnotation.getSValue().toString();
						}

						StringBuffer featTag = new StringBuffer("\t\t").append("<").append(TAG_FEAT_FEAT).append(" ").append(ATT_HREF).append("=\"#").append(escapeNCName(((SDominanceRelation) edge).getSName())).append("\" ").append(ATT_FEAT_FEAT_VAL).append("=\"").append(annoString).append("\"/>");

						PrintWriter annoOutput = domRelAnnotationWriterTable.get(domRelAnnoFileName);

						if (annoOutput == null) {
							File domRelAnnoFile = new File(documentPath.toFileString() + "/" + domRelAnnoFileName);
							try {
								if (!domRelAnnoFile.exists()) {
									if (!(domRelAnnoFile.createNewFile()))
										logger.warn("Cannot create file '" + domRelAnnoFile.getName() + "', because it already exists.");
								}
								layerNodeFileNames.add(domRelAnnoFile.getName());

								annoOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(domRelAnnoFile.getAbsoluteFile()), "UTF8")), false);

							} catch (IOException e) {
								throw new PepperModuleException(this, "mapStructs: Could not write File " + domRelAnnoFile.getName() + ": " + e.getMessage());
							}
							annoOutput.write(createFileBeginning(PAULA_TYPE.FEAT, annoPaulaId, annoType, structFile.getName()));

							annoOutput.println(featTag);
							domRelAnnotationWriterTable.put(domRelAnnoFile.getName(), annoOutput);
						} else {
							annoOutput.println(featTag);
						}

					}
				}
			}

			output.println("\t\t</" + TAG_STRUCT_STRUCT + ">");
		}
		/**
		 * write all ending tags to files and close the streams
		 */
		for (PrintWriter annoOutput : domRelAnnotationWriterTable.values()) {
			annoOutput.println("\t</" + TAG_FEAT_FEATLIST + ">");
			annoOutput.println(PAULA_CLOSE_TAG);
			annoOutput.close();
		}
		domRelAnnotationWriterTable = null;

		output.println("\t" + STRUCT_LIST_CLOSE_TAG);
		output.println(PAULA_CLOSE_TAG);
		output.close();

		mapStructAnnotations(layerStructList, documentPath, structFile.getName(), layerNodeFileNames);

	}

	/**
	 * Maps pointing relations to files and calls the mapping routine for
	 * pointing relation annotations.
	 * 
	 * @param sDocumentGraph
	 *            the document graph
	 * @param documentPath
	 *            the base document path
	 * @param documentId
	 *            the document name
	 * @param layer
	 *            name of the current layer
	 * @param nodeFileMap
	 *            table containing all node names and the containing files
	 *            (name)
	 * @param layerPointingRelationList
	 *            list of all PointingRelations in the current layer
	 * @param layerNodeFileNames
	 *            set where all created filed (name) are stored
	 * @throws XMLStreamException 
	 */
	private void mapPointingRelations(SDocumentGraph sDocumentGraph, URI documentPath, String documentId, String layer, EList<SPointingRelation> layerPointingRelationList, Set<String> layerNodeFileNames) throws XMLStreamException {

		if (sDocumentGraph == null)
			throw new PepperModuleException(this, "Cannot map pointing relations because document graph is null");
		if (documentPath.isEmpty())
			throw new PepperModuleException(this, "Cannot map pointing relations because documentPath is empty (\"\")");
		if (documentId.isEmpty())
			throw new PepperModuleException(this, "Cannot map pointing relations because documentID is empty (\"\")");
		if (layer.isEmpty())
			throw new PepperModuleException(this, "Cannot map pointing relation files because layer name is empty (\"\")");
		if (layerPointingRelationList == null)
			throw new PepperModuleException(this, "Cannot map pointing relation files because there are no pointing relations in this layer");
		if (layerNodeFileNames == null)
			throw new PepperModuleException(this, "Cannot map pointing relation files because there is no set to save the file names to");

		/**
		 * Hashtable for storing the printwriters to pointing relation files and
		 * Hashtable containing all pointing relation names and the containing
		 * file (name)
		 */
		Hashtable<String, XMLStreamWriter> relWriterTable = new Hashtable<String, XMLStreamWriter>();
		Hashtable<String, String> relFileTable = new Hashtable<String, String>();

		for (SPointingRelation pointRel : layerPointingRelationList) {
			String type = "";
			// checking if type is existent for rellist (type is required!!!)
			if (pointRel.getSTypes() == null || pointRel.getSTypes().size() == 0) {
				// throw new
				// PepperModuleException(this,"MapPointingRelations: There is no type specified for rellist but type is required.");
				type = "notype";
			} else {
				type = pointRel.getSTypes().get(0);
			}
			String paulaID = layer + "." + documentId + ".pointRel" + "_" + type;

			/**
			 * create file Object for the pointing relation, get a PrintWriter
			 * from the Hashtable (relWriterTable) and put the pointing relation
			 * name and the file name to the relationFileTable
			 */
			File pointingRelFile = new File(documentPath.toFileString() + "/" + paulaID + ".xml");
			XMLStreamWriter xmlWriter = relWriterTable.get(pointingRelFile.getName());
			relFileTable.put(pointRel.getSName(), pointingRelFile.getName());
			
			/**
			 * if there is no Printwriter, yet, create it and write the file
			 * beginning
			 */
			if (xmlWriter == null) {
				try {
					if (!pointingRelFile.exists()) {
						if (!(pointingRelFile.createNewFile()))
							logger.warn("Cannot create file '" + pointingRelFile.getName() + "', because it already exists.");
					}
					layerNodeFileNames.add(pointingRelFile.getName());

					PrintWriter output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pointingRelFile.getAbsoluteFile()), "UTF8")), false);
					xmlWriter = xmlFactory.createXMLStreamWriter(output);
					createFileBeginning(PAULA_TYPE.REL, paulaID, type, null, xmlWriter);
				} catch (IOException e) {
					throw new PepperModuleException(this, "mapPointingRelations: Could not write File " + pointingRelFile.getName() + ": " + e.getMessage());
				}
				
				relWriterTable.put(pointingRelFile.getName(), xmlWriter);
			}
			
			//create rel tag string
			if ((pointRel.getSSource() != null) && (pointRel.getSTarget() != null)) {		
				xmlWriter.writeStartElement(TAG_REL_REL);
					xmlWriter.writeAttribute(ATT_ID, escapeNCName(pointRel.getSName()));
					
					String sourceFileName= nodeFileMap.get(pointRel.getSSource().getSElementId());
					String targetFileName= nodeFileMap.get(pointRel.getSTarget().getSElementId());
					if (	(sourceFileName!= null)&& 
							(targetFileName!= null)){
						xmlWriter.writeAttribute(ATT_HREF, sourceFileName+"#"+escapeNCName(pointRel.getSSource().getSName()));
						xmlWriter.writeAttribute(ATT_REL_REL_TARGET, targetFileName+"#"+escapeNCName(pointRel.getSTarget().getSName()));
					}else {
						logger.warn("Cannot write pointing relation '"+pointRel.getSId()+"' to disk, because I can not resolve the files for source or target object. ");
					}
				xmlWriter.writeEndElement();	
			}
		}

		/**
		 * Write all file closings and close the streams
		 */
		for (XMLStreamWriter xmlWriter: relWriterTable.values()) {
			xmlWriter.writeEndElement();
			//close TAG_PAULA
			xmlWriter.writeEndElement();
			xmlWriter.close();
		}
		mapPointingRelationAnnotations(documentPath, layerPointingRelationList, relFileTable, layerNodeFileNames);
	}

	/**
	 * Creates Annotations files for all token.
	 * 
	 * @param tokenFileMap
	 *            Hashtable containing all token names and the containing files
	 *            (name)
	 * @param layerTokenList
	 *            list with all tokens in the current layer
	 * @param documentPath
	 *            the base document path
	 * @param documentID
	 *            the document name
	 * @param layerNodeFileNames
	 *            set where all created files (name) are stored
	 * @return
	 */
	private void mapTokenAnnotations(EList<SToken> layerTokenList, URI documentPath, String documentID, Set<String> layerNodeFileNames) {

		if (layerTokenList == null)
			throw new PepperModuleException(this, "Cannot map token annotations: The token List is empty for this layer");
		if (documentPath == null)
			throw new PepperModuleException(this, "Cannot map token annotations: The documentPath is null");
		if (documentID.isEmpty())
			throw new PepperModuleException(this, "Cannot map token annotations: The documentID is not specified");
		if (layerNodeFileNames == null)
			throw new PepperModuleException(this, "Cannot map token annotations: There is no Set to save the filenames to");

		/**
		 * Create a File Table for annotation writers
		 */
		Hashtable<String, PrintWriter> annoFileTable = new Hashtable<String, PrintWriter>();

		/**
		 * Iterate over all tokens
		 */
		for (SToken sToken : layerTokenList) {

			String base = nodeFileMap.get(sToken.getSElementId());
			/**
			 * get the base token file name (without .xml)
			 */
			String baseTokenFileName = base.replace(".xml", "");

			String resFileName = null;
			/**
			 * Iterate over all annotations of this token
			 */
			for (SAnnotation sAnnotation : sToken.getSAnnotations()) {
				String annoString = null;
				// copy referenced files
				if (sAnnotation.getSValueSURI() != null) {
					annoString = copyFile(sAnnotation.getSValueSURI(), documentPath.toFileString());
				} else {
					annoString = StringEscapeUtils.escapeXml(sAnnotation.getSValueSTEXT());
				}
				StringBuffer featTag = new StringBuffer("\t\t").append("<").append(TAG_FEAT_FEAT).append(" ").append(ATT_HREF).append("=\"#").append(escapeNCName(sToken.getSName())).append("\" ").append(ATT_FEAT_FEAT_VAL).append("=\"").append(annoString).append("\"/>");

				String type = sAnnotation.getQName().replace("::", ".");
				String paulaID = baseTokenFileName + "_" + type;
				/**
				 * Create the token file name (baseName + AnnoName + .xml)
				 */
				String tokenFileName = paulaID + ".xml";

				/**
				 * Reference one PrintWriter
				 */
				PrintWriter output = annoFileTable.get(tokenFileName);

				/**
				 * If Reference is null, we have to create a anno file
				 */
				if (output != null) {
					output.println(featTag.toString());

				} else {

					File annoFile = new File(documentPath.toFileString() + "/" + tokenFileName);
					try {
						if (!annoFile.exists()) {
							if (!(annoFile.createNewFile()))
								logger.warn("Cannot create file '" + annoFile.getName() + "', because it already exists.");
						}
						layerNodeFileNames.add(annoFile.getName());
						/**
						 * Write Preamble and Tag
						 */
						output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(annoFile), "UTF8")), false);
						output.write(createFileBeginning(PAULA_TYPE.FEAT, paulaID, type, base));

						output.println(featTag.toString());

						/**
						 * Put file (Writer) in FileTable for further access
						 */
						annoFileTable.put(tokenFileName, output);

					} catch (IOException e) {
						throw new PepperModuleException(this, "mapTokenAnnotations: Could not write File " + annoFile.getAbsolutePath() + ": " + e.getMessage());
					}
				}
			}
		}
		/**
		 * Close all Writers
		 */
		for (PrintWriter output : annoFileTable.values()) {
			output.println("\t</" + TAG_FEAT_FEATLIST + ">");
			output.println(PAULA_CLOSE_TAG);
			output.close();
		}
		annoFileTable = null;
	}

	/**
	 * Creates Annotation files for spans.
	 * 
	 * @param layerSpanList
	 *            a list with all Spans, found in a specific layer
	 * @param documentPath
	 *            the base document path
	 * @param baseSpanFile
	 *            The filename of the Span file without ".xml" (the PAULA ID)
	 * @param layerNodeFileNames
	 *            set where all created files (name) are stored
	 * @return
	 */
	private void mapSpanAnnotations(EList<SSpan> layerSpanList, URI documentPath, String baseSpanFile, Set<String> layerNodeFileNames) {

		if (layerSpanList == null)
			throw new PepperModuleException(this, "Cannot map span annotations: There are no spans in this layer");
		if (documentPath == null)
			throw new PepperModuleException(this, "Cannot map span annotations: No document path was specified");
		if (baseSpanFile.isEmpty())
			throw new PepperModuleException(this, "Cannot map span annotations: No base span file paula id was specified");
		if (layerNodeFileNames == null)
			throw new PepperModuleException(this, "Cannot map span annotations: There is no Set fo save the file names to");

		Hashtable<String, PrintWriter> annoFileTable = new Hashtable<String, PrintWriter>();
		File annoFile;

		for (SSpan sSpan : layerSpanList) {
			for (SAnnotation sAnnotation : sSpan.getSAnnotations()) {
				String annoString = null;
				// copy referenced files
				if (sAnnotation.getSValueSURI() != null) {
					annoString = copyFile(sAnnotation.getSValueSURI(), documentPath.toFileString());
				} else {
					annoString = StringEscapeUtils.escapeXml(sAnnotation.getSValueSTEXT());
				}

				String type = sAnnotation.getQName().replace("::", ".");
				String qName = baseSpanFile + "_" + type;
				/**
				 * create the feat tag
				 */
				StringBuffer featTag = new StringBuffer("\t\t").append("<").append(TAG_FEAT_FEAT).append(" ").append(ATT_HREF).append("=\"#").append(escapeNCName(sSpan.getSName())).append("\" ").append(ATT_FEAT_FEAT_VAL).append("=\"").append(annoString).append("\"/>");

				/**
				 * reference one PrintWriter from the annotation file Table
				 */
				PrintWriter output = annoFileTable.get(qName);

				/**
				 * If there is a PrintWriter to an annotation file, then write
				 * the feat tag
				 */
				if (output != null) {
					output.println(featTag.toString());
				} else {
					annoFile = new File(documentPath.toFileString() + "/" + qName + ".xml");
					try {
						if (!annoFile.exists()) {
							if (!(annoFile.createNewFile()))
								logger.warn("Cannot create file '" + annoFile.getName() + "', because it already exists.");
						}

						layerNodeFileNames.add(annoFile.getName());

						output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(annoFile.getAbsoluteFile()), "UTF8")), true);

						/**
						 * Write the feat file beginning and the first feat tag
						 * to the file
						 */
						output.println(createFileBeginning(PAULA_TYPE.FEAT, qName, type, baseSpanFile + ".xml"));
						output.println(featTag.toString());

						/**
						 * put the PrintWriter into the Hashtable for later
						 * access
						 */
						annoFileTable.put(qName, output);

					} catch (IOException e) {
						throw new PepperModuleException(this, "mapSpanAnnotations: Could not write File " + annoFile.getAbsolutePath() + ": " + e.getMessage());
					}

				}
			}
		}
		/**
		 * Write the closing tags, close all streams and dereference the
		 * annotation file Table
		 */
		for (PrintWriter output : annoFileTable.values()) {
			output.println("\t</" + TAG_FEAT_FEATLIST + ">");
			output.println(PAULA_CLOSE_TAG);
			output.close();
		}
		annoFileTable = null;
	}

	/**
	 * Maps Struct annotations to files.
	 * 
	 * @param layerStructList
	 *            a list with all Structs, found in a specific layer
	 * @param documentPath
	 *            the base document path
	 * @param baseStructFile
	 *            The filename of the base Struct file
	 * @param layerNodeFileNames
	 *            set where all created files (name) are stored
	 * @return
	 */
	private void mapStructAnnotations(EList<SStructure> layerStructList, URI documentPath, String baseStructFile, Set<String> layerNodeFileNames) {

		if (layerStructList == null)
			throw new PepperModuleException(this, "Cannot map struct annotations: There are no spans in this layer");
		if (documentPath == null)
			throw new PepperModuleException(this, "Cannot map struct annotations: No document path was specified");
		if (baseStructFile.isEmpty())
			throw new PepperModuleException(this, "Cannot map struct annotations: No base span file paula id was specified");
		if (layerNodeFileNames == null)
			throw new PepperModuleException(this, "Cannot map struct annotations: There is no Set fo save the file names to");

		Hashtable<String, PrintWriter> annoFileTable = new Hashtable<String, PrintWriter>();
		File annotationFile;

		for (SStructure sSpan : layerStructList) {
			for (SAnnotation sAnnotation : sSpan.getSAnnotations()) {
				String annoString = null;
				// copy referenced files
				if (sAnnotation.getSValueSURI() != null) {
					annoString = copyFile(sAnnotation.getSValueSURI(), documentPath.toFileString());
				} else {
					annoString = StringEscapeUtils.escapeXml(sAnnotation.getSValueSTEXT());
				}

				String type = sAnnotation.getQName().replace("::", ".");
				String qName = baseStructFile.replace(".xml", "_" + type + ".xml");
				/**
				 * create the feat tag
				 */
				StringBuffer featTag = new StringBuffer("\t\t").append("<").append(TAG_FEAT_FEAT).append(" ").append(ATT_HREF).append("=\"#").append(escapeNCName(sSpan.getSName())).append("\" ").append(ATT_FEAT_FEAT_VAL).append("=\"").append(annoString).append("\"/>");

				/**
				 * reference one PrintWriter from the annotation file Table
				 */
				PrintWriter output = annoFileTable.get(qName);

				/**
				 * If there is a PrintWriter to an annotation file, then write
				 * the feat tag
				 */
				if (output != null) {
					output.println(featTag.toString());
				} else {
					annotationFile = new File(documentPath.toFileString() + "/" + qName);
					try {
						if (!annotationFile.exists()) {
							if (!(annotationFile.createNewFile()))
								logger.warn("Cannot create file '" + annotationFile.getName() + "', because it already exists.");
						}

						layerNodeFileNames.add(annotationFile.getName());

						output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(annotationFile.getAbsoluteFile()), "UTF8")), true);

						/**
						 * Write the feat file beginning and the first feat tag
						 * to the file
						 */
						output.println(createFileBeginning(PAULA_TYPE.FEAT, qName, type, baseStructFile));
						output.println(featTag.toString());

						/**
						 * put the PrintWriter into the Hashtable for later
						 * access
						 */
						annoFileTable.put(qName, output);

					} catch (IOException e) {
						throw new PepperModuleException(this, "mapStructAnnotations: Could not write File " + annotationFile.getAbsolutePath() + ": " + e.getMessage());
					}
				}
			}
		}
		/**
		 * Write the closing tags, close all streams and dereference the
		 * annotation file Table
		 */
		for (PrintWriter output : annoFileTable.values()) {
			output.println("\t</" + TAG_FEAT_FEATLIST + ">");
			output.println(PAULA_CLOSE_TAG);
			output.close();
		}
		annoFileTable = null;

	}

	/**
	 * Maps all document MetaAnnotations (like genre,...) to files
	 * 
	 * @param sDocumentGraph
	 *            the document graph
	 * @param documentPath
	 *            the base document path
	 * @param documentId
	 *            the document name
	 * @param layerNodeFileNames
	 *            set where all created files (name) are stored
	 * @return
	 */
	private void mapMetaAnnotations(SDocumentGraph sDocumentGraph, URI documentPath, String documentId, Set<String> layerNodeFileNames) {

		if (sDocumentGraph == null)
			throw new PepperModuleException(this, "Cannot map Meta annotations: There is no reference to the document graph");
		if (documentPath == null)
			throw new PepperModuleException(this, "Cannot map Meta annotations: No document path was specified");
		if (documentId.isEmpty())
			throw new PepperModuleException(this, "Cannot map Meta annotations: The document ID was not specified");

		Hashtable<String, PrintWriter> annoFileTable = new Hashtable<String, PrintWriter>();

		String base = "merged." + documentId + ".anno.xml";

		/**
		 * iterate over all meta annotations
		 */
		for (SMetaAnnotation anno : sDocumentGraph.getSDocument().getSMetaAnnotations()) {

			StringBuffer featTag = new StringBuffer("\t\t").append("<").append(TAG_FEAT_FEAT).append(" ").append(ATT_HREF).append("=\"#").append(escapeNCName(anno.getSName())).append("\" ").append(ATT_FEAT_FEAT_VAL).append("=\"").append(StringEscapeUtils.escapeXml(anno.getSValueSTEXT())).append("\"/>");

			String type = anno.getQName().replace("::", ".");
			String paulaID = "merged." + documentId + ".anno_" + type;
			/**
			 * Create the anno file name (paulaId + .xml)
			 */
			String annoFileName = paulaID + "." + PepperImporter.ENDING_XML + "";

			/**
			 * Reference one PrintWriter
			 */
			PrintWriter output = annoFileTable.get(annoFileName);

			/**
			 * If Reference is null, we have to create a anno file
			 */
			if (output != null) {
				output.println(featTag.toString());

			} else {
				File annoFile = new File(documentPath.toFileString() + "/" + annoFileName);
				try {
					if (!annoFile.exists()) {
						if (!(annoFile.createNewFile()))
							logger.warn("Cannot create file '" + annoFile.getName() + "', because it already exists.");
					}

					layerNodeFileNames.add(annoFileName);

					/**
					 * Write Preamble and Tag
					 */
					output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(annoFile), "UTF8")), false);
					output.write(createFileBeginning(PAULA_TYPE.FEAT, paulaID, type, base));
					output.println(featTag.toString());

					/**
					 * Put file (Writer) in FileTable for further access
					 */
					annoFileTable.put(annoFileName, output);

				} catch (IOException e) {
					throw new PepperModuleException(this, "mapTokenAnnotations: Could not write File " + annoFile.getAbsolutePath() + ": " + e.getMessage());
				}
			}
		}
		/**
		 * write all file endings and close the streams
		 */
		for (PrintWriter output : annoFileTable.values()) {
			output.println("\t</" + TAG_FEAT_FEATLIST + ">");
			output.println(PAULA_CLOSE_TAG);
			output.close();
		}
	}

	/**
	 * Maps all Pointing relation annotations to files.
	 * 
	 * @param documentPath
	 *            the base document path
	 * @param layerPointingRelationList
	 *            List with pointing relations in the current layer
	 * @param relFileTable
	 *            list containing all pointing relation names and the containing
	 *            files (name)
	 * @param layerNodeFileNames
	 *            set where all created files (name) are stored
	 * @return
	 */
	private void mapPointingRelationAnnotations(URI documentPath, EList<SPointingRelation> layerPointingRelationList, Hashtable<String, String> relFileTable, Set<String> layerNodeFileNames) {

		if (documentPath == null)
			throw new PepperModuleException(this, "Cannot map pointing relation annotations: No document path was specified");
		if (layerPointingRelationList == null)
			throw new PepperModuleException(this, "Cannot map pointing relation annotations: There are no pointing relations in this layer");
		if (relFileTable == null)
			throw new PepperModuleException(this, "Cannot map pointing relation annotations: There are no pointing relations files");
		if (layerNodeFileNames == null)
			throw new PepperModuleException(this, "Cannot map pointing relation annotations: There is no Set fo save the file names to");

		/**
		 * create Hashtable for annotation PrintWriters
		 */
		Hashtable<String, PrintWriter> annoFileTable = new Hashtable<String, PrintWriter>();

		/**
		 * iterate over all pointing relations
		 */
		for (SPointingRelation rel : layerPointingRelationList) {

			String relationFile = relFileTable.get(rel.getSName()).replace(".xml", "");
			String base = relFileTable.get(rel.getSName());

			/**
			 * iterate over all annotations of one pointing relations
			 */
			for (SAnnotation anno : rel.getSAnnotations()) {
				// copy referenced files

				String annoString = null;
				// copy referenced files
				if (anno.getSValueSURI() != null) {
					annoString = copyFile(anno.getSValueSURI(), documentPath.toFileString());
				} else {
					annoString = StringEscapeUtils.escapeXml(anno.getSValueSTEXT());
				}

				/**
				 * create feat tag string
				 */
				StringBuffer featTag = new StringBuffer("\t\t").append("<").append(TAG_FEAT_FEAT).append(" ").append(ATT_HREF).append("=\"#").append(escapeNCName(rel.getSName())).append("\" ").append(ATT_FEAT_FEAT_VAL).append("=\"").append(annoString).append("\"/>");

				String type = anno.getQName().replace("::", ".");
				String paulaID = relationFile + "_" + type;
				String annoFileName = paulaID + ".xml";

				PrintWriter output = annoFileTable.get(annoFileName);

				/**
				 * If Reference is null, we have to create a anno file
				 */
				if (output != null) {
					output.println(featTag.toString());

				} else {
					File annoFile = new File(documentPath.toFileString() + "/" + annoFileName);
					try {
						if (!annoFile.exists()) {
							if (!(annoFile.createNewFile()))
								logger.warn("Cannot create file '" + annoFile.getName() + "', because it already exists.");
						}

						layerNodeFileNames.add(annoFile.getName());
						/**
						 * Write Preamble and Tag
						 */
						output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(annoFile), "UTF8")), false);
						output.write(createFileBeginning(PAULA_TYPE.FEAT, paulaID, type, base));
						output.println(featTag.toString());

						/**
						 * Put file (Writer) in FileTable for further access
						 */
						annoFileTable.put(annoFileName, output);

					} catch (IOException e) {
						throw new PepperModuleException(this, "mapRelAnnotations: Could not write File " + annoFile.getAbsolutePath() + ": " + e.getMessage());
					}
				}
			}
		}
		/**
		 * write all file closings and close the streams
		 */
		for (PrintWriter output : annoFileTable.values()) {
			output.println("\t</" + TAG_FEAT_FEATLIST + ">");
			output.println(PAULA_CLOSE_TAG);
			output.close();
		}
		annoFileTable = null;
	}

	/**
	 * Creates the mark tag for the span file , containing the overlapped tokens
	 * 
	 * @param sName
	 *            the span name
	 * @param dSFileMap
	 *            Hashtable containing all data source names and the containing
	 *            files (name)
	 * @param overlappedTokenList
	 *            List of overlapped tokens
	 * @param dataSourceCount
	 *            Number of textual data sources
	 * @param firstDSName
	 *            name of the first data source
	 * @return string representation of the mark tag
	 */
	private String createSpanFileMarkTag(String sName, Hashtable<String, String> dSFileMap, EList<SToken> overlappedTokenList, int dataSourceCount, String firstDSName) {

		if (sName.isEmpty())
			throw new PepperModuleException(this, "Cannot create span file mark tag: No span name was specified");
		if (dSFileMap == null)
			throw new PepperModuleException(this, "Cannot create span file mark tag: There is no token--DS file map");
		if (overlappedTokenList.isEmpty())
			throw new PepperModuleException(this, "Cannot create span file mark tag: There are no overlapped tokens");
		if (dataSourceCount == 0)
			throw new PepperModuleException(this, "Cannot create span file mark tag: There are no data sources");
		if (firstDSName.isEmpty())
			throw new PepperModuleException(this, "Cannot create span file mark tag: No first DS name was specified");

		/**
		 * get a list of textual relations in order to be able to check whether
		 * we found all tokens
		 */
		EList<STextualRelation> rel = overlappedTokenList.get(0).getSDocumentGraph().getSTextualRelations();

		String sTextualDSName;
		String tokenFile;
		String tokenPath;

		/**
		 * create the mark tag beginning (everything except the token id list)
		 */
		StringBuffer buffer = new StringBuffer("\t\t<").append(TAG_MARK_MARK).append(" ").append(ATT_ID).append("=\"").append(escapeNCName(sName)).append("\" ").append(ATT_HREF).append("=\"");

		/**
		 * If we only have one data source, we do not need to provide the file
		 * where each token is contained else we have to
		 */
		if (dataSourceCount == 1) {
			for (SToken token : overlappedTokenList) {
				if (overlappedTokenList.indexOf(token) < overlappedTokenList.size() - 1) {
					buffer.append("#").append(escapeNCName(token.getSName())).append(" ");
				} else {
					buffer.append("#").append(escapeNCName(token.getSName()));
				}
			}
		} else {
			for (SToken token : overlappedTokenList) {

				/**
				 * get the name of the sTextualRelation (token)
				 */
				sTextualDSName = rel.get(rel.indexOf(token)).getSTarget().getSName();
				/**
				 * get the token file: (path/to/DS/xx.tok.i.xml) -->
				 * xx.tok.i.xml
				 */
				tokenPath = dSFileMap.get(sTextualDSName);
				tokenFile = tokenPath.substring(tokenPath.lastIndexOf("/" + 1));
				/**
				 * write all tokens: (#tok_1, #tok_2 , ... , #tok_n-1, #tok_n)
				 * all tok names (except the last) have a following colon
				 * 
				 * if the token points to the base DS, ommit the base DS name
				 * else write dsName#tok_i
				 * 
				 */
				if (overlappedTokenList.indexOf(token) < overlappedTokenList.size() - 1) {

					if (sTextualDSName.equals(firstDSName)) {
						buffer.append("#").append(token.getSName()).append(" ");
					} else {
						buffer.append(tokenFile).append("#").append(token.getSName()).append(" ");
					}
				} else {
					if (sTextualDSName.equals(firstDSName)) {
						buffer.append("#").append(token.getSName()).append(" ");
					} else {
						buffer.append(tokenFile).append("#").append(token.getSName());
					}
				}
			}
		}
		buffer.append("\"/>");

		return buffer.toString();
	}

	/**
	 * Method for construction of the header paula files (struct, mark, feat,
	 * rel) preamble (Headers and StructList Tag)
	 * 
	 * @param paulaType
	 *            type of paula file see {@link PAULA_TYPE}
	 * @param paulaID
	 *            the PAULA ID (filename without file ending (.xml))
	 * @param type
	 *            the type, here mark
	 * @param base
	 *            base span/token/struct/ file (the first if there is more then
	 *            one)
	 * @return String representation of the Preamble
	 */
	private String createFileBeginning(PAULA_TYPE paulaType, String paulaID, String type, String base) {
		if (paulaType == null)
			throw new PepperModuleException(this, "Cannot create '" + paulaType + "' file beginning: This seems to be an internal problem.");
		if (paulaID.isEmpty())
			throw new PepperModuleException(this, "Cannot create '" + paulaType + "' file beginning: No Paula ID was specified");
		if (type.isEmpty())
			throw new PepperModuleException(this, "Cannot create '" + paulaType + "' file beginning: No type was specified");

		StringBuffer buffer = new StringBuffer(TAG_HEADER_XML);
		buffer.append(LINE_SEPARATOR);
		buffer.append(paulaType.getDocTypeTag());
		buffer.append(LINE_SEPARATOR).append(TAG_PAULA_OPEN);
		buffer.append(LINE_SEPARATOR).append("\t");
		buffer.append("<" + TAG_HEADER + " " + ATT_HEADER_PAULA_ID[0] + "=\"" + paulaID + "\"/>");
		buffer.append(LINE_SEPARATOR).append("\t");
		buffer.append("<" + paulaType.getListElementName()).append(" ");
		buffer.append(buildXMLNS("xlink", XLINK_URI)).append(" ");
		buffer.append(ATT_TYPE).append("=\"");
		buffer.append(type).append("\" ");
		if (base != null)
			buffer.append(ATT_BASE).append("=\"").append(base).append("\"");
		buffer.append(">");
		buffer.append(LINE_SEPARATOR);
		return buffer.toString();
	}
	
	/**
	 * Method for construction of the header paula files (struct, mark, feat,
	 * rel) preamble (Headers and StructList Tag)
	 * @param paulaType type of paula file see {@link PAULA_TYPE}
	 * @param paulaID the PAULA ID (filename without file ending (.xml))
	 * @param type the type, here mark
	 * @param base base span/token/struct/ file (the first if there is more then one)
	 * @return String representation of the Preamble
	 * @throws XMLStreamException 
	 */
	private void createFileBeginning(PAULA_TYPE paulaType, String paulaID, String type, String base, XMLStreamWriter xmlwriter) throws XMLStreamException{
		if (paulaType == null)
			throw new PepperModuleException(this, "Cannot create '" + paulaType + "' file beginning: This seems to be an internal problem.");
		if (paulaID.isEmpty())
			throw new PepperModuleException(this, "Cannot create '" + paulaType + "' file beginning: No Paula ID was specified");
		if (type.isEmpty())
			throw new PepperModuleException(this, "Cannot create '" + paulaType + "' file beginning: No type was specified");
		
		xmlwriter.writeStartDocument();
		xmlwriter.writeDTD(paulaType.getDocTypeTag());
		xmlwriter.writeStartElement(TAG_PAULA);
			xmlwriter.writeAttribute(ATT_VERSION, VERSION);
			xmlwriter.writeStartElement(TAG_HEADER);
				xmlwriter.writeAttribute(ATT_PAULA_ID, paulaID);
			xmlwriter.writeEndElement();
			xmlwriter.writeStartElement(paulaType.getListElementName());
				xmlwriter.writeNamespace("xlink", XLINK_URI);
				xmlwriter.writeAttribute(ATT_TYPE, type);
				if (base != null){
					xmlwriter.writeAttribute(ATT_BASE, base);
				}			
	}

	/**
	 * This Method creates the xml-namespace for a specified alias (e.g xlink)
	 * and a URI where the definition of the alias may be found
	 * 
	 * @param alias
	 *            alias for a namespace
	 * @param uri
	 *            the source of the alias definition
	 * @return string representation of the xmlns attribute
	 */
	private String buildXMLNS(String alias, String uri) {
		return (new StringBuffer()).append("xmlns:").append(alias).append("=\"").append(uri).append("\"").toString();
	}

	/**
	 * Checks whether the file is valid by the DTD which is noted in the file
	 * WARNING: the dtd-file needs to be in the same directory
	 * 
	 * @param fileToValidate
	 *            the file which needs to be validated
	 * @return true if the fileToValidate matches the specified DTD false else
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
			return false;
		} catch (SAXException e) {
			return false;
		} catch (IOException e) {
			return (false);
		}
		return true;
	}

	/**
	 * Method for copying file to outputPath. This is used for copying DTDs and
	 * media files (e.g. mp3-files)
	 * 
	 * @param file
	 *            the file (URI) to copy
	 * @param outputPath
	 *            the target path to copy to
	 * @return the filename in the form &quot; file:/filename &quot;
	 */
	private String copyFile(URI file, String outputPath) {
		File inFile = new File(file.toFileString());
		File outFile = new File(outputPath + "/" + inFile.getName());

		FileInputStream in = null;
		FileOutputStream out = null;
		String outFileString = null;
		try {
			in = new FileInputStream(file.toFileString());
			out = new FileOutputStream(outFile);
			int c;

			while ((c = in.read()) != -1) {
				out.write(c);
			}
			outFileString = "file:/" + outFile.getName();
		} catch (IOException e) {
			throw new PepperModuleException(this, "Cannot copy dtd '" + file + "' to path '" + outFileString + "'", e);
		}
		return outFileString;
	}

	/**
	 * Method for setting a reference to the path where the resources for the
	 * PAULAExporter (e.g. DTD-files) are located.
	 * 
	 * @param resources
	 */
	public static void setResourcePath(URI resources) {
		resourcePath = resources;
	}

	/**
	 * Implementation for FilenameFilter. This is needed for fetching only the
	 * DTD-files from resource path for copying to output folder.
	 */
	public boolean accept(File f, String s) {
		return s.toLowerCase().endsWith(".dtd");
	}
}