package de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.util.xPointer.tests;

import java.util.Vector;

import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.util.xPointer.XPtrInterpreter;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.util.xPointer.XPtrRef;
import junit.framework.TestCase;

/**
 * Checks if the class {@link XPtrInterpreter} works correctly and parses XPointer syntax in correct way.
 * @author Florian Zipser
 *
 */
public class XPtrInterpreterTest extends TestCase 
{
	private XPtrInterpreter fixture= null;

	public void setFixture(XPtrInterpreter fixture) {
		this.fixture = fixture;
	}

	public XPtrInterpreter getFixture() {
		return fixture;
	}
	
	@Override
	public void setUp()
	{
		this.setFixture(new XPtrInterpreter());
	}
	
	/**
	 * Tests if the {@link XPtrInterpreter} can parse a sequence of shorthand pointers correctly.
	 * @throws Exception 
	 * 
	 */
	public void testSeqShortHandPointer() throws Exception
	{
		String xptr= "(#ref1, #ref2)";
		this.getFixture().setBase("base.xml");
		this.getFixture().setXPtr(xptr);
		Vector<XPtrRef> xPtrRefs= this.getFixture().getResult();
		assertNotNull(xPtrRefs);
		assertEquals(2, xPtrRefs.size());
		
		assertEquals("ref1",xPtrRefs.get(0).getID());
		assertEquals("base.xml",xPtrRefs.get(0).getDoc());
		
		assertEquals("ref2",xPtrRefs.get(1).getID());
		assertEquals("base.xml",xPtrRefs.get(1).getDoc());
	}
}
