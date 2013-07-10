package org.daverog.jaxrs.iodocs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.Descriptions;
import org.daverog.jaxrs.iodocs.IoDocsParameter.Location;
import org.daverog.jaxrs.iodocs.IoDocsParameter.Type;
import org.springframework.util.StringUtils;


import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class IoDocsGenerator {
	
	private static final List<Class<?>> validRequestBodyClasses = Lists.newArrayList(new Class<?>[]{
		java.lang.String.class,
		byte[].class,
		java.io.InputStream.class,
		java.io.Reader.class,
		java.io.File.class,
		javax.activation.DataSource.class,
		javax.xml.transform.Source.class,
		javax.xml.bind.JAXBElement.class,
		javax.ws.rs.core.MultivaluedMap.class,
		javax.ws.rs.core.StreamingOutput.class
	});
	
	/**
	 * Generate Mashery I/O Docs based on annotated JAX-RS classes
	 * 
	 * Features
	 * - Recognises HeaderParam, QueryParam, PathParam, and the 
	 *   supported request body serialisation classes (String, InputStream etc..)
	 * - Supports Integer, Boolean and String parameter types directly
	 * - All other parameter types will use I/O Docs type 'string'  
	 * - Boolean supported as "true"/"false" enumeration
	 * - Supports default parameter values for the supported parameter types
	 *   - IoDocsDefaultBoolean
	 *   - IoDocsDefaultString
	 *   - IoDocsDefaultInteger
	 * - Supports removal of selected methods or parameters from I/O docs using
	 *   - IoDocsIgnore
	 * - Supports required properties using IoDocsRequired
	 * - Supports Enumeration values and descriptions using IoDocsEnum(Description)
	 * - Supports parameter extensions to enhance regularly used 
	 *   parameters
	 * 
	 * @param name The name of the API (not shown to the public)
	 * @param title The title of the API
	 * @param description A more detailed description of the API
	 * @param version The current version of the API
	 * @param basePath The base path from which all paths are relative
	 * @param endpoints The classes that contain JAX-RS annotated RESTful methods
	 * @param extensionParameters If parameters match the name, location and type of
	 *        these parameters, they will inherit the description and enum values
	 * @return The I/O Docs JSON 
	 */
	public String generateIoDocs(
			String name, 
			String title, 
			String description, 
			String version, 
			String basePath, 
			Class<?>[] endpoints,
			List<IoDocsParameter> extensionParameters) {
		LinkedHashMap<String, Object> json = Maps.newLinkedHashMap();
		LinkedHashMap<String, Object> auth = Maps.newLinkedHashMap();
		LinkedHashMap<String, Object> key = Maps.newLinkedHashMap();
		LinkedHashMap<String, Object> resources = Maps.newLinkedHashMap();
		LinkedHashMap<String, Object> productMethods = Maps.newLinkedHashMap();
		LinkedHashMap<String, Object> methods = Maps.newLinkedHashMap();
		json.put("name", name);
		json.put("title", title);
		json.put("description", description);
		json.put("version", version);
		json.put("basePath", basePath);
		json.put("protocol", "rest");
		json.put("auth", auth);
		auth.put("key", key);
		key.put("location", "query");
		key.put("param", "api_key");
		json.put("resources", resources);
		resources.put("Product Methods", productMethods);
		productMethods.put("methods", methods);
		
		for (Class<?> endpoint : endpoints) {
			for (IoDocsMethod method : getMethodsFromEndpoint(endpoint)) {
				methods.put(method.getName(), 
						method.getData(extensionParameters));
			}
		}
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(json);
	}

	private <T> List<IoDocsMethod> getMethodsFromEndpoint(
			Class<T> jaxRsClass) {
		List<IoDocsMethod> methods = Lists.newArrayList();
		String endpointPath = "";
		if (jaxRsClass.getAnnotation(Path.class) != null) 
			endpointPath = jaxRsClass.getAnnotation(Path.class).value();
		
		for (Method method : jaxRsClass.getMethods()) {
			String name = jaxRsClass.getSimpleName()+"_"+method.getName();
			String httpMethod = null;
			String path = null;
			String description = null;
			List<IoDocsParameter> parameters = Lists.<IoDocsParameter>newArrayList();
			
			if(method.isAnnotationPresent(IoDocsIgnore.class)) continue;
			
			for (Annotation annotation : method.getAnnotations()) {
				if (annotation.annotationType().equals(IoDocsName.class)) {
					name = ((IoDocsName)annotation).value();
				} else if (annotation.annotationType().isAnnotationPresent(HttpMethod.class)) {
					String restMethodPath = endpointPath;
					if (method.getAnnotation(Path.class) != null) 
						restMethodPath = endpointPath + method.getAnnotation(Path.class).value();
					
					Descriptions descriptions = method.getAnnotation(Descriptions.class);

					httpMethod = annotation.annotationType().getAnnotation(HttpMethod.class).value();
					path = restMethodPath;
					
					if (descriptions != null) {
						String fullDescription = StringUtils.collectionToCommaDelimitedString(
								Lists.transform(Lists.newArrayList(descriptions.value()), 
										new Function<Description, String>(){
											public String apply(Description description) {
												return description.value();
											}
										}));
						if (!fullDescription.isEmpty())
							description = fullDescription;
					}
					
					for (int paramIndex = 0; paramIndex < method.getParameterTypes().length; paramIndex++) {
						Annotation[] parameterAnnotations = method.getParameterAnnotations()[paramIndex];

						boolean ignore = false;
						for (Annotation parameterAnnotation : parameterAnnotations) {
							if (parameterAnnotation.annotationType().equals(IoDocsIgnore.class))
								ignore = true;
						}						
						if (ignore) continue;
						
						Class<?> typeClass = method.getParameterTypes()[paramIndex];

						String paramName = null;
						String paramDescription = null;
						boolean required = false;
						Object defaultValue = null;
						Location location = null;
						Type paramType = getType(typeClass);
						List<String> enumeration = Lists.newArrayList();
						List<String> enumDescriptions = Lists.newArrayList();

						//Convert boolean to string "true", "false" enum
						//because I/O docs treats boolean as 0 or 1
						if (paramType == Type.BOOLEAN) {
							enumeration.add("true");
							enumeration.add("false");
							enumDescriptions.add("true");
							enumDescriptions.add("false");
							paramType = Type.STRING;
						}
						
						boolean isAParameter = false;
						
						for (Annotation parameterAnnotation : parameterAnnotations) {
							if (parameterAnnotation.annotationType().equals(QueryParam.class)) {
								isAParameter = true;
								paramName = ((QueryParam)parameterAnnotation).value();
								location = Location.query;
							} else if (parameterAnnotation.annotationType().equals(PathParam.class)) {
								isAParameter = true;
								paramName = ((PathParam)parameterAnnotation).value();
								location = Location.pathReplace;
							} else if (parameterAnnotation.annotationType().equals(HeaderParam.class)) {
								isAParameter = true;
								paramName = ((HeaderParam)parameterAnnotation).value();
								location = Location.header;
							} else if (parameterAnnotation.annotationType().equals(Description.class)) {
								paramDescription = ((Description)parameterAnnotation).value();
							} else if (parameterAnnotation.annotationType().equals(IoDocsDefaultBoolean.class)) {
								defaultValue = ((IoDocsDefaultBoolean)parameterAnnotation).value() + "";
							} else if (parameterAnnotation.annotationType().equals(IoDocsDefaultInteger.class)) {
								defaultValue = ((IoDocsDefaultInteger)parameterAnnotation).value();
							} else if (parameterAnnotation.annotationType().equals(IoDocsDefaultString.class)) {
								defaultValue = ((IoDocsDefaultString)parameterAnnotation).value();
							} else if (parameterAnnotation.annotationType().equals(IoDocsRequired.class)) {
								required = true;
							} else if (parameterAnnotation.annotationType().equals(IoDocsEnum.class)) {
								enumeration = Lists.newArrayList(((IoDocsEnum)parameterAnnotation).value());
							} else if (parameterAnnotation.annotationType().equals(IoDocsEnumDescriptions.class)) {
								enumDescriptions = Lists.newArrayList(((IoDocsEnumDescriptions)parameterAnnotation).value());
							}
							
						}
						
						if (!isAParameter && validRequestBodyClasses.contains(typeClass)) {
							paramName = "requestBody";
							paramType = Type.TEXTAREA;
							location = Location.body;
						}
						
						if (paramName != null)
							parameters.add(new IoDocsParameter(
								paramName, paramDescription, location, 
								paramType, required, defaultValue,
								enumeration, enumDescriptions));
					}
				}
			}
			
			if (httpMethod != null)
				methods.add(new IoDocsMethod(
					name, httpMethod, description, path, parameters));
		}
		return methods;
	}
	
	private Type getType(Class<?> typeClass) {
		if (Integer.class.isAssignableFrom(typeClass) || int.class.isAssignableFrom(typeClass))
			return Type.INT;
		if (Boolean.class.isAssignableFrom(typeClass) || boolean.class.isAssignableFrom(typeClass))
			return Type.BOOLEAN;
		return Type.STRING;
	}
	
}
