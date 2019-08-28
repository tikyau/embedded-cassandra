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

package com.github.nosan.embedded.cassandra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import com.github.nosan.embedded.cassandra.api.Cassandra;
import com.github.nosan.embedded.cassandra.api.Version;
import com.github.nosan.embedded.cassandra.artifact.Artifact;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EmbeddedCassandraFactory}.
 *
 * @author Dmytro Nosan
 */
class EmbeddedCassandraFactoryTests {

	private final EmbeddedCassandraFactory cassandraFactory = new EmbeddedCassandraFactory();

	@Test
	void testName() {
		this.cassandraFactory.setName("myname");
		assertThat(this.cassandraFactory.getName()).isEqualTo("myname");
		Cassandra cassandra = this.cassandraFactory.create();
		assertThat(cassandra.getName()).isEqualTo("myname");
	}

	@Test
	void testWorkingDir(@TempDir Path temporaryFolder) {
		this.cassandraFactory.setWorkingDirectory(temporaryFolder);
		Cassandra cassandra = this.cassandraFactory.create();
		assertThat(cassandra).hasFieldOrPropertyWithValue("workingDirectory", temporaryFolder);
	}

	@Test
	void testArtifact(@TempDir Path temporaryFolder) {
		final Version version = Version.of("3.11.4");
		Artifact artifact = () -> new Artifact.Resource() {

			@Override
			public Path getDirectory() {
				return temporaryFolder;
			}

			@Override
			public Version getVersion() {
				return version;
			}
		};
		this.cassandraFactory.setArtifact(artifact);
		Cassandra cassandra = this.cassandraFactory.create();
		assertThat(cassandra).hasFieldOrPropertyWithValue("artifactDirectory", temporaryFolder)
				.hasFieldOrPropertyWithValue("version", version);

	}

	@Test
	void testJavaHome(@TempDir Path temporaryFolder) {
		this.cassandraFactory.setJavaHome(() -> temporaryFolder);
		Cassandra cassandra = this.cassandraFactory.create();
		Object node = ReflectionTestUtils.getField(ReflectionTestUtils.getField(cassandra, "database"), "node");
		assertThat(node).hasFieldOrPropertyWithValue("javaHome", temporaryFolder);
	}

	@Test
	void testLogger() {
		Logger mylogger = LoggerFactory.getLogger("mylogger");
		this.cassandraFactory.setLogger(mylogger);
		Cassandra cassandra = this.cassandraFactory.create();
		Object database = ReflectionTestUtils.getField(cassandra, "database");
		assertThat(database).hasFieldOrPropertyWithValue("logger", mylogger);
	}

	@Test
	void testDaemon() {
		this.cassandraFactory.setDaemon(true);
		Cassandra cassandra = this.cassandraFactory.create();
		Object database = ReflectionTestUtils.getField(cassandra, "database");
		assertThat(database).hasFieldOrPropertyWithValue("daemon", true);
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void testRootAllowed() {
		this.cassandraFactory.setRootAllowed(true);
		Cassandra cassandra = this.cassandraFactory.create();
		Object node = ReflectionTestUtils.getField(ReflectionTestUtils.getField(cassandra, "database"), "node");
		assertThat(node).hasFieldOrPropertyWithValue("allowRoot", true);
	}

	@Test
	void getJvmOptions() {
		this.cassandraFactory.getJvmOptions().add("-Xmx512m");
		Cassandra cassandra = this.cassandraFactory.create();
		Object node = ReflectionTestUtils.getField(ReflectionTestUtils.getField(cassandra, "database"), "node");
		assertThat(node).hasFieldOrPropertyWithValue("jvmOptions", Collections.singletonList("-Xmx512m"));
	}

	@Test
	void getSystemProperties() {
		this.cassandraFactory.getSystemProperties().put("key", "value");
		Cassandra cassandra = this.cassandraFactory.create();
		Object node = ReflectionTestUtils.getField(ReflectionTestUtils.getField(cassandra, "database"), "node");
		assertThat(node).hasFieldOrPropertyWithValue("systemProperties", Collections.singletonMap("key", "value"));
	}

	@Test
	void getEnvironmentVariables() {
		this.cassandraFactory.getEnvironmentVariables().put("key", "value");
		Cassandra cassandra = this.cassandraFactory.create();
		Object node = ReflectionTestUtils.getField(ReflectionTestUtils.getField(cassandra, "database"), "node");
		assertThat(node).hasFieldOrPropertyWithValue("environmentVariables", Collections.singletonMap("key", "value"));
	}

	@Test
	void getProperties() {
		this.cassandraFactory.getProperties().put("key", "value");
		Cassandra cassandra = this.cassandraFactory.create();
		Object node = ReflectionTestUtils.getField(ReflectionTestUtils.getField(cassandra, "database"), "node");
		assertThat(node).hasFieldOrPropertyWithValue("properties", Collections.singletonMap("key", "value"));
	}

	@Test
	void testTimeout() {
		this.cassandraFactory.setTimeout(Duration.ofSeconds(60));
		Cassandra cassandra = this.cassandraFactory.create();
		Object database = ReflectionTestUtils.getField(cassandra, "database");
		assertThat(database).hasFieldOrPropertyWithValue("timeout", Duration.ofSeconds(60));
	}

	@Test
	void testPort() {
		this.cassandraFactory.setPort(9042);
		assertThat(this.cassandraFactory.getPort()).isEqualTo(9042);
		Cassandra cassandra = this.cassandraFactory.create();
		Object node = ReflectionTestUtils.getField(ReflectionTestUtils.getField(cassandra, "database"), "node");
		assertThat(node).hasFieldOrPropertyWithValue("systemProperties",
				Collections.singletonMap("cassandra.native_transport_port", 9042));
	}

	@Test
	void testSslPort() {
		this.cassandraFactory.setSslPort(9042);
		assertThat(this.cassandraFactory.getSslPort()).isEqualTo(9042);
		Cassandra cassandra = this.cassandraFactory.create();
		Object node = ReflectionTestUtils.getField(ReflectionTestUtils.getField(cassandra, "database"), "node");
		assertThat(node)
				.hasFieldOrPropertyWithValue("properties", Collections.singletonMap("native_transport_port_ssl", 9042));
	}

	@Test
	void testRpcPort() {
		this.cassandraFactory.setRpcPort(9160);
		assertThat(this.cassandraFactory.getRpcPort()).isEqualTo(9160);
		Cassandra cassandra = this.cassandraFactory.create();
		Object node = ReflectionTestUtils.getField(ReflectionTestUtils.getField(cassandra, "database"), "node");
		assertThat(node)
				.hasFieldOrPropertyWithValue("systemProperties", Collections.singletonMap("cassandra.rpc_port", 9160));
	}

	@Test
	void testConfigurationFile(@TempDir Path temporaryFolder) throws IOException {
		Path file = Files.createFile(temporaryFolder.resolve("cassandra.yaml"));
		this.cassandraFactory.setConfigurationFile(file.toUri());
		assertThat(this.cassandraFactory.getConfigurationFile()).isEqualTo(file.toUri().toURL());
		Cassandra cassandra = this.cassandraFactory.create();
		Object node = ReflectionTestUtils.getField(ReflectionTestUtils.getField(cassandra, "database"), "node");
		assertThat(node).hasFieldOrPropertyWithValue("systemProperties",
				Collections.singletonMap("cassandra.config", file.toUri().toURL().toString()));

	}

	@Test
	void testStoragePort() {
		this.cassandraFactory.setStoragePort(7000);
		assertThat(this.cassandraFactory.getStoragePort()).isEqualTo(7000);
		Cassandra cassandra = this.cassandraFactory.create();
		Object node = ReflectionTestUtils.getField(ReflectionTestUtils.getField(cassandra, "database"), "node");
		assertThat(node).hasFieldOrPropertyWithValue("systemProperties",
				Collections.singletonMap("cassandra.storage_port", 7000));
	}

	@Test
	void testStoragePortSsl() {
		this.cassandraFactory.setStorageSslPort(7001);
		assertThat(this.cassandraFactory.getStorageSslPort()).isEqualTo(7001);
		Cassandra cassandra = this.cassandraFactory.create();
		Object node = ReflectionTestUtils.getField(ReflectionTestUtils.getField(cassandra, "database"), "node");
		assertThat(node).hasFieldOrPropertyWithValue("systemProperties",
				Collections.singletonMap("cassandra.ssl_storage_port", 7001));
	}

	@Test
	void testJmxLocal() {
		this.cassandraFactory.setJmxLocalPort(7199);
		assertThat(this.cassandraFactory.getJmxLocalPort()).isEqualTo(7199);
		Cassandra cassandra = this.cassandraFactory.create();
		Object node = ReflectionTestUtils.getField(ReflectionTestUtils.getField(cassandra, "database"), "node");
		assertThat(node).hasFieldOrPropertyWithValue("systemProperties",
				Collections.singletonMap("cassandra.jmx.local.port", 7199));
	}

	@Test
	@SuppressWarnings("unchecked")
	void testShutdownHook() throws Exception {
		this.cassandraFactory.setRegisterShutdownHook(true);
		this.cassandraFactory.setName("myname");
		Cassandra cassandra = this.cassandraFactory.create();
		Map<Thread, Thread> hooks = (Map<Thread, Thread>) ReflectionTestUtils
				.getField(Class.forName("java.lang.ApplicationShutdownHooks"), "hooks");
		assertThat(hooks.keySet()).anyMatch(thread -> thread.getName().equals("myname-sh"));
	}

}