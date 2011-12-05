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
package de.hu_berlin.german.korpling.saltnpepper.pepperModules.paula.util.xPointer.tests;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		String xptr= "(#xpointer(id('tok_6')/range-to(id('tok_8'))), #xpointer(id('tok_6')/range-to(id('tok_8'))))";
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
		
		//TODO currently, the following cannot be tested, because of the XPtrInterpreter, does not work correctly. Before solving the prolem, a correct solution of how XPointers work must be found and introduced in the PAULA standard.
//		assertEquals(true, xPtrRefs.get(1).isRange());
//		assertEquals("tok_6", xPtrRefs.get(0).getLeft());
//		assertEquals("tok_8", xPtrRefs.get(0).getRight());
	}
	
	public void testBla()
	{
////		String REGEX_ID_VAL=	"[ ]*[^'xpointer'][a-zA-Z0-9_-[.]]+\\s*";
//		String REGEX_ID_VAL=	"[ ]*[a-zA-Z0-9_-[.]]+\\s*";
//		String REGEX_SHORTHAND_PTR=	"\\s*#" + REGEX_ID_VAL;
//		//full ShorthandPointernotation for simple tokens starting with a file name (file.xml#shPointer)
//		String REGEX_FULL_SHORTHAND_PTR= "[^#]+\\.xml"+ "#"+ REGEX_ID_VAL;
//		//Pointer with id() function
//		String REGEX_ID_PRTR= "id[(][']"+  REGEX_ID_VAL +"['][)]";
//		//general range 
//		String REGEX_RANGE= REGEX_ID_PRTR + "[/]range-to" + "[()]"+REGEX_ID_PRTR + "[()]";
//		//token range
//		String REGEX_RANGE_PTR= 	"xpointer[(]" + REGEX_RANGE+"[)]";
//		//token sequence
//		String REGEX_SEQ_PTR= "[(](" + REGEX_RANGE_PTR +"|"+ REGEX_SHORTHAND_PTR +"|"+REGEX_FULL_SHORTHAND_PTR+")"+"([,]("+ REGEX_RANGE_PTR +"|"+ REGEX_SHORTHAND_PTR +"|"+REGEX_FULL_SHORTHAND_PTR+"))*"+ "[)]";
//		
//		
//		String patternStr= REGEX_SEQ_PTR;
//		
//		Pattern pattern= Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
//		Matcher matcher= pattern.matcher("(xpointer(id('tok_6')/range-to(id('tok_8'))), xpointer(id('tok_6')/range-to(id('tok_8'))))");
//		while (matcher.find())
//		{
//			System.out.println("found: "+ matcher.group());
//		}
	}
	 
}
