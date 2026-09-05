/*
 * Original file can be found at
 * https://github.com/FabricMC/fabric-api/blob/1.14/fabric-resource-loader-v0/src/main/java/net/fabricmc/fabric/impl/resource/loader/DeferredInputStream.java
 * 
 * Modifications by OrnitheMC:
 * - remove unused field and methods
 * - update formatting to match the rest of the project
 */
/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
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

package org.polyfrost.oneconfig.internal.legacy.ornithe;

//? if = 1.8.9 {
/*import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

class DeferredNioExecutionHandler {

	private static final ThreadLocal<Boolean> DEFERRED_REQUIRED = new ThreadLocal<>();
	private static ExecutorService EXECUTOR_SERVICE;

	public static boolean shouldDefer() {
		Boolean deferRequired = DEFERRED_REQUIRED.get();

		if (deferRequired == null) {
			deferRequired = false;

			StackTraceElement[] elements = Thread.currentThread().getStackTrace();

			for (StackTraceElement element : elements) {
				if (element.getClassName().startsWith("paulscode.sound.")) {
					deferRequired = true;
					break;
				}
			}

			DEFERRED_REQUIRED.set(deferRequired);
		}

		return deferRequired;
	}

	public static <V> V submit(Callable<V> callable) throws IOException {
		if (EXECUTOR_SERVICE == null) {
			EXECUTOR_SERVICE = Executors.newSingleThreadExecutor(
				new ThreadFactoryBuilder()
					.setNameFormat("OSL Deferred I/O Thread")
					.build()
			);
		}

		Future<V> future = EXECUTOR_SERVICE.submit(callable);
		return getSubmittedFuture(future);
	}

	private static <V> V getSubmittedFuture(Future<V> future) throws IOException {
		while (true) {
			try {
				return future.get();
			} catch (ExecutionException e) {
				Throwable t = e.getCause();

				if (t instanceof IOException) {
					throw (IOException) t;
				} else {
					throw new RuntimeException("ExecutionException which should not happen!", t);
				}
			} catch (InterruptedException e) {
				// keep calm, carry on...
			}
		}
	}
}
*///?}
