package com.hitaledo.mcgpt;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
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

    public String apiPost(String url, String instructions, String sender, String question, String apikey,
            String model) {
        String content = "Url is empty.";
        if (!url.isEmpty()) {
            content = "Response was not ok.";
            try {
                HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                if (!apikey.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + apikey);
                }
                connection.setDoOutput(true);
                String messages = "{\"role\": \"user\",\"content\": \"" + question + "\"}";
                if (!getHistory(sender).isEmpty()) {
                    messages = getHistory(sender) + "," + messages;
                }
                String newHistory = messages;
                if (!instructions.isEmpty()) {
                    if (!apikey.isEmpty()) {
                        messages = "{\"role\": \"system\",\"content\": \"" + instructions + "\"}" + "," + messages;
                    } else {
                        messages = "{\"role\": \"user\",\"content\": \"" + instructions + "\"}" + "," + messages;
                    }
                }
                messages = "[" + messages + "]";
                String data = "{\"model\": \"" + model + "\", \"messages\": " + messages + "}";
                OutputStream os = connection.getOutputStream();
                byte[] postData = data.getBytes("utf-8");
                os.write(postData, 0, postData.length);
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    content = "Field content was not found in JSON response.";
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
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
                    }
                    saveHistory(sender, newHistory);
                }
                connection.disconnect();
            } catch (Exception e) {
                content = e.getMessage();
            }
        }
        return content;
    }

    public boolean onCommand(CommandSender sender, Command gpt, String label, String[] args) {
        String url = "";
        String instructions = "";
        String apikey = "";
        String model = "";
        try {
            url = plugin.getConfig().getString("Config.url");
        } catch (Exception e) {
            plugin.getLogger().info(ChatColor.GREEN + "Url config not found. Using default value: " + url);
        }
        try {
            instructions = plugin.getConfig().getString("Config.instructions");
        } catch (Exception e) {
            plugin.getLogger()
                    .info(ChatColor.GREEN + "Instructions config not found. Using default value: " + instructions);
        }
        try {
            apikey = plugin.getConfig().getString("Config.apikey");
        } catch (Exception e) {
            plugin.getLogger().info(ChatColor.GREEN + "Api key config not found. Using default value: " + apikey);
        }
        try {
            model = plugin.getConfig().getString("Config.model");
        } catch (Exception e) {
            plugin.getLogger().info(ChatColor.GREEN + "Model config not found. Using default value: " + model);
        }
        final String finalUrl = url;
        final String finalInstructions = instructions;
        final String finalApikey = apikey;
        final String finalModel = model;
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length >= 1) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        String response = apiPost(finalUrl, finalInstructions, player.getName(),
                                String.join(" ", args), finalApikey,
                                finalModel);
                        Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(response));
                    }
                }.runTaskAsynchronously(plugin);
            } else {
                player.sendMessage("This command needs at least one arg");
            }
        } else {
            if (args.length >= 1) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        String response = apiPost(finalUrl, finalInstructions, "console",
                                String.join(" ", args),
                                finalApikey,
                                finalModel);
                        Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger().info(response));
                    }
                }.runTaskAsynchronously(plugin);
            } else {
                plugin.getLogger().info("This command needs at least one arg");
            }
        }
        return true;
    }

    public boolean saveHistory(String sender, String history) {
        plugin.dataConfig.set(sender, history);
        try {
            plugin.dataConfig.save(plugin.data);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public String getHistory(String sender) {
        try {
            return plugin.dataConfig.getString(sender);
        } catch (Exception e) {
            return "";
        }
    }
}
