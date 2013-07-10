package org.daverog.jaxrs.iodocs;

import java.util.LinkedHashMap;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class IoDocsParameter {

	public enum Location {
		query, pathReplace, header, body
	}

	public enum Type {
		STRING("string"), 
		INT("int"), 
		BOOLEAN("boolean"), 
		TEXTAREA("textarea");
		
		private String name;
		
		Type(String name) {
			this.name= name;
		}
	}

	private String name;
	private String description;
	private Location location;
	private Type type;
	private boolean required;
	private Object defaultValue;
	private List<String> enumeration;
	private List<String> enumDescriptions;
	
	public IoDocsParameter(String name, String description, Location location,
			Type type, boolean required, Object defaultValue) {
		this(name, description, location, type, required, defaultValue,
			Lists.<String>newArrayList(),
			Lists.<String>newArrayList());
	}
	
	public IoDocsParameter(String name, String description, Location location,
			Type type, boolean required, Object defaultValue,
			List<String> enumeration, List<String> enumDescriptions) {
		this.name = name;
		this.description = description;
		this.location = location;
		this.type = type;
		this.required = required;
		this.defaultValue = defaultValue;
		this.enumeration = enumeration;
		this.enumDescriptions = enumDescriptions;
	}

	public LinkedHashMap<String, Object> getData() {
		LinkedHashMap<String, Object> data = Maps.newLinkedHashMap();

		if (type == Type.BOOLEAN) {
			enumeration.clear();
			enumeration.add("true");
			enumeration.add("false");
			enumDescriptions.clear();
			enumDescriptions.add("True");
			enumDescriptions.add("False");
		}

		if (location == Location.body) {
			data.put("type", "textarea");
		} else {
			data.put("type", type.name);
		}
			
		data.put("location", location + "");
		data.put("description", description);
		data.put("default", defaultValue);
		
		if (required) data.put("required", true);

		if (!enumDescriptions.isEmpty()	&& enumeration.size() != enumDescriptions.size()) {
			data.put("warning", "Enumeration size ("
				+ enumeration.size()
				+ ") is not equal to enumeration description size ("
				+ enumDescriptions.size() + ")");
		}

		if (!enumeration.isEmpty()) {
			data.put("enum", enumeration);
			if (!enumDescriptions.isEmpty()) {
				data.put("enumDescriptions", enumDescriptions);
			}
		}
		
		return data;
	}

	public String getName() {
		switch (location) {
			case body: return "requestBody";
			case pathReplace: return ":" + name;
			default: return name;
		}
	}

	public void extend(List<IoDocsParameter> extensionParameters) {
		for (IoDocsParameter extension : extensionParameters) {
			if (extension.name.equals(name) &&
				extension.type == type &&
				extension.location == location) {
				if (description == null) description = extension.description;
				if (enumeration == null || enumeration.isEmpty()) {
					enumeration = extension.enumeration;
					enumDescriptions = extension.enumDescriptions;
				}
				if (!required) required = extension.required;
				if (defaultValue == null) defaultValue = extension.defaultValue;
			}
		}
	}

}
