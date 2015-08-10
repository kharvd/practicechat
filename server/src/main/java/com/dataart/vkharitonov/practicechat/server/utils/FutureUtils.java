package com.dataart.vkharitonov.practicechat.server.utils;

import java.util.concurrent.CompletableFuture;

public final class FutureUtils {

    public static <T> CompletableFuture<T> failure(Throwable e) {
        CompletableFuture<T> failure = new CompletableFuture<>();
        failure.completeExceptionally(e);
        return failure;
    }
}
