package com.wire.blender;

import java.util.List;
import java.util.ArrayList;

public class Blender {
    static {
        System.loadLibrary("blender"); // Load native library at runtime
    }

    private long blenderPointer;

    private final List<BlenderListener> listeners = new ArrayList<>();

    public void log(String msg) {
    }

    public void registerListener(BlenderListener listener) {
        listeners.add(listener);
    }


    private void onConfigRequest() {
        for (BlenderListener listener : listeners) {
            //listener.onConfigRequest(id);
        }
    }


    private void onCallingMessage(String id,
                                  String userId,
                                  String clientId,
                                  String peerId,
                                  String peerClientId,
                                  String content,
                                  boolean trans) {

        for (BlenderListener listener : listeners) {
		listener.onCallingMessage(id,
					  userId,
					  clientId,
					  peerId,
					  peerClientId,
					  content,
					  trans);
        }
    }

    public native void recvConfig(String config);

    public native void recvMessage(String convId, String userId,
                                   String clientId, String content);

    public native void init(String name, String userid, String clientid,
			    String localAddr, int minPort, int maxPort);
}
