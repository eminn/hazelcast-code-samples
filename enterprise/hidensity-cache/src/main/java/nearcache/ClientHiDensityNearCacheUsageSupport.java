package nearcache;

import com.hazelcast.cache.ICache;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.cache.impl.HazelcastClientCacheManager;
import com.hazelcast.client.cache.impl.HazelcastClientCachingProvider;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.impl.HazelcastClientProxy;
import com.hazelcast.config.CacheConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionConfig;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.NativeMemoryConfig;
import com.hazelcast.config.NearCacheConfig;
import com.hazelcast.examples.nearcache.ClientNearCacheUsageSupport;
import com.hazelcast.instance.GroupProperties;
import com.hazelcast.memory.MemoryManager;
import com.hazelcast.memory.MemorySize;
import com.hazelcast.memory.MemoryUnit;
import com.hazelcast.nio.serialization.EnterpriseSerializationService;

import javax.cache.spi.CachingProvider;

public abstract class ClientHiDensityNearCacheUsageSupport extends ClientNearCacheUsageSupport {

    // Pass your license key as system property like
    // "-Dhazelcast.enterprise.license.key=<YOUR_LICENCE_KEY_HERE>"
    protected static final String LICENSE_KEY = System.getProperty(GroupProperties.PROP_ENTERPRISE_LICENSE_KEY);

    protected static final MemorySize SERVER_NATIVE_MEMORY_SIZE = new MemorySize(256, MemoryUnit.MEGABYTES);
    protected static final MemorySize CLIENT_NATIVE_MEMORY_SIZE = new MemorySize(128, MemoryUnit.MEGABYTES);

    public ClientHiDensityNearCacheUsageSupport() {
        super(InMemoryFormat.NATIVE);
    }

    protected Config createConfig() {
        Config config = super.createConfig();
        NativeMemoryConfig nativeMemoryConfig =
                new NativeMemoryConfig()
                        .setSize(SERVER_NATIVE_MEMORY_SIZE)
                        .setEnabled(true);
        config.setNativeMemoryConfig(nativeMemoryConfig);
        config.setLicenseKey(LICENSE_KEY);
        return config;
    }

    protected ClientConfig createClientConfig() {
        ClientConfig clientConfig = super.createClientConfig();
        NativeMemoryConfig nativeMemoryConfig =
                new NativeMemoryConfig()
                        .setSize(CLIENT_NATIVE_MEMORY_SIZE)
                        .setEnabled(true);
        clientConfig.setNativeMemoryConfig(nativeMemoryConfig);
        clientConfig.setLicenseKey(LICENSE_KEY);
        return clientConfig;
    }

    @Override
    protected CacheConfig createCacheConfig(String cacheName, InMemoryFormat inMemoryFormat) {
        inMemoryFormat = InMemoryFormat.NATIVE;
        CacheConfig cacheConfig = super.createCacheConfig(cacheName, inMemoryFormat);
        if (inMemoryFormat == InMemoryFormat.NATIVE) {
            EvictionConfig evictionConfig = new EvictionConfig();
            evictionConfig.setMaximumSizePolicy(EvictionConfig.MaxSizePolicy.USED_NATIVE_MEMORY_PERCENTAGE);
            evictionConfig.setSize(99);
            cacheConfig.setEvictionConfig(evictionConfig);
        }
        return cacheConfig;
    }

    @Override
    protected NearCacheConfig createNearCacheConfig(String cacheName, InMemoryFormat inMemoryFormat) {
        NearCacheConfig nearCacheConfig = super.createNearCacheConfig(cacheName, inMemoryFormat);
        EvictionConfig evictionConfig = new EvictionConfig();
        if (inMemoryFormat == InMemoryFormat.NATIVE) {
            evictionConfig.setMaximumSizePolicy(EvictionConfig.MaxSizePolicy.USED_NATIVE_MEMORY_PERCENTAGE);
            evictionConfig.setSize(99);
            nearCacheConfig.setEvictionConfig(evictionConfig);
        }
        return nearCacheConfig;
    }

    public class HiDensityNearCacheSupportContext<K, V> {

        public final ICache<K, V> cache;
        public final MemoryManager memoryManager;

        HiDensityNearCacheSupportContext(ICache<K, V> cache, MemoryManager memoryManager) {
            this.cache = cache;
            this.memoryManager = memoryManager;
        }

    }

    protected <K, V> HiDensityNearCacheSupportContext<K, V> createHiDensityCacheWithHiDensityNearCache() {
        return createHiDensityCacheWithHiDensityNearCache(DEFAULT_CACHE_NAME, createNearCacheConfig());
    }

    protected <K, V> HiDensityNearCacheSupportContext<K, V> createHiDensityCacheWithHiDensityNearCache(String cacheName) {
        return createHiDensityCacheWithHiDensityNearCache(cacheName, createNearCacheConfig(cacheName));
    }

    protected <K, V> HiDensityNearCacheSupportContext<K, V> createHiDensityCacheWithHiDensityNearCache(
            InMemoryFormat inMemoryFormat) {
        return createHiDensityCacheWithHiDensityNearCache(DEFAULT_CACHE_NAME, createNearCacheConfig(inMemoryFormat));
    }

    protected <K, V> HiDensityNearCacheSupportContext<K, V> createHiDensityCacheWithHiDensityNearCache(String cacheName,
            NearCacheConfig nearCacheConfig) {
        ClientConfig clientConfig = createClientConfig();
        clientConfig.addNearCacheConfig(nearCacheConfig);
        HazelcastClientProxy client = (HazelcastClientProxy) HazelcastClient.newHazelcastClient(clientConfig);
        CachingProvider provider = HazelcastClientCachingProvider.createCachingProvider(client);
        HazelcastClientCacheManager cacheManager = (HazelcastClientCacheManager) provider.getCacheManager();

        CacheConfig<K, V> cacheConfig = createCacheConfig(nearCacheConfig.getInMemoryFormat());
        ICache<K, V> cache = cacheManager.createCache(cacheName, cacheConfig);

        clients.add(client);

        EnterpriseSerializationService enterpriseSerializationService =
                (EnterpriseSerializationService) client.getSerializationService();

        return new HiDensityNearCacheSupportContext(cache, enterpriseSerializationService.getMemoryManager());
    }

}
