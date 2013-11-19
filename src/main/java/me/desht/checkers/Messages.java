package me.desht.checkers;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.desht.dhutils.JARUtil;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.google.common.base.Joiner;

public class Messages {

	static Configuration fallbackMessages = null;
	static Configuration messages = null;

	public static void init(String locale) {
		File langDir = DirectoryStructure.getLanguagesDirectory();

		try {
			JARUtil ju = new JARUtil(CheckersPlugin.getInstance());
			for (String lang : MiscUtil.listFilesinJAR(ju.getJarFile(), "datafiles/lang", ".yml")) {
				ju.extractResource(lang, langDir);
			}
		} catch (IOException e) {
			LogUtils.severe("can't determine message files to extract!");
			e.printStackTrace();
		}

		try {
			fallbackMessages = loadMessageFile("default");
		} catch (CheckersException e) {
			LogUtils.severe("can't load fallback messages file!", e);
		}

		try {
			setMessageLocale(locale);
		} catch (CheckersException e) {
			LogUtils.warning("can't load messages for " + locale + ": using default");
			messages = fallbackMessages;
		}
	}

	public static void setMessageLocale(String wantedLocale) throws CheckersException {
		messages = loadMessageFile(wantedLocale);
	}

	private static Configuration loadMessageFile(String wantedLocale) throws CheckersException {
		File langDir = DirectoryStructure.getLanguagesDirectory();
		File wanted = new File(langDir, wantedLocale + ".yml");
		File located = locateMessageFile(wanted);
		if (located == null) {
			throw new CheckersException("Unknown locale '" + wantedLocale + "'");
		}
		YamlConfiguration conf;
		try {
			conf = MiscUtil.loadYamlUTF8(located);
		} catch (Exception e) {
			throw new CheckersException("Can't load message file [" + located + "]: " + e.getMessage());
		}

		// ensure that the config we're loading has all of the messages that the fallback has
		// make a note of any missing translations
		if (fallbackMessages != null && conf.getKeys(true).size() != fallbackMessages.getKeys(true).size()) {
			Map<String,String> missingKeys = new HashMap<String, String>();
			for (String key : fallbackMessages.getKeys(true)) {
				if (!conf.contains(key) && !fallbackMessages.isConfigurationSection(key)) {
					conf.set(key, fallbackMessages.get(key));
					missingKeys.put(key, fallbackMessages.get(key).toString());
				}
			}
			conf.set("NEEDS_TRANSLATION", missingKeys);
			try {
				conf.save(located);
			} catch (IOException e) {
				LogUtils.warning("Can't write " + located + ": " + e.getMessage());
			}
		}

		return conf;
	}

	private static File locateMessageFile(File wanted) {
		if (wanted == null) {
			return null;
		}
		if (wanted.isFile() && wanted.canRead()) {
			return wanted;
		} else {
			String basename = wanted.getName().replaceAll("\\.yml$", "");
			if (basename.contains("_")) {
				basename = basename.replaceAll("_.+$", "");
			}
			File actual = new File(wanted.getParent(), basename + ".yml");
			if (actual.isFile() && actual.canRead()) {
				return actual;
			} else {
				return null;
			}
		}
	}

	private static String getString(Configuration conf, String key) {
		String s = null;
		Object o = conf.get(key);
		if (o instanceof String) {
			s = o.toString();
		} else if (o instanceof List<?>) {
			@SuppressWarnings("unchecked")
			List<String> l = (List<String>) o;
			s = Joiner.on("\n").join(l);
		}
		return s;
	}

	public static List<String> getStringList(String key) {
		List<String> l;
		if (messages == null) {
			LogUtils.warning("**No messages catalog!?!");
			l = new ArrayList<String>();
			l.add("!" + key + "!");
		} else {
			l = messages.getStringList(key);
			if (l == null) {
				LogUtils.warning("Missing message key '" + key + "'");
				l = fallbackMessages.getStringList(key);
				if (l == null) {
					l = new ArrayList<String>();
					l.add("!" + key + "!");
				}
			}
		}
		return l;
	}

	public static String getString(String key) {
		if (messages == null) {
			LogUtils.warning("No messages catalog!?!");
			return "!" + key + "!";
		}
		String s = getString(messages, key);
		if (s == null) {
			LogUtils.warning("Missing message key '" + key + "'");
			s = getString(fallbackMessages, key);
			if (s == null) {
				s = "!" + key + "!";
			}
		}
		return s;
	}

	public static String getString(String key, Object... args) {
		try {
			return MessageFormat.format(getString(key), args);
		} catch (Exception e) {
			LogUtils.severe("Error fomatting message for " + key + ": " + e.getMessage());
			return getString(key);
		}
	}
}