package world.dahua.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.onvoid.webrtc.*;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class RtcClient {

    private Channel channel;
    private RTCPeerConnection peerConnection;
    private PeerConnectionFactory factory;
    private ObjectMapper mapper = new ObjectMapper();

    public RtcClient(Channel channel) {
        this.channel = channel;
    }

    public void handleOffer(String sdp) {
        factory = new PeerConnectionFactory();
        RTCConfiguration config = new RTCConfiguration();
        RTCIceServer iceServer = new RTCIceServer();
        iceServer.urls.add("stun:172.23.35.20:3478");
        config.iceServers.add(iceServer);
//        config.portAllocatorConfig.maxPort = 10001;
//        config.portAllocatorConfig.minPort = 10000;
        config.portAllocatorConfig.setDisableTcp(true);
//        config.portAllocatorConfig.setDisableUdp(true);
        peerConnection = factory.createPeerConnection(config, new PeerConnectionObserver() {
            @Override
            public void onIceCandidate(RTCIceCandidate candidate) {
                try {
                    channel.writeAndFlush(new TextWebSocketFrame(mapper.writeValueAsString(
                            Map.of(
                                    "type", "ice-candidate",
                                    "candidate", candidate
                            )
                    )));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onDataChannel(RTCDataChannel dataChannel) {
                dataChannel.registerObserver(new RTCDataChannelObserver() {
                    @Override
                    public void onBufferedAmountChange(long l) {

                    }

                    @Override
                    public void onStateChange() {

                    }

                    @Override
                    public void onMessage(RTCDataChannelBuffer buffer) {
                        if (buffer.binary) {
                            // Handle binary data
                            System.out.println(buffer.data);
                        } else {
                            // Handle text data
                            ByteBuffer data = buffer.data;

                            // 2. 复制缓冲区数据（避免原缓冲区被释放后数据丢失）
                            byte[] bytes = new byte[data.remaining()];
                            data.get(bytes);

                            // 3. 转换为字符串（使用 UTF-8 编码，根据实际情况调整）
                            System.out.println(new String(bytes, StandardCharsets.UTF_8));
                        }
                    }
                });
            }
        });
        RTCSessionDescription remoteDescription = new RTCSessionDescription(RTCSdpType.OFFER, sdp);
        peerConnection.setRemoteDescription(remoteDescription, new SetSessionDescriptionObserver() {
            @Override
            public void onSuccess() {
                System.out.println("Remote description set successfully");
            }

            @Override
            public void onFailure(String error) {
                System.err.println("Failed to set remote description: " + error);
            }
        });
        RTCAnswerOptions options = new RTCAnswerOptions();
        peerConnection.createAnswer(options, new CreateSessionDescriptionObserver() {
            @Override
            public void onSuccess(RTCSessionDescription description) {
                peerConnection.setLocalDescription(description, new SetSessionDescriptionObserver() {
                    @Override
                    public void onSuccess() {
                        try {
                            channel.writeAndFlush(new TextWebSocketFrame(mapper.writeValueAsString(
                                    Map.of(
                                            "type", "answer",
                                            "sdp", description.sdp
                                    )
                            )));
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        System.err.println("Failed to set local description: " + error);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                System.err.println("Failed to create offer: " + error);
            }
        });
        RTCDataChannel dataChannel = peerConnection.createDataChannel("server-channel", new RTCDataChannelInit());
        dataChannel.registerObserver(new RTCDataChannelObserver() {
            @Override
            public void onBufferedAmountChange(long previousAmount) {
                // Called when the buffered amount changes
                long currentAmount = dataChannel.getBufferedAmount();
                System.out.println("Buffered amount changed from " + previousAmount +
                        " to " + currentAmount + " bytes");
            }

            @Override
            public void onStateChange() {
                // Called when the data channel state changes
                RTCDataChannelState state = dataChannel.getState();
                System.out.println("Data channel state changed to: " + state);

                // Handle different states
                switch (state) {
                    case CONNECTING:
                        System.out.println("Data channel is being established");
                        break;
                    case OPEN:
                        System.out.println("Data channel is open and ready to use");
                        break;
                    case CLOSING:
                        System.out.println("Data channel is being closed");
                        break;
                    case CLOSED:
                        System.out.println("Data channel is closed");
                        break;
                }
            }

            @Override
            public void onMessage(RTCDataChannelBuffer buffer) {
                // Called when a message is received
                // IMPORTANT: The buffer data will be freed after this method returns,
                // so you must copy it if you need to use it asynchronously

                if (buffer.binary) {
                    // Handle binary data
                    System.out.println(buffer.data);
                } else {
                    // Handle text data
                    System.out.println(buffer.data);
                }
            }

        });
    }

    public void handleIceCandidate(String sdpMid, int sdpMLineIndex, String candidate) {
        RTCIceCandidate rtcIceCandidate = new RTCIceCandidate(sdpMid, sdpMLineIndex, candidate);
        peerConnection.addIceCandidate(rtcIceCandidate);
    }

    public void close() {
        peerConnection.close();
    }
}