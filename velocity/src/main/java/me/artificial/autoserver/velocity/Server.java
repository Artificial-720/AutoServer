package me.artificial.autoserver.velocity;

import java.util.concurrent.CompletableFuture;

public interface Server {
    /**
     * Starts the server asynchronously.
     *
     * @return A CompletableFuture that completes with a success message or exceptionally if the process fails.
     */
    CompletableFuture<String> start();

    /**
     * Stops the server asynchronously.
     *
     * @return A CompletableFuture that completes with a success message or exceptionally if the process fails.
     */
    CompletableFuture<String> stop();
}
