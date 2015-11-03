package be.maximvdw.mvdwupdater.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import be.maximvdw.mvdwupdater.ui.SendConsole;

/**
 * Configuration
 * 
 * Config File
 * 
 * @project MVdW Software Plugin Interface
 * @version 12-06-2014
 * @author Maxim Van de Wynckel (Maximvdw)
 * @site http://www.mvdw-software.be/
 */
public class Configuration {
	private static Plugin plugin = null; // Plugin instance
	/**
	 * Initialize the configuration
	 * 
	 * @param plugin
	 *            Plugin
	 */
	public Configuration(Plugin plugin) {
		Configuration.plugin = plugin;
	}

	/**
	 * Initialize the configuration
	 * 
	 * @param plugin
	 *            Plugin
	 * @param config
	 *            Config version
	 */
	public Configuration(Plugin plugin, int config) {
		Configuration.plugin = plugin;
		loadConfig();
	}

	// Yaml Configuration
	private static YamlConfiguration pluginConfig;
	private static File configFile;
	private static boolean loaded = false;


	/**
	 * Gets the configuration file.
	 * 
	 * @return the myConfig
	 */
	public static YamlConfiguration getConfig() {
		if (!loaded) {
			loadConfig();
		}
		return pluginConfig;
	}

	/**
	 * Gets the configuration file.
	 * 
	 * @return Configuration file
	 */
	public static File getConfigFile() {
		return configFile;
	}

	/**
	 * Get file contents
	 * 
	 * @return Contents
	 */
	public static String getContents() {
		try {
			FileInputStream fis = new FileInputStream(configFile);
			byte[] data = new byte[(int) configFile.length()];
			fis.read(data);
			fis.close();
			//
			String s = new String(data, "UTF-8");
			return s;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * Loads the configuration file from the .jar.
	 */
	public static void loadConfig() {
		configFile = new File(plugin.getDataFolder(), "config.yml");
		if (configFile.exists()) {
			pluginConfig = new YamlConfiguration();
			try {
				pluginConfig.load(configFile);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			loaded = true;
		} else {
			try {
				plugin.getDataFolder().mkdir();
				InputStream jarURL = plugin.getClass().getResourceAsStream(
						"/config.yml");
				if (jarURL != null) {
					SendConsole.info("Copying '" + configFile
							+ "' from the resources!");
					copyFile(jarURL, configFile);
					pluginConfig = new YamlConfiguration();
					pluginConfig.load(configFile);
					loaded = true;
				} else {
					SendConsole
							.severe("Configuration file not found inside the plugin!");
					SendConsole
							.severe("This error occurs when you forced a reload!");
				}
				jarURL.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Copies a file to a new location.
	 * 
	 * @param in
	 *            InputStream
	 * @param out
	 *            File
	 * 
	 * @throws Exception
	 */
	static private void copyFile(InputStream in, File out) throws Exception {
		InputStream fis = in;
		FileOutputStream fos = new FileOutputStream(out);
		try {
			byte[] buf = new byte[1024];
			int i = 0;
			while ((i = fis.read(buf)) != -1) {
				fos.write(buf, 0, i);
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (fis != null) {
				fis.close();
			}
			if (fos != null) {
				fos.close();
			}
		}
	}
}
