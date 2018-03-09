package com.wire.bots.cali;

import com.wire.blender.BlenderListener;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.tools.Logger;

public class DuleListener implements BlenderListener {
    private final ClientRepo repo;

    DuleListener(ClientRepo repo) {
        this.repo = repo;
    }

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

        WireClient wireClient = repo.getWireClient(id);
        try {
            wireClient.call(content);
        } catch (Exception e) {
            Logger.error("onCallingMessage: %s", e);
        }
    }
}
