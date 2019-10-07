/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.nosan.embedded.cassandra.api.cql;

import java.util.Collections;
import java.util.List;

import com.github.nosan.embedded.cassandra.annotations.Nullable;

/**
 * Abstract {@link CqlScript} that parses a CQL script into the statements.
 *
 * @author Dmytro Nosan
 * @since 3.0.0
 */
public abstract class AbstractCqlScript implements CqlScript {

	@Override
	public final List<String> getStatements() {
		return Collections.unmodifiableList(new Parser(getScript()).getStatements());
	}

	/**
	 * Returns the {@code CQL} script.
	 *
	 * @return the script to be parsed.
	 */
	@Nullable
	protected abstract String getScript();

}
