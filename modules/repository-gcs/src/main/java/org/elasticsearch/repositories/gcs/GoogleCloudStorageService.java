/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.repositories.gcs;

import com.google.api.client.googleapis.GoogleUtils;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.SecurityUtils;
import com.google.api.gax.retrying.ResultRetryAlgorithm;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.ServiceOptions;
import com.google.cloud.http.HttpTransportOptions;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.StorageRetryStrategy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.util.Maps;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.core.SuppressForbidden;
import org.elasticsearch.core.TimeValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.elasticsearch.core.Strings.format;

public class GoogleCloudStorageService {

    private static final Logger logger = LogManager.getLogger(GoogleCloudStorageService.class);

    private volatile Map<String, GoogleCloudStorageClientSettings> clientSettings = emptyMap();

    private final boolean isServerless;

    public GoogleCloudStorageService() {
        this.isServerless = false;
    }

    public GoogleCloudStorageService(boolean isServerless) {
        this.isServerless = isServerless;
    }

    public boolean isServerless() {
        return isServerless;
    }

    /**
     * Dictionary of client instances. Client instances are built lazily from the
     * latest settings. Clients are cached by a composite repositoryName key.
     */
    private volatile Map<String, MeteredStorage> clientCache = emptyMap();

    /**
     * Refreshes the client settings and clears the client cache. Subsequent calls to
     * {@code GoogleCloudStorageService#client} will return new clients constructed
     * using the parameter settings.
     *
     * @param clientsSettings the new settings used for building clients for subsequent requests
     */
    public synchronized void refreshAndClearCache(Map<String, GoogleCloudStorageClientSettings> clientsSettings) {
        this.clientCache = emptyMap();
        this.clientSettings = Maps.ofEntries(clientsSettings.entrySet());
    }

    /**
     * Attempts to retrieve a client from the cache. If the client does not exist it
     * will be created from the latest settings and will populate the cache. The
     * returned instance should not be cached by the calling code. Instead, for each
     * use, the (possibly updated) instance should be requested by calling this
     * method.
     *
     * @param clientName name of the client settings used to create the client
     * @param repositoryName name of the repository that would use the client
     * @return a cached client storage instance that can be used to manage objects
     *         (blobs)
     */
    public MeteredStorage client(final String clientName, final String repositoryName, final GcsRepositoryStatsCollector statsCollector)
        throws IOException {
        {
            final MeteredStorage storage = clientCache.get(repositoryName);
            if (storage != null) {
                return storage;
            }
        }
        synchronized (this) {
            final MeteredStorage existing = clientCache.get(repositoryName);

            if (existing != null) {
                return existing;
            }

            final GoogleCloudStorageClientSettings settings = clientSettings.get(clientName);

            if (settings == null) {
                throw new IllegalArgumentException(
                    "Unknown client name ["
                        + clientName
                        + "]. Existing client configs: "
                        + Strings.collectionToDelimitedString(clientSettings.keySet(), ",")
                );
            }

            logger.debug(() -> format("creating GCS client with client_name [%s], endpoint [%s]", clientName, settings.getHost()));
            final MeteredStorage storage = createClient(settings, statsCollector);
            clientCache = Maps.copyMapWithAddedEntry(clientCache, repositoryName, storage);
            return storage;
        }
    }

    synchronized void closeRepositoryClients(String repositoryName) {
        clientCache = clientCache.entrySet()
            .stream()
            .filter(entry -> entry.getKey().equals(repositoryName) == false)
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Creates a client that can be used to manage Google Cloud Storage objects. The client is thread-safe.
     *
     * @param gcsClientSettings client settings to use, including secure settings
     * @return a new client storage instance that can be used to manage objects
     *         (blobs)
     */
    private MeteredStorage createClient(GoogleCloudStorageClientSettings gcsClientSettings, GcsRepositoryStatsCollector statsCollector)
        throws IOException {

        final NetHttpTransport.Builder builder = new NetHttpTransport.Builder();
        // requires java.lang.RuntimePermission "setFactory"
        // Pin the TLS trust certificates.
        // We manually load the key store from jks instead of using GoogleUtils.getCertificateTrustStore() because that uses a .p12
        // store format not compatible with FIPS mode.
        final HttpTransport httpTransport;
        try {
            final KeyStore certTrustStore = SecurityUtils.getJavaKeyStore();
            try (InputStream keyStoreStream = GoogleUtils.class.getResourceAsStream("google.jks")) {
                SecurityUtils.loadKeyStore(certTrustStore, keyStoreStream, "notasecret");
            }
            builder.trustCertificates(certTrustStore);
            Proxy proxy = gcsClientSettings.getProxy();
            if (proxy != null) {
                builder.setProxy(proxy);
                notifyProxyIsSet(proxy);
            }
            httpTransport = builder.build();
        } catch (RuntimeException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final HttpTransportOptions httpTransportOptions = new HttpTransportOptions(
            HttpTransportOptions.newBuilder()
                .setConnectTimeout(toTimeout(gcsClientSettings.getConnectTimeout()))
                .setReadTimeout(toTimeout(gcsClientSettings.getReadTimeout()))
                .setHttpTransportFactory(() -> httpTransport)
        ) {

            @Override
            public HttpRequestInitializer getHttpRequestInitializer(ServiceOptions<?, ?> serviceOptions) {
                HttpRequestInitializer requestInitializer = super.getHttpRequestInitializer(serviceOptions);

                return (httpRequest) -> {
                    if (requestInitializer != null) requestInitializer.initialize(httpRequest);
                    httpRequest.setResponseInterceptor(GcsRepositoryStatsCollector.METERING_INTERCEPTOR);
                };
            }
        };

        final StorageOptions storageOptions = createStorageOptions(gcsClientSettings, httpTransportOptions);
        return new MeteredStorage(storageOptions.getService(), statsCollector);
    }

    StorageOptions createStorageOptions(
        final GoogleCloudStorageClientSettings gcsClientSettings,
        final HttpTransportOptions httpTransportOptions
    ) {
        final StorageOptions.Builder storageOptionsBuilder = StorageOptions.newBuilder()
            .setStorageRetryStrategy(getRetryStrategy())
            .setTransportOptions(httpTransportOptions)
            .setHeaderProvider(() -> {
                return Strings.hasLength(gcsClientSettings.getApplicationName())
                    ? Map.of("user-agent", gcsClientSettings.getApplicationName())
                    : Map.of();
            });
        if (Strings.hasLength(gcsClientSettings.getHost())) {
            storageOptionsBuilder.setHost(gcsClientSettings.getHost());
        }
        if (Strings.hasLength(gcsClientSettings.getProjectId())) {
            storageOptionsBuilder.setProjectId(gcsClientSettings.getProjectId());
        } else {
            String defaultProjectId = null;
            try {
                defaultProjectId = ServiceOptions.getDefaultProjectId();
                if (defaultProjectId != null) {
                    storageOptionsBuilder.setProjectId(defaultProjectId);
                }
            } catch (Exception e) {
                logger.warn("failed to load default project id", e);
            }
            if (defaultProjectId == null) {
                try {
                    // fallback to manually load project ID here as the above ServiceOptions method has the metadata endpoint hardcoded,
                    // which makes it impossible to test
                    final String projectId = getDefaultProjectId(gcsClientSettings.getProxy());
                    if (projectId != null) {
                        storageOptionsBuilder.setProjectId(projectId);
                    }
                } catch (Exception e) {
                    logger.warn("failed to load default project id fallback", e);
                }
            }
        }
        if (gcsClientSettings.getCredential() == null) {
            try {
                storageOptionsBuilder.setCredentials(GoogleCredentials.getApplicationDefault());
            } catch (Exception e) {
                logger.warn("failed to load Application Default Credentials", e);
            }
        } else {
            ServiceAccountCredentials serviceAccountCredentials = gcsClientSettings.getCredential();
            // override token server URI
            final URI tokenServerUri = gcsClientSettings.getTokenUri();
            if (Strings.hasLength(tokenServerUri.toString())) {
                // Rebuild the service account credentials in order to use a custom Token url.
                // This is mostly used for testing purpose.
                serviceAccountCredentials = serviceAccountCredentials.toBuilder().setTokenServerUri(tokenServerUri).build();
            }
            storageOptionsBuilder.setCredentials(serviceAccountCredentials);
        }
        return storageOptionsBuilder.build();
    }

    protected StorageRetryStrategy getRetryStrategy() {
        return ShouldRetryDecorator.decorate(
            StorageRetryStrategy.getLegacyStorageRetryStrategy(),
            (Throwable prevThrowable, Object prevResponse, ResultRetryAlgorithm<Object> delegate) -> {
                // Retry in the event of an unknown host exception
                if (ExceptionsHelper.unwrap(prevThrowable, UnknownHostException.class) != null) {
                    return true;
                }
                // Also retry on `SocketException`s
                if (ExceptionsHelper.unwrap(prevThrowable, SocketException.class) != null) {
                    return true;
                }
                return delegate.shouldRetry(prevThrowable, prevResponse);
            }
        );
    }

    /**
     * This method imitates what MetadataConfig.getProjectId() does, but does not have the endpoint hardcoded.
     */
    @SuppressForbidden(reason = "ok to open connection here")
    static String getDefaultProjectId(@Nullable Proxy proxy) throws IOException {
        String metaHost = System.getenv("GCE_METADATA_HOST");
        if (metaHost == null) {
            metaHost = "metadata.google.internal";
        }
        URL url = new URL("http://" + metaHost + "/computeMetadata/v1/project/project-id");
        HttpURLConnection connection = (HttpURLConnection) (proxy != null ? url.openConnection(proxy) : url.openConnection());
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("Metadata-Flavor", "Google");
        try (InputStream input = connection.getInputStream()) {
            if (connection.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, UTF_8))) {
                    return reader.readLine();
                }
            }
        }
        return null;
    }

    /**
     * Converts timeout values from the settings to a timeout value for the Google
     * Cloud SDK
     **/
    static Integer toTimeout(final TimeValue timeout) {
        // Null or zero in settings means the default timeout
        if (timeout == null || TimeValue.ZERO.equals(timeout)) {
            // negative value means using the default value
            return -1;
        }
        // -1 means infinite timeout
        if (TimeValue.MINUS_ONE.equals(timeout)) {
            // 0 is the infinite timeout expected by Google Cloud SDK
            return 0;
        }
        return Math.toIntExact(timeout.getMillis());
    }

    // used for unit testing
    void notifyProxyIsSet(Proxy proxy) {}
}
