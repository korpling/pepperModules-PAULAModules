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

import java.util.Collection;

import org.corpus_tools.pepper.impl.PepperImporterImpl;
import org.corpus_tools.pepper.modules.PepperImporter;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.graph.Identifier;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;

/**
 * This importer reads a corpus in PAULA format and maps it to a SALT corpus.
 * The mapping of each document is done in a separate thread.
 * 
 * @author Florian Zipser
 * @version 1.0
 * 
 */
@Component(name = "PAULAImporterComponent", factory = "PepperImporterComponentFactory")
public class PAULAImporter extends PepperImporterImpl implements PepperImporter {
	public PAULAImporter() {
		super();

		// setting name of module
		setName("PAULAImporter");
		setSupplierContact(URI.createURI("saltnpepper@lists.hu-berlin.de"));
		setSupplierHomepage(URI.createURI("https://github.com/korpling/pepperModules-PAULAModules"));
		setDesc("The PAULA importer imports data comming from the PAULA format to a Salt model. ");
		// set list of formats supported by this module
		this.addSupportedFormat("paula", "1.0", null);

		this.getDocumentEndings().add(ENDING_LEAF_FOLDER);
	}

	/**
	 * Stores the endings which are used for paula-files
	 */
	private String[] PAULA_FILE_ENDINGS = { "xml", "paula" };

	/**
	 * Creates a mapper of type {@link PAULA2SaltMapper}. {@inheritDoc
	 * PepperModule#createPepperMapper(Identifier)}
	 */
	@Override
	public PepperMapper createPepperMapper(Identifier Identifier) {
		PAULA2SaltMapper mapper = new PAULA2SaltMapper();
		mapper.setPAULA_FILE_ENDINGS(PAULA_FILE_ENDINGS);

		if (Identifier.getIdentifiableElement() instanceof SCorpus) {
			// avoid importing of SCorpus, in case of SCorpus was artificially
			// created and links to the same path as a SDocument object
			Collection<URI> pathes = this.getIdentifier2ResourceTable().values();
			int i = 0;
			for (URI path : pathes) {
				if (path.equals(this.getIdentifier2ResourceTable().get(Identifier)))
					i++;
			}
			if (i > 1)
				mapper.setIsArtificialSCorpus(true);
		}
		return (mapper);
	}
}
