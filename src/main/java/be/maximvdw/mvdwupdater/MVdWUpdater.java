package be.maximvdw.mvdwupdater;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.logging.LogFactory;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import be.maximvdw.mvdwupdater.config.Configuration;
import be.maximvdw.mvdwupdater.config.YamlBuilder;
import be.maximvdw.mvdwupdater.config.YamlStorage;
import be.maximvdw.mvdwupdater.ui.SendConsole;
import be.maximvdw.spigotsite.SpigotSiteCore;
import be.maximvdw.spigotsite.api.SpigotSiteAPI;
import be.maximvdw.spigotsite.api.exceptions.ConnectionFailedException;
import be.maximvdw.spigotsite.api.resource.Resource;
import be.maximvdw.spigotsite.api.resource.ResourceManager;
import be.maximvdw.spigotsite.api.user.User;
import be.maximvdw.spigotsite.api.user.UserManager;
import be.maximvdw.spigotsite.api.user.exceptions.InvalidCredentialsException;
import be.maximvdw.spigotsite.user.SpigotUser;

public class MVdWUpdater extends JavaPlugin {
	private SpigotSiteAPI api = null;
	private SpigotUser user = null;

	@Override
	public void onEnable() {
		new SendConsole(this);
		// MOTD
		SendConsole.info("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
		SendConsole.info("Plugin: " + this.getName() + " v" + this.getDescription().getVersion());
		SendConsole.info("Author: Maximvdw");
		SendConsole.info("Site: http://www.mvdw-software.com/");
		SendConsole.info("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");

		SendConsole.info("Turning off HTMLUnit logging ...");
		try {
			LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log",
					"org.apache.commons.logging.impl.NoOpLog");

			java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
			java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
		} catch (Exception ex) {
			SendConsole.warning("Unable to turn of HTMLUnit logging!");
		}

		SendConsole.info("Initializing Spigot connection...");
		SendConsole.info("This can take a while!");
		try {
			api = new SpigotSiteCore(); // Initialize spigot site API
		} catch (Exception ex) {
			SendConsole.severe("Unable to load Spigot Core!");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		SendConsole.info("Spigot Site Core initialized!");

		try {
			// Load configuration
			new Configuration(this);
			// Load credential config
			loadCredentialConfig();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void updatePlugin(Plugin plugin, int resourceId, UpdateMethod method, User user) {
//		List<Resource> premiums = getPurchasedResources(user);
//		for (Resource premium : premiums) {
//			if (premium.getResourceId() == resourceId) {
//
//			}
//		}
		
	}

	/**
	 * Authenticate spigot user
	 * 
	 * @param username
	 *            Username
	 * @param password
	 *            Password
	 * @return SpigotUser
	 * @throws InvalidCredentialsException
	 *             Username or Password do not match
	 */
	public SpigotUser authenticate(String username, String password) throws InvalidCredentialsException {
		UserManager userManager = api.getUserManager();
		SpigotUser user = (SpigotUser) userManager.authenticate(username, password);
		return user;
	}

	/**
	 * Get purchased resources
	 * 
	 * @param user
	 *            Spigot user
	 * @return List of premium resources
	 * @throws ConnectionFailedException
	 */
	public List<Resource> getPurchasedResources(User user) throws ConnectionFailedException {
		ResourceManager resourceManager = api.getResourceManager();
		return resourceManager.getPurchasedResources(user);
	}

	/**
	 * Check if the user bought the resource id
	 * 
	 * @param user
	 *            Spigot User
	 * @param resourceId
	 *            Resource id
	 * @return Has bought
	 * @throws ConnectionFailedException
	 */
	public boolean hasBought(SpigotUser user, int resourceId) throws ConnectionFailedException {
		for (Resource resource : getPurchasedResources(user)) {
			if (resource.getResourceId() == resourceId)
				return true;
		}
		return false;
	}

	/**
	 * Get Spigot Site API
	 * 
	 * @return Spigot Site API
	 */
	public SpigotSiteAPI getSpigotSiteAPI() {
		return api;
	}

	/**
	 * Get Spigot Site user
	 * 
	 * @return Spigot Site user
	 */
	public SpigotUser getSpigotUser() {
		return user;
	}

	/**
	 * Get the latest version STRING on spigot
	 * 
	 * @param resourceId
	 *            Resource ID
	 * @return Version string
	 */
	public String getResourceVersionString(int resourceId) {
		try {
			HttpURLConnection con = (HttpURLConnection) new URL("http://www.spigotmc.org/api/general.php")
					.openConnection();
			con.setDoOutput(true);
			con.setRequestMethod("POST");
			con.getOutputStream().write(
					("key=98BE0FE67F88AB82B4C197FAF1DC3B69206EFDCC4D3B80FC83A00037510B99B4&resource=" + resourceId)
							.getBytes("UTF-8"));
			String version = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
			if (version.length() <= 7) {
				return version;
			}
		} catch (Exception ex) {
			SendConsole.info("Failed to check for a update on spigot.");
		}
		return null;
	}

	/**
	 * Load the configuration
	 */
	private void loadCredentialConfig() {
		YamlBuilder yamlBuilder = new YamlBuilder();
		yamlBuilder.addPart("# ---------------------------------------- #");
		yamlBuilder.addPart("# SpigotMC.org credentials");
		yamlBuilder.addPart("#   Enter your SpigotMC.org credentials in");
		yamlBuilder.addPart("#   this yaml file and restart the plugin");
		yamlBuilder.addPart("#   The plugin will log you in and store");
		yamlBuilder.addPart("#   the session cookies. Your password you");
		yamlBuilder.addPart("#   entered in this config will be removed.");
		yamlBuilder.addPart("# ---------------------------------------- #");
		yamlBuilder.addEmptyPart();
		yamlBuilder.addPart(" DO NOT EDIT THE CONFIG VERSION!");
		yamlBuilder.addPart("config", 1);
		yamlBuilder.addEmptyPart();
		yamlBuilder.addEmptyPart();
		yamlBuilder.addPart(" Enter your SpigotMC.org username");
		yamlBuilder.addPart("username", "");
		yamlBuilder.addEmptyPart();
		yamlBuilder.addPart(" Enter the corresponding password");
		yamlBuilder.addPart("password", "");

		YamlStorage credentialStorage = new YamlStorage(this, "", "credentials", 1, yamlBuilder);
		String username = credentialStorage.getConfig().getString("username");
		String password = credentialStorage.getConfig().getString("password");
		Object cookiesObj = credentialStorage.getConfig().get("cookies");

		try {
			UserManager userManager = api.getUserManager();
			if (!username.equals("") && !password.equals("")) {
				SendConsole.info("Logging in with: " + username);
				user = (SpigotUser) userManager.authenticate(username, password);
				SendConsole.info("Succesfully logged in! ID=" + user.getUserId());
				SendConsole.info("Removing password from credentials.yml ...");
				credentialStorage.getConfig().set("username", "");
				credentialStorage.getConfig().set("password", "");
				credentialStorage.getConfig().set("cookies", user.getCookies());
				credentialStorage.getConfig().save(credentialStorage.getConfigFile());
				SendConsole.info("Cleaned the credentials.yml file!");

			} else {
				if (cookiesObj != null) {
					Map<String, String> cookies = new HashMap<String, String>();
					ConfigurationSection section = credentialStorage.getConfig().getConfigurationSection("cookies");
					for (String key : section.getKeys(false)) {
						String value = section.getString(key);
						cookies.put(key, value);
					}
					if (cookies.size() > 0) {
						user = new SpigotUser(username);
						((SpigotUser) user).setCookies(cookies);
					}
				}
			}
		} catch (Exception ex) {
			SendConsole.severe("Unable to authenticate the SpigotMC.org user!");
		}

		if (user != null) {
			try {
				List<Resource> resources = getPurchasedResources(user);
				SendConsole.info("Purchased resources: " + resources.size());
				for (Resource resource : resources) {
					SendConsole.info("\t" + resource.getResourceName());
				}
			} catch (Exception ex) {
				SendConsole.warning("Unable to fetch purchased resources!");
			}
		}
	}

	/**
	 * Get the resource version object
	 * 
	 * @param resourceId
	 *            Resource id
	 * @return Version object
	 */
	public Version getResourceVersion(int resourceId) {
		return new Version(getResourceVersionString(resourceId));
	}
}
