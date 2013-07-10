package org.daverog.jaxrs.iodocs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.Descriptions;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.daverog.jaxrs.iodocs.IoDocsParameter.Location;
import org.daverog.jaxrs.iodocs.IoDocsParameter.Type;
import org.junit.Test;

import com.google.common.collect.Lists;

public class IoDocsTest {
	
	@Test
	public void a_couple_of_APIs_are_correctly_converted_to_IO_Docs_JSON() {
		assertEquals(
			loadClasspathResourceAsString("ping.json"), 
			new IoDocsGenerator().generateIoDocs(
					"name", 
					"title", 
					"description", 
					"1.0", 
					"http://api.com/", 
					new Class<?>[]{PingApi.class, QueryApi.class},
					Lists.<IoDocsParameter>newArrayList(new IoDocsParameter(
							"Accept", "Accept mime-type", 
							Location.header, Type.STRING, true, 
							"text/plain", 
							Lists.<String>newArrayList("text/plain"), 
							Lists.<String>newArrayList("Plain text")))));
	}
	
	@Test
	public void enumerations_are_supported() {
		assertEquals(
			loadClasspathResourceAsString("enum.json"), 
			new IoDocsGenerator().generateIoDocs(
				"name", 
				"title", 
				"description", 
				"1.0", 
				"http://api.com/", 
				new Class<?>[]{EnumApi.class},
				Lists.<IoDocsParameter>newArrayList()));
	}
	
	@Test
	public void path_parameters_are_supported() {
		assertEquals(
			loadClasspathResourceAsString("path-param.json"), 
			new IoDocsGenerator().generateIoDocs(
				"name", 
				"title", 
				"description", 
				"1.0", 
				"http://api.com/", 
				new Class<?>[]{PathApi.class},
				Lists.<IoDocsParameter>newArrayList()));
	}

	@Test
	public void header_parameters_are_supported() {
		assertEquals(
			loadClasspathResourceAsString("header-param.json"), 
			new IoDocsGenerator().generateIoDocs(
				"name", 
				"title", 
				"description", 
				"1.0", 
				"http://api.com/", 
				new Class<?>[]{HeaderApi.class},
				Lists.<IoDocsParameter>newArrayList()));
	}
	
	@Test
	public void request_body_can_be_PUT() {
		assertEquals(
			loadClasspathResourceAsString("put-body.json"), 
			new IoDocsGenerator().generateIoDocs(
				"name", 
				"title", 
				"description", 
				"1.0", 
				"http://api.com/", 
				new Class<?>[]{PutBodyApi.class},
				Lists.<IoDocsParameter>newArrayList()));
	}
	
	@Test
	public void if_the_size_of_the_enumeration_is_different_to_the_size_of_the_corresponding_descriptions_a_warning_is_added_to_the_json() {
		String ioDocsWithEnumMismatch = new IoDocsGenerator().generateIoDocs(
				"name", "title", 
				"description", "1.0", 
				"http://api.com/", 
				new Class<?>[]{EnumMismatch.class},
				Lists.<IoDocsParameter>newArrayList());
		assertTrue(	
			ioDocsWithEnumMismatch,
			ioDocsWithEnumMismatch.contains(
			"\"warning\": \"Enumeration size (2) is not equal to enumeration description size (3)\""));
	}
	
	@Path("/ping")
	public class PingApi {
		@GET
		@Descriptions({
			@Description(value="Check that the API is active", target= DocTarget.METHOD)
		})
		public void ping(@Context Request request) {}
	}
	
	@Path("/query")
	public class QueryApi {
		@GET
		@Path("/resource")
		@IoDocsName("queryName")
		@Descriptions({
			@Description(value="Run a query", target= DocTarget.METHOD)
		})
		public void query(
		  @Description("Param1 does something") 
		  @QueryParam("param1") 
		  @IoDocsRequired
		  @IoDocsDefaultInteger(2) 
		  int param1,
		  @QueryParam("paramIgnore") 
		  @IoDocsIgnore
		  int paramIgnore,
		  @Description("Param2 does something") 
		  @QueryParam("param2") 
		  @IoDocsDefaultBoolean(true) 
		  boolean param2,
		  @HeaderParam("Accept") String accept) {}
		
		@GET
		@Path("/this-should-not-be-include")
		@IoDocsIgnore
		public void query(){}
	}
	
	@Path("/enum")
	public class EnumApi {
		@GET
		public void query(
			@QueryParam("enum") 
			@IoDocsEnum({"a", "b"}) 
			@IoDocsEnumDescriptions({"Letter a", "Letter b"}) 
			boolean enumeration) {}
	}
	
	@Path("/enum-mismatch")
	public class EnumMismatch {
		@GET
		public void query(
			@QueryParam("Enum mismatch") 
			@IoDocsEnum({"a", "b"}) 
			@IoDocsEnumDescriptions({"a", "b", "c"}) 
			String enumMismatch) {}
	}
	
	@Path("/path/:pathParam")
	public class PathApi {
		@GET
		public void query(
			@PathParam("pathParam") 
			String pathParam) {}
	}
	
	@Path("/resource")
	public class HeaderApi {
		@GET
		public void query(
				@HeaderParam("Accept") 
				String accept) {}
	}
	
	@Path("/resource")
	public class PutBodyApi {
		@PUT
		public void create(String bodyData) {}
	}
	
	public static String loadClasspathResourceAsString(String filename) {
		try {
			return IOUtils.toString(ClassLoader.getSystemResourceAsStream(filename), "UTF-8");
		}
		catch (Exception e) {
			fail("Could not load resource from classpath '" + filename + "': " + e.getMessage());
			return "";
		}
	}
	
}
