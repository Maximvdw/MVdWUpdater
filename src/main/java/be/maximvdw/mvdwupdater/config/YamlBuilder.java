package be.maximvdw.mvdwupdater.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * YamlBuilder
 * 
 * YAML Builder
 * 
 * @project MVdW Software Plugin Interface
 * @version 25-08-2014
 * @author Maxim Van de Wynckel (Maximvdw)
 * @site http://www.mvdw-software.com/
 */
public class YamlBuilder {
	// Yaml parts
	private List<YamlPart> parts = new ArrayList<YamlPart>();

	public YamlBuilder() {

	}

	public boolean writeToFile(File file) {
		FileOutputStream fop = null;

		try {
			fop = new FileOutputStream(file);

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			// get the content in bytes
			byte[] contentInBytes = toString().getBytes();

			fop.write(contentInBytes);
			fop.flush();
			fop.close();

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				if (fop != null) {
					fop.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	public void loadConfig(YamlConfiguration config) {
		for (YamlPart part : getParts()) {
			if (part instanceof YamlSectionPart) {
				((YamlSectionPart) part).loadConfig("", config);
			} else if (part instanceof YamlKeyValuePart) {
				((YamlKeyValuePart) part).loadConfig("", config);
			}
		}
	}

	/**
	 * Add a YAML part
	 * 
	 * @param part
	 *            YAML part
	 * @return Yaml Builder
	 */
	public YamlBuilder addPart(YamlPart part) {
		parts.add(part);
		return this;
	}

	/**
	 * Add a YAML part
	 * 
	 * @param part
	 *            comment
	 * @return Yaml Builder
	 */
	public YamlBuilder addPart(String comment) {
		parts.add(new YamlCommentPart(comment));
		return this;
	}

	/**
	 * Add an empty YAML part
	 * 
	 * @return Yaml builder
	 */
	public YamlBuilder addEmptyPart() {
		parts.add(new YamlEmptyPart());
		return this;
	}

	/**
	 * Add a part
	 * 
	 * @param key
	 *            Key
	 * @param value
	 *            Value
	 * @return YamlBuilder
	 */
	public YamlBuilder addPart(String key, Object value) {
		parts.add(new YamlKeyValuePart(key, value));
		return this;
	}

	/**
	 * Get Value
	 * 
	 * @param path
	 *            Path
	 * @return Object
	 */
	public Object getValue(String path) {
		String[] keys = path.split(".");
		if (keys.length == 0)
			return null;
		for (YamlPart part : getParts()) {
			if (part instanceof YamlSectionPart) {
				if (keys.length > 1) {
					String newPath = keys[1];
					for (int i = 2; i < keys.length; i++) {
						newPath += "." + keys[i];
					}
					return getValue(newPath);
				} else {
					return null;
				}
			} else if (part instanceof YamlKeyValuePart && keys.length == 1) {
				if (((YamlKeyValuePart) part).getKey()
						.equalsIgnoreCase(keys[0])) {
					return ((YamlKeyValuePart) part).getValue();
				}
			}
		}

		return null;
	}

	/**
	 * Get YAML Parts
	 * 
	 * @return yaml parts
	 */
	public List<YamlPart> getParts() {
		return parts;
	}

	/**
	 * Set YAML Parts
	 * 
	 * @param parts
	 *            yaml parts
	 */
	public void setParts(List<YamlPart> parts) {
		this.parts = parts;
	}

	@Override
	public String toString() {
		String result = "";
		for (YamlPart part : getParts()) {
			result += part.toString() + System.getProperty("line.separator");
		}
		return result;
	}

	public static class YamlPart {

	}

	public static class YamlCommentPart extends YamlPart {
		private String comment = "";

		public YamlCommentPart(String comment) {
			setComment(comment);
		}

		public String getComment() {
			return comment;
		}

		public void setComment(String comment) {
			this.comment = comment;
		}

		@Override
		public String toString() {
			return "#" + comment;
		}
	}

	public static class YamlKeyValuePart extends YamlPart {
		private String key = "";
		private Object value = "";

		public YamlKeyValuePart(String key, Object value) {
			setKey(key);
			setValue(value);
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		@Override
		public String toString() {
			if (value instanceof String) {
				return new YamlStringPart(key, value.toString()).toString();
			}
			return getKey() + ": " + value;
		}

		public void loadConfig(String path, YamlConfiguration config) {
			setValue(config.get(path + "." + getKey()));
		}
	}

	public static class YamlStringPart extends YamlKeyValuePart {

		public YamlStringPart(String key, String value) {
			super(key, value);
		}

		@Override
		public String toString() {
			if (getValue().toString().contains("\""))
				return getKey() + ": '" + getValue() + "'";
			if (getValue().toString().contains("'"))
				return getKey() + ": \"" + getValue() + "\"";
			return getKey() + ": '"
					+ getValue().toString().replace("\"", "\\\"") + "'";
		}
	}

	public static class YamlIntegerPart extends YamlKeyValuePart {

		public YamlIntegerPart(String key, int value) {
			super(key, value);
		}

	}

	public static class YamlEmptyPart extends YamlPart {
		@Override
		public String toString() {
			return "";
		}
	}

	public static class YamlStringListPart extends YamlPart {
		private String key = "";
		private List<String> items = new ArrayList<String>();

		public YamlStringListPart(String key) {
			setKey(key);
		}

		public YamlStringListPart addItem(String item) {
			items.add(item);
			return this;
		}

		public List<String> getItems() {
			return items;
		}

		public void setItems(List<String> items) {
			this.items = items;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public void loadConfig(String path, YamlConfiguration config) {
			setItems(config.getStringList(path + "." + getKey()));
		}

		public String toString() {
			String result = getKey() + ": ";
			if (getItems().size() == 0)
				result += "[]" + System.getProperty("line.separator");
			else {
				result += System.getProperty("line.separator");
				for (String item : getItems()) {
					if (item.contains("\""))
						result += "- '" + item + "'"
								+ System.getProperty("line.separator");
					if (item.contains("'"))
						result += "- \"" + item + "\""
								+ System.getProperty("line.separator");

					result += "- '" + item + "'"
							+ System.getProperty("line.separator");

				}
			}
			return result;
		}
	}

	public static class YamlSectionPart extends YamlPart {
		private String key = "";
		// Yaml parts
		private List<YamlPart> parts = new ArrayList<YamlPart>();

		public YamlSectionPart(String key) {
			setKey(key);
		}

		public List<YamlPart> getParts() {
			return parts;
		}

		public void setParts(List<YamlPart> parts) {
			this.parts = parts;
		}

		/**
		 * Add a YAML part
		 * 
		 * @param part
		 *            YAML part
		 * @return Yaml Builder
		 */
		public YamlSectionPart addPart(YamlPart part) {
			parts.add(part);
			return this;
		}

		/**
		 * Add a YAML part
		 * 
		 * @param part
		 *            comment
		 * @return Yaml Builder
		 */
		public YamlSectionPart addPart(String comment) {
			parts.add(new YamlCommentPart(comment));
			return this;
		}

		/**
		 * Add an empty YAML part
		 * 
		 * @return Yaml builder
		 */
		public YamlSectionPart addEmptyPart() {
			parts.add(new YamlEmptyPart());
			return this;
		}

		/**
		 * Add a part
		 * 
		 * @param key
		 *            Key
		 * @param value
		 *            Value
		 * @return YamlBuilder
		 */
		public YamlSectionPart addPart(String key, Object value) {
			parts.add(new YamlKeyValuePart(key, value));
			return this;
		}

		/**
		 * Does the yaml part has parts
		 * 
		 * @return Parts
		 */
		public boolean hasParts() {
			return parts.size() > 0;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String toString() {
			String result = getKey() + ":"
					+ System.getProperty("line.separator");
			for (YamlPart part : getParts()) {
				String[] lines = part.toString().split(
						System.getProperty("line.separator"));
				for (String line : lines) {
					result += "  " + line
							+ System.getProperty("line.separator");
				}
			}

			return result;
		}

		public Object getValue(String path) {
			String[] keys = path.split(".");
			if (keys.length == 0)
				return null;
			for (YamlPart part : getParts()) {
				if (part instanceof YamlSectionPart) {
					if (keys.length > 1) {
						String newPath = keys[1];
						for (int i = 2; i < keys.length; i++) {
							newPath += "." + keys[i];
						}
						return getValue(newPath);
					} else {
						return null;
					}
				} else if (part instanceof YamlKeyValuePart && keys.length == 1) {
					if (((YamlKeyValuePart) part).getKey().equalsIgnoreCase(
							keys[0])) {
						return ((YamlKeyValuePart) part).getValue();
					}
				}
			}

			return null;
		}

		public void loadConfig(String path, YamlConfiguration config) {
			for (YamlPart part : getParts()) {
				if (part instanceof YamlSectionPart) {
					((YamlSectionPart) part).loadConfig((path.equals("") ? ""
							: ".") + getKey(), config);
				} else if (part instanceof YamlKeyValuePart) {
					((YamlKeyValuePart) part).loadConfig((path.equals("") ? ""
							: ".") + getKey(), config);
				}
			}
		}
	}
}
