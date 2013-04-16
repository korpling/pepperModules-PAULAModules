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
 *This reader reads a paula file which is compatible to paula_text.dtd.
 * 
 * @author Florian Zipser
 * @version 1.0
 */
public class PAULATextReader extends PAULASpecificReader
{
	
	
	private StringBuffer text= null;						//primary data
	private boolean startText= false;				//gibt an, ob das aktuelle Element Texte enthaelt
	private String paulaID= null;					//Paula_id

	
//	 --------------------------- SAX methods ---------------------------
	public void startDocument()	throws SAXException
	{
//		try
//		{
//			//Mapper Bescheid geben, dass das Parsen beginnt
//			this.mapper.startDocument(this, this.paulaFile, this.korpusPath);
////			if (this.logger != null) this.logger.log(LogService.LOG_DEBUG, MSG_STD + "reading document: "+ this.paulaFile.getCanonicalPath());
//		}
//		catch (Exception e)
//		{
//			throw new SAXException("An error occurs while parsing document: "+ e.getMessage());
//		}
	}
	
	public void endDocument()	throws SAXException
	{
//		try
//		{
//			//Mapper Bescheid geben, dass das Parsen beginnt
//			this.mapper.endDocument(this, this.paulaFile, this.korpusPath);
//		}
//		catch (Exception e)
//		{
//			throw new SAXException("An error occurs while parsing document: "+ e.getMessage());
//		}
	}

	/**
	 * Liest den Primaertext dieses Dokumentes aus und schreibt ees in das interne 
	 * Textfeld.
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	public void characters(	char[] ch,
            				int start,
            				int length) throws SAXException
    {
		//Der folgende Text sind Primaerdaten
		if (this.startText)
		{
			StringBuffer textNode= new StringBuffer();
	    	for (int i= 0; i < length; i++)
	    		{ textNode.append(ch[start+i]); }
	    	//Leerzeichen entfernen
	    	//textNode= textNode.trim();
	    	//Text speichern, wenn es einen gibt
	    	if (textNode.length() > 0) 
	    	{
	    		if (this.text== null)
	    			this.text= new StringBuffer();
	    		this.text.append(textNode.toString());
	    	}
		}
    }
	
	/**
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(	String uri,
            					String localName,
            					String qName,
            					Attributes attributes) throws SAXException
    {
		{//calls super-class for setting paula-id, paula-type and xml-base
			super.startElement(uri, localName, qName, attributes);
		}//calls super-class for setting paula-id, paula-type and xml-base
		//BODY-element found
		if (this.isTAGorAttribute(qName, TAG_TEXT_BODY))
			this.startText= true;
    }
	
	/**
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(	String uri,
            				String localName,
            				String qName) throws SAXException
    {
		//Element erreicht bei dem der Text endet
		if (this.isTAGorAttribute(qName, TAG_TEXT_BODY))
		{
			this.startText= false;
			//aus den Primaerdaten einen PD-Knoten im Korpusgraphen erstellen
			try
			{
				//PrimDataConnector im Mapper aufrufen
				this.getMapper().paulaTEXTConnector(this.getPaulaFile(), this.paulaID, this.text.toString());
			}
			catch (Exception e)
			{ 
				throw new SAXException(e); 
			}
		}
		this.text= null;
    }
}
