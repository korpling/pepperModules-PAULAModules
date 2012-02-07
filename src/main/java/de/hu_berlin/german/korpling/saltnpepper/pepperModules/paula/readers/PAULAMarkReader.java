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
 *This reader reads a paula file which is compatible to paula_mark.dtd.
 * 
 * @author Florian Zipser
 * @version 1.0
 */
public class PAULAMarkReader extends PAULASpecificReader
{
	private final static String ATT_TOK="tok";
//	 --------------------------- SAX methods ---------------------------
	
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
		//Tag MARK gefunden
		if (this.isTAGorAttribute(qName, TAG_MARK_MARK))
		{
			String markID= null;	//mark.id-Wert
			String markHref= null;	//mark.href-Wert
			String markType= null;	//mark.type-Wert
			for(int i= 0; i < attributes.getLength(); i++)
			{	
				
				//Attribut MARK.ID gefunden
				if (this.isTAGorAttribute(attributes.getQName(i), ATT_MARK_MARK_ID))
					markID= attributes.getValue(i);
				//Attribut MARK.HREF gefunden
				else if (this.isTAGorAttribute(attributes.getQName(i), ATT_MARK_MARK_HREF))
					markHref= attributes.getValue(i);
				//Attribut MARK.TYPE gefunden
				else if (this.isTAGorAttribute(attributes.getQName(i), ATT_MARK_MARK_TYPE))
					markType= attributes.getValue(i);
			}
			{// ignore virtual markeables
				
				if ((markType != null) && 
						(!markType.isEmpty())){
					if (markType.equalsIgnoreCase(VALUE_MARK_MARK_TYPE))
						return;
				}
				
			}// ignore virtual markeables
			{//checking if href contains a new not already read file
				this.checkForFileReference(markHref);
			}//checking if href contains a new not already read file
				
			if (	(this.getPaulaType()!= null)&&
					(this.getPaulaType().equalsIgnoreCase(ATT_TOK)))
			{//callback to mapper when type is tok	
				this.getMapper().paulaMARK_TOKConnector(this.getPaulaFile(), this.getPaulaID(), this.getPaulaType(), this.getXmlBase(), markID, markHref, markType);
			}//callback to mapper when type is tok
			else
			{//callback to mapper when type is normal mark
				this.getMapper().paulaMARKConnector(this.getPaulaFile(), this.getPaulaID(), this.getPaulaType(), this.getXmlBase(), markID, markHref, markType);
			}//callback to mapper when type is normal mark
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
