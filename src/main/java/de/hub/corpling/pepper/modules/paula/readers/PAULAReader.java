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

import org.osgi.service.log.LogService;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

import de.hub.corpling.pepper.modules.paula.PAULA2SaltMapper;
import de.hub.corpling.pepper.modules.paula.PAULAFileDelegator;

/**
 * This class is an analyzer of PAULA-files to delegate the real reading to more specialized readers.
 * 
 * @author Florian Zipser
 * @version 1.0
 */
public class PAULAReader extends DefaultHandler2 
{
	// ================================================ start: LogService	
	private LogService logService;

	public void setLogService(LogService logService) 
	{
		this.logService = logService;
	}
	
	public LogService getLogService() 
	{
		return(this.logService);
	}
	
	public void unsetLogService(LogService logService) {
		logService= null;
	}
// ================================================ end: LogService

//=================================== start: mapper for callback
	/**
	 * PAULA2SaltMapper for callback.
	 */
	private PAULA2SaltMapper mapper= null;
	
	/**
	 * @param mapper the mapper to set
	 */
	public void setMapper(PAULA2SaltMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * @return the mapper
	 */
	public PAULA2SaltMapper getMapper() {
		return mapper;
	}
//=================================== end: mapper for callback
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

	public static final String PAULA_DTD_HEADER= "paula_header.dtd";
	public static final String PAULA_DTD_TEXT= "paula_text.dtd";
	public static final String PAULA_DTD_MARK= "paula_mark.dtd";
	public static final String PAULA_DTD_STRUCT= "paula_struct.dtd";
	public static final String PAULA_DTD_REL= "paula_rel.dtd";
	public static final String PAULA_DTD_FEAT= "paula_feat.dtd";
	public static final String PAULA_DTD_MULTIFEAT= "paula_multiFeat.dtd";
	
	
	/**
	 * Stores the currently used dtd
	 */
	private String currentDTD= null;
	
	/**
	 * Stores the specific reader to which all events shall be delgated to.
	 */
	private PAULASpecificReader specificPAULAReader= null;

//	 --------------------------- SAX Methoden ---------------------------	
	/**
	 * extracts the dtd of current read paula-file.
	 */
	public void startDTD(String name, String publicId, String systemId) throws SAXException
	{
		{//setting current DTD
			if (	(publicId!= null) && 
					(!publicId.equalsIgnoreCase("")))
				currentDTD= publicId;
			 else
			 {
				 if (	(systemId!= null) && 
						(!systemId.equalsIgnoreCase("")))
					 currentDTD= systemId;
			 }
		 }//setting current DTD
		if (	(currentDTD== null)||
				(currentDTD.equalsIgnoreCase("")))
			throw new SAXException("Error in file '"+this.getPaulaFile().getAbsolutePath()+"', no dtd is given.");
		String parts[]= currentDTD.split("/");
		currentDTD= parts[parts.length-1];
		if (currentDTD.equalsIgnoreCase(PAULA_DTD_TEXT)) 
			 this.specificPAULAReader= new PAULATextReader();
		else if (currentDTD.equalsIgnoreCase(PAULA_DTD_MARK))
			this.specificPAULAReader= new PAULAMarkReader();
		else if (currentDTD.equalsIgnoreCase(PAULA_DTD_STRUCT))
			this.specificPAULAReader= new PAULAStructReader();
		else if (currentDTD.equalsIgnoreCase(PAULA_DTD_REL))
			this.specificPAULAReader= new PAULARelReader();
		else if (currentDTD.equalsIgnoreCase(PAULA_DTD_FEAT))
			this.specificPAULAReader= new PAULAFeatReader();
		if (specificPAULAReader== null)
			throw new SAXException("Cannot parse paula-file '"+this.getPaulaFile().getAbsolutePath()+"', because no reader object for the dtd '"+currentDTD+"' has been found.");
//		System.out.println("=================> PAULAReader startDTD: "+ specificPAULAReader.getClass().getName());
		this.specificPAULAReader.setMapper(this.getMapper());
		this.specificPAULAReader.setPaulaFile(this.getPaulaFile());
		this.specificPAULAReader.setPaulaFileDelegator(this.getPaulaFileDelegator());
//		System.out.println("=================> PAULASpecificReader file: "+specificPAULAReader.getPaulaFile().getAbsolutePath());
	 }
     
	/**
	 * Delegates to specific reader.
	 */
	@Override
	public void characters(	char[] ch,
			int start,
			int length) throws SAXException
	{
		if (this.specificPAULAReader== null)
		{
			if (this.getLogService()!= null)
				this.getLogService().log(LogService.LOG_WARNING, "1 Cannot read document '"+this.getPaulaFile().getAbsolutePath()+"', this file belongs to no or an unknown dtd.");
		}
		else this.specificPAULAReader.characters(ch, start, length);
	}  
	
	/**
	 * Delegates to specific reader.
	 */
	@Override
	public void startElement(	String uri,
            					String localName,
            					String qName,
            					Attributes attributes) throws SAXException
    {
		if (this.specificPAULAReader== null)
		{
			if (this.getLogService()!= null)
				this.getLogService().log(LogService.LOG_WARNING, "2 Cannot read document '"+this.getPaulaFile().getAbsolutePath()+"', this file belongs to no or an unknown dtd.");
		}
		else this.specificPAULAReader.startElement(uri, localName, qName, attributes);
    }
	
	/**
	 * Delegates to specific reader.
	 */
	@Override
	public void endElement(	String uri,
				            String localName,
				            String qName) throws SAXException
    {
		if (this.specificPAULAReader== null)
		{
			if (this.getLogService()!= null)
				this.getLogService().log(LogService.LOG_WARNING, "3 Cannot read document '"+this.getPaulaFile().getAbsolutePath()+"', this file belongs to no or an unknown dtd.");
		}
		else this.specificPAULAReader.endElement(uri, localName, qName);
    }
	
	/**
	 * Delegates to specific reader.
	 */
	@Override
	public void startDocument()	throws SAXException
	{
		if (this.specificPAULAReader!= null)
			this.specificPAULAReader.startDocument();
	}
	
	/**
	 * Delegates to specific reader.
	 */
	@Override
	public void endDocument() throws SAXException
	{
		
		if (this.specificPAULAReader== null)
		{
			if (this.getLogService()!= null)
				this.getLogService().log(LogService.LOG_WARNING, "5 Cannot read document '"+this.getPaulaFile().getAbsolutePath()+"', this file belongs to no or an unknown dtd.");
		}
		else this.specificPAULAReader.endDocument();
	}
	
	

	
		
}
