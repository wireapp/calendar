package com.wire.blender;

import java.util.ArrayList;
import java.util.List;

public class Blender {
    static {
        System.loadLibrary("blender"); // Load native library at runtime
    }

    private long blenderPointer;

    private List<BlenderListener> listeners = new ArrayList<>();

    public void log(String msg) {
        System.out.println("JAVA:Blender: " + msg);
    }

    public void registerListener(BlenderListener listener) {
        listeners.add(listener);
    }

    private void onCallingMessage(String id,
                                  String userId,
                                  String clientId,
                                  String peerId,
                                  String peerClientId,
                                  String content,
                                  boolean trans)
    {
        for (BlenderListener lsnr : listeners) {
            lsnr.onCallingMessage(id, userId, clientId,
                    peerId, peerClientId,
                    content, trans);
        }
    }

    public native void recvMessage(String convid, String userid,
                                   String clientid, String content);
    public native void init(String config, String userid, String clientid);
}
