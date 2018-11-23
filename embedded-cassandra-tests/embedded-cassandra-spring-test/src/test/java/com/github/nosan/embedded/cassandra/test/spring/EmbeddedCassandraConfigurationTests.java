/*
 * Copyright 2018-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.nosan.embedded.cassandra.test.spring;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EmbeddedCassandraContextCustomizer}.
 *
 * @author Dmytro Nosan
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@EmbeddedCassandra(scripts = "/init.cql", statements = "CREATE TABLE IF NOT EXISTS test.roles (   id text PRIMARY" +
		"  KEY );")
@DirtiesContext
public class EmbeddedCassandraConfigurationTests {


	@Autowired
	private Cluster cluster;


	@Test
	public void shouldSelectFromRoles() {
		try (Session session = this.cluster.connect()) {
			assertThat(session.execute("SELECT * FROM  test.roles").wasApplied())
					.isTrue();
		}
	}

}
