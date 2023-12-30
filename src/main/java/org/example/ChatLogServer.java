package org.example;

import com.google.gson.Gson;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ChatLogServer {

    private static final Map<String, List<ChatMessage>> chatLogs = new HashMap<>();
    private static final AtomicLong messageIdGenerator = new AtomicLong(1);

    public static void main(String[] args) {
        Spark.port(8080);

        // Create a new chatlog entry
        Spark.post("/chatlogs/:user", (req, res) -> createChatLog(req, res), new JsonTransformer());

        // Get chatlogs for a user
        Spark.get("/chatlogs/:user", (req, res) -> getChatLogs(req, res), new JsonTransformer());

        // Delete all chat logs for a user
        Spark.delete("/chatlogs/:user", (req, res) -> deleteAllChatLogs(req, res), new JsonTransformer());

        // Delete a specific chat log for a user
        Spark.delete("/chatlogs/:user/:msgid", (req, res) -> deleteChatLog(req, res), new JsonTransformer());
    }

    private static class JsonTransformer implements spark.ResponseTransformer {
        private Gson gson = new Gson();

        @Override
        public String render(Object model) {
            return gson.toJson(model);
        }
    }

    private static class ChatMessage {
        String message;
        long timestamp;
        boolean isSent;

        ChatMessage(String message, long timestamp, boolean isSent) {
            this.message = message;
            this.timestamp = timestamp;
            this.isSent = isSent;
        }
    }

    private static String createChatLog(Request req, Response res) {
        String user = req.params(":user");
        String requestBody = req.body();

        Gson gson = new Gson();
        ChatMessage chatMessage = gson.fromJson(requestBody, ChatMessage.class);

        String messageId = String.valueOf(messageIdGenerator.getAndIncrement());

        chatLogs.computeIfAbsent(user, k -> new ArrayList<>()).add(chatMessage);

        // Respond with the unique messageID
        return messageId;
    }

    private static List<ChatMessage> getChatLogs(Request req, Response res) {
        String user = req.params(":user");
        int limit = Integer.parseInt(req.queryParamOrDefault("limit", "10"));

        List<ChatMessage> userChatLogs = chatLogs.getOrDefault(user, new ArrayList<>());
        int endIndex = Math.min(limit, userChatLogs.size());

        // Respond with chat logs in reverse order
        return new ArrayList<>(userChatLogs.subList(0, endIndex));
    }

    private static String deleteAllChatLogs(Request req, Response res) {
        String user = req.params(":user");
        chatLogs.remove(user);

        // Respond with success message
        return "All chat logs for user " + user + " have been deleted.";
    }

    private static String deleteChatLog(Request req, Response res) {
        String user = req.params(":user");
        String msgId = req.params(":msgid");

        List<ChatMessage> userChatLogs = chatLogs.get(user);
        if (userChatLogs != null) {
            boolean removed = userChatLogs
                    .removeIf(message -> String.valueOf(messageIdGenerator.getAndIncrement()).equals(msgId));
            if (!removed) {
                res.status(404);
                return "Message not found with ID: " + msgId;
            }
        } else {
            res.status(404);
            return "User not found: " + user;
        }

        // Respond with success message
        return "Chat log with ID " + msgId + " for user " + user + " has been deleted.";
    }
}
