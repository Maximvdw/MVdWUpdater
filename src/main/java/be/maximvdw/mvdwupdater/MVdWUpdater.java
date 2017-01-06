package be.maximvdw.mvdwupdater;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import be.maximvdw.mvdwupdater.utils.LibDownloader;
import be.maximvdw.spigotsite.api.user.exceptions.TwoFactorAuthenticationException;
import org.apache.commons.logging.LogFactory;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.UnknownDependencyException;
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

		SendConsole.info("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
		SendConsole.info("Plugin: " + this.getName() + " v" + this.getDescription().getVersion());
		SendConsole.info("Author: Maximvdw");
		SendConsole.info("Site: http://www.mvdw-software.com/");
		SendConsole.info("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");

		LibDownloader.downloadLib(LibDownloader.Library.HTMMLUNIT);

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

	/**
	 * Update the plugin
	 * @param plugin Plugin to update
	 * @param resourceId Spigot Resource id
	 * @param method Update Method
	 * @param user Spigot user
	 * @throws ConnectionFailedException Thrown when the connection failed
	 * @throws UnknownDependencyException Unknown dependecy found
	 * @throws InvalidPluginException Plugin not valid
	 * @throws InvalidDescriptionException Plugin description not valid
	 */
	public void updatePlugin(Plugin plugin, int resourceId, UpdateMethod method, User user)
			throws ConnectionFailedException, UnknownDependencyException, InvalidPluginException,
			InvalidDescriptionException {
	    if (getSpigotUser() == null){
	        return;
        }
		List<Resource> premiums = getPurchasedResources(user);
		for (Resource premium : premiums) {
			if (premium.getResourceId() == resourceId) {
				// Resource id found
				SendConsole.info("Checking for new updates for: " + plugin.getName() + " ...");

				if (isUpdateAvailable(plugin, resourceId)) {
					SendConsole.info("An update for '" + plugin.getName() + "' is available");

					SendConsole.info("Getting download link ...");
					Resource premiumResource = getSpigotSiteAPI().getResourceManager()
							.getResourceById(premium.getResourceId(),getSpigotUser());

					switch (method) {
					case INSTALL_ON_RESTART:
						// Download the plugin to the update folder
						File pluginFile = null;
						try {
							pluginFile = new File(URLDecoder.decode(
									plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath(),
									"UTF-8"));
						} catch (UnsupportedEncodingException e) {

						}
						
						File outputFile = null;
						try{
							outputFile = new File(Bukkit.getUpdateFolderFile(), pluginFile.getName());
						}catch (Exception ex){
							
						}

						if (pluginFile != null && outputFile != null) {
							SendConsole.info("Downloading '" + plugin.getName() + "' ...");
							premiumResource.downloadResource(user, outputFile);

							SendConsole.info("An new update for " + plugin.getName() + " is ready to be installed on next restart!");
						}
						break;
					case DOWNLOAD_AND_INSTALL:
						// Remove the plugin
						SendConsole.info("Disabling '" + plugin.getName() + "' ...");
						Bukkit.getPluginManager().disablePlugin(plugin);
						pluginFile = null;
						try {
							pluginFile = new File(URLDecoder.decode(
									plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath(),
									"UTF-8"));
						} catch (UnsupportedEncodingException e) {

						}

						if (pluginFile != null) {
							SendConsole.info("Deleting '" + pluginFile.getName() + "' ...");
							pluginFile.delete();

							SendConsole.info("Downloading '" + plugin.getName() + "' ...");
							premiumResource.downloadResource(user, pluginFile);

							SendConsole.info("Loading '" + plugin.getName() + "' ...");
							Bukkit.getPluginManager().loadPlugin(pluginFile);
						}
						break;
					}
				}

				break;
			}
		}
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
    public SpigotUser authenticate(String username, String password) throws InvalidCredentialsException, TwoFactorAuthenticationException {
        UserManager userManager = api.getUserManager();
        SpigotUser user = (SpigotUser) userManager.authenticate(username, password);
        return user;
    }

	/**
	 * Authenticate spigot user
	 * 
	 * @param username
	 *            Username
	 * @param password
	 *            Password
	 * @param totpSecret
	 * 			  TOTP Secret
	 * @return SpigotUser
	 * @throws InvalidCredentialsException
	 *             Username or Password do not match
	 */
	public SpigotUser authenticate(String username, String password, String totpSecret) throws InvalidCredentialsException, TwoFactorAuthenticationException {
		UserManager userManager = api.getUserManager();
        if (totpSecret.equals("")) { totpSecret = null; }
		SpigotUser user = (SpigotUser) userManager.authenticate(username, password,totpSecret);
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
        if (getSpigotUser() == null){
            new ArrayList<Resource>();
        }
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
        if (getSpigotUser() == null){
            return false;
        }
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
		yamlBuilder.addPart("#   the session cookies.");
		yamlBuilder.addPart("# ---------------------------------------- #");
		yamlBuilder.addEmptyPart();
		yamlBuilder.addPart(" DO NOT EDIT THE CONFIG VERSION!");
		yamlBuilder.addPart("config", 2);
		yamlBuilder.addEmptyPart();
		yamlBuilder.addEmptyPart();
		yamlBuilder.addPart(" Enter your SpigotMC.org username");
		yamlBuilder.addPart("username", "");
		yamlBuilder.addEmptyPart();
		yamlBuilder.addPart(" Enter the corresponding password");
		yamlBuilder.addPart("password", "");
        yamlBuilder.addEmptyPart();
        yamlBuilder.addPart(" Enter the 2FA Secret if you have 2FA enabled");
        yamlBuilder.addPart(" See plugin page for more info");
        yamlBuilder.addPart("2fasecret", "");


        YamlStorage credentialStorage = new YamlStorage(this, "", "credentials", 2, yamlBuilder);
		String username = credentialStorage.getConfig().getString("username");
		String password = credentialStorage.getConfig().getString("password");
        String totpSecret = credentialStorage.getConfig().getString("2fasecret");
		Object cookiesObj = credentialStorage.getConfig().get("cookies");

		try {
			if (!password.equals("")) {
				SendConsole.info("Logging in with: " + username);
				user = authenticate(username, password,totpSecret);
				SendConsole.info("Succesfully logged in! ID=" + user.getUserId());
				SendConsole.info("Adding cookies to credentials.yml ...");
				credentialStorage.getConfig().set("cookies", user.getCookies());
				credentialStorage.getConfig().save(credentialStorage.getConfigFile());
				SendConsole.info("Saved the credentials.yml file!");

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
						user.setCookies(cookies);
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

	/**
	 * Get if there is a new update available
	 * 
	 * @param plugin
	 *            Plugin to update
	 * @param resourceId
	 *            Spigot Resource ID
	 * @return Boolean update available
	 */
	public boolean isUpdateAvailable(Plugin plugin, int resourceId) {
		Version pluginVersion = new Version(plugin.getDescription().getVersion());
		if (pluginVersion.compare(getResourceVersion(resourceId)) == 1) {
			return true;
		}
		return false;
	}
}
