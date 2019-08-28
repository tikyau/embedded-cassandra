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
import java.util.concurrent.TimeUnit;

import com.github.nosan.embedded.cassandra.commons.ProcessId;

/**
 * Abstract {@link NodeProcess} that pre-implements common methods.
 *
 * @author Dmytro Nosan
 */
abstract class AbstractNodeProcess implements NodeProcess {

	private final ProcessId processId;

	AbstractNodeProcess(ProcessId processId) {
		this.processId = processId;
	}

	@Override
	public Process getProcess() {
		return this.processId.getProcess();
	}

	@Override
	public long getPid() {
		return this.processId.getPid();
	}

	@Override
	public boolean isAlive() {
		return getProcess().isAlive();
	}

	@Override
	public String toString() {
		return String.format("%s:%s", getClass().getSimpleName(), getPid());
	}

	@Override
	public final void stop() throws IOException, InterruptedException {
		ProcessId processId = this.processId;
		Process process = processId.getProcess();
		if (process.isAlive()) {
			doStop();
			if (!process.waitFor(5, TimeUnit.SECONDS)) {
				//retry to stop
				doStop();
				if (!process.waitFor(5, TimeUnit.SECONDS)) {
					process.destroy();
				}
			}
			if (process.isAlive()) {
				throw new IOException(String.format("'%s' has not been stopped and still running", toString()));
			}
			//closes I/O streams.
			process.destroy();
		}
	}

	/**
	 * Stops {@code Cassandra's} node.
	 *
	 * @throws IOException if  {@code Cassandra's} node cannot be stopped
	 * @throws InterruptedException if  {@code Cassandra's} node has been interrupted.
	 */
	abstract void doStop() throws IOException, InterruptedException;

}