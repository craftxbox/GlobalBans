package com.craftxbox.globalbans.i18n;

import java.io.File;
import java.util.HashMap;

public class I18nLibrary {

	public static I18nStore EN_US = new I18nStore("en_US");
	
	private HashMap<String,I18nStore> localesAvailable = new HashMap<>();
	
	public I18nLibrary() {
		File localesDir = new File("locale/");
		File[] locales = localesDir.listFiles();
		for(File i : locales) {
			localesAvailable.put(i.getName().split("\\.")[0], new I18nStore(i.getName().split("\\.")[0]));
		}
	}
	
	public I18nStore get(String locale) {
		return localesAvailable.get(locale);
	}
	
	public void reload() {
		File localesDir = new File("locale/");
		File[] locales = localesDir.listFiles();
		for(File i : locales) {
			localesAvailable.put(i.getName().split("\\.")[0], new I18nStore(i.getName().split("\\.")[0]));
		}
	}
}
