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
package de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.readers;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 *This reader reads a paula file which is compatible to paula_feat.dtd.
 * 
 * @author Florian Zipser
 * @version 1.0
 */
public class PAULAStructReader extends PAULASpecificReader
{
	public PAULAStructReader() 
	{
		this.init();
	}
	
	/**
	 * initializes this object
	 */
	private void init()
	{
		this.numStruct= 0;
		this.numRel= 0;	
		this.isAnnoSet= false;
	}
//	 --------------------------- SAX mezhods ---------------------------
	/**
	 * number of read struct elements
	 */
	private long numStruct= 0;
	
	/**
	 * number of read rel elements
	 */
	private long numRel= 0;
	
	/**
	 * stores struct.id
	 */
	private String structID= null;
	
	/**
	 * stores string, which identifies feats of document or corpus
	 */
	private static final String KW_ANNO_TYPE= "annoSet";
	
	/**
	 * flag stores if this file is the annoSet
	 */
	private Boolean isAnnoSet= false;
	/**
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(	String uri,
            					String localName,
            					String qName,
            					Attributes attributes) throws SAXException
    {
		{//calls super-class for setting paula-id, paula-type and xml-base
			super.startElement(uri, localName, qName, attributes);
		}//calls super-class for setting paula-id, paula-type and xml-base

		//STRUCTLIST-element found
		if (this.isTAGorAttribute(qName, TAG_STRUCT_STRUCTLIST))
		{
			if (	(this.getPaulaType()!= null) &&
					(this.getPaulaType().equalsIgnoreCase(KW_ANNO_TYPE)))
				this.isAnnoSet= true;
		}
		//STRUCT-element found
		else if (this.isTAGorAttribute(qName, TAG_STRUCT_STRUCT))
		{
			if (!isAnnoSet)
			{//do only if file is not the annoSet	
				this.numStruct++;
				for(int i= 0; i < attributes.getLength(); i++)
				{
					//ATTIBUTE STRUCT.ID
					if (this.isTAGorAttribute(attributes.getQName(i), ATT_STRUCT_STRUCT_ID))
						this.structID= attributes.getValue(i);
				}
			}//do only if file is not the annoSet
		}
		//REL-element found
		else if (this.isTAGorAttribute(qName, TAG_STRUCT_REL))
		{
			if (!isAnnoSet)
			{//do only if file is not the annoSet	
				this.numRel++;
				String relID= "";		//Attributwert von STRUCT.ID
				String relType= "";		//Attributwert von STRUCT.TYPE
				String relHref= "";		//Attributwert von STRUCT.HREF
				
				for(int i= 0; i < attributes.getLength(); i++)
				{
					//ATTIBUTE REL.ID
					if (this.isTAGorAttribute(attributes.getQName(i), ATT_STRUCT_REL_ID))
						relID= attributes.getValue(i);
					//ATTIBUTE REL.TYPE
					else if (this.isTAGorAttribute(attributes.getQName(i), ATT_STRUCT_REL_TYPE))
						relType= attributes.getValue(i);
					//ATTIBUTE REL.HREF
					else if (this.isTAGorAttribute(attributes.getQName(i), ATT_STRUCT_REL_HREF))
						relHref= attributes.getValue(i);
				}
				{//checking if href contains a new not already read file
					this.checkForFileReference(relHref);
				}//checking if href contains a new not already read file
				{//callback for mapper
					this.getMapper().paulaSTRUCTConnector(this.getPaulaFile(), this.getPaulaID(), this.getPaulaType(), this.getXmlBase(), this.structID, relID, relHref, relType);
				}//callback for mapper
			}//do only if file is not the annoSet	
		}
    }
		
	@Override
	public void endDocument()
	{
		this.getMapper().endDocument(this, this.getPaulaFile());
	}

}
