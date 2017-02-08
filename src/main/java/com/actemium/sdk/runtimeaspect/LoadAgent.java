package com.actemium.sdk.runtimeaspect;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadAgent {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoadAgent.class);
	private static final AtomicBoolean DEJA_FAIT = new AtomicBoolean(false);

	protected static final AtomicReference<String> agentJar = new AtomicReference<>(null); 

	private LoadAgent() {}


	public static synchronized Instrumentation init(){
		if(DEJA_FAIT.get()){
			if (AgentSdk.isInstrumentationAvailable())
				return AgentSdk.getInstrumentation();
			else
				throw new AspectException("Deja initialisé, mais n'a pas pu aboutir une premiere fois...");
		}			
		try {
			DEJA_FAIT.set(true);
			final String agentPath = createAgent();
			final String pid = getProcessId();
			DynamicInstrumentationReflections.addPathToSystemClassLoader(agentPath);
			final JdkFilesFinder jdkFilesFinder = new JdkFilesFinder();
			final File toolsJar = jdkFilesFinder.findToolsJar();
			DynamicInstrumentationReflections.addPathToSystemClassLoader(toolsJar.getAbsolutePath());
			final File attachLib = jdkFilesFinder.findAttachLib();
			DynamicInstrumentationReflections.addPathToJavaLibraryPath(attachLib.getParentFile());
			VirtualMachine vm = VirtualMachine.attach(pid);
			if(!vm.getSystemProperties().contains("agent.sdk.installed"))
				vm.loadAgent(agentPath);
			return AgentSdk.getInstrumentation();
		} catch (Exception e) {
			throw new AspectException(e);
		}
	}

	/**
	 * retourne le process id de la machine virtuelle.
	 */

	private static String getProcessId() {
		final String vm = ManagementFactory.getRuntimeMXBean().getName();
		return vm.substring(0, vm.indexOf('@'));
	}


	/**
	 * cree un jar temporaire avec l'agent dedans
	 */
	private static String createAgent() {
		if(agentJar.get()==null) {
			synchronized(agentJar) {
				if(agentJar.get()==null) {

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
						agentJar.set(tmpFile.getAbsolutePath());
					} catch (IOException e) {
						throw new AspectException("Impossible d'ecrire le jar de l'agent.", e);
					}

				}
			}
		}
		return agentJar.get();
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

}
