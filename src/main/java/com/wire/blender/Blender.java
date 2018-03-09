package com.wire.blender;

public class Blender {
    static {
        System.loadLibrary("blender"); // Load native library at runtime
    }

    public void log(String msg) {
        System.out.println("JAVA:Blender: " + msg);
    }

    public native void init(String config);

    public native void recvMessage(String id, String userId, String clientId, String content);

    public native void registerListener(BlenderListener listener);
}
