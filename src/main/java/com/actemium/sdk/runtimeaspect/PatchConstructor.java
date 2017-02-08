package com.actemium.sdk.runtimeaspect;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

public class PatchConstructor implements ClassFileTransformer {
	private static final Logger LOGGER = LoggerFactory.getLogger(PatchConstructor.class);

	protected final String className;
	protected final ClassLoader classLoader;


	public PatchConstructor(Class<?> clazz) {
		this.className = clazz.getName().replace('.', '/');
		classLoader = clazz.getClassLoader();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader, java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
	 */
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		if(className.equals(this.className) && loader.equals(classLoader)) {
			return instrument(className, loader, classfileBuffer);
		}
		return classfileBuffer;
	}


	public static byte[] instrument(String className, ClassLoader classLoader, byte[] byteCode) {
		String binName  = className.replace('/', '.');
		try {
			ClassPool cPool = new ClassPool(true);
			cPool.appendClassPath(new LoaderClassPath(classLoader));
			cPool.appendClassPath(new ByteArrayClassPath(binName, byteCode));
			CtClass ctClazz;
			ctClazz = cPool.get(binName);
			if(!hasIdAttribute(ctClazz)){
				LOGGER.info("la classe " + binName + "n'a pas d'attribut id et ne va donc pas être modifiée.");
				return byteCode;
			}
			int modifies = 0;
			for(CtConstructor constructor : ctClazz.getDeclaredConstructors()) {
				ctClazz.removeConstructor(constructor);
				String newCode = "com.actemium.sdk.GlobalObjectManager.getInstance().metEnCache(this,false, true);";
				constructor.insertAfter(newCode);
				ctClazz.addConstructor(constructor);
				modifies++;
			}
			LOGGER.info("la classe " + binName + " a ete enregistre pour être gérée dans le GOM : " + modifies + " constructeurs modifies !");
			return ctClazz.toBytecode();
		} catch (NotFoundException | CannotCompileException | IOException e) {
			LOGGER.error("Impossible de compiler la classe transformée[" + binName + "] : ", e);
			throw new AspectException("Impossible de compiler la classe transformée[" + binName + "] : ", e);
		}
	}

	private static boolean hasIdAttribute(CtClass ctClazz) {
		for(CtField field : ctClazz.getDeclaredFields()){
			if("id".equals(field.getName()))
				return true;
		}
		return false;
	}
}