package com.lunatech.doclets.jax.jaxrs.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import com.lunatech.doclets.jax.Utils;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Tag;

public class ResourceMethod implements Comparable<ResourceMethod> {

	private static final Class<?>[] MethodAnnotations = new Class<?>[] {
			GET.class, POST.class, PUT.class, HEAD.class, DELETE.class };

	private MethodDoc declaringMethod;
	private String path;
	private ResourceClass resource;
	private ClassDoc declaringClass;
	private MethodDoc method;
	final Map<String, MethodParameter> pathParameters = new HashMap<String, MethodParameter>();
	final Map<String, MethodParameter> matrixParameters = new HashMap<String, MethodParameter>();
	final Map<String, MethodParameter> queryParameters = new HashMap<String, MethodParameter>();
	private List<AnnotationDesc> methods = new LinkedList<AnnotationDesc>();
	private AnnotationDesc producesAnnotation;
	private AnnotationDesc consumesAnnotation;

	private MethodParameter inputParameter;

	public ResourceMethod(MethodDoc method, MethodDoc declaringMethod,
			ResourceClass resource) {
		this.resource = resource;
		this.method = method;
		this.declaringClass = resource.getDeclaringClass();
		this.declaringMethod = declaringMethod;
		setupPath();
		setupParameters();
		setupMethods();
		setupMIMEs();
	}

	private void setupMIMEs() {
		producesAnnotation = Utils.findMethodAnnotation(declaringClass, method,
				Produces.class);
		consumesAnnotation = Utils.findMethodAnnotation(declaringClass, method,
				Consumes.class);
		if (producesAnnotation == null) {
			producesAnnotation = resource.getProducesAnnotation();
		}
		if (consumesAnnotation == null) {
			consumesAnnotation = resource.getConsumesAnnotation();
		}
	}

	private void setupMethods() {
		for (Class<?> methodAnnotation : MethodAnnotations) {
			final AnnotationDesc annotation = Utils.findMethodAnnotation(
					declaringClass, method, methodAnnotation);
			if (annotation != null)
				methods.add(annotation);
		}
	}

	private void setupParameters() {
		int i = -1;
		for (final Parameter parameter : method.parameters()) {
			i++;
			final AnnotationDesc pathParamAnnotation = Utils
					.findParameterAnnotation(declaringMethod, parameter, i,
							PathParam.class);
			if (pathParamAnnotation != null) {
				String name = (String) Utils
						.getAnnotationValue(pathParamAnnotation);
				pathParameters.put(name, new MethodParameter(parameter, i,
						pathParamAnnotation, MethodParameterType.Path,
						declaringMethod));
				continue;
			}
			final AnnotationDesc matrixParamAnnotation = Utils
					.findParameterAnnotation(declaringMethod, parameter, i,
							MatrixParam.class);
			if (matrixParamAnnotation != null) {
				String name = (String) Utils
						.getAnnotationValue(matrixParamAnnotation);
				matrixParameters.put(name, new MethodParameter(parameter, i,
						matrixParamAnnotation, MethodParameterType.Matrix,
						declaringMethod));
				continue;
			}
			final AnnotationDesc queryParamAnnotation = Utils
					.findParameterAnnotation(declaringMethod, parameter, i,
							QueryParam.class);
			if (queryParamAnnotation != null) {
				String name = (String) Utils
						.getAnnotationValue(queryParamAnnotation);
				queryParameters.put(name, new MethodParameter(parameter, i,
						queryParamAnnotation, MethodParameterType.Query,
						declaringMethod));
				continue;
			}
			final AnnotationDesc contextAnnotation = Utils
					.findParameterAnnotation(declaringMethod, parameter, i,
							Context.class);
			if (contextAnnotation == null) {
				this.inputParameter = new MethodParameter(parameter, i, null,
						MethodParameterType.Input, declaringMethod);
			}
		}
	}

	private void setupPath() {
		final AnnotationDesc pathAnnotation = Utils.findMethodAnnotation(
				declaringClass, method, Path.class);
		final String rootPath = (String) Utils.getAnnotationValue(resource
				.getRootPathAnnotation());

		if (pathAnnotation != null) {
			String path = (String) Utils.getAnnotationValue(pathAnnotation);
			this.path = Utils.appendURLFragments(rootPath, path);
		} else
			this.path = rootPath;
	}

	public int compareTo(ResourceMethod other) {
		return path.compareTo(other.path);
	}

	public String toString() {
		StringBuffer strbuf = new StringBuffer(path);
		strbuf.append(" ");
		for (AnnotationDesc method : methods) {
			strbuf.append(method.annotationType().name());
			strbuf.append(" ");
		}
		return strbuf.toString();
	}

	public List<String> getMethods() {
		List<String> httpMethods = new ArrayList<String>(methods.size());
		for (AnnotationDesc method : methods) {
			httpMethods.add(method.annotationType().name());
		}
		return httpMethods;
	}

	public List<String> getProduces() {
		if (producesAnnotation == null)
			return Collections.emptyList();
		List<String> producedMIMEs = new ArrayList<String>();
		for (String mime : Utils.getAnnotationValues(producesAnnotation)) {
			producedMIMEs.add(mime);
		}
		return producedMIMEs;
	}

	public List<String> getConsumes() {
		if (consumesAnnotation == null)
			return Collections.emptyList();
		List<String> consumedMIMEs = new ArrayList<String>();
		for (String mime : Utils.getAnnotationValues(consumesAnnotation)) {
			consumedMIMEs.add(mime);
		}
		return consumedMIMEs;
	}

	public MethodDoc getJavaDoc() {
		return declaringMethod;
	}

	public String getDoc() {
		return declaringMethod.commentText();
	}

	public Tag[] getDocFirstSentence() {
		return declaringMethod.firstSentenceTags();
	}

	public Map<String, MethodParameter> getQueryParameters() {
		return queryParameters;
	}

	public Map<String, MethodParameter> getPathParameters() {
		return pathParameters;
	}

	public Map<String, MethodParameter> getMatrixParameters() {
		return matrixParameters;
	}

	public String getPath() {
		return path;
	}

	public MethodParameter getInputParameter() {
		return inputParameter;
	}

	public boolean isGET() {
		for (AnnotationDesc method : methods) {
			if (method.annotationType().name().equals("GET"))
				return true;
		}
		return false;
	}

	public String getURL(Resource resource) {
		StringBuffer strbuf = new StringBuffer(resource.getAbsolutePath());
		Map<String, MethodParameter> queryParameters = getQueryParameters();
		if (!queryParameters.isEmpty()) {
			strbuf.append("?");
			boolean first = true;
			for (String name : queryParameters.keySet()) {
				if (!first)
					strbuf.append("&");
				strbuf.append(name);
				first = false;
			}
		}
		return strbuf.toString();
	}

}
