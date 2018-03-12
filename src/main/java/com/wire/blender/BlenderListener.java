package com.wire.blender;

public interface BlenderListener {
	public void onCallingMessage(String id,
				     String userId,
				     String clientId,
				     String peerId,
				     String peerClientId,
				     String content,
				     boolean trans);

	public void onConfigRequest();
}
	
