# JAX-RS Mashery I/O Docs Generator

A library to generate Mashery I/O Docs JSON based on JAX-RS annotated classes.

It simply uses reflection to analyse JAX-RS annotated classes to build up information about the available endpoints, which can be used to generate the I/O Docs JSON.

As JAX-RS cannot provide all the information needed by I/O Docs, so a number of I/O Docs-specific annotations can be used in conjunction with JAX-RS to further enrich the documentation.

The library does not automatically determine which JAX-RS classes are deployed in the API. Although this is no doubt possible, I think this would tightly-couple this libary to particular versions of JAX-RS. Please let me know if you can think of a lightweight way of achieving this. For now, you will have to duplicate the list of JAX-RS classes in the CXF config, and in the I/O Docs generator call.

# Getting started

To generate the JSON String, do the following:

```java 
new IoDocsGenerator().generateIoDocs(
	"name",   
	"title", 
	"description", 
	"1.0", 
	"http://api.com/", 
	new Class<?>[]{
		EndpointOne.class,
		EndpointTwo.class,
	},
	Lists.<IoDocsParameter>newArrayList())
```

This can be integrated into RESTful endpoint, to allow the JSON to be accessed from the API itself:

```java
@GET
@Descriptions({@Description(value="Provide I/O Docs JSON", target= DocTarget.METHOD)})
@Produces({MimeTypes.APPLICATION_JSON})
public Response ioDocs(@Context Request request) {
	return Response.ok(new IoDocsGenerator().generateIoDocs(
		"My API", 
		"My API", 
		"Foo bar", 
		"1.0", 
		"http://bbc.api.mashery.com/my-api", 
		new Class<?>[]{
			EndpointOne.class,
			EndpointTwo.class,
		},
		Lists.<IoDocsParameter>newArrayList())
}
```

Ideally, there would be a mechanism for Mashery to consume the JSON periodically from a known URL like my-api/iodocs to allow the complete automation of the documentation. At the time of writing, this is not available.

# Features

* Recognises `HeaderParam`, `QueryParam`, `PathParam`, and the supported request body serialisation classes (`String`, `InputStream` etc..)
* Supports `Integer`, `Boolean` and `String` parameter types directly
* All other parameter types will use I/O Docs type 'string'  
* `Boolean` supported as "true"/"false" enumeration
* Supports default parameter values for the supported parameter types (`IoDocsDefaultBoolean`, `IoDocsDefaultString`, `IoDocsDefaultInteger`)
* Supports removal of selected methods or parameters from I/O docs using `IoDocsIgnore`
* Supports required properties using `IoDocsRequired`
* Supports enumeration values and descriptions using `IoDocsEnum(Description)`
* Supports parameter extensions to enhance regularly used parameters