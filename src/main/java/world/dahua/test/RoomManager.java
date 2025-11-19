package world.dahua.test;

import io.netty.channel.Channel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    // 数据结构：roomId -> (userId -> Channel)
    public static final Map<String, Map<String, Channel>> rooms = new ConcurrentHashMap<>();

    // 用户加入房间
    public static void joinRoom(String roomId, String userId, Channel channel) {
        rooms.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(userId, channel);
    }

    // 获取房间内其他用户
    public static Map<String, Channel> getOtherUsers(String roomId, String excludeUserId) {
        Map<String, Channel> users = rooms.get(roomId);
        if (users == null) return new ConcurrentHashMap<>();

        Map<String, Channel> others = new ConcurrentHashMap<>();
        for (Map.Entry<String, Channel> entry : users.entrySet()) {
            if (!entry.getKey().equals(excludeUserId)) {
                others.put(entry.getKey(), entry.getValue());
            }
        }
        return others;
    }

    // 用户离开房间
    public static void leaveRoom(String roomId, String userId) {
        Map<String, Channel> users = rooms.get(roomId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) rooms.remove(roomId);
        }
    }
}