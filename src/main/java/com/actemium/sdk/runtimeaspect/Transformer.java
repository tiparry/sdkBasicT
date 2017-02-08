package com.actemium.sdk.runtimeaspect;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.HashSet;
import java.util.Set;

public class Transformer {

	private final Instrumentation instrumentation;

	private final Set<Class<?>> classesDejaTransformees = new HashSet<>();

	public Transformer(Instrumentation instrumentation) {
		this.instrumentation = instrumentation;
	}

	public synchronized void transform(Class<?> clazz) {
		if(classesDejaTransformees.contains(clazz))
			return;
		classesDejaTransformees.add(clazz);
		PatchConstructor dt = new PatchConstructor(clazz);
		instrumentation.addTransformer(dt, true);
		try {
			instrumentation.retransformClasses(clazz);
		} catch (UnmodifiableClassException e) {
			throw new AspectException("Failed to transform [" + clazz.getName() + "]", e);
		} finally {
			instrumentation.removeTransformer(dt);
		}       
	}
}