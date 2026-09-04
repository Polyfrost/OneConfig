package org.polyfrost.oneconfig.internal.legacy.ornithe;

//? if = 1.8.9 {
/*import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import net.ornithemc.osl.core.api.util.function.IOSupplier;

/^* Copied from OSL commit adbaa78fadd5092c13db1ec52e48d79f1f9a5ec7's LazyResource. ^/
public final class DeferredResource {
	private DeferredResource() {
	}

	public static IOSupplier<InputStream> inputStreamSupplier(Path path) {
		return () -> fileInputStream(path);
	}

	public static InputStream fileInputStream(Path path) throws IOException {
		if (DeferredNioExecutionHandler.shouldDefer()) {
			return DeferredNioExecutionHandler.submit(() -> new DeferredInputStream(Files.newInputStream(path)));
		} else {
			return Files.newInputStream(path);
		}
	}
}
*///?}
