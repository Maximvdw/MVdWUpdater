package be.maximvdw.mvdwupdater.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import be.maximvdw.mvdwupdater.ui.SendConsole;


/**
 * YamlStorage
 * 
 * YAML File storage
 * 
 * @project MVdW Software Plugin Interface
 * @version 12-04-2014
 * @author Maxim Van de Wynckel (Maximvdw)
 * @site http://www.mvdw-software.be/
 */
public class YamlStorage {
	private Plugin plugin = null; // Plugin instance
	private String configName = ""; // Configuration name
	private String pluginFolder = ""; // Plugin folder
	private String folder = ""; // Folder
	private String resourceFolder = ""; // Resource folder
	private boolean resources = false;
	private int configVersion = -1;
	private YamlBuilder template = null;

	/**
	 * Initialize the Yaml Configuration
	 * 
	 * @param plugin
	 *            Plugin
	 */
	public YamlStorage(Plugin plugin, String configName) {
		this(plugin, "", configName);
	}

	/**
	 * Initialize the Yaml Configuration
	 * 
	 * @param plugin
	 *            Plugin
	 */
	public YamlStorage(Plugin plugin, String folder, String configName) {
		this(plugin, folder, configName, false);
	}

	/**
	 * Initialize the Yaml Configuration
	 * 
	 * @param plugin
	 *            Plugin
	 */
	public YamlStorage(Plugin plugin, String folder, String configName, boolean resources) {
		this(plugin, folder, configName, resources, -1);
	}

	/**
	 * Initialize the Yaml Configuration
	 * 
	 * @param plugin
	 *            Plugin
	 */
	public YamlStorage(Plugin plugin, String folder, String configName, boolean resources, int version) {
		this.plugin = plugin;
		this.folder = folder;
		this.configName = configName;
		this.resources = resources;
		this.setConfigVersion(version);
		loadConfig();
		if (version != -1)
			if (getConfig().getInt("config") < version) {
				updateConfig();
			}
	}

	/**
	 * Initialize the Yaml Configuration
	 * 
	 * @param plugin
	 *            Plugin
	 */
	public YamlStorage(Plugin plugin, String folder, String configName, int version, YamlBuilder builder) {
		this.plugin = plugin;
		this.folder = folder;
		this.configName = configName;
		setTemplate(builder);

		loadConfig();
		if (getConfig().getInt("config") < version) {
			updateConfig();
		}
	}

	/**
	 * Initialize the Yaml Configuration
	 * 
	 * @param plugin
	 *            Plugin
	 */
	public YamlStorage(Plugin plugin, String folder, String configName, String resourceFolder, boolean resources) {
		this.plugin = plugin;
		this.folder = folder;
		this.configName = configName;
		this.resourceFolder = resourceFolder;
		this.resources = resources;
		loadConfig();
	}

	public YamlStorage(Plugin plugin, String folder, String configName, String resourceFolder, boolean resources,
			int version) {
		this.plugin = plugin;
		this.folder = folder;
		this.configName = configName;
		this.resourceFolder = resourceFolder;
		this.resources = resources;
		this.setConfigVersion(version);
		loadConfig();
		if (getConfig().getInt("config") < version) {
			updateConfig();
		}
	}

	/**
	 * Initialize the Yaml Configuration
	 * 
	 * @param plugin
	 *            Plugin
	 */
	public YamlStorage(String pluginFolder, String folder, String configName) {
		this.folder = folder;
		this.pluginFolder = pluginFolder;
		this.configName = configName;
		loadConfig();
	}

	// Yaml Configuration
	private YamlConfiguration pluginConfig;
	private File configFile;
	private boolean loaded = false;

	/**
	 * Gets the configuration file.
	 * 
	 * @return the myConfig
	 */
	public YamlConfiguration getConfig() {
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
	public File getConfigFile() {
		return configFile;
	}

	/**
	 * Get file contents
	 * 
	 * @return Contents
	 */
	public String getContents() {
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
	 * Loads the configuration file
	 */
	public void loadConfig() {
		if (plugin != null)
			configFile = new File(plugin.getDataFolder() + "/" + folder, configName + ".yml");
		else
			configFile = new File("plugins/" + pluginFolder + "/" + folder, configName + ".yml");
		if (configFile.exists()) {
			pluginConfig = new YamlConfiguration();
			try {
				pluginConfig.load(configFile);
			} catch (FileNotFoundException ex) {
				SendConsole.severe("Error in [" + configName + "] - File not found!");
			} catch (IOException ex) {
				SendConsole.severe("Error in [" + configName + "] - Error while reading!");
			} catch (InvalidConfigurationException ex) {
				SendConsole.severe("Error in [" + configName + "] - Corrupted YML!");
				ex.printStackTrace();
			}
			loaded = true;
		} else {
			try {
				InputStream jarURL = null;
				if (plugin != null) {
					plugin.getDataFolder().mkdir();
					new File(plugin.getDataFolder() + "/" + folder).mkdirs();
					jarURL = plugin.getClass().getResourceAsStream(
							"/" + resourceFolder + (resourceFolder == "" ? "" : "/") + configName + ".yml");
				} else {
					new File("plugins/" + pluginFolder + "/" + folder).mkdirs();
				}
				if (jarURL != null) {
					SendConsole.info("Copying '" + configFile + "' from the resources!");
					copyFile(jarURL, configFile);
				} else {
					if (!resources) {
						SendConsole.info("Creating new file '" + configFile + "'!");
						configFile.createNewFile();
						if (getTemplate() != null) {
							SendConsole.info("Using template for file '" + configFile + "' ...");
							getTemplate().writeToFile(getConfigFile());
						}
					} else {
						return;
					}
				}

				pluginConfig = new YamlConfiguration();
				pluginConfig.load(configFile);
				loaded = true;
				jarURL.close();
			} catch (Exception e) {
				SendConsole.severe("Error while loading " + configFile + "!");
			}
		}
	}

	/**
	 * Update configuration
	 */
	public void updateConfig() {
		SendConsole.warning("Updating " + getConfigFile().getName() + "!");
		configFile.renameTo(new File((plugin == null ? "plugins/ " : plugin.getDataFolder()) + "/" + folder,
				configFile.getName().replace(".yml", "") + new Date().getTime() + ".yml"));
		configFile = null;
		loadConfig();
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

	public int getConfigVersion() {
		return configVersion;
	}

	public void setConfigVersion(int configVersion) {
		this.configVersion = configVersion;
	}

	public YamlBuilder getTemplate() {
		return template;
	}

	public void setTemplate(YamlBuilder template) {
		this.template = template;
	}
}
