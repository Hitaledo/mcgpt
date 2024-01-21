package com.hitaledo.mcgpt;

import java.io.File;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;

public class App extends JavaPlugin {
    String pluginName = "Minecraft GPT";
    File config;

    @Override
    public void onEnable() {
        config = new File(getDataFolder(), "config.yml");
        if (!config.exists()) {
            saveDefaultConfig();
        }
        getCommand("gpt").setExecutor(new Gpt(this));
        getLogger().info(ChatColor.GREEN + pluginName + " has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info(ChatColor.GREEN + pluginName + " has been disabled!");
    }
}