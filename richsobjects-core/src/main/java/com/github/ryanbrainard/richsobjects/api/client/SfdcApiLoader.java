package com.github.ryanbrainard.richsobjects.api.client;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author Ryan Brainard
 */
public class SfdcApiLoader {

    private SfdcApiLoader() {}

    private static final ServiceLoader<SfdcApiSessionProvider> sessionLoader = loadProvider(SfdcApiSessionProvider.class);
    private static final ServiceLoader<SfdcApiClientProvider> clientLoader = loadProvider(SfdcApiClientProvider.class);
    private static final ServiceLoader<SfdcApiCacheProvider> cacheLoader = loadProvider(SfdcApiCacheProvider.class);
    
    public static SfdcApiClient get(double version) {
        final SfdcApiSessionProvider sessionProvider = getFirstOrThrow(sessionLoader);
        final SfdcApiClientProvider clientProvider = getFirstOrThrow(clientLoader);

        // create underlying client
        SfdcApiClient client = clientProvider.get(
                sessionProvider.getAccessToken(),
                sessionProvider.getApiEndpoint(),
                String.format("v%.1f", version)
        );

        // decorate client with caching layers
        final String cacheKey = sessionProvider.getAccessToken() + version;
        for (SfdcApiCacheProvider cacheProvider : cacheLoader) {
            client = cacheProvider.get(cacheKey, client);
        }

        return client;
    }

    private static <P> ServiceLoader<P> loadProvider(Class<P> providerClass) {
        return ServiceLoader.load(providerClass, SfdcApiLoader.class.getClassLoader());
    }

    private static <S> S getFirstOrThrow(ServiceLoader<S> loader) {
        final Iterator<S> providerIterator = loader.iterator();
        if (!providerIterator.hasNext()) {
            throw new IllegalStateException(
                    "Could not load service from " + loader +
                    "\nEnsure an entry in META-INF/services has been loaded on the classpath.");
        }
        return providerIterator.next();
    }
}