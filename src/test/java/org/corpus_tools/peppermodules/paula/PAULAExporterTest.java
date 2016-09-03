/**
 * Copyright 2009 Humboldt-Universität zu Berlin, INRIA.
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
package org.corpus_tools.peppermodules.paula;

import static org.assertj.core.api.Assertions.assertThat;

import org.corpus_tools.pepper.common.CorpusDesc;
import org.corpus_tools.pepper.common.FormatDesc;
import org.corpus_tools.pepper.common.ModuleFitness;
import org.corpus_tools.pepper.common.ModuleFitness.FitnessFeature;
import org.corpus_tools.pepper.core.ModuleFitnessChecker;
import org.corpus_tools.pepper.testFramework.PepperExporterTest;
import org.corpus_tools.pepper.testFramework.PepperTestUtil;
import org.junit.Before;
import org.junit.Test;

public class PAULAExporterTest extends PepperExporterTest {
	private PAULAExporter fixture = null;

	@Before
	public void beforeEach() {
		fixture = new PAULAExporter();

		// set formats to support
		FormatDesc formatDef = new FormatDesc();
		formatDef.setFormatName(PAULAExporter.FORMAT_NAME);
		formatDef.setFormatVersion(PAULAExporter.FORMAT_VERSION);
		this.addSupportedFormat(formatDef);

		// set corpus definition
		CorpusDesc corpDef = new CorpusDesc();
		corpDef.setFormatDesc(formatDef);
	}

	@Test
	public void whenSelfTestingModule_thenResultShouldBeTrue() {

		final ModuleFitness fitness = new ModuleFitnessChecker(PepperTestUtil.createDefaultPepper()).selfTest(fixture);
		assertThat(fitness.getFitness(FitnessFeature.HAS_SELFTEST)).isTrue();
		assertThat(fitness.getFitness(FitnessFeature.HAS_PASSED_SELFTEST)).isTrue();
	}
}
