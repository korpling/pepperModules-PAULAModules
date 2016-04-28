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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.PepperImporter;
import org.corpus_tools.pepper.modules.PepperModule;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.pepper.util.XMLStreamWriter;
import org.corpus_tools.salt.common.SDominanceRelation;
import org.corpus_tools.salt.common.SMedialDS;
import org.corpus_tools.salt.common.SMedialRelation;
import org.corpus_tools.salt.common.SPointingRelation;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SStructure;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STextualRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SAnnotationContainer;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SMetaAnnotation;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SPathElement;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.graph.IdentifiableElement;
import org.corpus_tools.salt.graph.Label;
import org.corpus_tools.salt.graph.Relation;
import org.eclipse.emf.common.util.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * Maps SCorpusGraph objects to a folder structure and maps a SDocumentStructure
 * to the necessary files containing the document data in PAULA notation.
 * 
 * @author Mario Frank
 * @author Florian Zipser
 */

public class Salt2PAULAMapper extends PepperMapperImpl implements PAULAXMLDictionary, FilenameFilter {
	private static final Logger logger = LoggerFactory.getLogger(PAULAExporter.MODULE_NAME);
  
	private URI resourcePath = null;

	/** Returns the path to the location of additional resources. **/
	public URI getResourcePath() {
		return resourcePath;
	}

	/**
	 * Method for setting a reference to the path where the resources for the
	 * PAULAExporter (e.g. DTD-files) are located.
	 * 
	 * @param resources
	 */
	public void setResourcePath(URI resources) {
		resourcePath = resources;
	}

	/**
	 * Implementation for FilenameFilter. This is needed for fetching only the
	 * DTD-files from resource path for copying to output folder.
	 */
	public boolean accept(File f, String s) {
		return s.toLowerCase().endsWith(".dtd");
	}

	public static final String PATH_DTD = "dtd_11/";

	/**
	 * Maps {@link SMetaAnnotation} obejcts.
	 */
	@Override
	public DOCUMENT_STATUS mapSCorpus() {
		mapSMetaAnnotations(getDocument());
		return (DOCUMENT_STATUS.COMPLETED);
	}

	/**
	 * {@inheritDoc PepperMapper#setDocument(SDocument)}
	 * 
	 * OVERRIDE THIS METHOD FOR CUSTOMIZED MAPPING.
	 */
	@Override
	public DOCUMENT_STATUS mapSDocument() {
		if (getDocument() == null) {
			throw new PepperModuleException(this, "Cannot export document structure because sDocument is null");
		}
		if (getDocument().getDocumentGraph() == null) {
			throw new PepperModuleException(this, "Cannot export document structure because sDocumentGraph is null");
		}
		if (this.getResourceURI() == null) {
			throw new PepperModuleException(this, "Cannot export document structure because documentPath is null for '" + getDocument().getIdentifier() + "'.");
		}
		// copy DTD-files to output-path
		if (getResourcePath() != null) {
			File dtdDirectory = new File(getResourcePath().toFileString() + "/" + PATH_DTD);
			if ((dtdDirectory.exists()) && (dtdDirectory.listFiles(this) != null)) {
				for (File DTDFile : dtdDirectory.listFiles(this)) {
					copyFile(URI.createFileURI(DTDFile.getAbsolutePath()), this.getResourceURI().toFileString());
				}
			} else {
				logger.warn("Cannot copy dtds fom resource directory, because resource directory '" + dtdDirectory.getAbsolutePath() + "' does not exist.");
			}
		} else {
			logger.warn("There is no reference to a resource path!");
		}

		try {
			mapTextualDataSources();
			mapTokens();
			mapSMedialDS();
			mapSpans();
			mapStructures();
			mapPointingRelations();
			mapSMetaAnnotations(getDocument());
		} catch (Exception ex) {
			throw new PepperModuleException(this, "Could write document " + getDocument().getId() + " to path " + getResourcePath(), ex);
		} finally {
			for (PAULAPrinter printer : paulaPrinters.values()) {
				printer.close();
			}
		}

		return (DOCUMENT_STATUS.COMPLETED);
	}

	/**
	 * A factory to create {@link XMLStreamWriter} objects.
	 */
	private final XMLOutputFactory xmlFactory = XMLOutputFactory.newFactory();

	/** A set storing all already used paula files. **/
	private Map<File, PAULAPrinter> paulaPrinters = new HashMap<File, Salt2PAULAMapper.PAULAPrinter>();

	/**
	 * Returns a {@link PAULAPrinter} corresponding to the given file. If no
	 * paulaPrinter exists so far, one is created
	 **/
	private PAULAPrinter getPAULAPrinter(File paulaFile) {
		PAULAPrinter retVal = paulaPrinters.get(paulaFile);
		if (retVal == null) {
			retVal = new PAULAPrinter(paulaFile);
			paulaPrinters.put(paulaFile, retVal);
		}
		return (retVal);
	}

	/** A helper class to create a {@link XMLStreamWriter} object. **/
	class PAULAPrinter {
		XMLStreamWriter xml = null;
		private PrintWriter output = null;
		private File paulaFile = null;
		private File base = null;

		public PAULAPrinter(File paulaFile) {
			this.paulaFile = paulaFile;
			try {
				output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(paulaFile), "UTF8")), false);
				xml = new XMLStreamWriter(xmlFactory.createXMLStreamWriter(output));
				xml.setPrettyPrint(getProps().isHumanReadable());
				xml.writeStartDocument();
			} catch (IOException e) {
				throw new PepperModuleException(Salt2PAULAMapper.this, "Cannot open file '" + paulaFile.getAbsolutePath() + "' to write to, because of a nested exception. ", e);
			} catch (XMLStreamException e) {
				throw new PepperModuleException(Salt2PAULAMapper.this, "Cannot write in file '" + paulaFile.getAbsolutePath() + "', because of a nested exception. ", e);
			}
		}

		/**
		 * Closes the internal streams of this object. Make sure, to call this
		 * method for all {@link PAULAPrinter} objects even in case of
		 * exception.
		 **/
		public void close() {

			try {
				if (hasPreamble) {
					// close preamble
					xml.writeEndDocument();
					xml.flush();
				}
				// always close the actual xml writer
				xml.close();
			} catch (XMLStreamException e) {
				throw new PepperModuleException(Salt2PAULAMapper.this, "Cannot write in file '" + paulaFile.getAbsolutePath() + "', because of a nested exception. ", e);
			}
			output.flush();
			output.close();
		}

		/** returns whether preamble has been written **/
		public boolean hasPreamble() {
			return (hasPreamble);
		}

		boolean hasPreamble = false;

		/**
		 * Prints the preamble to multiple types of paula files.
		 * 
		 * @param paulaType
		 * @param paulaID
		 * @param type
		 * @param base
		 * @param xmlwriter
		 * @throws XMLStreamException
		 */
		public void printPreambel(PAULA_TYPE paulaType, String type, File base) {
			if (!hasPreamble) {
				if (paulaType == null) {
					throw new PepperModuleException(Salt2PAULAMapper.this, "Cannot create '" + paulaType + "' file beginning: This seems to be an internal problem.");
				}
				if (type.isEmpty()) {
					type = paulaType.getFileInfix();
				}
				this.base = base;
				try {
					xml.writeDTD(paulaType.getDocTypeTag());
					xml.writeStartElement(TAG_PAULA);
					xml.writeAttribute(ATT_VERSION, VERSION);
					xml.writeEmptyElement(TAG_HEADER);
					xml.writeAttribute(ATT_PAULA_ID, paulaFile.getName().replace("." + PepperModule.ENDING_XML, ""));
					xml.writeStartElement(paulaType.getListElementName());
					xml.writeNamespace("xlink", XLINK_URI);
					xml.writeAttribute(ATT_TYPE, type);
					if (base != null) {
						xml.writeAttribute(ATT_BASE, base.getName());
					}
				} catch (XMLStreamException e) {
					throw new PepperModuleException(Salt2PAULAMapper.this, "Cannot write in file '" + paulaFile.getAbsolutePath() + "', because of a nested exception. ", e);
				}
				hasPreamble = true;
			}
		}
	}

	private void mapSMetaAnnotations(SAnnotationContainer container) {
		if (container != null) {
			if ((container.getMetaAnnotations() != null) && (container.getMetaAnnotations().size() > 0)) {

				// create anno-xml file
				String pathName = getResourceURI().toFileString();
				if (!pathName.endsWith("/")) {
					pathName = pathName + "/";
				}
				File annoFile = new File(pathName + "anno.xml");
				annoFile.getParentFile().mkdirs();
				PAULAPrinter printer = getPAULAPrinter(annoFile);
				try {
					printer.xml.writeDTD(PAULA_TEXT_DOCTYPE_TAG);
					printer.xml.writeStartElement(TAG_PAULA);
					printer.xml.writeAttribute(ATT_VERSION, VERSION);
					printer.xml.writeStartElement(TAG_HEADER);
					printer.xml.writeAttribute(ATT_PAULA_ID, "anno.xml");
					printer.xml.writeAttribute(ATT_TYPE, PAULA_TYPE.STRUCT.toString());
					printer.xml.writeEndElement();
					printer.xml.writeStartElement(TAG_STRUCT_STRUCTLIST);
					printer.xml.writeNamespace("xlink", XLINK_URI);
					printer.xml.writeAttribute(ATT_TYPE, "annoSet");
					printer.xml.writeStartElement(TAG_STRUCT_STRUCT);
					printer.xml.writeAttribute(ATT_ID, "anno_1");
					printer.xml.writeEndElement();
					printer.xml.writeEndElement();
					printer.xml.writeEndElement();
					printer.xml.writeEndDocument();
				} catch (XMLStreamException e) {
					throw new PepperModuleException(Salt2PAULAMapper.this, "Cannot write in file '" + annoFile.getAbsolutePath() + "', because of a nested exception. ", e);
				} finally {
					printer.close();
				}

				for (SMetaAnnotation meta : container.getMetaAnnotations()) {
					// create a file for each meta annotation
					String type = meta.getQName().replace("::", ".");
					String paulaID = "anno_" + type;
					String annoFileName = paulaID + "." + PepperImporter.ENDING_XML;

					File metaAnnoFile = new File(pathName + annoFileName);
					metaAnnoFile.getParentFile().mkdirs();
					printer = getPAULAPrinter(metaAnnoFile);
					if (!printer.hasPreamble) {
						printer.printPreambel(PAULA_TYPE.FEAT, type, annoFile);
					}
					try {
						String annoString = meta.getValue_STEXT();
						if (annoString != null) {
							printer.xml.writeEmptyElement(TAG_FEAT_FEAT);
							printer.xml.writeAttribute(ATT_HREF, "#anno_1");
							printer.xml.writeAttribute(ATT_FEAT_FEAT_VAL, annoString);
						}
					} catch (XMLStreamException e) {
						throw new PepperModuleException(Salt2PAULAMapper.this, "Cannot write in file '" + metaAnnoFile.getAbsolutePath() + "', because of a nested exception. ", e);
					}
				}
			}
		}

	}

	/**
	 * Maps all {@link STextualDS} objects.
	 */
	public void mapTextualDataSources() {
		// Iterate over all Textual Data Sources
		for (STextualDS sTextualDS : getDocument().getDocumentGraph().getTextualDSs()) {
			File paulaFile = generateFileName(sTextualDS);
			PAULAPrinter printer = getPAULAPrinter(paulaFile);
			// Write the Text file content
			try {
				printer.xml.writeDTD(PAULA_TEXT_DOCTYPE_TAG);
				printer.xml.writeStartElement(TAG_PAULA);
				printer.xml.writeAttribute(ATT_VERSION, VERSION);
				printer.xml.writeStartElement(TAG_HEADER);
				printer.xml.writeAttribute(ATT_PAULA_ID, paulaFile.getName().replace("." + PepperImporter.ENDING_XML, ""));
				printer.xml.writeAttribute(ATT_TYPE, PAULA_TYPE.TEXT.toString());
				printer.xml.writeEndElement();
				// disable pretty print for the body in order not to generate extra spaces
				boolean originalPrettyPrintVal = printer.xml.getPrettyPrint();
				printer.xml.setPrettyPrint(false);
				printer.xml.writeStartElement(TAG_TEXT_BODY);
				printer.xml.writeCharacters(sTextualDS.getText());
				printer.xml.writeEndElement();
				// reset to original pretty print policy
				printer.xml.setPrettyPrint(originalPrettyPrintVal);
				printer.xml.writeEndElement();
			} catch (XMLStreamException e) {
				throw new PepperModuleException(Salt2PAULAMapper.this, "Cannot write in file '" + paulaFile.getAbsolutePath() + "', because of a nested exception. ", e);
			} finally {
				printer.close();
			}
		}
	}

	/**
	 * Maps all {@link SToken} objects.
	 */
	public void mapTokens() {
		PAULAPrinter printer = null;
		for (STextualRelation sTextRel : getDocument().getDocumentGraph().getTextualRelations()) {
			SToken sToken = sTextRel.getSource();
			if (sToken != null) {
				File paulaFile = generateFileName(sToken);
				printer = getPAULAPrinter(paulaFile);
				if (!printer.hasPreamble) {
					printer.printPreambel(PAULA_TYPE.TOK, "tok", generateFileName(sTextRel.getTarget()));
				}
				try {
					if (((PAULAExporterProperties) getProperties()).isHumanReadable()) {
						printer.xml.writeComment(escapeComment(getDocument().getDocumentGraph().getText(sToken)));
					}
					printer.xml.writeEmptyElement(TAG_MARK_MARK);
					printer.xml.writeAttribute(ATT_ID, checkId(sToken.getPath().fragment()));
					Integer start = sTextRel.getStart() + 1;
					Integer end = sTextRel.getEnd() - sTextRel.getStart();
					String xPointer = "#xpointer(string-range(//body,''," + start + "," + end + "))";
					printer.xml.writeAttribute(ATT_HREF, xPointer);
				} catch (ClassCastException | XMLStreamException e) {
					e.printStackTrace();
					throw new PepperModuleException(Salt2PAULAMapper.this, "Cannot write in file '" + paulaFile.getAbsolutePath() + "', because of a nested exception. ", e);
				}
			}
			mapAnnotations(sToken);
		}
		if (printer != null) {
			printer.close();
		}
	}

	/**
	 * Maps audio data as data source. When audio data are connected to tokens,
	 * a span for each connection is created and annotated with an audio
	 * annotation referencing the audio file. When no Token is connected to the
	 * audio source, a span is created for all tokens and an audio annotation is
	 * added to that span.
	 */
	public void mapSMedialDS() {
		Multimap<SMedialDS, SToken> map = LinkedHashMultimap.create();
		if ((getDocument().getDocumentGraph().getMedialRelations() != null) && (getDocument().getDocumentGraph().getMedialRelations().size() > 0)) {
			/**
			 * Create a markable file which addresses all tokens, which have
			 * references to the SAudioDS
			 */
			for (SMedialRelation rel : getDocument().getDocumentGraph().getMedialRelations()) {
				map.put(rel.getTarget(), rel.getSource());
			}
		} else {
			if ((getDocument().getDocumentGraph().getMedialDSs() != null) && (getDocument().getDocumentGraph().getMedialDSs().size() > 0))
				for (SMedialDS audioDS : getDocument().getDocumentGraph().getMedialDSs()) {
					map.putAll(audioDS, getDocument().getDocumentGraph().getTokens());
				}
		}
		if (map.size() > 0) {
			StringBuffer fileName = new StringBuffer();
			fileName.append(getResourceURI().toFileString());
			if (!fileName.toString().endsWith("/")) {
				fileName.append("/");
			}
			fileName.append(getDocument().getName());
			fileName.append(".");
			fileName.append(PAULA_TYPE.MARK.getFileInfix());
			fileName.append(".");
			fileName.append("audio");
			fileName.append(".");
			fileName.append(PepperModule.ENDING_XML);
			File audioMarkFile = new File(fileName.toString());

			PAULAPrinter printer = getPAULAPrinter(audioMarkFile);

			if (!printer.hasPreamble) {
				printer.printPreambel(PAULA_TYPE.MARK, "audio", generateFileName(getDocument().getDocumentGraph().getTokens().get(0)));
			}

			for (SMedialDS audio : map.keySet()) {
				try {
					printer.xml.writeEmptyElement(TAG_MARK_MARK);
					if (audio.getPath().fragment() != null) {
						printer.xml.writeAttribute(ATT_ID, checkId(audio.getPath().fragment()));
					} else {
						printer.xml.writeAttribute(ATT_ID, audio.getId());
					}
					printer.xml.writeAttribute(ATT_HREF, generateXPointer(new ArrayList<SToken>(map.get(audio)), printer.base));
				} catch (XMLStreamException e) {
					throw new PepperModuleException(Salt2PAULAMapper.this, "Cannot write in file '" + audioMarkFile.getAbsolutePath() + "', because of a nested exception. ", e);
				}
			}

			/**
			 * Create a feature file which addresses all tokens, which addresses
			 * the audio markable file
			 */
			// copy referenced files
			File audioFeatFile = new File(audioMarkFile.getAbsolutePath().replace("." + PepperModule.ENDING_XML, "_feat." + PepperModule.ENDING_XML));
			printer = getPAULAPrinter(audioFeatFile);
			printer.printPreambel(PAULA_TYPE.FEAT, KW_AUDIO, audioMarkFile);

			for (SMedialDS audio : getDocument().getDocumentGraph().getMedialDSs()) {
				/**
				 * Copy audio file and
				 */
				String target = audioMarkFile.getAbsoluteFile().getParent();
				if (!target.endsWith("/")) {
					target = target + "/";
				}
				target = target + audio.getMediaReference().lastSegment();
				File audioFile = new File(target);
				try {
					String source = audio.getMediaReference().toFileString();
					if (source == null) {
						source = audio.getMediaReference().toString();
					}
					Files.copy(new File(source).toPath(), audioFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new PepperModuleException(Salt2PAULAMapper.this, "Cannot copy audio file '" + audio.getMediaReference() + "', to +'" + target + "'. ", e);
				}
				/**
				 * Create a feature file which addresses all tokens, which
				 * addresses the audio markable file
				 */
				try {
					printer.xml.writeEmptyElement(TAG_FEAT_FEAT);
					printer.xml.writeAttribute(ATT_HREF, "#" + audio.getPath().fragment());
					printer.xml.writeAttribute(ATT_FEAT_FEAT_VAL, audioFile.getName());
				} catch (XMLStreamException e) {
					throw new PepperModuleException(Salt2PAULAMapper.this, "Cannot write in file '" + audioFeatFile.getAbsolutePath() + "', because of a nested exception. ", e);
				}

			}
		}
	}

	/**
	 * Maps all {@link SSpan} objects.
	 */
	public void mapSpans() {
		PAULAPrinter printer = null;
		for (SSpan sSpan : getDocument().getDocumentGraph().getSpans()) {
			List<SToken> tokens = getDocument().getDocumentGraph().getSortedTokenByText(getDocument().getDocumentGraph().getOverlappedTokens(sSpan));
			if (!tokens.isEmpty()) {
				File paulaFile = generateFileName(sSpan);
				printer = getPAULAPrinter(paulaFile);

				if (!printer.hasPreamble) {
					printer.printPreambel(PAULA_TYPE.MARK, generatePaulaType(sSpan), generateFileName(tokens.get(0)));
				}
				try {
					if (((PAULAExporterProperties) getProperties()).isHumanReadable()) {
						printer.xml.writeComment(escapeComment(getDocument().getDocumentGraph().getText(sSpan)));
					}
					printer.xml.writeEmptyElement(TAG_MARK_MARK);
					if (sSpan.getPath().fragment() != null) {
						printer.xml.writeAttribute(ATT_ID, checkId(sSpan.getPath().fragment()));
					} else {
						printer.xml.writeAttribute(ATT_ID, sSpan.getId());
					}
					printer.xml.writeAttribute(ATT_HREF, generateXPointer(tokens, printer.base));
				} catch (XMLStreamException e) {
					throw new PepperModuleException(Salt2PAULAMapper.this, "Cannot write in file '" + paulaFile.getAbsolutePath() + "', because of a nested exception. ", e);
				}
			}
			mapAnnotations(sSpan);

		}
		if (printer != null) {
			printer.close();
		}
	}

	/**
	 * Maps {@link SStructure}
	 */
	private void mapStructures() {
		for (SStructure struct : getDocument().getDocumentGraph().getStructures()) {
			File paulaFile = generateFileName(struct);
			PAULAPrinter printer = getPAULAPrinter(paulaFile);
			if (!printer.hasPreamble) {
				printer.printPreambel(PAULA_TYPE.STRUCT, generatePaulaType(struct), null);
			}
			try {
				if (((PAULAExporterProperties) getProperties()).isHumanReadable()) {
					printer.xml.writeComment(escapeComment(getDocument().getDocumentGraph().getText(struct)));
				}
				printer.xml.writeStartElement(TAG_STRUCT_STRUCT);
				printer.xml.writeAttribute(ATT_ID, checkId(struct.getPath().fragment()));
				for (Relation relation : getDocument().getDocumentGraph().getOutRelations(struct.getId())) {
					if (relation instanceof SDominanceRelation) {
						SDominanceRelation domRel = (SDominanceRelation) relation;
						printer.xml.writeEmptyElement(TAG_STRUCT_REL);
						String idVal = checkId(domRel.getPath().fragment());
						if (idVal != null) {
							printer.xml.writeAttribute(ATT_ID, idVal);
						}
						if ((domRel.getType() != null) && (!domRel.getType().isEmpty())) {
							printer.xml.writeAttribute(ATT_STRUCT_REL_TYPE, domRel.getType());
						}
						printer.xml.writeAttribute(ATT_HREF, generateXPointer(domRel.getTarget(), printer.base));
						mapAnnotations(domRel);
					}
				}
				printer.xml.writeEndElement();
			} catch (XMLStreamException e) {
				throw new PepperModuleException(Salt2PAULAMapper.this, "Cannot write in file '" + paulaFile.getAbsolutePath() + "', because of a nested exception. ", e);
			}
			mapAnnotations(struct);
		}
	}

	/**
	 * Maps {@link SPointingRelation}s.
	 * 
	 * @throws XMLStreamException
	 */
	private void mapPointingRelations() {
		for (SPointingRelation pointRel : getDocument().getDocumentGraph().getPointingRelations()) {
			String type = "";
			if (pointRel.getType() == null || pointRel.getType().isEmpty()) {
				type = "notype";
			} else {
				type = pointRel.getType();
			}

			File paulaFile = generateFileName(pointRel);
			PAULAPrinter printer = getPAULAPrinter(paulaFile);
			if (!printer.hasPreamble) {
				printer.printPreambel(PAULA_TYPE.REL, type, null);
			}

			// create rel tag string
			if ((pointRel.getSource() != null) && (pointRel.getTarget() != null)) {
				try {
					printer.xml.writeEmptyElement(TAG_REL_REL);
					String idVal = checkId(pointRel.getPath().fragment());
					if (idVal != null) {
						printer.xml.writeAttribute(ATT_ID, idVal);
					}
					printer.xml.writeAttribute(ATT_HREF, generateXPointer(pointRel.getSource(), printer.base));
					printer.xml.writeAttribute(ATT_REL_REL_TARGET, generateXPointer(pointRel.getTarget(), printer.base));
				} catch (XMLStreamException e) {
					throw new PepperModuleException(Salt2PAULAMapper.this, "Cannot write in file '" + paulaFile.getAbsolutePath() + "', because of a nested exception. ", e);
				}
				mapAnnotations(pointRel);
			}
		}
	}

	/**
	 * Maps annotations of type {@link SAnnotation} and {@link SMetaAnnotation}
	 * 
	 * @param annoSource
	 */
	private void mapAnnotations(SAnnotationContainer annoSource) {
		if (annoSource != null) {
			for (SAnnotation anno : annoSource.getAnnotations()) {
				String annoString = null;
				// copy referenced files
				if (anno.getValue_SURI() != null) {
					annoString = copyFile(anno.getValue_SURI(), getResourceURI().toFileString());
				} else {
					annoString = anno.getValue_STEXT();
				}
				File paulaFile = generateFileName(anno);
				PAULAPrinter printer = getPAULAPrinter(paulaFile);
				if (!printer.hasPreamble) {
					String type = anno.getQName().replace("::", ".");
					if (annoSource instanceof SNode) {
						printer.printPreambel(PAULA_TYPE.FEAT, type, generateFileName((SNode) annoSource));
					} else if (annoSource instanceof SRelation) {
						printer.printPreambel(PAULA_TYPE.FEAT, type, generateFileName((SRelation) annoSource));
					}
				}
				try {
					if (annoString != null) {
						printer.xml.writeEmptyElement(TAG_FEAT_FEAT);
						printer.xml.writeAttribute(ATT_HREF, generateXPointer((IdentifiableElement) annoSource, printer.base));
						printer.xml.writeAttribute(ATT_FEAT_FEAT_VAL, annoString);
					}
				} catch (XMLStreamException e) {
					throw new PepperModuleException(Salt2PAULAMapper.this, "Cannot write in file '" + paulaFile.getAbsolutePath() + "', because of a nested exception. ", e);
				}
			}
		}
	}

	/** a prefix for ids, which starts with a numeric **/
	public static final String ID_PREFIX = "id";

	/**
	 * Checks whether an id starts with a numeric, if true, the id will be
	 * prefixed with {@link #ID_PREFIX}
	 **/
	public String checkId(String id) {
		if ((id != null) && (Character.isDigit(id.charAt(0)))) {
			return (ID_PREFIX + id);
		}
		return (id);
	}

	/**
	 * Generates an xpointer for a set of {@link SNode}s.
	 * 
	 * @param targets
	 * @return
	 */
	public String generateXPointer(IdentifiableElement target, File base) {
		StringBuilder retVal = new StringBuilder();
		if (target != null) {
			// write single node #tok_1
			File baseFile = null;
			if (target instanceof SNode) {
				baseFile = generateFileName((SNode) target);
			} else if (target instanceof SRelation) {
				baseFile = generateFileName((SRelation) target);
			}
			if (!baseFile.equals(base)) {
				retVal.append(baseFile.getName());
			}
			retVal.append("#");
			String fragment = ((SPathElement) target).getPath().fragment();
			if (fragment == null) {
				// fix to fix a bug in mmaxmodules, where id was created
				// manually
				fragment = target.getId();
			}
			retVal.append(checkId(fragment));
		}
		return (retVal.toString());
	}

	/**
	 * Generates an xpointer for a set of {@link SIdentifiableElement}s.
	 * 
	 * @param targets
	 * @return
	 */
	public String generateXPointer(List<? extends IdentifiableElement> targets, File base) {
		StringBuilder retVal = new StringBuilder();
		if ((targets != null) && (targets.size() > 0)) {
			if (targets.size() == 1) {
				retVal.append(generateXPointer(targets.get(0), base));
			} else {
				// write all nodes e.g.: (#tok_1 #tok2 ... #tok_n)
				int i = 0;
				for (IdentifiableElement target : targets) {
					if (i != 0) {
						retVal.append(" ");
					}
					retVal.append(generateXPointer(target, base));
					i++;
				}
			}
		}
		return (retVal.toString());
	}
	
	/**
	 * Generates a Paula type from the layers of passed {@link SNode} object.
	 * 
	 * @param sNode
	 * @return
	 */
	protected String generatePaulaType(IdentifiableElement id) {
		if(id instanceof STextualDS || id instanceof SToken) {
			return generatePaulaType(id, true);
		} else {
			return generatePaulaType(id, false);
		}
	}

	/**
	 * Generates a Paula type from the layers of passed {@link SNode} object.
	 * 
	 * @param sNode
	 * @param emptyFallback If true the fallback for an empty namespace is the empty string, otherwise the default layer name from the configuration is used.
	 * @return
	 */
	protected String generatePaulaType(IdentifiableElement id, boolean emptyFallback) {
		String layers = emptyFallback ? "" : getProps().getEmptyNamespace();
		if (id != null) {
			Set<SLayer> sLayers = null;
			if (id instanceof SNode) {
				sLayers = ((SNode) id).getLayers();
			} else if (id instanceof SRelation) {
				sLayers = ((SRelation) id).getLayers();
			}
			if (sLayers.size() > 0) {
				// if node belongs to several layers, sort layers by name
				if (sLayers.size() == 1) {
					layers = escapeNamespace(sLayers.iterator().next().getName());
				} else {
					List<String> layerList = new ArrayList<String>();
					for (SLayer sLayer : sLayers) {
						layerList.add(sLayer.getName());
					}
					Collections.sort(layerList, String.CASE_INSENSITIVE_ORDER);
					int i = 0;
					for (String layerName : layerList) {
						if (i == 0) {
							layers = escapeNamespace(layerName);
						} else {
							layers = layers + "_" + escapeNamespace(layerName);
						}
					}
				}
			}
		}
		return (layers);
	}
	
	private String escapeNamespace(String namespace) {
		if(namespace == null) {
			return null;
		}
		
		// PAULA spec only allows alpha numeric ACSII characters
		return namespace.replaceAll("[^\\p{Alnum}]", "_");
	}
 	
	private STextualDS getSTextForSToken(SToken tok) {
		STextualDS textualDS = null;
		for(SRelation rel : tok.getOutRelations()) {
			if(rel instanceof STextualRelation) {
				textualDS = ((STextualRelation) rel).getTarget();
				break;
			}
		}
		return textualDS;
	}
	

	/**
	 * Returns a filename, where to store the given SNode. The pattern, which is
	 * used to compute the files name is: <br/>
	 * layers"."documentId"."TYPE_POSTFIX".xml" <br/>
	 * If node belongs to several layers, they are sorted by name.
	 * 
	 * @param sNode
	 *            {@link SNode} to which a filename has to be generated
	 * @return file name matching to given {@link SNode}
	 */
	public File generateFileName(SNode sNode) {
		File retFile = null;
		if (sNode != null) {
			StringBuilder fileName = new StringBuilder();

			String layers = generatePaulaType(sNode);
			fileName.append(layers);
			if (!layers.isEmpty()) {
				fileName.append(".");
			}
			fileName.append(generateFileNameBase(sNode));

			fileName.append(".");
			fileName.append(PepperModule.ENDING_XML);
			String pathName = getResourceURI().toFileString();
			if (!pathName.endsWith("/")) {
				pathName = pathName + "/";
			}
			retFile = new File(pathName + fileName.toString());
			retFile.getParentFile().mkdirs();
		}

		return (retFile);
	}
	
	/**
	 * Calculates the base name (thus no namespace and .xml ending) of a {@link SNode}.
	 * @param sNode
	 * @return
	 */
	private String generateFileNameBase(SNode sNode) {
		StringBuilder base = new StringBuilder();
		if (sNode != null) {

			if (sNode instanceof STextualDS) {
				base.append(getDocument().getName());
				if (getDocument().getDocumentGraph().getTextualDSs().size() > 1) {
					base.append(".");
					base.append(sNode.getPath().fragment());
				}
				base.append(".");
				base.append(PAULA_TYPE.TEXT.getFileInfix());
				
			} else if (sNode instanceof SToken) {
				base.append(getDocument().getName());
				STextualDS sText = getSTextForSToken((SToken) sNode);
				if ((sText != null) && (getDocument().getDocumentGraph().getTextualDSs().size() > 1)) {
					base.append(".");
					base.append(sText.getPath().fragment());
				}
				base.append(".");
				base.append(PAULA_TYPE.TOK.getFileInfix());
			} else {
				
				base.append(getDocument().getName());
				base.append(".");
				if (sNode instanceof SSpan) {
					base.append(PAULA_TYPE.MARK.getFileInfix());
				} else if (sNode instanceof SStructure) {
					base.append(PAULA_TYPE.STRUCT.getFileInfix());
				}
			}
		}

		return base.toString();
	}


	/**
	 * Returns a filename, where to store the given {@link SRelation}.
	 * 
	 * @param sNode
	 *            {@link SNode} to which a filename has to be generated
	 * @return file name matching to given {@link SNode}
	 */
	public File generateFileName(SRelation<? extends SNode, ? extends SNode> sRelation) {
		if (sRelation instanceof SDominanceRelation) {
			return (generateFileName(sRelation.getSource()));
		} else if (sRelation instanceof SPointingRelation) {
			StringBuilder fileName = new StringBuilder();
			String layers = generatePaulaType(sRelation);
			fileName.append(layers);
			if (!layers.isEmpty()) {
				fileName.append(".");
			}
			
			fileName.append(generateFileNameBase(sRelation));
			
			fileName.append(".");
			fileName.append(PepperModule.ENDING_XML);
			String pathName = getResourceURI().toFileString();
			if (!pathName.endsWith("/")) {
				pathName = pathName + "/";
			}
			File retFile = new File(pathName + fileName.toString());
			retFile.getParentFile().mkdirs();
			return (retFile);

		} else
			return (null);
	}
	
	/**
	 * Calculates the base name (thus no namespace and .xml ending) of a {@link SRelation}.
	 * @param sNode
	 * @return
	 */
	private String generateFileNameBase(SRelation<? extends SNode, ? extends SNode> sRelation) {
		StringBuilder base = new StringBuilder();
	
		base.append(getDocument().getName());
		base.append(".");
		String type = "";
		if (sRelation.getType() == null || sRelation.getType().isEmpty()) {
			type = "notype";
		} else {
			type = sRelation.getType();
		}
		base.append(type);
	
		return base.toString();
	}

	/**
	 * Returns a filename, where to store the given SAnnotation.
	 * 
	 * @param sNode
	 *            {@link SNode} to which a filename has to be generated
	 * @return file name matching to given {@link SNode}
	 */
	public File generateFileName(Label anno) {

		String baseFileName = null;
		if (anno.getContainer() instanceof SNode) {
			baseFileName = generateFileNameBase((SNode) anno.getContainer());
		} else if (anno.getContainer() instanceof SRelation) {
			baseFileName = generateFileNameBase((SRelation) anno.getContainer());
		}

		StringBuilder fileName = new StringBuilder();
		
		// use *annotation* namespace as prefix
		String namespace = getProps().getEmptyNamespace();
		if(anno.getNamespace() != null) {
			namespace = anno.getNamespace();
		}
		fileName.append(namespace);
		fileName.append(".");
		fileName.append(baseFileName);
		fileName.append("_");
		fileName.append(anno.getName());
		fileName.append(".xml");
		
		
		return (new File( new File(getResourceURI().toFileString()), fileName.toString()));
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
			throw new PepperModuleException(this, "Cannot copy file '" + file + "' to path '" + outFileString + "'", e);
		}
		return outFileString;
	}
  
  /**
   * In comments the string "--" is not allowed, thus replace it with "&lt;hypen&gt;&lt;hypen&gt;".
   * Since comments are only intended for humans there is no need to have a special 
   * encoding/decoding scheme here.
   * This also appends a whitespace to the end of the string if ends with "-"
   * to avoid any possible confusion with constructs like "--->".
   * @param txt
   * @return 
   */
  private String escapeComment(String txt) {
    if(txt == null) {
      return null;
    }
    
    String result = txt.replace("--", "<hyphen><hyphen>");
    if(result.endsWith("-")) {
    	result = result + " ";
    }
    return result;
  }
  
  public PAULAExporterProperties getProps() {
  	return (PAULAExporterProperties) getProperties();
  }
}