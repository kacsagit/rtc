package com.example.kata.rtc.pnwebrtc;

import android.util.Log;

import com.google.gson.JsonObject;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

/**
 * <h1>PubNub Peer object to hold information on {@link PeerConnection}</h1>
 * <pre>
 * Author:  Kevin Gleason - Boston College '16
 * File:    PnPeer.java
 * Date:    7/22/15
 * Use:     Store information about various Peer Connections
 * &copy; 2009 - 2015 PubNub, Inc.
 * </pre>
 */
public class PnPeer implements SdpObserver, PeerConnection.Observer {
    public static final String TAG = "PnPeer";
    public static final String STATUS_CONNECTING   = "CONNECTING";
    public static final String STATUS_CONNECTED    = "CONNECTED"; // TODO: Where to change status to this?
    public static final String STATUS_DISCONNECTED = "DISCONNECTED";
    public static final String TYPE_NONE           = "NONE";
    public static final String TYPE_OFFER          = "offer";
    public static final String TYPE_ANSWER         = "answer";

    private PnPeerConnectionClient pcClient;
    PeerConnection pc;
    String id;
    String type;
    String status;
    boolean dialed;
    boolean received;
    // Todo: Maybe attach MediaStream as private var?

    public PnPeer(String id, PnPeerConnectionClient pcClient) {
        Log.d(TAG, "new Peer: " + id);
        this.id = id;
        this.type = TYPE_NONE;
        this.dialed = false;
        this.received = false;
        this.pcClient = pcClient;
        this.pc = pcClient.pcFactory.createPeerConnection(pcClient.signalingParams.iceServers,
                pcClient.signalingParams.pcConstraints, this);
        setStatus(STATUS_CONNECTING);
        pc.addStream(pcClient.getLocalMediaStream());
    }

    public synchronized void setStatus(String status){
        this.status = status;
        pcClient.mRtcListener.onPeerStatusChanged(this);
    }

    public String getStatus() {
        return status;
    }

    public void setType(String type){this.type = type;}

    public String getType() {
        return type;
    }

    public boolean isDialed() {
        return dialed;
    }

    public void setDialed(boolean dialed) {
        this.dialed = dialed;
    }

    public boolean isReceived() {
        return received;
    }

    public void setReceived(boolean received) {
        this.received = received;
    }

    public PeerConnection getPc() {
        return pc;
    }

    public String getId() {
        return id;
    }

    public void hangup(){
        if (this.status.equals(STATUS_DISCONNECTED)) return; // Already hung up on.
        this.pcClient.removePeer(this.id);
        setStatus(STATUS_DISCONNECTED);
    }

    @Override
    public void onCreateSuccess(final SessionDescription sdp) {
        // TODO: modify sdp to use pcParams prefered codecs
            JsonObject payload = new JsonObject();
            payload.addProperty("type", sdp.type.canonicalForm());
            payload.addProperty("sdp", sdp.description);
            pcClient.transmitMessage(id, payload);
            pc.setLocalDescription(PnPeer.this, sdp);

    }

    @Override
    public void onSetSuccess() {
    }

    @Override
    public void onCreateFailure(String s) {
    }

    @Override
    public void onSetFailure(String s) {
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        if (this.status.equals(STATUS_DISCONNECTED)) return; // Already hung up on.
        if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
            pcClient.removePeer(id); // TODO: Ponder. Also, might want to Pub a disconnect.
            setStatus(STATUS_DISCONNECTED);
        }
    }

    // Todo: Look into what this should be used for
    @Override
    public void onIceConnectionReceivingChange(boolean iceConnectionReceivingChange){

    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
            JsonObject payload = new JsonObject();
            payload.addProperty("sdpMLineIndex", candidate.sdpMLineIndex);
            payload.addProperty("sdpMid", candidate.sdpMid);
            payload.addProperty("candidate", candidate.sdp);
            pcClient.transmitMessage(id, payload);

    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        Log.d(TAG, "onAddStream " + mediaStream.label());
        // remote streams are displayed from 1 to MAX_PEER (0 is localStream)
        pcClient.mRtcListener.onAddRemoteStream(mediaStream, PnPeer.this);
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        Log.d(TAG, "onRemoveStream " + mediaStream.label());
        PnPeer peer = pcClient.removePeer(id);
        pcClient.mRtcListener.onRemoveRemoteStream(mediaStream, peer);
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        Log.d(TAG, "onDataChannel ");
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.d(TAG, "onRenegotiationNeeded " );
    }

    /**
     * Overriding toString for debugging purposes.
     * @return String representation of a peer.
     */
    @Override
    public String toString(){
        return this.id + " Status: " + this.status + " Dialed: " + this.dialed +
                " Received: " + this.received + " Type: " + this.type;
    }

}
