package com.actemium.sdk.runtimeaspect;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.spi.AttachProvider;

import sun.tools.attach.BsdVirtualMachine;
import sun.tools.attach.LinuxVirtualMachine;
import sun.tools.attach.SolarisVirtualMachine;
import sun.tools.attach.WindowsVirtualMachine;

public class LoadAgent {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoadAgent.class);
	private static final AtomicReference<Instrumentation> instrumentation = new AtomicReference<>();

	private static final AtomicBoolean DEJA_FAIT = new AtomicBoolean(false);
	private static final AttachProvider ATTACH_PROVIDER = new AttachProvider() {
		@Override  public String name() { return null; }
		@Override  public String type() { return null; }
		@Override  public VirtualMachine attachVirtualMachine(String id) { return null; }
		@Override  public List<VirtualMachineDescriptor> listVirtualMachines() { return null; }
	};
	public static final boolean HOTSPOT_VM;
	static
	{
		String vmName = System.getProperty("java.vm.name");
		HOTSPOT_VM = vmName.contains("HotSpot") || vmName.contains("OpenJDK");
	}

	private LoadAgent() {}


	public static synchronized Instrumentation init(){
		if(DEJA_FAIT.get()){
			if (instrumentation.get() != null)
				return instrumentation.get();
			else{
				LOGGER.trace("Deja initialisé, mais n'a pas pu aboutir une premiere fois...");
				throw new AspectException("Deja initialisé, mais n'a pas pu aboutir une premiere fois...");
			}
		}			
		DEJA_FAIT.set(true);
		final String agentPath = CreationBibliotheque.getAgent();
		DynamicInstrumentationReflections.addPathToSystemClassLoader(agentPath);
		VirtualMachine vm;

		LOGGER.debug("recupération de la Machine Virtuelle");
		if (AttachProvider.providers().isEmpty()) {
			if (HOTSPOT_VM) {
				LOGGER.debug("la machine virtuelle est une embarquée");
				vm = getVirtualMachineImplementationFromEmbeddedOnes();
			}
			else {
				LOGGER.trace("impossible d'obtenir une instance de machine virtuelle");
				throw new AspectException("impossible d'obtenir une instance de machine virtuelle");
			}
		}
		else {
			LOGGER.debug("récupération de la machine virtuelle qui tourne");
			vm = attachToRunningVM();
		}

		loadAgentAndDetachFromRunningVM(vm, agentPath);
		return instrumentation.get();
	}

	/**
	 * retourne le process id de la machine virtuelle.
	 */

	private static String getProcessId() {
		final String vm = ManagementFactory.getRuntimeMXBean().getName();
		return vm.substring(0, vm.indexOf('@'));
	}



	private static VirtualMachine getVirtualMachineImplementationFromEmbeddedOnes()
	{
		Class<? extends VirtualMachine> vmClass = findVirtualMachineClassAccordingToOS();
		String pid = getProcessId();

		try {
			// This is only done with Reflection to avoid the JVM pre-loading all the XyzVirtualMachine classes.
			Constructor<? extends VirtualMachine> vmConstructor = vmClass.getConstructor(AttachProvider.class, String.class);
			return vmConstructor.newInstance(ATTACH_PROVIDER, pid);
		}
		catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e){
			LOGGER.error("exception d'introspection...", e);
			throw new AspectException("exception d'introspection...", e);
		}
		catch (NoClassDefFoundError | UnsatisfiedLinkError e) {
			LOGGER.error("Native library for Attach API not available in this JRE", e);
			throw new AspectException("Native library for Attach API not available in this JRE", e);
		}
	}


	private static Class<? extends VirtualMachine> findVirtualMachineClassAccordingToOS()
	{
		if (File.separatorChar == '\\') {
			addAttachLibraryPath("windows", "attach.dll");
			return WindowsVirtualMachine.class;
		}

		String osName = System.getProperty("os.name");

		if (osName.startsWith("Linux") || osName.startsWith("LINUX")) {
			addAttachLibraryPath("linux", "libattach.so");
			return LinuxVirtualMachine.class;
		}

		if(osName.startsWith("Mac OS X")){
			addAttachLibraryPath("mac", "libattach.dylib");
			return BsdVirtualMachine.class;
		}

		if (osName.contains("FreeBSD"))
			return BsdVirtualMachine.class;

		if (osName.startsWith("Solaris") || osName.contains("SunOS")) {
			addAttachLibraryPath("solaris", "libattach.so");
			return SolarisVirtualMachine.class;
		}

		LOGGER.trace("Cannot use Attach API on unknown OS: " + osName);
		throw new AspectException("Cannot use Attach API on unknown OS: " + osName);
	}

	private static void addAttachLibraryPath(String os, String nomBibliotheque) {
		try{
			System.loadLibrary("attach");
		}catch(UnsatisfiedLinkError e){
			LOGGER.info("on est sur un JRE et non sur un JDK, on ajoute la librairie Attach à la main car la commande System.loadLibrary(\"attach\") donne l'erreur suivante", e);
			try {
				LOGGER.info("creation de la bibliotheque temporaire " + nomBibliotheque);
				String pathToAdd = CreationBibliotheque.getAttachBibl(os, nomBibliotheque);
				Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
				usrPathsField.setAccessible(true);
				final String[] paths = (String[])usrPathsField.get(null);

				for(String path : paths) {
					if(path.equals(pathToAdd)) {
						return;
					}
				}
				final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
				newPaths[newPaths.length-1] = pathToAdd;
				usrPathsField.set(null, newPaths);
				System.loadLibrary("attach");
				LOGGER.info("la bibliothèque attach est bien ajoutée au systeme path");
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException | UnsatisfiedLinkError e1) {
				LOGGER.error("impossible d'ajouter la bibliotheque attach à chaud...", e1);
				throw new AspectException("impossible d'ajouter la bibliotheque attach à chaud...", e1);
			}
		}
	}



	private static VirtualMachine attachToRunningVM()
	{
		String pid = getProcessId();

		try {
			return VirtualMachine.attach(pid);
		}
		catch (AttachNotSupportedException | IOException e) {
			LOGGER.error("impossible de s'attacher à la VM", e);
			throw new AspectException("impossible de s'attacher à la VM", e);
		}
	}

	private static void loadAgentAndDetachFromRunningVM(VirtualMachine vm, String jarFilePath)
	{
		try {
			LOGGER.debug("charge agent...");
			vm.loadAgent(jarFilePath);
			LOGGER.debug("agent chargé");
			instrumentation.set(getInstrumentation());
			LOGGER.debug("instrumentation récupérée! ");
			vm.detach();
			LOGGER.debug("agent detachée !");
		}
		catch (AgentLoadException | AgentInitializationException | IOException e) {
			LOGGER.error("Impossible de charger l'agent dans la VM ou de la détacher : " + jarFilePath, e);
			throw new AspectException("Impossible de charger l'agent dans la VM ou de la détacher : " + jarFilePath, e);
		}
	}


	private static Instrumentation getInstrumentation() {
		try{
			//obligatoire de passer par introspection via System classloader si lancé dans un thread avec un autre classloader 
			Class<?> clazz = Class.forName("com.actemium.sdk.runtimeaspect.AgentSdk", true, ClassLoader.getSystemClassLoader());
			Field f = clazz.getDeclaredField("instrumentation");
			f.setAccessible(true);
			Instrumentation ret = (Instrumentation) f.get(null);
			if(ret == null)
				throw new AspectException("l'instrumentation récupérée est null");
			return ret;
		}catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException | ClassNotFoundException e){
			LOGGER.error("impossible de récupérer l'instrumentation en introspection...", e);
			throw new AspectException("impossible de récupérer l'instrumentation en introspection...", e);
		}
	}
	
}
