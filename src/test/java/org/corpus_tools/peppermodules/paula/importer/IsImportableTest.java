package org.corpus_tools.peppermodules.paula.importer;

import static org.junit.Assert.assertEquals;

import org.corpus_tools.pepper.testFramework.PepperTestUtil;
import org.corpus_tools.peppermodules.paula.PAULAImporter;
import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Test;

public class IsImportableTest {

	private PAULAImporter fixture;

	public PAULAImporter getFixture() {
		return fixture;
	}

	public void setFixture(PAULAImporter fixture) {
		this.fixture = fixture;
	}

	@Before
	public void beforeEach() {
		setFixture(new PAULAImporter());
	}

	public static String getTestResources() {
		return (PepperTestUtil.getTestResources() + "isImportable/");
	}

	@Test
	public void whenCorpusPathContainsNoPAULAFiles_thenReturn0() {
		URI corpusPath = URI.createFileURI(getTestResources() + "noPaula/");
		assertEquals(Double.valueOf(0.0), getFixture().isImportable(corpusPath));
	}

	@Test
	public void whenCorpusPathContainsNoFilesWithPaulaEnding_thenReturn0() {
		URI corpusPath = URI.createFileURI(getTestResources() + "fakePaula/");
		assertEquals(Double.valueOf(0.0), getFixture().isImportable(corpusPath));
	}

	@Test
	public void whenCorpusPathContainsOnlyPaulaFiles_thenReturn1() {
		URI corpusPath = URI.createFileURI(getTestResources() + "onlyPaula/");
		assertEquals(Double.valueOf(1.0), getFixture().isImportable(corpusPath));
	}

	@Test
	public void whenCorpusPathContainsPaulaAndNonePaulaFiles_thenReturn1() {
		URI corpusPath = URI.createFileURI(getTestResources() + "mixedContent/");
		assertEquals(Double.valueOf(1.0), getFixture().isImportable(corpusPath));
	}
}
