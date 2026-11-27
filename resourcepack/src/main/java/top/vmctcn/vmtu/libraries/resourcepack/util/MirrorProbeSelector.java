package top.vmctcn.vmtu.libraries.resourcepack.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

final class MirrorProbeSelector {
    private MirrorProbeSelector() {}

    // Owns the supplied executor and shuts it down on every exit path.
    static String firstSuccessful(List<Callable<String>> probes, ExecutorService executor,
                                  long timeout, TimeUnit unit) {
        List<Future<String>> futures = new ArrayList<>();
        CompletionService<String> completed = new ExecutorCompletionService<>(executor);
        long start = System.nanoTime();
        long budget = unit.toNanos(timeout);
        try {
            for (Callable<String> probe : probes) {
                futures.add(completed.submit(probe));
            }
            for (int remaining = futures.size(); remaining > 0; remaining--) {
                long nanos = budget - (System.nanoTime() - start);
                if (nanos <= 0) return null;
                Future<String> result = completed.poll(nanos, TimeUnit.NANOSECONDS);
                if (result == null) return null;
                try {
                    String url = result.get();
                    if (url != null) return url;
                } catch (ExecutionException | CancellationException ignored) {
                    // One failed probe must not discard another successful result.
                }
            }
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            for (Future<String> future : futures) {
                if (!future.isDone()) future.cancel(true);
            }
            executor.shutdownNow();
        }
    }
}
