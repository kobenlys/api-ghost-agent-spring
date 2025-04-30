package com.apighost.agent.model;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class DtoSchema {

	private String className;
	private List<FieldMeta> fields;

	public DtoSchema(String className, List<FieldMeta> fields) {
		this.className = className;
		this.fields = fields;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public List<FieldMeta> getFields() {
		return fields;
	}

	public void setFields(List<FieldMeta> fields) {
		this.fields = fields;
	}

	/* 수정 사항
	* */
	public static DtoSchema fromClass(Class<?> clazz) {
		if (clazz.isPrimitive() || clazz.getName().startsWith("java.")) {
			return new DtoSchema(clazz.getSimpleName(), List.of());
		}

		List<FieldMeta> fieldMetas = new ArrayList<>();
		for (Field field : clazz.getDeclaredFields()) {
			fieldMetas.add(new FieldMeta(field.getName(), field.getType().getSimpleName()));
		}
		return new DtoSchema(clazz.getSimpleName(), fieldMetas);
	}
}
