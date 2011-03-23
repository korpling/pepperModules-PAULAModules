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
package de.hub.corpling.pepper.modules.paula.readers;

import java.io.File;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

import de.hub.corpling.pepper.modules.paula.PAULA2SaltMapper;
import de.hub.corpling.pepper.modules.paula.PAULAFileDelegator;
import de.hub.corpling.pepper.modules.paula.util.xPointer.XPtrInterpreter;

public abstract class PAULASpecificReader extends DefaultHandler2 
{
	//tags and attributes for files of type TEXT(text.dtd)
	protected static final String[] TAG_TEXT_BODY= {"body"};		//Tagname des Tags body
	
	//specification of all paula tags and attributes
	protected static final String[] ATT_ID=	{"id"};				//Attribut id
	
	//allgemeine Tags und Attribute fuer alle PAULA-Dokumente (Header)
	protected static final String[] TAG_HEADER= {"header"};				//Tagname des Tags header
	protected static final String[] ATT_HEADER_PAULA_ID= {"paula_id", "sfb_id"};	//Attributname des Attributes header.paula_id
	protected static final String[] ATT_HEADER_ID=	{"id"};				//Attributname des Attributes header.id
	protected static final String[] ATT_HEADER_TYPE=	{"type"};				//Attributname des Attributes header.id
	
	
	
	//Tags und Attribute for Dateien vom paulatyp MARK(mark.dtd)
	protected static final String[] TAG_MARK_MARKLIST= 	{"marklist"};			//Tagname des Tags markList
	protected static final String[] ATT_MARK_MARKLIST_BASE= 	{"xml:base"};	//Attributname des Attributs markList.base	
	protected static final String[] ATT_MARK_MARKLIST_TYPE= 	{"type"};		//Attributname des Attributs markList.type
	
	protected static final String[] TAG_MARK_MARK= 			{"mark"};		//Tagname des Tags mark
	protected static final String[] ATT_MARK_MARK_ID= 		{"id"};			//Attributname des Attributs mark.id
	protected static final String[] ATT_MARK_MARK_HREF= 	{"xlink:href"};		//Attributname des Attributs mark.href
	protected static final String[] ATT_MARK_MARK_TYPE= 	{"type"};		//Attributname des Attributs mark.type
	
	//tags from dtd paula_rel.dtd
	protected static final String[] TAG_REL_RELLIST= 	{"rellist"};			//Tagname des Tags relList
	protected static final String[] ATT_REL_RELLIST_BASE= 	{"xml:base"};	//Attributname des Attributs markList.base	
	protected static final String[] ATT_REL_RELLIST_TYPE= 	{"type"};		//Attributname des Attributs markList.type
	
	protected static final String[] TAG_REL_REL= 		{"rel"};			//Tagname des Tags rel
	protected static final String[] ATT_REL_REL_ID= 		{"id"};				//Attributname des Attributs rel.id
	protected static final String[] ATT_REL_REL_HREF= 	{"xlink:href"};		//Attributname des Attributs rel.href
	protected static final String[] ATT_REL_REL_TARGET= 	{"target"};		//Attributname des Attributs rel.href
	
	//Tags und Attribute for Dateien vom paulatyp STRUCT(struct.dtd)
	protected static final String[] TAG_STRUCT_STRUCTLIST= 			{"structlist"};		//Tagname des Tags structList
	protected static final String[] ATT_STRUCT_STRUCTLIST_BASE= 	{"xml:base"};		//Attributname des Attributs structList.base	
	protected static final String[] ATT_STRUCT_STRUCTLIST_TYPE= 	{"type"};			//Attributname des Attributs structList.type
	
	protected static final String[] TAG_STRUCT_STRUCT= 			{"struct"};		//Tagname des Tags struct
	protected static final String[] ATT_STRUCT_STRUCT_ID= 		{"id"};			//Attributname des Attributs struct.id

	protected static final String[] TAG_STRUCT_REL= 		{"rel"};			//Tagname des Tags rel
	protected static final String[] ATT_STRUCT_REL_ID= 		{"id"};				//Attributname des Attributs rel.id
	protected static final String[] ATT_STRUCT_REL_HREF= 	{"xlink:href"};		//Attributname des Attributs rel.href
	protected static final String[] ATT_STRUCT_REL_TYPE= 	{"type"};			//Attributname des Attributs rel.type
	
	//Tags und Attribute for Dateien vom paulatyp FEAT(feat.dtd)
	protected static final String[] TAG_FEAT_FEATLIST= 		{"featList"};		//Tagname des Tags featList
	protected static final String[] ATT_FEAT_FEATLIST_BASE= {"xml:base"};		//Attributname des Attributs featList.base	
	protected static final String[] ATT_FEAT_FEATLIST_TYPE= {"type"};			//Attributname des Attributs featList.type
	
	//Tags und Attribute for Dateien vom paulatyp FEAT(feat.dtd)
	protected static final String[] TAG_FEAT_FEAT= 		{"feat"};			//Tagname des Tags feat
	protected static final String[] ATT_FEAT_FEAT_ID= 	{"id"};				//Attributname des Attributs feat.id
	protected static final String[] ATT_FEAT_FEAT_HREF= {"xlink:href"};		//Attributname des Attributs feat.href
	protected static final String[] ATT_FEAT_FEAT_TAR= 	{"target"};			//Attributname des Attributs feat.target
	protected static final String[] ATT_FEAT_FEAT_VAL= 	{"value"};			//Attributname des Attributs feat.value
	protected static final String[] ATT_FEAT_FEAT_DESC= {"description"};	//Attributname des Attributs feat.description
	protected static final String[] ATT_FEAT_FEAT_EXP= 	{"example"};		//Attributname des Attributs feat.example
	
	//Tags und Attribute for Dateien vom paulatyp MULTIFEAT(multi.dtd)
	protected static final String[] TAG_MULTI_MULTIFEATLIST= 		{"multifeatlist"};		//Tagname des Tags featList
	protected static final String[] ATT_MULTI_MULTIFEATLIST_BASE= 	{"xml:base"};			//Attributname des Attributs featList.base	
	protected static final String[] ATT_MULTI_MULTIFEATLIST_TYPE= 	{"type"};				//Attributname des Attributs featList.type
	
	//Tags und Attribute for Dateien vom paulatyp MULTIFEAT(feat.dtd)
	protected static final String[] TAG_MULTI_MULTIFEAT= 		{"multifeat"};		//Tagname des Tags feat
	protected static final String[] ATT_MULTI_MULTIFEAT_ID= 	{"id"};				//Attributname des Attributs multifeat.id
	protected static final String[] ATT_MULTI_MULTIFEAT_HREF= 	{"xlink:href", "href"};		//Attributname des Attributs multifeat.href
	
	//Tags und Attribute for Dateien vom paulatyp MULTIFEAT(feat.dtd)
	protected static final String[] TAG_MULTI_FEAT= 			{"feat"};		//Tagname des Tags feat
	protected static final String[] ATT_MULTI_FEAT_ID= 			{"id"};					//Attributname des Attributs feat.id
	protected static final String[] ATT_MULTI_FEAT_NAME= 		{"name"};		//Attributname des Attributs feat.name
	protected static final String[] ATT_MULTI_FEAT_VALUE= 		{"value"};		//Attributname des Attributs feat.value
	
	/**
	 * Gibt zur�ck, ob ein gegebener Attribut oder Tagname in der �bergebenen Liste
	 * von Attribut- oder Wertnamen enthalten ist.
	 * @param val String - zu suchender Attribut- oder Tagname
	 * @param list String[] - Stringliste in der gesucht werden soll 
	 */
	public boolean isTAGorAttribute(String val, String[] list)
	{
		boolean retVal= false; 
		for (String element: list)
			if (element.equalsIgnoreCase(val)) retVal= true;
		return(retVal);
	}
//=============================== start: paulaFile
	/**
	 * Stores the current read paula-file
	 */
	private File paulaFile= null;
	/**
	 * @param paulaFile the paulaFile to set
	 */
	public void setPaulaFile(File paulaFile) {
		this.paulaFile = paulaFile;
	}

	/**
	 * @return the paulaFile
	 */
	public File getPaulaFile() {
		return paulaFile;
	}
//=============================== end: paulaFile
//=============================== start: paula-id
	/**
	 * Id of current paula-document. Tag in paula-fiule: header.paula_id.
	 */
	private String paulaID= null;
	/**
	 * Sets Id of current paula-document. Tag in paula-fiule: header.paula_id.
	 * @param paulaID the paulaID to set
	 */
	public void setPaulaID(String paulaID) {
		this.paulaID = paulaID;
	}

	/**
	 * Returns Id of current paula-document. Tag in paula-fiule: header.paula_id.
	 * @return the paulaID
	 */
	public String getPaulaID() {
		return paulaID;
	}
//=============================== end: paula-id
//=============================== start: xml-base
	/**
	 * base document of this file. Base means the anchor to which all not fully qualified links refer to.
	 */
	private String xmlBase= null;
	/**
	 * Sets base document of this file. Base means the anchor to which all not fully qualified links refer to.
	 * @param xmlBase the xmlBase to set
	 */
	public void setXmlBase(String xmlBase) {
		this.xmlBase = xmlBase;
	}

	/**
	 * Returns base document of this file. Base means the anchor to which all not fully qualified links refer to.
	 * @return the xmlBase
	 */
	public String getXmlBase() {
		return xmlBase;
	}
//=============================== end: xml-base

//=============================== start: paula-type
	/**
	 * the type of the paula-document.
	 */
	private String paulaType= null;
	
	/**
	 * Returns the type of the paula-document.
	 */
	public String getPaulaType() {
		return paulaType;
	}

	/**
	 * Sets the type of the paula-document.
	 * @param paulaType
	 */
	public void setPaulaType(String paulaType) {
		this.paulaType = paulaType;
	}
//=============================== end: paula-type
//=============================== start: mapper	
	/**
	 * The mapper object for callback
	 */
	private PAULA2SaltMapper mapper= null;
	
	public PAULA2SaltMapper getMapper() {
		return mapper;
	}

	public void setMapper(PAULA2SaltMapper mapper) {
		this.mapper = mapper;
	}
//=============================== end: mapper
//=============================== start: paula-file-delegator
	/**
	 * The PAULAFileDelegator, which takes controll over loading paula--files
	 */
	private PAULAFileDelegator paulaFileDelegator= null;
	
	/**
	 * @param paulaFileDelegator the paulaFileDelegator to set
	 */
	public void setPaulaFileDelegator(PAULAFileDelegator paulaFileDelegator) {
		this.paulaFileDelegator = paulaFileDelegator;
	}

	/**
	 * @return the paulaFileDelegator
	 */
	public PAULAFileDelegator getPaulaFileDelegator() {
		return paulaFileDelegator;
	}
//=============================== end: paula-file-delegator
	/**
	 * Checks the given String (in XPointer-syntax), if it contains a reference to another file
	 * and initialize to read this file.
	 * @param xptr - the attribute to check
	 */
	protected void checkForFileReference(String xPtr)
	{
		if (	(xPtr!= null) &&
				(!xPtr.equalsIgnoreCase("")))
		{	
			XPtrInterpreter xPtrInterpreter= new XPtrInterpreter();
			xPtrInterpreter.setInterpreter(null, xPtr);
			if (	(xPtrInterpreter.getDoc()!= null) &&
					(!xPtrInterpreter.getDoc().equalsIgnoreCase("")))
			{//if xpointer contains a document
				File referedFile= new File(xPtrInterpreter.getDoc());
				this.getPaulaFileDelegator().startPaulaFile(referedFile);
			}//if xpointer contains a document
		}
	}
//=============================== start: sax-methods
	/**
	 * Extracts some general information, which can be necessary for all derived specific readers.
	 * If a file has been detected in xml-base, this methods makes sure, that it will be imported before.
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(	String uri,
            					String localName,
            					String qName,
            					Attributes attributes) throws SAXException
    {
		try
		{
			//TAG HEADER
			if (this.isTAGorAttribute(qName, TAG_HEADER))
			{
				for(int i= 0; i < attributes.getLength(); i++)
				{	
					{//setting paula-id
						if (this.isTAGorAttribute(attributes.getQName(i), ATT_HEADER_PAULA_ID))
							this.setPaulaID(attributes.getValue(i));
					}//setting paula-id
				}
			}
			//Tag *LIST 
			else if (	(this.isTAGorAttribute(qName, TAG_MARK_MARKLIST)) ||
						(this.isTAGorAttribute(qName, TAG_STRUCT_STRUCTLIST)) ||
						(this.isTAGorAttribute(qName, TAG_REL_RELLIST))||
						(this.isTAGorAttribute(qName, TAG_FEAT_FEATLIST))||
						(this.isTAGorAttribute(qName, TAG_MULTI_MULTIFEATLIST)))
			{//set values xml-base, paula-type
				for(int i= 0; i < attributes.getLength(); i++)
				{	
					//Attribut MARKLIST.BASE gefunden
					if (this.isTAGorAttribute(attributes.getQName(i), ATT_MARK_MARKLIST_BASE))
						this.setXmlBase(attributes.getValue(i));
					//Attribut MARKLIST.TYPE gefunden
					else if (this.isTAGorAttribute(attributes.getQName(i), ATT_MARK_MARKLIST_TYPE))
						this.setPaulaType(attributes.getValue(i));
				}
				{//making sure, that file refered by xml-base has been read or will be read
					if (	(this.getXmlBase()!= null)&&
							(!this.getXmlBase().equalsIgnoreCase("")))
					{
						File referedFile= new File(this.getXmlBase());
						this.getPaulaFileDelegator().startPaulaFile(referedFile);
					}
				}//making sure, that file refered by xml-base has been read or will be read
			}
		}
		catch (Exception e)
		{
			throw new SAXException(e);
		}
    }
//=============================== end: sax-methods
}
