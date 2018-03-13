package com.wire.bots.cali;

import com.wire.blender.Blender;
import com.wire.blender.BlenderListener;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.tools.Logger;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class DuleListener implements BlenderListener {
    private final ClientRepo repo;
    private final Blender blender;
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);

    DuleListener(ClientRepo repo, Blender blender) {
        this.repo = repo;
        this.blender = blender;
    }

    @Override
    public void onCallingMessage(String id,
                                 String userId,
                                 String clientId,
                                 String peerId,
                                 String peerClientId,
                                 String content,
                                 boolean trans) {

        Logger.info("id: %s, user: (%s-%s), peer: (%s-%s), content: %s, transient: %s",
                id,
                userId,
                clientId,
                peerId,
                peerClientId,
                content,
                trans);

        executor.execute(() -> {
            try {
                repo.getWireClient(id)
                        .call(content);
            } catch (Exception e) {
                Logger.error("onCallingMessage: %s", e);
            }
        });
    }

    @Override
    public void onConfigRequest(String id) {
        executor.execute(() -> {
            try {
                Logger.info("onConfigRequest: Requesting for: ", id);

                WireClient wireClient = repo.getWireClient(id);
                String config = "";
                //blender.recvConfig(config);
            } catch (Exception e) {
                Logger.error("onConfigRequest: %s", e);
            }
        });
    }
}
