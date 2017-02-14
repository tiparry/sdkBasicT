package com.actemium.sdk.runtimeaspect;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.UUID;

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

	private static final String GENERE_LONG_SI_NULL_OU_EGAL_ZERO = "if(id == null || id == 0) id = java.util.UUID.randomUUID().getLeastSignificantBits();" + System.lineSeparator();
	private static final String GENERE_INT_SI_NULL_OU_EGAL_ZERO = "if(id == null || id == 0) id = java.util.UUID.randomUUID().clockSequence();" + System.lineSeparator();
	private static final String GENERE_STRING_SI_NULL_OU_VIDE = "if(id == null || id.isEmpty()) id = java.util.UUID.randomUUID().toString();" + System.lineSeparator();
	private static final String GENERE_UUID_SI_NULL = "if(id == null) id = java.util.UUID.randomUUID();" + System.lineSeparator();

	protected final String className;
	protected final ClassLoader classLoader;
	
	protected AspectException exception;


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
			return patchConstructors(className, loader, classfileBuffer);
		}
		return classfileBuffer;
	}


	public byte[] patchConstructors(String className, ClassLoader classLoader, byte[] byteCode) {
		String binName  = className.replace('/', '.');
		try {
			ClassPool cPool = new ClassPool(true);
			cPool.appendClassPath(new LoaderClassPath(classLoader));
			cPool.appendClassPath(new ByteArrayClassPath(binName, byteCode));
			CtClass ctClazz;
			ctClazz = cPool.get(binName);
			String newCode = codeAAjouter(ctClazz);
			if(newCode.isEmpty()){
				LOGGER.trace("la classe " + binName + "n'a pas d'attribut id et ne va donc pas être modifiée.");
				return byteCode;
			}
			int modifies = 0;
			LOGGER.trace("on va modifier les constructeurs de la classe " + className);
			for(CtConstructor constructor : ctClazz.getDeclaredConstructors()) {
				ctClazz.removeConstructor(constructor);
				constructor.insertAfter(newCode);
				ctClazz.addConstructor(constructor);
				modifies++;
			}
			LOGGER.trace("la classe " + binName + " a ete enregistre pour être gérée dans le GOM : " + modifies + " constructeurs modifies !");
			return ctClazz.toBytecode();
		} catch (NotFoundException | CannotCompileException | IOException e) {
			LOGGER.trace("Impossible de compiler la classe transformée[" + binName + "] : ", e);
			this.exception = new AspectException("Impossible de compiler la classe transformée[" + binName + "] .", e);
			return byteCode;
		}
	}

	private static String codeAAjouter(CtClass ctClazz) {
		StringBuilder sb = new StringBuilder();
		for(CtField field : ctClazz.getDeclaredFields()){
			if("id".equals(field.getName())){
				String typeid = null;
				try {
					typeid = field.getType().getName();
				} catch (NotFoundException e) {
					LOGGER.trace("attention, le type " + typeid + " est inconnu, il faut que le constructeur en génère un", e);
					return "com.actemium.sdk.GlobalObjectManager.getInstance().metEnCache(this,false, true);";
				}
				if(int.class.getName().equals(typeid) || Integer.class.getName().equals(typeid)){
					sb.append(GENERE_INT_SI_NULL_OU_EGAL_ZERO);
				}else if(long.class.getName().equals(typeid) || Long.class.getName().equals(typeid)){
					sb.append(GENERE_LONG_SI_NULL_OU_EGAL_ZERO);
				}else if(String.class.getName().equals(typeid)){
					sb.append(GENERE_STRING_SI_NULL_OU_VIDE);
				}else if(UUID.class.getName().equals(typeid)){
					sb.append(GENERE_UUID_SI_NULL);
				}else{
					LOGGER.trace("attention, le type " + typeid + " est inconnu, il faut que le constructeur en génère un");
				}
				sb.append("com.actemium.sdk.GlobalObjectManager.getInstance().metEnCache(this,false, true);");
				return sb.toString();
			}
		}
		return sb.toString();
	}
}