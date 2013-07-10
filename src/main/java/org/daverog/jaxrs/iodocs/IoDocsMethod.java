package org.daverog.jaxrs.iodocs;

import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Maps;

public class IoDocsMethod {
	
	private String name;
	private String httpMethod;
	private String description;
	private String path;
	private List<IoDocsParameter> parameters;
	
	public IoDocsMethod(
			String name, String httpMethod, String description,
			String path, List<IoDocsParameter> parameters) {
		this.name = name;
		this.httpMethod = httpMethod;
		this.description = description;
		this.path = path;
		this.parameters = parameters;
	}

	public LinkedHashMap<String, Object> getData(
			List<IoDocsParameter> baseParameters) {
		LinkedHashMap<String, Object> method = Maps.newLinkedHashMap();
		
		method.put("httpMethod", httpMethod);
		method.put("path", path);
		
		if (!StringUtils.isBlank(description))
			method.put("description", description);

		LinkedHashMap<String, Object> parameterData = 
				getParameterData(baseParameters);
		
		if (!parameterData.isEmpty()) {
			method.put("parameters", parameterData);
		}
		
		return method;
	}

	private LinkedHashMap<String, Object> getParameterData(
			List<IoDocsParameter> extensionParameters) {
		LinkedHashMap<String, Object> parameterData = Maps.newLinkedHashMap();

		for (IoDocsParameter parameter : parameters){
			parameter.extend(extensionParameters);
			parameterData.put(parameter.getName(), parameter.getData());
		}
		
		return parameterData;
	}

	public String getName() {
		return name;
	}

}
