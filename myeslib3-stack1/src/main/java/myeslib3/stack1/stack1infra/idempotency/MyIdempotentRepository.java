/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package myeslib3.stack1.stack1infra.idempotency;

import org.apache.camel.api.management.ManagedOperation;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.com.github.benmanes.caffeine.cache.LoadingCache;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.ServiceSupport;

/**
 * A database based implementation of {@link IdempotentRepository}.
 */
@ManagedResource(description = "Memory based idempotent repository")
public class MyIdempotentRepository extends ServiceSupport implements IdempotentRepository<String> {

	private final LoadingCache<String, String> cache;

	public MyIdempotentRepository(LoadingCache<String, String> cache) {
		this.cache = cache;
	}

	@ManagedOperation(description = "Adds the key to the store")
	public boolean add(String key) {
		synchronized (cache) {
			if (cache.get(key) != null) {
				return false;
			} else {
				cache.put(key, key);
				return cache.get(key) != null;
			}
		}
	}

	@ManagedOperation(description = "Does the store contain the given key")
	public boolean contains(String key) {
		synchronized (cache) {
			return cache.get(key) != null;
		}
	}

	@ManagedOperation(description = "Remove the key serialize the store")
	public boolean remove(String key) {
		synchronized (cache) {
			cache.invalidate(key);
			return true;
		}
	}

	public boolean confirm(String key) {
		// noop
		return true;
	}

	@ManagedOperation(description = "Clear the store")
	public void clear() {
//        synchronized (cache) {
//            cache.invalidateAll();
//        }
	}

	@Override
	protected void doStart() throws Exception {
	}

	@Override
	protected void doStop() throws Exception {
	}
}
