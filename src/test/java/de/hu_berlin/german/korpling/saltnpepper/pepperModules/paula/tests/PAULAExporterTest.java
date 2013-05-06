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
