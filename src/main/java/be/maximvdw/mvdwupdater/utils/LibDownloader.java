package be.maximvdw.mvdwupdater.utils;


import be.maximvdw.mvdwupdater.ui.SendConsole;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class LibDownloader {
    private static final Class[] parameters = new Class[]{URL.class};

    public enum Library {
        HTMMLUNIT("http://repo.mvdw-software.be/content/groups/public/com/gargoylesoftware/HTMLUnit/2.15/HTMLUnit-2.15-OSGi.jar",
                "HTMLUnit 2.15", "Used for HTTP connections with cloudflare protected sites", "htmlunit_2_15");

        private String url = "";
        private String name = "";
        private String description = "";
        private String fileName = "";

        Library(String url, String name, String description, String fileName) {
            setUrl(url);
            setName(name);
            setDescription(description);
            setFileName(fileName);
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }

    /**
     * Download a library
     *
     * @param lib Library to download
     */
    public static void downloadLib(Library lib) {
        downloadLib(lib.getUrl(), lib.getName(), lib.getDescription(), lib.getFileName());
    }

    /**
     * Download a library
     *
     * @param url         URL to download it from
     * @param name        Name of the lib
     * @param description Description
     * @param fileName    filename to save it as
     */
    public static void downloadLib(String url, String name, String description, String fileName) {
        String localPath = "./plugins/MVdWPlugin/lib/" + fileName + ".jar";
        if (!(new File(localPath).exists())) {
            SendConsole.info("Downloading " + name + " ...");
            SendConsole.info("Description: " + description);
            try {
                HtmlUtils.downloadFile(url, localPath);
            } catch (IOException e) {
                SendConsole.severe("An error occured while downloading a required lib.");
                e.printStackTrace();
            }
        }
        SendConsole.info("Loading dependency " + name + " ...");
        try {
            addURL(new URL("jar:file:" + localPath + "!/"));
            SendConsole.info(name + " is now loaded!");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void addURL(URL u) throws IOException {

        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class sysclass = URLClassLoader.class;

        try {
            Method method = sysclass.getDeclaredMethod("addURL", parameters);
            method.setAccessible(true);
            method.invoke(sysloader, new Object[]{u});
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException("Error, could not add URL to system classloader");
        }

    }
}
