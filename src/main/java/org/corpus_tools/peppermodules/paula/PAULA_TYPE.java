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

/**
 * 
 * @author Florian Zipser
 *
 */
public enum PAULA_TYPE {
	TEXT(PAULAXMLDictionary.PAULA_TEXT_DOCTYPE_TAG, PAULAXMLDictionary.TAG_TEXT_BODY, PAULAXMLDictionary.TAG_TEXT_BODY,
			"text"),
	//
	TOK(PAULAXMLDictionary.PAULA_MARK_DOCTYPE_TAG, PAULAXMLDictionary.TAG_MARK_MARKLIST,
			PAULAXMLDictionary.TAG_MARK_MARK, "tok"),
	//
	MARK(PAULAXMLDictionary.PAULA_MARK_DOCTYPE_TAG, PAULAXMLDictionary.TAG_MARK_MARKLIST,
			PAULAXMLDictionary.TAG_MARK_MARK, "mark"),
	//
	STRUCT(PAULAXMLDictionary.PAULA_STRUCT_DOCTYPE_TAG, PAULAXMLDictionary.TAG_STRUCT_STRUCTLIST,
			PAULAXMLDictionary.TAG_STRUCT_STRUCT, "struct"),
	//
	REL(PAULAXMLDictionary.PAULA_REL_DOCTYPE_TAG, PAULAXMLDictionary.TAG_REL_RELLIST, PAULAXMLDictionary.TAG_REL_REL,
			"rel"),
	//
	FEAT(PAULAXMLDictionary.PAULA_FEAT_DOCTYPE_TAG, PAULAXMLDictionary.TAG_FEAT_FEATLIST,
			PAULAXMLDictionary.TAG_FEAT_FEAT, "feat");

	private final String docTypeTag;
	private final String listElementName;
	private final String elementName;
	private final String fileInfix;

	PAULA_TYPE(String docTypeTag, String listElementName, String elementName, String fileInfix) {
		this.docTypeTag = docTypeTag;
		this.listElementName = listElementName;
		this.elementName = elementName;
		this.fileInfix = fileInfix;
	}

	public String getListElementName() {
		return listElementName;
	}

	public String getElementName() {
		return elementName;
	}

	public String getDocTypeTag() {
		return docTypeTag;
	}

	public String getFileInfix() {
		return fileInfix;
	}
}
