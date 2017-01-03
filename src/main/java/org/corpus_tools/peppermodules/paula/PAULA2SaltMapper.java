/**
 * Copyright 2009 Humboldt-Universität zu Berlin, INRIA.
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
package org.corpus_tools.peppermodules.paula;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.peppermodules.paula.readers.PAULASpecificReader;
import org.corpus_tools.peppermodules.paula.readers.PAULAStructReader;
import org.corpus_tools.peppermodules.paula.util.xPointer.XPtrInterpreter;
import org.corpus_tools.peppermodules.paula.util.xPointer.XPtrRef;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SDominanceRelation;
import org.corpus_tools.salt.common.SMedialDS;
import org.corpus_tools.salt.common.SMedialRelation;
import org.corpus_tools.salt.common.SPointingRelation;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SSpanningRelation;
import org.corpus_tools.salt.common.SStructure;
import org.corpus_tools.salt.common.SStructuredNode;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STextualRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SAnnotationContainer;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.graph.Identifier;
import org.eclipse.emf.common.util.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PAULA2SaltMapper extends PepperMapperImpl {
	private static final Logger logger = LoggerFactory.getLogger(PAULA2SaltMapper.class);

	public PAULA2SaltMapper() {
		initialize();
	}

	/**
	 * {@inheritDoc PepperMapperImpl#initialize()} Initializes some hashtables
	 * used for storing ids during mapping.
	 */
	@Override
	protected void initialize() {
		elementNamingTable = new Hashtable<String, String>();
		elementOrderTable = new Hashtable<String, Collection<String>>();
		this.stagingArea = new Hashtable<String, Identifier>();
	}

	private Boolean isArtificialSCorpus = false;

	public Boolean getIsArtificialSCorpus() {
		return isArtificialSCorpus;
	}

	public void setIsArtificialSCorpus(Boolean isArtificialSCorpus) {
		this.isArtificialSCorpus = isArtificialSCorpus;
	}

	/**
	 * {@inheritDoc PepperMapper#setDocument(SDocument)}
	 * 
	 * OVERRIDE THIS METHOD FOR CUSTOMIZED MAPPING.
	 */
	@Override
	public DOCUMENT_STATUS mapSCorpus() {
		if (!isArtificialSCorpus) {// only if SCorpus was not artificially
									// created and points to a real path and not
									// to the one of a SDocument
			PAULAFileDelegator paulaFileDelegator = new PAULAFileDelegator();
			paulaFileDelegator.setMapper(this);

			if (getResourceURI() == null) {
				throw new PepperModuleException(this,
						"Cannot map an SCorpus, because no resource path is given for '" + getCorpus().getId() + "'.");
			}
			File paulaPath = new File(getResourceURI().toFileString());
			paulaFileDelegator.setPaulaPath(paulaPath);
			// map all xml-documents
			for (File paulaFile : paulaPath.listFiles()) {
				String[] parts = paulaFile.getName().split("[.]");
				if (parts.length > 1) {
					for (String ending : this.getPAULA_FILE_ENDINGS()) {
						if (parts[parts.length - 1].equalsIgnoreCase(ending)) {
							paulaFileDelegator.getPaulaFiles().add(paulaFile);
						}
					}
				}
			}
			if ((paulaFileDelegator.getPaulaFiles() != null) && (paulaFileDelegator.getPaulaFiles().size() != 0))
				paulaFileDelegator.startPaulaFiles();
		} // only if SCorpus was not artificially created and points to a real
			// path and not to the one of a SDocument
			// map all xml-documents
		return (DOCUMENT_STATUS.COMPLETED);
	}

	/**
	 * {@inheritDoc PepperMapper#setDocument(SDocument)}
	 * 
	 * OVERRIDE THIS METHOD FOR CUSTOMIZED MAPPING.
	 */
	@Override
	public DOCUMENT_STATUS mapSDocument() {
		if (this.getResourceURI() == null) {
			throw new PepperModuleException(this,
					"Cannot map a paula-document to SDocument, because the path for paula-document is empty.");
		}
		if (!getResourceURI().toFileString().endsWith("/")) {
			setResourceURI(URI.createFileURI(getResourceURI().toFileString() + "/"));
		}
		if (getDocument() == null) {
			throw new PepperModuleException(this,
					"Cannot map a paula-document to SDocument, because the SDocument is empty.");
		}
		if ((this.getPAULA_FILE_ENDINGS() == null) || (this.getPAULA_FILE_ENDINGS().length == 0)) {
			throw new PepperModuleException(this,
					"Cannot map a paula-document to SDocument, no paula-xml-document endings are given.");
		}

		// create SDocumentGraph
		SDocumentGraph sDocGraph = SaltFactory.createSDocumentGraph();
		sDocGraph.setName(getDocument().getName() + "_graph");
		getDocument().setDocumentGraph(sDocGraph);
		// create SDocumentGraph

		PAULAFileDelegator paulaFileDelegator = new PAULAFileDelegator();
		paulaFileDelegator.setMapper(this);
		File paulaPath = new File(this.getResourceURI().toFileString());
		paulaFileDelegator.setPaulaPath(paulaPath);

		// map all xml-documents
		for (File paulaFile : paulaPath.listFiles()) {
			String[] parts = paulaFile.getName().split("[.]");
			if (parts.length > 1) {
				for (String ending : this.getPAULA_FILE_ENDINGS()) {
					if (parts[parts.length - 1].equalsIgnoreCase(ending)) {
						paulaFileDelegator.getPaulaFiles().add(paulaFile);
					}
				}
			}
		}
		paulaFileDelegator.startPaulaFiles();

		return (DOCUMENT_STATUS.COMPLETED);
	}

	// ================================================ start: handling
	// PAULA-file endings
	/**
	 * Stores the endings which are used for paula-files
	 */
	private String[] PAULA_FILE_ENDINGS = null;

	/**
	 * @param pAULA_FILE_ENDINGS
	 *            the pAULA_FILE_ENDINGS to set
	 */
	public void setPAULA_FILE_ENDINGS(String[] pAULA_FILE_ENDINGS) {
		this.PAULA_FILE_ENDINGS = pAULA_FILE_ENDINGS;
	}

	/**
	 * @return the pAULA_FILE_ENDINGS
	 */
	public String[] getPAULA_FILE_ENDINGS() {
		return this.PAULA_FILE_ENDINGS;
	}

	// ======================================= start: staging area
	/**
	 * Stores the nodes and relations which has not been seen, but were
	 * referenced from other nodes. These nodes and relations are already in the
	 * SDocument-graph.
	 */
	private Hashtable<String, Identifier> stagingArea = null;
	// ======================================= end: staging area
	/**
	 * global naming table for all elements stores paulaId of one element and
	 * corresponding salt id PAULAId, SaltId
	 */
	private Map<String, String> elementNamingTable = null;

	/**
	 * Seperator, to be used in uniquenames.
	 */
	private static final String KW_NAME_SEP = "#";

	/**
	 * stores paula-document-names and corresponding paula-elements in readed
	 * order importent for interpreting xpointer (ranges)
	 */
	private Map<String, Collection<String>> elementOrderTable = null;

	/**
	 * Extracts the namespace of the paula file name and returns it.
	 * 
	 * @param paulaFile
	 *            the name of the paula file
	 * @return the namespace of the file.
	 */
	private String extractNSFromPAULAFile(File paulaFile) {
		String retVal = null;
		if (paulaFile != null) {
			String[] parts = paulaFile.getName().split("[.]");
			if (parts[0] != null && !parts[0].equals(getProps().getEmptyNamespace())) {
				retVal = parts[0];
			}
		}
		return (retVal);
	}

	/**
	 * Attaches the given sNode to the sLayer, corresponding to the given layer
	 * name. If no Layer for this name exists, a new one will be created.
	 * 
	 * @param sNode
	 *            node which shall be attached
	 * @param sLayerName
	 *            name of the SLayer
	 */
	private SLayer attachSNode2SLayer(SNode sNode, String sLayerName) {
		SLayer retVal = null;

		if (sLayerName != null && sNode != null) {

			// search if layer already exists
			for (SLayer sLayer : getDocument().getDocumentGraph().getLayers()) {
				if (sLayer.getName().equalsIgnoreCase(sLayerName)) {
					retVal = sLayer;
					break;
				}
			}

			if (retVal == null) {// create new layer if not exists
				retVal = SaltFactory.createSLayer();
				retVal.setName(sLayerName);
				getDocument().getDocumentGraph().addLayer(retVal);
			} // create new layer if not exists

			// add sNode to sLayer
			sNode.addLayer(retVal);
		}
		return (retVal);
	}

	/**
	 * Attaches the given sRelation to the sLayer, corresponding to the given
	 * layer name. If no Layer for this name exists, a new one will be created.
	 * 
	 * @param sNode
	 *            node which shall be attached
	 * @param sLayerName
	 *            name of the SLayer
	 */
	private SLayer attachSRelation2SLayer(SRelation sRel, String sLayerName) {
		SLayer retVal = null;

		if (sLayerName != null && sRel != null) {
			// search if layer already exists
			for (SLayer sLayer : getDocument().getDocumentGraph().getLayers()) {
				if (sLayer.getName().equalsIgnoreCase(sLayerName)) {
					retVal = sLayer;
					break;
				}
			}

			if (retVal == null) {// create new layer if not exists
				retVal = SaltFactory.createSLayer();
				retVal.setName(sLayerName);
				getDocument().getDocumentGraph().addLayer(retVal);
			} // create new layer if not exists

			// add sNode to sLayer
			sRel.addLayer(retVal);
		}
		return (retVal);
	}

	// =============================================== start: PAULA-connectors
	/**
	 * Recieves data from PAULATextReader and maps them to Salt.
	 * 
	 * @param corpusPath
	 * @param paulaFile
	 * @param paulaId
	 * @param text
	 * @throws Exception
	 */
	public void paulaTEXTConnector(File paulaFile, String paulaId, String text) {
		if (getDocument() == null) {
			throw new PepperModuleException(this,
					"Cannot map primary data to salt document, because no salt document is given.");
		}
		if (getDocument().getDocumentGraph() == null) {
			throw new PepperModuleException(this,
					"Cannot map primary data to salt document, because no salt document-graph is given.");
		}
		// create uniqueName
		String uniqueName = paulaFile.getName();
		String saltName = "text";
		String[] splittedName = paulaFile.getName().split("[.]");
		if (splittedName.length >= 4) {
			// ns.<...>.name.text.xml
			saltName = splittedName[splittedName.length - 3];
		}
		STextualDS sTextualDS = null;

		// check staging area
		if (this.stagingArea.containsKey(uniqueName)) {
			// take node which already exists in SDocumentGraph
			sTextualDS = (STextualDS) getDocument().getDocumentGraph()
					.getNode(this.stagingArea.get(uniqueName).getId());
		} // take node which already exists in SDocumentGraph
		else {// create new node for SDocument-graph
				// create element
			sTextualDS = SaltFactory.createSTextualDS();
			sTextualDS.setName(saltName);
			getDocument().getDocumentGraph().addNode(sTextualDS);
		} // create new node for SDocument-graph

		sTextualDS.setText(text);
		// create entry in naming table
		elementNamingTable.put(uniqueName, sTextualDS.getId());
	}

	/**
	 * Recieves data from PAULAMarkReader in case of paula-type= tok and maps
	 * them to Salt.
	 * 
	 * @param corpusPath
	 * @param paulaFile
	 * @param paulaId
	 * @param text
	 * @throws Exception
	 */
	public void paulaMARK_TOKConnector(File paulaFile, String paulaId, String paulaType, String xmlBase, String markID,
			String href, String markType) {
		String uniqueName = paulaFile.getName() + KW_NAME_SEP + markID;
		{
			if (elementNamingTable == null)
				throw new PepperModuleException(this,
						"The map elementNamingTable was not initialized, this might be a bug.");
			// create entry in element order table (file: elements)
			if (elementOrderTable.get(paulaFile.getName()) == null) {
				Collection<String> orderedElementSlot = new ArrayList<>();
				elementOrderTable.put(paulaFile.getName(), orderedElementSlot);
			}
			Collection<String> orderedElementSlot = elementOrderTable.get(paulaFile.getName());
			orderedElementSlot.add(uniqueName);
		}

		// Objekt zum Interpretieren des XLinks in mark.href initialisieren
		List<XPtrRef> xPtrRefs = null;

		// extract
		XPtrInterpreter xPtrInter = new XPtrInterpreter();
		xPtrInter.setInterpreter(xmlBase, href);
		try {
			xPtrRefs = xPtrInter.getResult();
		} catch (Exception e) {
			throw new PepperModuleException(this, "Cannot read href (" + href + ") in file " + paulaFile + ".", e);
		}

		int runs = 0;
		// search for STextualDS
		STextualDS sTextDS = null;
		Integer left = null; // left offset
		Integer right = null; // right offset
		for (XPtrRef xPtrRef : xPtrRefs) {
			if (xPtrRef.getDoc() == null) {
				throw new PepperModuleException(this, "Cannot find a file reference in xpointer '" + xPtrRef + "'.");
			}
			runs++;
			// if there is more than one reference
			if (runs > 1) {
				throw new PepperModuleException(this,
						"There are too many references for a token node element: " + href);
			}
			// when XPointer refers to a text
			else if (xPtrRef.getType() == XPtrRef.POINTERTYPE.TEXT) {
				String textNodeName = elementNamingTable.get(xPtrRef.getDoc());
				sTextDS = (STextualDS) getDocument().getDocumentGraph().getNode(textNodeName);
				if (sTextDS == null) {
					throw new PepperModuleException(this,
							"Cannot create token '" + markID + "' of file '" + paulaId
									+ "', because the referred TextualDS object for text '" + textNodeName
									+ "' is empty. Known STextualDS objects are: " + elementNamingTable + ". ");
				}
				try {
					left = Integer.valueOf(xPtrRef.getLeft());
					right = Integer.valueOf(xPtrRef.getRight());
					// arrange left and right value
					left = left - 1;
					right = left + right;
					if (left > right) {
						throw new PepperModuleException(this,
								"Cannot create token, because its left value is higher than its right value. Error in document "
										+ paulaFile.getName() + ". The left value is '" + left
										+ "', hwereas the right value is '" + right + "'.");
					}
					if (left < 0) {
						throw new PepperModuleException(this,
								"Cannot create token, because its left value is smaller than 0. Error in document "
										+ paulaFile.getName() + ". The left value is '" + left + "'.");
					}
					if (right > sTextDS.getText().length()) {
						throw new PepperModuleException(this,
								"Cannot create token, because its right value is higher than the size of the text. Error in document "
										+ paulaFile.getName() + ". The right value is '" + right
										+ "', but the length of the text only has the length of '"
										+ sTextDS.getText().length() + "'.");
					}
				} catch (Exception e) {
					throw new PepperModuleException(this,
							"The left or right border of XPointer is not set in a correct way: " + href, e);
				}
			}
			// when XPointer does not refer to a text
			else {
				throw new PepperModuleException(this,
						"An XPointer of the parsed document does not refer to a xml-textelement. Incorrect pointer: "
								+ "base: " + xPtrRef.getDoc() + ", element: " + href + ", type: " + xPtrRef.getType());
			}
		}
		// if no sTextDS exists-> error
		if (sTextDS == null) {
			throw new PepperModuleException(this,
					"No primary data node found for token element: " + paulaFile.getName() + KW_NAME_SEP + markID);
		}
		// create SToken object
		SToken sToken = SaltFactory.createSToken();
		// sToken.setName(markID);
		sToken.setName(markID);
		getDocument().getDocumentGraph().addNode(sToken);

		// create entry in naming table
		elementNamingTable.put(uniqueName, sToken.getId());

		// create relation
		STextualRelation textRel = SaltFactory.createSTextualRelation();
		textRel.setSource(sToken);
		textRel.setTarget(sTextDS);
		textRel.setStart(left);
		textRel.setEnd(right);
		getDocument().getDocumentGraph().addRelation(textRel);
	}

	/**
	 * Returns a list of all paula-element-ids refered by the given
	 * xpointer-expression.
	 * 
	 * @param xmlBase
	 * @param href
	 */
	private Collection<String> getPAULAElementIds(String xmlBase, String href) {
		Collection<String> refPaulaIds = null;
		try {
			refPaulaIds = new ArrayList<String>();
			XPtrInterpreter xPtrInter = new XPtrInterpreter();
			xPtrInter.setInterpreter(xmlBase, href);
			List<XPtrRef> xPtrRefs = null;
			try {
				xPtrRefs = xPtrInter.getResult();
			} catch (Exception e) {
				// workaround if href are sequences of shorthand pointers like
				// "#id1 id2 id3"
				if ((href != null) && (href.contains(" "))) {
					String hrefs[] = href.split(" ");
					if (hrefs.length > 0) {
						xPtrRefs = new ArrayList<XPtrRef>();
						for (String idPtr : hrefs) {
							xPtrInter = new XPtrInterpreter();
							xPtrInter.setInterpreter(xmlBase, idPtr);
							try {
								xPtrRefs.addAll(xPtrInter.getResult());
							} catch (Exception e1) {
								throw new PepperModuleException(this, "Cannot parse xpointer in document '"
										+ getResourceURI() + "' because of a nested exception.  ", e);
							}
						}
					}
				} else
					throw e;
			} // workaround if href are sequences of shorthand pointers like
				// "#id1 id2 id3"
			for (XPtrRef xPtrRef : xPtrRefs) {
				// Fehler, wenn XPointer-Reference vom falschen Typ
				if (xPtrRef.getType() != XPtrRef.POINTERTYPE.ELEMENT)
					throw new PepperModuleException(this,
							"The XPointer references in current file are incorrect. There only have to be element pointers and the following is not one of them: "
									+ href + ". Error in file: " + xmlBase);

				// wenn XPointer-Bezugsknoten einen Bereich umfasst
				if (xPtrRef.isRange()) {
					// erzeuge den Namen des linken Bezugsknotens
					String leftName = xPtrInter.getDoc() + KW_NAME_SEP + xPtrRef.getLeft();
					// erzeuge den Namen des rechten Bezugsknotens
					String rightName = xPtrInter.getDoc() + KW_NAME_SEP + xPtrRef.getRight();
					// extract all paula elements which are refered by this
					// pointer
					{

						boolean start = false;
						for (String paulaElementId : elementOrderTable.get(xPtrInter.getDoc())) {
							// if true, first element was found
							if (paulaElementId.equalsIgnoreCase(leftName))
								start = true;
							// if start of range is reached
							if (start) {
								refPaulaIds.add(paulaElementId);
							}
							// if last element was found, break
							if (paulaElementId.equalsIgnoreCase(rightName))
								break;
						}
					}
				}
				// wenn XPointer-Bezugsknoten einen einzelnen Knoten
				// referenziert
				else {
					String paulaElementId = xPtrRef.getDoc() + KW_NAME_SEP + xPtrRef.getID();
					refPaulaIds.add(paulaElementId);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new PepperModuleException(this,
					"Cannot compute paula-ids corresponding to xmlBase '" + xmlBase + "' and href '" + href + "'.", e);
		}

		return (refPaulaIds);
	}

	/**
	 * Recieves data from PAULAMarkReader and maps them to Salt.
	 * 
	 * @param corpusPath
	 * @param paulaFile
	 * @param paulaId
	 * @param text
	 * @throws Exception
	 */
	public void paulaMARKConnector(File paulaFile, String paulaId, String paulaType, String xmlBase, String markID,
			String href, String markType) {
		// create unique name for current node
		String uniqueName = paulaFile.getName() + KW_NAME_SEP + markID;
		{
			if (elementNamingTable == null)
				throw new PepperModuleException(this,
						"The map elementNamingTable was not initialized, this might be a bug.");
			// create entry in element order table (file: elements)
			if (elementOrderTable.get(paulaFile.getName()) == null) {
				Collection<String> orderedElementSlot = new ArrayList<String>();
				elementOrderTable.put(paulaFile.getName(), orderedElementSlot);
			}
			Collection<String> orderedElementSlot = elementOrderTable.get(paulaFile.getName());
			orderedElementSlot.add(uniqueName);
		}
		// create list of all refered elements
		Collection<String> refPAULAElementIds = this.getPAULAElementIds(xmlBase, href);

		List<SNode> referedElements = new ArrayList<SNode>();
		for (String refPAULAId : refPAULAElementIds) {
			String paulaIdEntry = elementNamingTable.get(refPAULAId);
			if (paulaIdEntry == null)
				throw new PepperModuleException(this, "Cannot map the markable '" + markID + "' of file '" + paulaId
						+ "', because the reference '" + refPAULAId + "'does not exist.");
			SNode dstElement = getDocument().getDocumentGraph().getNode(paulaIdEntry);
			if (dstElement == null) {
				logger.warn("[PAULAImporter] Cannot create span, because destination does not exist in graph: "
						+ refPAULAId + ". Error in file: " + this.getResourceURI().toFileString());
			} else
				referedElements.add(dstElement);
		}
		// if list of refered elements is empty, don't put relation or
		// referncing element in graph
		if (referedElements.size() == 0) {

			logger.warn("[PAULAImporter] Cannot create span, because it has no destination elements: " + uniqueName
					+ ". Error in file: " + this.getResourceURI().toFileString());
		} else {

			// create span element
			SSpan sSpan = SaltFactory.createSSpan();
			sSpan.setName(markID);
			getDocument().getDocumentGraph().addNode(sSpan);

			// adding sSpan to layer
			String sLayerName = this.extractNSFromPAULAFile(paulaFile);
			this.attachSNode2SLayer(sSpan, sLayerName);

			// create entry in naming table
			elementNamingTable.put(uniqueName, sSpan.getId());

			// create relations for all referenced tokens
			SSpanningRelation sSpanRel = null;
			for (String refPAULAId : refPAULAElementIds) {
				SNode dstNode = getDocument().getDocumentGraph().getNode(elementNamingTable.get(refPAULAId));
				if (dstNode == null) {

					logger.warn("[PAULAImporter] Cannot create span, because destination does not exist in graph: "
							+ refPAULAId + ". Error in file: " + this.getResourceURI().toFileString());
				} else {
					if (!(dstNode instanceof SToken)) {
						throw new PepperModuleException(this, "The referred Target Node '" + refPAULAId
								+ "' in document '" + xmlBase + "'is not of type SToken.");
					}
					sSpanRel = SaltFactory.createSSpanningRelation();
					sSpanRel.setSource(sSpan);
					sSpanRel.setTarget((SToken) dstNode);
					getDocument().getDocumentGraph().addRelation(sSpanRel);
					// adding sSpanRel to layer
					sLayerName = this.extractNSFromPAULAFile(paulaFile);
					attachSRelation2SLayer(sSpanRel, sLayerName);
				}
			}
		}
	}

	private static final String KW_FILE_VAL = "file:/";

	/**
	 * Receives data from PAULAFeatReader and maps them to Salt.
	 * 
	 * @param corpusPath
	 * @param paulaFile
	 * @param paulaId
	 * @param text
	 * @throws Exception
	 */
	public void paulaFEATConnector(File paulaFile, String paulaId, String paulaType, String xmlBase, String featID,
			String featHref, String featTar, String featVal, String featDesc, String featExp) {
		if ((paulaType == null) || (paulaType.isEmpty())) {
			logger.warn("[PAULAImporter] Cannot work with the given annotation of element: " + paulaId
					+ ", because the type-value is empty. Error in file: " + paulaFile + ".");
		} else {
			if ((featVal == null) || (featVal.isEmpty())) {
				logger.warn("[PAULAImporter] The feature value of an element in the following file is empty: "
						+ paulaFile + ". Therefore this feature is ignored. ");
			}

			Collection<String> paulaElementIds = this.getPAULAElementIds(xmlBase, featHref);
			SAnnotation sAnno = SaltFactory.createSAnnotation();

			if ((paulaType != null) && (!paulaType.isEmpty())) {
				// extract type name and namespace
				String[] parts = paulaType.split("[.]");
				if ((parts != null) && (parts.length > 0)) {
					sAnno.setName(parts[parts.length - 1]);
				}
				if ((parts != null) && (parts.length > 1)) {// namespace exists
					String namespace = "";
					for (int i = 0; i < parts.length - 1; i++) {
						if (i == 0)
							namespace = parts[0];
						else
							namespace = namespace + "." + parts[i];
						i++;
					}
					sAnno.setNamespace(namespace);
				} // namespace exists
				else if (getProps().getAnnoNamespaceFromFile()) {// compute
																	// namespace
																	// from file
																	// name
					String annoNamespace = this.extractNSFromPAULAFile(paulaFile);
					if (annoNamespace != null && !annoNamespace.isEmpty()) {
						sAnno.setNamespace(annoNamespace);
					}
				} // compute namespace from file name

				// a featVal can contain a simple textual value or even a file
				// reference, to find out whether a featVal is a file or a
				// simple value, we check whether the string contains a '.'
				// followed by another character and whether the file exists
				File file = null;
				if (featVal != null) {
					char[] featChar = featVal.toCharArray();
					boolean hasPeriod = false;
					boolean lastChrIsNotPeriod = false;
					for (char chr : featChar) {
						if (chr == '.') {
							hasPeriod = true;
						}
						if (hasPeriod && chr != '.') {
							lastChrIsNotPeriod = true;
						}
					}
					if (hasPeriod && lastChrIsNotPeriod) {
						URI location = URI.createFileURI(featVal).resolve(getResourceURI());
						file = new File(location.toFileString());
						if (!file.exists()) {
							file = null;
						}
					}
				}
				if (file != null) {
					// if featVal is a file reference and of type audio,
					// create an SAudio
					if (PAULAXMLDictionary.KW_AUDIO.equalsIgnoreCase(sAnno.getName())) {
						SMedialDS audio = SaltFactory.createSMedialDS();
						audio.setMediaReference(URI.createFileURI(file.getAbsolutePath()));
						getDocument().getDocumentGraph().addNode(audio);
						for (String paulaElementId : paulaElementIds) {
							if ((paulaElementId == null) || (paulaElementId.isEmpty())) {
								throw new PepperModuleException(this,
										"No element with xml-id:" + paulaElementId + " was found.");
							}
							String sElementName = elementNamingTable.get(paulaElementId);
							SNode refNode = getDocument().getDocumentGraph().getNode(sElementName);
							if (refNode != null) {
								List<SToken> tokens = getDocument().getDocumentGraph().getOverlappedTokens(refNode);
								if (tokens != null) {
									for (SToken tok : tokens) {
										SMedialRelation rel = SaltFactory.createSMedialRelation();
										rel.setTarget(audio);
										rel.setSource(tok);
										getDocument().getDocumentGraph().addRelation(rel);
									}
								}
							}
						}
						sAnno = null;
					} else {
						sAnno.setValue(URI.createFileURI(file.getAbsolutePath()));
					}
				} else {
					sAnno.setValue(featVal);
				}
			}
			if (sAnno != null) {
				// sanno is null, if annotation had an audio file as value
				for (String paulaElementId : paulaElementIds) {
					if ((paulaElementId == null) || (paulaElementId.isEmpty())) {
						throw new PepperModuleException(this,
								"No element with xml-id:" + paulaElementId + " was found.");
					}
					String sElementName = elementNamingTable.get(paulaElementId);
					if (sElementName == null) {
						logger.warn(
								"[PAULAImporter] An element was reffered by an annotation, which does not exist in paula file. The missing element is '"
										+ paulaElementId + "' and it was refferd in file'" + paulaFile.getAbsolutePath()
										+ "'.");
					} else {
						SNode refElement = getDocument().getDocumentGraph().getNode(sElementName);
						SRelation refRelation = getDocument().getDocumentGraph().getRelation(sElementName);
						if (refElement != null) {
							try {
								refElement.addAnnotation(sAnno);
							} catch (Exception e) {
								logger.warn("[PAULAImporter] Exception in paula file: "
										+ this.getResourceURI().toFileString() + " at element: " + featHref
										+ ". Original message is: " + e.getMessage());
							}
						} else if (refRelation != null) {
							refRelation.addAnnotation(sAnno);
						} else {
							throw new PepperModuleException(this,
									"No element with xml-id:" + paulaElementId + " was found.");
						}
					}
				}
			}
		}
	}

	/**
	 * Recieves data from PAULARelReader and maps them to Salt.
	 * 
	 * @param corpusPath
	 * @param paulaFile
	 * @param paulaId
	 * @param text
	 * @throws Exception
	 */
	public void paulaRELConnector(File paulaFile, String paulaId, String paulaType, String xmlBase, String relId,
			String srcHref, String dstHref) {
		if (((srcHref == null) || (srcHref.isEmpty())) || ((dstHref == null) || (dstHref.isEmpty()))
				|| ((srcHref.equalsIgnoreCase("empty") || (dstHref.equalsIgnoreCase("empty"))))) {

			logger.warn("[PAULAImporter] Cannot create pointing relation of file (" + paulaFile.getName()
					+ "), because source or destination is empty (see element '" + relId + "').");
		} else {
			if (srcHref.equalsIgnoreCase(dstHref))
				logger.warn("[PAULAImporter] Cannot create the pointing relation '" + srcHref + "' to '" + dstHref
						+ "' in document '" + getDocument().getId()
						+ "', because it is a cycle. The cycle was found in file (" + paulaFile.getName() + ").");
			else {
				Collection<String> paulaSrcElementIds = this.getPAULAElementIds(xmlBase, srcHref);
				Collection<String> paulaDstElementIds = this.getPAULAElementIds(xmlBase, dstHref);
				if ((paulaSrcElementIds == null) || (paulaSrcElementIds.size() == 0))
					throw new PepperModuleException(this,
							"The source of pointing relation in file: " + paulaFile.getName() + " is not set.");
				if ((paulaDstElementIds == null) || (paulaDstElementIds.size() == 0))
					throw new PepperModuleException(this,
							"The destination of pointing relation in file: " + paulaFile.getName() + " is not set.");
				if (elementNamingTable == null)
					throw new PepperModuleException(this,
							"The map elementNamingTable was not initialized, this might be a bug.");
				// if there are more than one sources or destinations create
				// cross product
				for (String paulaSrcElementId : paulaSrcElementIds) {
					for (String paulaDstElementId : paulaDstElementIds) {
						String saltSrcName = elementNamingTable.get(paulaSrcElementId);
						String saltDstName = elementNamingTable.get(paulaDstElementId);
						if ((saltSrcName == null) || (saltSrcName.isEmpty())) {
							logger.warn("[PAULAImporter] The requested source of relation (xml-id: " + paulaSrcElementId
									+ ") of file '" + paulaFile.getName() + "' does not exist.");
							return;
						}
						SPointingRelation pRel = SaltFactory.createSPointingRelation();
						// SDominanceRelation pRel=
						// SaltFactory.createSDominanceRelation();
						if ((saltDstName == null) || (saltDstName.isEmpty())) {
							logger.warn("[PAULAImporter] The requested destination of relation (xml-id: "
									+ paulaDstElementId + ") of file '" + paulaFile.getName() + "' does not exist.");
							return;
						}
						pRel.setName(relId);
						pRel.setType(paulaType);
						pRel.setSource((SStructuredNode) getDocument().getDocumentGraph().getNode(saltSrcName));
						pRel.setTarget((SStructuredNode) getDocument().getDocumentGraph().getNode(saltDstName));
						getDocument().getDocumentGraph().addRelation(pRel);
						// adding sSpanRel to layer
						String sLayerName = this.extractNSFromPAULAFile(paulaFile);
						attachSRelation2SLayer(pRel, sLayerName);
						// adding sSpanRel to layer

						// write SPointingRelation in elementNamingTable, to map
						// it with its paula id
						String uniqueName = paulaFile.getName() + KW_NAME_SEP + relId;
						elementNamingTable.put(uniqueName, pRel.getIdentifier().getId());
						// write SPointingRelation in elementNamingTable, to map
						// it with its paula id
					}
				}
			}
		}
	}

	/**
	 * Recieves data from PAULAFeatReader and maps them to Salt. To call in case
	 * of feats for corpus or document.
	 * 
	 * @param corpusPath
	 * @param paulaFile
	 * @param paulaId
	 * @param text
	 * @throws Exception
	 */
	public void paulaFEAT_METAConnector(File paulaFile, String paulaId, String paulaType, String xmlBase, String featID,
			String featHref, String featTar, String featVal, String featDesc, String featExp) {
		if ((paulaType == null) || (paulaType.isEmpty())) {
			logger.warn(
					"[PAULAImporter] Cannot add the given meta-annotation, because no annotation name is given in file '"
							+ paulaFile + "'.");
			return;
		}
		// creates a fullName for this meta annotation
		String fullName = paulaType;

		SAnnotationContainer sMetaAnnotatableElement = null;
		if (getDocument() != null) {
			sMetaAnnotatableElement = getDocument();
		} else if (this.getCorpus() != null) {
			sMetaAnnotatableElement = this.getCorpus();
		} else {
			throw new PepperModuleException(this, "Cannot map sMetaAnnotation '" + fullName + "=" + featVal
					+ "', because neither a SDocument object nor a SCorpus object is given. This might be a bug in PAULAModules.");
		}

		if (sMetaAnnotatableElement.getMetaAnnotation(fullName) == null)
			sMetaAnnotatableElement.createMetaAnnotation(null, paulaType, featVal);
	}

	/**
	 * Needed for storing dominance relations out of paula-struct-documents.
	 * Pre-Storing is necessary, because of struct-elements can refer to other
	 * struct-elements which aren't read at this time. Therefore the relations
	 * can be stored after reading all elements.
	 * 
	 * @author Florian Zipser
	 * 
	 */
	private static class DominanceRelationContainer {
		public String paulaId = null;
		public SDominanceRelation relation = null;
		public String xmlBase = null;
		public String href = null;
	}

	private Hashtable<File, List<DominanceRelationContainer>> dominanceRelationContainers = null;

	/**
	 * Recieves data from PAULAStrcutReader and maps them to Salt. T
	 * 
	 * @param corpusPath
	 * @param paulaFile
	 * @param paulaId
	 * @param text
	 * @throws Exception
	 */
	public void paulaSTRUCTConnector(File paulaFile, String paulaId, String paulaType, String xmlBase, String structID,
			String relID, String relHref, String relType) {
		// create unique name for element
		String uniqueNameStruct = paulaFile.getName() + KW_NAME_SEP + structID;
		String uniqueNameRel = paulaFile.getName() + KW_NAME_SEP + relID;
		// compute xml-base if given is empty
		if ((xmlBase == null) || (xmlBase.isEmpty())) {
			// if xml-base is empty, than set xml-base to current processed
			// paula-file
			xmlBase = paulaFile.getName();
		}

		// create entry in element order table (file: elements)
		if (elementOrderTable.get(paulaFile.getName()) == null) {
			Collection<String> orderedElementSlot = new ArrayList<String>();
			elementOrderTable.put(paulaFile.getName(), orderedElementSlot);
		}
		// check if struct is already inserted
		Collection<String> orderedElementSlot = elementOrderTable.get(paulaFile.getName());
		if (!orderedElementSlot.contains(uniqueNameStruct)) {
			orderedElementSlot.add(uniqueNameStruct);
		}
		if (!orderedElementSlot.contains(uniqueNameRel)) {
			orderedElementSlot.add(uniqueNameRel);
		}

		if (elementNamingTable.get(uniqueNameStruct) == null) {
			// create struct element
			SStructure sStruct = SaltFactory.createSStructure();
			sStruct.setName(structID);

			// sStruct.setId(structID); //not possible, because these id's are
			// not unique for one document file+id is unique but long
			getDocument().getDocumentGraph().addNode(sStruct);

			// adding sStruct to layer
			String sLayerName = this.extractNSFromPAULAFile(paulaFile);
			this.attachSNode2SLayer(sStruct, sLayerName);

			// create entry in naming table for struct
			elementNamingTable.put(uniqueNameStruct, sStruct.getId());
		}

		// pre creating relation
		SDominanceRelation domRel = SaltFactory.createSDominanceRelation();
		domRel.setName(relID);
		String saltDstName = elementNamingTable.get(uniqueNameStruct);
		domRel.setSource((SStructure) getDocument().getDocumentGraph().getNode(saltDstName));
		if ((relType != null) && (!relType.isEmpty())) {
			domRel.setType(relType);
		}

		// creating new container list
		if (dominanceRelationContainers == null)
			dominanceRelationContainers = new Hashtable<File, List<DominanceRelationContainer>>();

		List<DominanceRelationContainer> domRelSlot = null;
		domRelSlot = dominanceRelationContainers.get(paulaFile);
		if (domRelSlot == null) {
			domRelSlot = new ArrayList<DominanceRelationContainer>();
			this.dominanceRelationContainers.put(paulaFile, domRelSlot);
		}

		// creating dominance relation container
		DominanceRelationContainer domCon = new DominanceRelationContainer();
		domCon.paulaId = uniqueNameRel;
		domCon.relation = domRel;
		domCon.xmlBase = xmlBase;
		domCon.href = relHref;
		domRelSlot.add(domCon);
	}

	/**
	 * Will be called at the end of xml-document processing. In case of paula
	 * file was of type struct.dtd, all nodes and relations will now be added to
	 * graph.
	 */
	public void endDocument(PAULASpecificReader paulaReader, File paulaFile) {
		if (paulaReader instanceof PAULAStructReader) {
			// if PAULAReader is PAULAStructReader storing dominance relations
			// in graph
			if (dominanceRelationContainers != null) {
				for (DominanceRelationContainer domCon : dominanceRelationContainers.get(paulaFile)) {
					Collection<String> refPAULAElementIds = this.getPAULAElementIds(domCon.xmlBase, domCon.href);
					for (String refPAULAId : refPAULAElementIds) {
						String sNodeName = elementNamingTable.get(refPAULAId);
						if (sNodeName == null) {
							throw new PepperModuleException(this,
									"An element is referred, which was not already read. The reffered element is '"
											+ refPAULAId + "' and it was reffered in file '" + paulaFile + "'.");
						}
						SNode dstNode = getDocument().getDocumentGraph().getNode(sNodeName);
						if (dstNode == null) {
							throw new PepperModuleException(this,
									"No paula element with name: " + refPAULAId + " was found.");
						}
						domCon.relation.setTarget((SStructuredNode) dstNode);
						getDocument().getDocumentGraph().addRelation(domCon.relation);
						// adding sSpanRel to layer
						String sLayerName = this.extractNSFromPAULAFile(paulaFile);
						attachSRelation2SLayer(domCon.relation, sLayerName);
						// adding sSpanRel to layer
						// create entry in naming table for struct
						if (elementNamingTable.get(domCon.paulaId) == null) {
							elementNamingTable.put(domCon.paulaId, domCon.relation.getId());
						}
					}
				}
				dominanceRelationContainers = null;
			}
		} // if PAULAReader is PAULAStructReader
	}

	public PAULAImporterProperties getProps() {
		return (PAULAImporterProperties) getProperties();
	}

	// =============================================== end: PAULA-connectors
}
