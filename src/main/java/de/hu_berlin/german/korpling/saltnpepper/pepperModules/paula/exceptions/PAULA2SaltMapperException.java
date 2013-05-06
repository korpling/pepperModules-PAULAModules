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
package de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.exceptions;

import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperModules.exceptions.PepperMapperException;

public class PAULA2SaltMapperException extends PepperMapperException {	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8139718971714405400L;
	private static final String STD_MSG= "This exception was thrown by the PAULAImporter: "; 
	
    public PAULA2SaltMapperException(String s)
    { super(STD_MSG+ s); }
    
	public PAULA2SaltMapperException(String s, Throwable ex)
	{super(STD_MSG+s, ex); }
}
