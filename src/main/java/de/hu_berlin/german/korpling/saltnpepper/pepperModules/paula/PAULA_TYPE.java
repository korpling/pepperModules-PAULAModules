package de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula;

/**
 * 
 * @author Florian Zipser
 *
 */
public enum PAULA_TYPE {
	MARK (PAULAXMLStructure.PAULA_MARK_DOCTYPE_TAG, PAULAXMLStructure.TAG_MARK_MARKLIST, PAULAXMLStructure.TAG_MARK_MARK),
	STRUCT (PAULAXMLStructure.PAULA_STRUCT_DOCTYPE_TAG, PAULAXMLStructure.TAG_STRUCT_STRUCTLIST, PAULAXMLStructure.TAG_STRUCT_STRUCT),
	REL (PAULAXMLStructure.PAULA_REL_DOCTYPE_TAG, PAULAXMLStructure.TAG_REL_RELLIST, PAULAXMLStructure.TAG_REL_REL),
	FEAT (PAULAXMLStructure.PAULA_FEAT_DOCTYPE_TAG, PAULAXMLStructure.TAG_FEAT_FEATLIST, PAULAXMLStructure.TAG_FEAT_FEAT);
	
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
