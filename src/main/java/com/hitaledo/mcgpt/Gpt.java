package com.hitaledo.mcgpt;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Gpt implements CommandExecutor {
    private App plugin;

    public Gpt(App plugin) {
        this.plugin = plugin;
    }

    public String apiPost(String url, String instructions, String question, String apikey, String model) {
        if (!url.isEmpty()) {
            HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            if (!apikey.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + apikey);
            }
            connection.setDoOutput(true);
            String messages = "[{\"role\": \"system\",\"content\": \"" + instructions
                    + "\"},{\"role\": \"user\",\"content\": \"" + question + "\"}]";
            String data = "{\"model\": \"" + model + "\", \"messages\": \"" + messages + "\"}";
            try (OutputStream os = connection.getOutputStream()) {
                byte[] postData = data.getBytes("utf-8");
                os.write(postData, 0, postData.length);
            }
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String content = "Response format error.";
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    String regex = "\"content\":\"([^\"]+)\"";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(response.toString());
                    if (matcher.find()) {
                        content = matcher.group(1);
                    } else {
                        content = "Field content was not found in JSON response.";
                    }
                }
                return content;
            } else {
                return "Response was not ok.";
            }
            connection.disconnect();
        } else {
            return "Url is empty.";
        }
    }

    public boolean onCommand(CommandSender sender, Command gpt, String label, String[] args) {
        FileConfiguration config = plugin.getConfig();
        String url;
        String instructions;
        String apikey;
        String model;
        try {
            url = config.getString("Config.url");
        } catch (Exception e) {
            url = "";
            plugin.getLogger().info(ChatColor.GREEN + "Url config not found. Using default value: " + url);
        }
        try {
            instructions = config.getString("Config.instructions");
        } catch (Exception e) {
            instructions = "";
            plugin.getLogger()
                    .info(ChatColor.GREEN + "Instructions config not found. Using default value: " + instructions);
        }
        try {
            apikey = config.getString("Config.apikey");
        } catch (Exception e) {
            apikey = "";
            plugin.getLogger().info(ChatColor.GREEN + "Api key config not found. Using default value: " + apikey);
        }
        try {
            model = config.getString("Config.model");
        } catch (Exception e) {
            model = "";
            plugin.getLogger().info(ChatColor.GREEN + "Model config not found. Using default value: " + model);
        }
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 1) {
                player.sendMessage(apiPost(url, instructions, args[0], apikey, model));
            } else {
                player.sendMessage("This command needs exactly one arg");
            }
        } else {
            if (args.length == 1) {
                plugin.getLogger().info(apiPost(url, instructions, args[0], apikey, model));
            } else {
                plugin.getLogger().info("This command needs exactly one arg");
            }
        }
        return true;
    }
}
