/*
 * Copyright 2018-2019 the original author or authors.
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

package com.github.nosan.embedded.cassandra.local.artifact;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.nosan.embedded.cassandra.Version;
import com.github.nosan.embedded.cassandra.util.FileUtils;
import com.github.nosan.embedded.cassandra.util.MDCUtils;
import com.github.nosan.embedded.cassandra.util.StringUtils;
import com.github.nosan.embedded.cassandra.util.ThreadNameSupplier;

/**
 * {@link Artifact} implementation, that downloads {@code archive} from the internet, if {@code archive} doesn't exist
 * locally.
 *
 * @author Dmytro Nosan
 * @see RemoteArtifactFactory
 * @since 1.0.0
 */
class RemoteArtifact implements Artifact {

	private static final Logger log = LoggerFactory.getLogger(Artifact.class);

	@Nonnull
	private final Version version;

	@Nonnull
	private final Path directory;

	@Nonnull
	private final UrlFactory urlFactory;

	@Nullable
	private final Proxy proxy;

	@Nullable
	private final Duration readTimeout;

	@Nullable
	private final Duration connectTimeout;

	/**
	 * Creates a {@link RemoteArtifact}.
	 *
	 * @param version a version
	 * @param directory a directory to store/search an artifact (directory must be writable)
	 * @param urlFactory factory to create {@code URL}
	 * @param proxy proxy for {@code connection}
	 * @param connectTimeout connect timeout for {@code connection}
	 * @param readTimeout read timeout for {@code connection}
	 */
	RemoteArtifact(@Nonnull Version version, @Nonnull Path directory,
			@Nonnull UrlFactory urlFactory, @Nullable Proxy proxy,
			@Nullable Duration readTimeout, @Nullable Duration connectTimeout) {
		this.version = Objects.requireNonNull(version, "Version must not be null");
		this.directory = Objects.requireNonNull(directory, "Directory must not be null");
		this.urlFactory = Objects.requireNonNull(urlFactory, "URL Factory must not be null");
		this.proxy = proxy;
		this.readTimeout = readTimeout;
		this.connectTimeout = connectTimeout;
	}

	@Override
	@Nonnull
	public Path get() throws IOException {
		Version version = this.version;
		Proxy proxy = this.proxy;
		Duration readTimeout = this.readTimeout;
		Duration connectTimeout = this.connectTimeout;
		Path directory = this.directory;
		URL[] urls = this.urlFactory.create(version);
		Objects.requireNonNull(urls, "URLs must not be null");

		IOException exceptions = new IOException(String.format("Could not download a resource from URLs %s",
				Arrays.toString(urls)));

		for (URL url : urls) {
			try {
				Resource remoteResource = new RemoteResource(version, url, proxy, readTimeout, connectTimeout);
				Resource localResource = new LocalResource(directory, remoteResource);
				return localResource.getFile();
			}
			catch (Exception ex) {
				exceptions.addSuppressed(ex);
			}
		}
		throw exceptions;
	}

	/**
	 * Resource that abstracts from the actual type of underlying source.
	 */
	private interface Resource {

		/**
		 * Return a File handle for this resource.
		 *
		 * @return the file
		 * @throws IOException in case of any I/O errors
		 */
		@Nonnull
		Path getFile() throws IOException;

		/**
		 * Return a name for this resource.
		 *
		 * @return a resource name
		 */
		@Nonnull
		String getName();
	}

	/**
	 * Local {@link Resource} implementation.
	 */
	private static final class LocalResource implements Resource {

		@Nonnull
		private final Path directory;

		@Nonnull
		private final Resource resource;

		/**
		 * Creates a {@link LocalResource}.
		 *
		 * @param directory a directory to store/search an artifact (directory must be writable)
		 * @param resource a delegated resource
		 */
		LocalResource(@Nonnull Path directory, @Nonnull Resource resource) {
			this.directory = directory;
			this.resource = resource;
		}

		@Nonnull
		@Override
		public Path getFile() throws IOException {
			Path file = this.directory.resolve(getName());
			if (Files.exists(file)) {
				return file;
			}
			Path tempFile = this.resource.getFile();
			try {
				Files.createDirectories(file.getParent());
				return Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (IOException ex) {
				if (log.isDebugEnabled()) {
					log.error(String.format("Could not rename (%s) as (%s).", tempFile, file));
				}
				return file;
			}
		}

		@Nonnull
		@Override
		public String getName() {
			return this.resource.getName();
		}

	}

	/**
	 * Remote {@link Resource} implementation.
	 */
	private static final class RemoteResource implements Resource {

		private static final AtomicLong instanceCounter = new AtomicLong();

		@Nonnull
		private final ThreadNameSupplier threadNameSupplier = new ThreadNameSupplier(String.format("artifact-%d",
				instanceCounter.incrementAndGet()));

		@Nonnull
		private final ThreadFactory threadFactory = runnable -> {
			Thread thread = new Thread(runnable, this.threadNameSupplier.get());
			thread.setDaemon(true);
			return thread;
		};

		@Nonnull
		private final Version version;

		@Nonnull
		private final URL url;

		@Nullable
		private final Proxy proxy;

		@Nullable
		private final Duration readTimeout;

		@Nullable
		private final Duration connectTimeout;

		/**
		 * Creates a {@link RemoteResource}.
		 *
		 * @param version a version
		 * @param url URL to download a file
		 * @param proxy proxy for {@code connection}
		 * @param connectTimeout connect timeout for {@code connection}
		 * @param readTimeout read timeout for {@code connection}
		 */
		RemoteResource(@Nonnull Version version, @Nonnull URL url, @Nullable Proxy proxy,
				@Nullable Duration readTimeout,
				@Nullable Duration connectTimeout) {
			this.version = version;
			this.url = url;
			this.proxy = proxy;
			this.readTimeout = readTimeout;
			this.connectTimeout = connectTimeout;
		}

		@Nonnull
		@Override
		public Path getFile() throws IOException {
			URLConnection urlConnection = getUrlConnection(this.url, this.proxy, this.connectTimeout, this.readTimeout);
			long size = urlConnection.getContentLengthLong();

			ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(this.threadFactory);

			Path file = FileUtils.getTmpDirectory()
					.resolve(UUID.randomUUID().toString()).resolve(getFileName(this.url));
			file.toFile().deleteOnExit();

			try (InputStream inputStream = urlConnection.getInputStream()) {
				log.info("Downloading Apache Cassandra ({}) from ({}).", this.version, urlConnection.getURL());
				long start = System.currentTimeMillis();
				Files.createDirectories(file.getParent());
				Map<String, String> context = MDCUtils.getContext();
				executorService.scheduleAtFixedRate(() -> {
					MDCUtils.setContext(context);
					progress(file, size);
				}, 50, 3000, TimeUnit.MILLISECONDS);
				Files.copy(inputStream, file);
				long elapsed = System.currentTimeMillis() - start;
				log.info("Apache Cassandra ({}) has been downloaded ({} ms)", this.version, elapsed);
			}
			finally {
				executorService.shutdown();
			}
			return file;
		}

		@Nonnull
		@Override
		public String getName() {
			return getFileName(this.url);
		}

		private static String getFileName(URL url) {
			String name = url.getFile();
			if (StringUtils.hasText(name) && name.contains("/")) {
				name = name.substring(name.lastIndexOf("/") + 1);
			}
			if (!StringUtils.hasText(name)) {
				throw new IllegalArgumentException(
						String.format("There is no way to determine a file name from (%s)", url));
			}
			return name;
		}

		private static URLConnection getUrlConnection(URL url, Proxy proxy, Duration connectTimeout,
				Duration readTimeout) throws IOException {
			URLConnection urlConnection = (proxy != null) ? url.openConnection(proxy) : url.openConnection();
			if (urlConnection instanceof HttpURLConnection) {
				HttpURLConnection connection = (HttpURLConnection) urlConnection;
				if (connectTimeout != null) {
					connection.setConnectTimeout(Math.toIntExact(connectTimeout.toMillis()));
				}
				if (readTimeout != null) {
					connection.setReadTimeout(Math.toIntExact(readTimeout.toMillis()));
				}
				int status = connection.getResponseCode();
				if (status >= 200 && status < 300) {
					return connection;
				}
				else if (status >= 300 && status < 400) {
					String location = connection.getHeaderField("Location");
					if (StringUtils.hasText(location)) {
						return getUrlConnection(new URL(url, location), proxy, connectTimeout, readTimeout);
					}
				}
				else if (status >= 400) {
					throw new IOException(String.format("HTTP (%d %s) status for URL (%s) is invalid", status,
							connection.getResponseMessage(), url));
				}
			}
			return urlConnection;
		}

		private static void progress(Path file, long size) {
			if (size > 0 && Files.exists(file)) {
				try {
					long current = Files.size(file);
					log.info("Downloaded {} / {}  {}%", current, size, (current * 100) / size);
				}
				catch (IOException ignore) {
				}
			}
		}
	}

}
