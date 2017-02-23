package com.actemium.sdk.runtimeaspect;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreationBibliotheque {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreationBibliotheque.class);

	private static final AtomicReference<String> AGENT_JAR = new AtomicReference<>(null); 
	private static final AtomicReference<String> ATTACH_LIB = new AtomicReference<>(null);

	private CreationBibliotheque(){/*hide constructor*/}

	/**
	 * cree un jar temporaire avec l'agent dedans
	 */
	static String getAgent() {
		if(AGENT_JAR.get()==null) {
			synchronized(AGENT_JAR) {
				if(AGENT_JAR.get()==null) {

					File tmpFile;
					Manifest mf;
					try {
						tmpFile = File.createTempFile(AgentSdk.class.getName(), ".jar");
						StringBuilder manifest = new StringBuilder();
						manifest.append("Manifest-Version: 1.0\nAgent-Class: " + AgentSdk.class.getName() + "\n");
						manifest.append("Can-Redefine-Classes: true\n");
						manifest.append("Can-Retransform-Classes: true\n");
						manifest.append("Premain-Class: " + AgentSdk.class.getName() + "\n");
						ByteArrayInputStream bais = new ByteArrayInputStream(manifest.toString().getBytes());
						mf = new Manifest(bais);
					} catch (IOException e) {
						throw new AspectException("Impossible d'ecrire le jar de l'agent.", e);
					}
					try (FileOutputStream fos = new FileOutputStream(tmpFile, false);
							JarOutputStream jos = new JarOutputStream(fos, mf)){
						LOGGER.info("Temp File:" + tmpFile.getAbsolutePath());
						tmpFile.deleteOnExit();		
						addClassesToJar(jos, AgentSdk.class);
						jos.flush();
						fos.flush();
						AGENT_JAR.set(tmpFile.getAbsolutePath());
					} catch (IOException e) {
						throw new AspectException("Impossible d'ecrire le jar de l'agent.", e);
					}

				}
			}
		}
		return AGENT_JAR.get();
	}

	/**
	 * Ajoute les classes au jar
	 */
	private static void addClassesToJar(JarOutputStream jos, Class<?>...classes) throws IOException {
		for(Class<?> clazz: classes) {
			jos.putNextEntry(new ZipEntry(clazz.getName().replace('.', '/') + ".class"));
			jos.write(getClassBytes(clazz));
			jos.flush();
			jos.closeEntry();
		}
	}

	/**
	 * retourne le bytecode d'une classe
	 */
	private static byte[] getClassBytes(Class<?> clazz) {
		try(InputStream is = clazz.getClassLoader().getResourceAsStream(clazz.getName().replace('.', '/') + ".class")) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(is.available());
			byte[] buffer = new byte[8092];
			int bytesRead = -1;
			while((bytesRead = is.read(buffer))!=-1) {
				baos.write(buffer, 0, bytesRead);
			}
			baos.flush();
			return baos.toByteArray();
		} catch (Exception e) {
			throw new AspectException("impossible de lire le bytecode de la classe " + clazz.getName(), e);
		}
	}

	static synchronized String getAttachBibl(String os, String nomBibliotheque) {
		if(ATTACH_LIB.get() == null) {
			File tmpDir;
			File attachLib;
			try {
				tmpDir = Files.createTempDirectory("attachDir").toFile();
				tmpDir.deleteOnExit();
				attachLib = new File(tmpDir, nomBibliotheque);
				attachLib.deleteOnExit();
			} catch (IOException e) {
				LOGGER.error("impossible de creer la bibliothèque temporaire d'attach : " + nomBibliotheque , e);
				throw new AspectException("impossible de creer la bibliothèque temporaire d'attach : " + nomBibliotheque , e);
			}
			String nomStream = getNomStream(os, nomBibliotheque);
			try(InputStream is = CreationBibliotheque.class.getResourceAsStream(nomStream);
					FileOutputStream fos = new FileOutputStream(attachLib, false)){
				if(is == null)
					throw new AspectException("bibliothèque non supportée : " + nomStream);
				int bytesRead = -1;
				byte[] buffer = new byte[8 * 1024];
				while((bytesRead = is.read(buffer))!=-1) {
					fos.write(buffer, 0, bytesRead);
				}
				fos.flush();
				LOGGER.info("Temp attach File:" + attachLib.getAbsolutePath());
				ATTACH_LIB.set(tmpDir.getAbsolutePath());
			} catch (IOException e) {
				LOGGER.error("impossible de copier la bibliothèque temporaire d'attach : " + nomStream , e);
				throw new AspectException("impossible de copier la bibliothèque temporaire d'attach : " + nomStream , e);
			}
		}
		return ATTACH_LIB.get();
	}

	private static String getNomStream(String osType, String nomBibliotheque) {
		StringBuilder sb = new StringBuilder("/attachlib/");
		

		String os = System.getProperty("sun.arch.data.model");
		if("32".equals(os))
			sb.append("v32bits/");
		else if("64".equals(os))
			sb.append("v64bits/");
		else{
			LOGGER.error("Version d'OS non pris en charge pour la librairie Attach");
			throw new AspectException("Version d'OS non pris en charge pour la librairie Attach");
		}
		sb.append(osType).append("/").append(nomBibliotheque);
		LOGGER.info("la librairie demandée est " + sb.toString());
		return sb.toString();
	}

}
