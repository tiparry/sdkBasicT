package com.actemium.sdk.runtimeaspect;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
			if (AgentSdk.isInstrumentationAvailable())
				return AgentSdk.getInstrumentation();
			else{
				LOGGER.trace("Deja initialisé, mais n'a pas pu aboutir une premiere fois...");
				throw new AspectException("Deja initialisé, mais n'a pas pu aboutir une premiere fois...");
			}
		}			
		DEJA_FAIT.set(true);
		final String agentPath = CreateJar.getAgent();
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
		return AgentSdk.getInstrumentation();
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
		Class<?>[] parameterTypes = {AttachProvider.class, String.class};
		String pid = getProcessId();

		try {
			// This is only done with Reflection to avoid the JVM pre-loading all the XyzVirtualMachine classes.
			Constructor<? extends VirtualMachine> vmConstructor = vmClass.getConstructor(parameterTypes);
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
			return WindowsVirtualMachine.class;
		}

		String osName = System.getProperty("os.name");

		if (osName.startsWith("Linux") || osName.startsWith("LINUX")) {
			return LinuxVirtualMachine.class;
		}

		if (osName.contains("FreeBSD") || osName.startsWith("Mac OS X")) {
			return BsdVirtualMachine.class;
		}

		if (osName.startsWith("Solaris") || osName.contains("SunOS")) {
			return SolarisVirtualMachine.class;
		}

		LOGGER.trace("Cannot use Attach API on unknown OS: " + osName);
		throw new AspectException("Cannot use Attach API on unknown OS: " + osName);
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
			LOGGER.debug("load agent");
			vm.loadAgent(jarFilePath);
			LOGGER.debug("agent loaded... detach VM !");
			vm.detach();
			LOGGER.debug("VM detached !");
		}
		catch (AgentLoadException | AgentInitializationException | IOException e) {
			LOGGER.error("Impossible de charger l'agent dans la VM ou de la détacher", e);
			throw new AspectException("Impossible de charger l'agent dans la VM ou de la détacher", e);
		}
	}
}
