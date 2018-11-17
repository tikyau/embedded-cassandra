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

package com.github.nosan.embedded.cassandra.local;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

import com.github.nosan.embedded.cassandra.Settings;
import com.github.nosan.embedded.cassandra.Version;
import com.github.nosan.embedded.cassandra.util.PortUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link TransportUtils}.
 *
 * @author Dmytro Nosan
 */
public class TransportUtilsTests {

	@Test
	public void testEverything() throws Exception {
		Set<Integer> ports = new LinkedHashSet<>();
		while (ports.size() != 5) {
			ports.add(PortUtils.getPort());
		}
		List<ServerSocket> sockets = new ArrayList<>();
		Iterator<Integer> it = ports.iterator();
		TestSettings settings = new TestSettings(it.next(), it.next(), it.next(), it.next(), it.next());
		try {
			for (Integer port : ports) {
				sockets.add(new ServerSocket(port));
			}
			assertThat(TransportUtils.isReady(settings)).describedAs("Transport is not ready").isTrue();
			for (ServerSocket socket : sockets) {
				try {
					TransportUtils.verifyPorts(settings);
					fail("should not be here");
				}
				catch (IllegalArgumentException ignore) {
				}
				socket.close();
			}
		}
		finally {
			sockets.forEach(IOUtils::closeQuietly);
		}
		assertThat(TransportUtils.isDisabled(settings)).describedAs("Transport is not disabled").isTrue();

		TransportUtils.verifyPorts(settings);
	}


	private static final class TestSettings implements Settings {

		private final int port;

		private final Integer sslPort;

		private final int rpcPort;

		private final int storagePort;

		private final int sslStoragePort;


		private TestSettings(int storagePort, int sslStoragePort, int port, Integer sslPort, int rpcPort) {
			this.port = port;
			this.sslPort = sslPort;
			this.rpcPort = rpcPort;
			this.storagePort = storagePort;
			this.sslStoragePort = sslStoragePort;
		}

		@Nonnull
		@Override
		public String getClusterName() {
			return null;
		}

		@Override
		public int getStoragePort() {
			return this.storagePort;
		}

		@Override
		public int getSslStoragePort() {
			return this.sslStoragePort;
		}

		@Nullable
		@Override
		public String getListenAddress() {
			return null;
		}

		@Nullable
		@Override
		public String getListenInterface() {
			return null;
		}

		@Nullable
		@Override
		public String getBroadcastAddress() {
			return null;
		}

		@Nullable
		@Override
		public String getRpcAddress() {
			return null;
		}

		@Nullable
		@Override
		public String getRpcInterface() {
			return null;
		}

		@Nullable
		@Override
		public String getBroadcastRpcAddress() {
			return null;
		}

		@Override
		public boolean isStartNativeTransport() {
			return true;
		}

		@Override
		public int getPort() {
			return this.port;
		}

		@Nullable
		@Override
		public Integer getSslPort() {
			return this.sslPort;
		}

		@Override
		public boolean isStartRpc() {
			return true;
		}

		@Override
		public int getRpcPort() {
			return this.rpcPort;
		}

		@Nonnull
		@Override
		public Version getVersion() {
			return null;
		}

		@Override
		public boolean isListenOnBroadcastAddress() {
			return false;
		}

		@Override
		public boolean isListenInterfacePreferIpv6() {
			return false;
		}

		@Override
		public boolean isRpcInterfacePreferIpv6() {
			return false;
		}
	}
}