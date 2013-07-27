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
package de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.tests;

import junit.framework.TestCase;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.PAULAExporter;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.exceptions.PAULAExporterException;

public class PAULAExporterTest extends TestCase
{
	private PAULAExporter fixture= null;
	
	public PAULAExporter getFixture() {
		return fixture;
	}

	public void setFixture(PAULAExporter fixture) {
		this.fixture = fixture;
	} 
	
	public void setUp()
	{
		this.setFixture(new PAULAExporter());
	}
		
	public void testMapCorpusStructure(){
		try {
		this.getFixture().mapCorpusStructure(null, null);
		fail("Null corpus Graph");
		} catch (PAULAExporterException e){
			
		}	
		
	}
}
