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
import java.util.Collections;
import java.util.List;

import org.corpus_tools.pepper.common.PepperConfiguration;
import org.corpus_tools.pepper.core.SelfTestDesc;
import org.corpus_tools.pepper.exceptions.PepperFWException;
import org.corpus_tools.pepper.impl.PepperExporterImpl;
import org.corpus_tools.pepper.modules.PepperExporter;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.graph.Identifier;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;

/**
 * This class exports data from Salt to the PAULA 1.0 format.
 * 
 * @author Mario Frank
 * @author Florian Zipser
 *
 */
@Component(name = "PAULAExporterComponent", factory = "PepperExporterComponentFactory")
public class PAULAExporter extends PepperExporterImpl implements PepperExporter {
	public static final String MODULE_NAME = "PAULAExporter";
	public static final String FORMAT_NAME = "paula";
	public static final String FORMAT_VERSION = "1.0";

	public PAULAExporter() {
		super();
		// setting name of module
		setName(MODULE_NAME);
		setSupplierContact(URI.createURI(PepperConfiguration.EMAIL));
		setSupplierHomepage(URI.createURI("https://github.com/korpling/pepperModules-PAULAModules"));
		setDesc("The PAULA exporter exports data comming a Salt model to the PAULA format. ");
		// set list of formats supported by this module
		addSupportedFormat(FORMAT_NAME, FORMAT_VERSION, null);
		setProperties(new PAULAExporterProperties());
	}

	@Override
	public SelfTestDesc getSelfTestDesc() {
		return new SelfTestDesc(
				getResources().appendSegment("selfTests").appendSegment("paulaExporter").appendSegment("in"),
				getResources().appendSegment("selfTests").appendSegment("paulaExporter").appendSegment("expected"));
	}

	@Override
	public void exportCorpusStructure() {
		for (SCorpusGraph sCorpusGraph : getSaltProject().getCorpusGraphs()) {
			if (sCorpusGraph == null) {
				throw new PepperFWException(
						"No SCorpusGraph was passed for exportCorpusStructure(SCorpusGraph corpusGraph). This might be a bug of the pepper framework.");
			} else {
				Salt2PAULAMapper mapper = new Salt2PAULAMapper();
				mapper.setResourcePath(this.getResources());
				mapCorpusStructure(sCorpusGraph, getCorpusDesc().getCorpusPath());

				if ((getIdentifier2ResourceTable().size() == 0)) {
					throw new PepperModuleException(this, "Cannot export SCorpusGraph '" + sCorpusGraph.getName()
							+ "', because of an unknown reason.");
				}
			}
		}
	}

	/**
	 * Maps the SCorpusStructure to a folder structure on disk relative to the
	 * given corpusPath.
	 * 
	 * @param sCorpusGraph
	 * @param corpusPath
	 * @return null, if no document directory could be created <br>
	 *         HashTable&lt;Identifier,URI&gt; else.<br>
	 *         Comment: URI is the complete document path
	 */
	public void mapCorpusStructure(SCorpusGraph sCorpusGraph, URI corpusPath) {
		if (sCorpusGraph == null) {
			throw new PepperModuleException("Cannot export corpus structure, because sCorpusGraph is null.");
		}
		if (corpusPath == null) {
			throw new PepperModuleException("Cannot export corpus structure, because the path to export to is null.");
		}

		for (SCorpus sCorpus : sCorpusGraph.getCorpora()) {
			// initialize URI with the location of the output folder
			URI resourceURI = corpusPath;
			for (String segment : sCorpus.getPath().segments()) {
				// append each path segment of the coprus to the URI
				resourceURI = resourceURI.appendSegment(segment);
			}
			File folder = new File(resourceURI.toFileString());
			if (!folder.exists()) {
				if (!(folder.mkdirs())) {
					throw new PepperModuleException("Cannot create directory " + resourceURI.toFileString());
				}
			}

			getIdentifier2ResourceTable().put(sCorpus.getIdentifier(), resourceURI);
		}

		List<SDocument> sDocumentList = Collections.synchronizedList(sCorpusGraph.getDocuments());

		// Check whether corpus path ends with Path separator. If not, hang it
		// on, else convert it to String as it is
		String corpusPathString = corpusPath.toFileString().replace("//", "/");
		if (!corpusPathString.endsWith("/")) {
			corpusPathString = corpusPathString.concat("/");
		} else {
			corpusPathString = corpusPath.toFileString();
		}

		for (SDocument sDocument : sDocumentList) {
			String completeDocumentPath = corpusPathString;
			String relativeDocumentPath;
			relativeDocumentPath = sDocument.getIdentifier().getValue().toString().replace("salt:/", "");
			// remove leading path separator, if existent
			if (relativeDocumentPath.substring(0, 1).equals(File.pathSeparator)) {
				completeDocumentPath = completeDocumentPath.concat(relativeDocumentPath.substring(1));
			} else {
				completeDocumentPath = completeDocumentPath.concat(relativeDocumentPath);
			}

			// Check whether directory exists and throw an exception if it does.
			// Else create it
			// We don't need this... we just overwrite the document
			if ((new File(completeDocumentPath).isDirectory())) {
				getIdentifier2ResourceTable().put(sDocument.getIdentifier(),
						org.eclipse.emf.common.util.URI.createFileURI(completeDocumentPath));
			} else {
				if (!((new File(completeDocumentPath)).mkdirs())) {
					throw new PepperModuleException("Cannot create directory " + completeDocumentPath);
				} else {
					getIdentifier2ResourceTable().put(sDocument.getIdentifier(),
							org.eclipse.emf.common.util.URI.createFileURI(completeDocumentPath));
				}
			}
		}
	}

	@Override
	public PepperMapper createPepperMapper(Identifier Identifier) {
		Salt2PAULAMapper mapper = new Salt2PAULAMapper();
		URI resource = this.getIdentifier2ResourceTable().get(Identifier);
		mapper.setResourceURI(resource);
		mapper.setResourcePath(this.getResources());
		return (mapper);
	}
}
