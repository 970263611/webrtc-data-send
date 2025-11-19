package world.dahua.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.Map;

public class SignalingHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private RtcClient rtcClient;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 通道激活时创建WebRTC客户端
        rtcClient = new RtcClient(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) throws Exception {
        String json = frame.text();
        JsonNode node = mapper.readTree(json);
        String type = node.get("type").asText();

        switch (type) {
            case "offer":
                // 处理前端Offer
                String sdp = node.get("sdp").asText();
                rtcClient.handleOffer(sdp);
                break;

            case "ice-candidate":
                // 处理前端ICE候选者
                JsonNode candidateNode = node.get("candidate");
                String sdpMid = candidateNode.get("sdpMid").asText();
                int sdpMLineIndex = candidateNode.get("sdpMLineIndex").asInt();
                String candidate = candidateNode.get("candidate").asText();
                rtcClient.handleIceCandidate(sdpMid, sdpMLineIndex, candidate);
                break;

            case "join":
                // 回复加入成功
                ctx.writeAndFlush(new TextWebSocketFrame(mapper.writeValueAsString(
                        Map.of(
                                "type", "join-ack",
                                "success", true,
                                "message", "已连接服务器（webrtc-java 0.14.0）"
                        )
                )));
                break;
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 通道关闭时释放资源
        if (rtcClient != null) {
            rtcClient.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}