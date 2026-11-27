package top.vmctcn.vmtu.libraries.resourcepack.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public final class MirrorProbeSelectorTest {
    private static final String URL = "https://mirror.example/";

    public static void main(String[] args) throws Exception {
        // Inline execution guarantees BOTH futures are done before selection begins.
        check(URL.equals(select(new InlineExecutor(), () -> null, () -> URL)), "completed null + success");
        check(URL.equals(select(new InlineExecutor(), () -> { throw new IllegalStateException(); }, () -> URL)), "exception + success");
        check(select(new InlineExecutor(), () -> null, () -> null) == null, "all fail");
        check(select(new InlineExecutor()) == null, "empty probes");
        cancellation(false);
        cancellation(true);
        interruptedCaller();
        System.out.println("MirrorProbeSelector: 7 tests passed");
    }

    @SafeVarargs
    private static String select(ExecutorService executor, Callable<String>... probes) {
        return MirrorProbeSelector.firstSuccessful(Arrays.asList(probes), executor, 5, TimeUnit.SECONDS);
    }

    private static void cancellation(boolean timeout) throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        Callable<String> slow = () -> {
            started.countDown();
            try {
                new CountDownLatch(1).await();
                return null;
            } catch (InterruptedException e) {
                interrupted.countDown();
                throw e;
            }
        };
        // Ensure the slow task is actually running before selection starts its clock.
        ExecutorService gated = new AbstractExecutorService() {
            private boolean first = true;
            public void execute(Runnable task) {
                workers.execute(task);
                if (first) {
                    first = false;
                    try { check(started.await(5, TimeUnit.SECONDS), "slow started"); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
                }
            }
            public void shutdown() { workers.shutdown(); }
            public List<Runnable> shutdownNow() { return workers.shutdownNow(); }
            public boolean isShutdown() { return workers.isShutdown(); }
            public boolean isTerminated() { return workers.isTerminated(); }
            public boolean awaitTermination(long t, TimeUnit u) throws InterruptedException { return workers.awaitTermination(t, u); }
        };
        try {
            List<Callable<String>> probes = timeout ? Collections.singletonList(slow) : Arrays.asList(slow, () -> URL);
            String result = MirrorProbeSelector.firstSuccessful(probes, gated, timeout ? 0 : 5, TimeUnit.SECONDS);
            check(timeout ? result == null : URL.equals(result), "selection result");
            check(interrupted.await(5, TimeUnit.SECONDS), "running loser interrupted");
            check(workers.isShutdown(), "executor shut down");
        } finally { workers.shutdownNow(); }
    }

    private static void interruptedCaller() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Thread.currentThread().interrupt();
        try {
            check(select(executor, () -> { new CountDownLatch(1).await(); return null; }) == null, "interrupted fallback");
            check(Thread.currentThread().isInterrupted(), "interrupt flag preserved");
            check(executor.isShutdown(), "interrupted executor shut down");
        } finally { Thread.interrupted(); executor.shutdownNow(); }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class InlineExecutor extends AbstractExecutorService {
        private boolean shutdown;
        public void execute(Runnable task) { task.run(); }
        public void shutdown() { shutdown = true; }
        public List<Runnable> shutdownNow() { shutdown(); return Collections.emptyList(); }
        public boolean isShutdown() { return shutdown; }
        public boolean isTerminated() { return shutdown; }
        public boolean awaitTermination(long t, TimeUnit u) { return shutdown; }
    }
}
