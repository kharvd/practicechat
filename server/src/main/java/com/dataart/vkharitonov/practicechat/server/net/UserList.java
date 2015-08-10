package com.dataart.vkharitonov.practicechat.server.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Maintains a list of connected users
 */
public class UserList {

    private final static Logger log = LoggerFactory.getLogger(UserList.class.getName());

    private final Map<String, ClientInteractor> clients = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CompletableFuture<ClientInteractor> addInteractor(String username, ClientInteractor interactor) {
        return CompletableFuture.supplyAsync(() -> {
            ClientInteractor prev = clients.put(username, interactor);

            if (prev != null) {
                log.info("User {} already connected, reconnecting", username);
            }

            return prev;
        }, executor);
    }

    public CompletableFuture<ClientInteractor> removeInteractor(String username) {
        return CompletableFuture.supplyAsync(() -> clients.remove(username), executor);
    }

    public ClientInteractor getInteractor(String username) {
        return clients.get(username);
    }

    public CompletableFuture<Collection<ClientInteractor>> getInteractors() {
        return CompletableFuture.supplyAsync(() -> new ArrayList<>(clients.values()), executor);
    }

    public CompletableFuture<List<String>> usersList() {
        return CompletableFuture.supplyAsync(() -> clients.keySet().stream().collect(Collectors.toList()), executor);
    }

    public boolean isOnline(String username) {
        return clients.containsKey(username);
    }

    public CompletableFuture<Void> removeAll() {
        return CompletableFuture.runAsync(clients::clear, executor);
    }

    public CompletableFuture<Collection<ClientInteractor>> removeAllAndShutdown() {
        return getInteractors().thenComposeAsync(list ->
                        removeAll().thenApplyAsync(aVoid -> {
                            executor.shutdown();
                            return list;
                        })
        );
    }
}
