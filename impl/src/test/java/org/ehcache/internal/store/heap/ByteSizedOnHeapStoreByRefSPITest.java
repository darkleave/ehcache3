/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehcache.internal.store.heap;

import org.ehcache.config.EvictionVeto;
import org.ehcache.config.ResourcePools;
import org.ehcache.config.StoreConfigurationImpl;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.Expirations;
import org.ehcache.expiry.Expiry;
import org.ehcache.internal.SystemTimeSource;
import org.ehcache.internal.TimeSource;
import org.ehcache.internal.copy.IdentityCopier;
import org.ehcache.internal.sizeof.DefaultSizeOfEngine;
import org.ehcache.internal.store.StoreFactory;
import org.ehcache.internal.store.StoreSPITest;
import org.ehcache.internal.store.heap.OnHeapStore;
import org.ehcache.internal.store.heap.holders.CopiedOnHeapValueHolder;
import org.ehcache.spi.ServiceLocator;
import org.ehcache.spi.cache.Store;
import org.ehcache.spi.copy.Copier;
import org.ehcache.spi.service.ServiceConfiguration;
import org.junit.Before;

import static org.ehcache.config.ResourcePoolsBuilder.newResourcePoolsBuilder;

public class ByteSizedOnHeapStoreByRefSPITest extends StoreSPITest<String, String> {

  private StoreFactory<String, String> storeFactory;
  private static final int MAGIC_NUM = 500;

  @Override
  protected StoreFactory<String, String> getStoreFactory() {
    return storeFactory;
  }

  @Before
  public void setUp() {
    storeFactory = new StoreFactory<String, String>() {

      final Copier DEFAULT_COPIER = new IdentityCopier();

      @Override
      public Store<String, String> newStore() {
        return newStore(null, null, Expirations.noExpiration(), SystemTimeSource.INSTANCE);
      }

      @Override
      public Store<String, String> newStoreWithCapacity(long capacity) {
        return newStore(capacity, null, Expirations.noExpiration(), SystemTimeSource.INSTANCE);
      }

      @Override
      public Store<String, String> newStoreWithExpiry(Expiry<String, String> expiry, TimeSource timeSource) {
        return newStore(null, null, expiry, timeSource);
      }

      @Override
      public Store<String, String> newStoreWithEvictionVeto(EvictionVeto<String, String> evictionVeto) {
        return newStore(null, evictionVeto, Expirations.noExpiration(), SystemTimeSource.INSTANCE);
      }

      private Store<String, String> newStore(Long capacity, EvictionVeto<String, String> evictionVeto, Expiry<? super String, ? super String> expiry, TimeSource timeSource) {
        ResourcePools resourcePools = buildResourcePools(capacity);
        Store.Configuration<String, String> config = new StoreConfigurationImpl<String, String>(getKeyType(), getValueType(), evictionVeto, getClass().getClassLoader(), expiry, resourcePools, null, null);
        return new OnHeapStore<String, String>(config, timeSource, DEFAULT_COPIER, DEFAULT_COPIER, new DefaultSizeOfEngine(Long.MAX_VALUE, Long.MAX_VALUE));
      }

      @Override
      public Store.ValueHolder<String> newValueHolder(final String value) {
        return new CopiedOnHeapValueHolder<String>(value, SystemTimeSource.INSTANCE.getTimeMillis(), false, DEFAULT_COPIER);
      }

      private ResourcePools buildResourcePools(Comparable<Long> capacityConstraint) {
        if (capacityConstraint == null) {
          return newResourcePoolsBuilder().heap(10, MemoryUnit.KB).build();
        } else {
          return newResourcePoolsBuilder().heap((Long)capacityConstraint * MAGIC_NUM, MemoryUnit.B).build();
        }
      }

      @Override
      public Class<String> getKeyType() {
        return String.class;
      }

      @Override
      public Class<String> getValueType() {
        return String.class;
      }

      @Override
      public ServiceConfiguration<?>[] getServiceConfigurations() {
        return new ServiceConfiguration[0];
      }

      @Override
      public String createKey(long seed) {
        return new String("" + seed);
      }

      @Override
      public String createValue(long seed) {
        return new String("" + seed);
      }

      @Override
      public void close(final Store<String, String> store) {
        OnHeapStore.Provider.close((OnHeapStore)store);
      }

      @Override
      public ServiceLocator getServiceProvider() {
        ServiceLocator locator = new ServiceLocator();
        try {
          locator.startAllServices();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        return locator;
      }
    };
  }

}
