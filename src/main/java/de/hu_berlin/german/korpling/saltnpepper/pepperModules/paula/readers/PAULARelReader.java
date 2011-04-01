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
 *This reader reads a paula file which is compatible to paula_rel.dtd.
 * 
 * @author Florian Zipser
 * @version 1.0
 */
public class PAULARelReader extends PAULASpecificReader
{
//	 --------------------------- SAX mezhods ---------------------------
	@Override
	public void startDocument()	throws SAXException
	{
	}
	
	@Override
	public void endDocument()	throws SAXException
	{
	}

	@Override
	public void characters(	char[] ch,
            				int start,
            				int length) throws SAXException
    {
	
    }
	
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
		//REL element found
		if (this.isTAGorAttribute(qName, TAG_REL_REL))
		{
			String RELID= null;		//REL.id
			String RELHref= null;	//REL.href
			String RELTarget= null;	//REL.type
			for(int i= 0; i < attributes.getLength(); i++)
			{	
				//REL.ID attribute found
				if (this.isTAGorAttribute(attributes.getQName(i), ATT_REL_REL_ID))
					RELID= attributes.getValue(i);
				//REL.HREF attribute found
				else if (this.isTAGorAttribute(attributes.getQName(i), ATT_REL_REL_HREF))
					RELHref= attributes.getValue(i);
				//REL.TYPE attribute found
				else if (this.isTAGorAttribute(attributes.getQName(i), ATT_REL_REL_TARGET))
					RELTarget= attributes.getValue(i);
			}
			
			{//checking if href contains a new not already read file
				this.checkForFileReference(RELHref);
				this.checkForFileReference(RELTarget);
			}//checking if href contains a new not already read file
			
			this.getMapper().paulaRELConnector(this.getPaulaFile(), this.getPaulaID(), this.getPaulaType(), this.getXmlBase(), RELID, RELHref, RELTarget);
		}
    }
	
	/**
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(	String uri,
            				String localName,
            				String qName) throws SAXException
    {
		
	}
}
