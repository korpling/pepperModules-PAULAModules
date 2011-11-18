package de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.util.xPointer.tests;

import java.util.Vector;

import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.util.xPointer.XPtrInterpreter;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.util.xPointer.XPtrRef;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.util.xPointer.XPtrRef.POINTERTYPE;
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
	 * <ul>
	 * 	<li>(#ref1, #ref2)</li>
	 * </ul>
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
	/**
	 * Tests if the {@link XPtrInterpreter} can parse a sequence of shorthand pointers correctly.
	 * <ul>
	 * 	<li>(#ref1, #ref2)</li>
	 * </ul>
	 * @throws Exception 
	 * 
	 */
	public void testSeqOfRanges() throws Exception
	{
		String base= "base.xml";
		String xptr= "(xpointer(id('tok_6')/range-to(id('tok_8'))), xpointer(id('tok_6')/range-to(id('tok_8'))))";
		this.getFixture().setBase(base);
		this.getFixture().setXPtr(xptr);
		Vector<XPtrRef> xPtrRefs= this.getFixture().getResult();
		assertNotNull(xPtrRefs);
		assertEquals(2, xPtrRefs.size());
		assertNotNull(xPtrRefs.get(0));
		assertEquals(base, xPtrRefs.get(0).getDoc());
		assertEquals(POINTERTYPE.ELEMENT, xPtrRefs.get(0).getType());
		assertEquals(true, xPtrRefs.get(0).isRange());
		assertEquals("tok_6", xPtrRefs.get(0).getLeft());
		assertEquals("tok_8", xPtrRefs.get(0).getRight());
		
		assertNotNull(xPtrRefs.get(1));
		assertEquals(base, xPtrRefs.get(1).getDoc());
		assertEquals(POINTERTYPE.ELEMENT, xPtrRefs.get(1).getType());
		assertEquals(true, xPtrRefs.get(1).isRange());
		assertEquals("tok_6", xPtrRefs.get(0).getLeft());
		assertEquals("tok_8", xPtrRefs.get(0).getRight());
	}
	 
}
