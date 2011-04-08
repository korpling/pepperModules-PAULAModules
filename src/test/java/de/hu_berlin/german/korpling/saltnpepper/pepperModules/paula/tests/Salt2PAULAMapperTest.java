package de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.tests;

import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.Salt2PAULAMapper;
import junit.framework.TestCase;

public class Salt2PAULAMapperTest extends TestCase {
	private Salt2PAULAMapper fixture = null;

	public Salt2PAULAMapper getFixture() {
		return fixture;
	}

	public void setFixture(Salt2PAULAMapper fixture) {
		this.fixture = fixture;
	}
	
	@Override	
	public void setUp(){
		this.setFixture(new Salt2PAULAMapper());
	}
	
	public void testMapCorpusStructure(){
		assertEquals(null,fixture.mapCorpusStructure(null, null));
			
		
	}
	
	
}
