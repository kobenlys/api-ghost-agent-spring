package com.apighost.agent.model;

import java.util.List;

public class EndPoint {

	private String httpMethod;
	private String path;
	private List<String> produces;
	private List<String> consumes;
	private DtoSchema requestSchema;

	public EndPoint(String httpMethod, String path, List<String> produces, List<String> consumes,
		DtoSchema requestSchema) {
		this.httpMethod = httpMethod;
		this.path = path;
		this.produces = produces;
		this.consumes = consumes;
		this.requestSchema = requestSchema;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public List<String> getProduces() {
		return produces;
	}

	public void setProduces(List<String> produces) {
		this.produces = produces;
	}

	public List<String> getConsumes() {
		return consumes;
	}

	public void setConsumes(List<String> consumes) {
		this.consumes = consumes;
	}

	public DtoSchema getRequestSchema() {
		return requestSchema;
	}

	public void setRequestSchema(DtoSchema requestSchema) {
		this.requestSchema = requestSchema;
	}
}
