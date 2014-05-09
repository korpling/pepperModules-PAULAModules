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

/**
 * 
 * @author Florian Zipser
 *
 */
public enum PAULA_TYPE {
	MARK (PAULAXMLDictionary.PAULA_MARK_DOCTYPE_TAG, PAULAXMLDictionary.TAG_MARK_MARKLIST, PAULAXMLDictionary.TAG_MARK_MARK),
	STRUCT (PAULAXMLDictionary.PAULA_STRUCT_DOCTYPE_TAG, PAULAXMLDictionary.TAG_STRUCT_STRUCTLIST, PAULAXMLDictionary.TAG_STRUCT_STRUCT),
	REL (PAULAXMLDictionary.PAULA_REL_DOCTYPE_TAG, PAULAXMLDictionary.TAG_REL_RELLIST, PAULAXMLDictionary.TAG_REL_REL),
	FEAT (PAULAXMLDictionary.PAULA_FEAT_DOCTYPE_TAG, PAULAXMLDictionary.TAG_FEAT_FEATLIST, PAULAXMLDictionary.TAG_FEAT_FEAT);
	
	private final String listElementName;
	private final String elementName;
	private final String docTypeTag;
	
	PAULA_TYPE(	String docTypeTag,
				String listElementName,
				String elementName)
	{
		this.docTypeTag= docTypeTag;
		this.listElementName= listElementName;
		this.elementName= elementName;
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
}
