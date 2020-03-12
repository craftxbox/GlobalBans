package com.craftxbox.globalbans.i18n;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class I18nStore {

	private HashMap<String,String> localeMap = new HashMap<>();
	
	I18nStore(String lang) {
		File localeFile = new File("locale/"+lang+".json");
		StringBuilder localeJsonBuilder = new StringBuilder();
		try (BufferedReader statusReader = new BufferedReader(new FileReader("status_list.tsv"))) {
			String currentLine = null;

			while ((currentLine = statusReader.readLine()) != null) {
				localeJsonBuilder.append(currentLine);
			}
		} catch (IOException e) {
			//TODO
		}
		String localeJson = localeJsonBuilder.toString();
		try {
			localeMap = new ObjectMapper().readValue(localeJson, new TypeReference<HashMap<String, String>>() {});
		} catch ( JsonProcessingException e) {
			throw new BadLocaleException();
		}
	}
	
	public String get(String key) {
		return localeMap.get(key);
	}
}
