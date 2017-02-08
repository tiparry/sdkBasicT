package com.actemium.sdk.runtimeaspect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VirtualMachine {
	private static final Logger LOGGER = LoggerFactory.getLogger(VirtualMachine.class);

	///INTROSPECTION

	private static final Class<?> VIRTUAL_MACHINE_CLASSE = getVirtualMachineClasse();
	private static final Method ATTACH_METHODE = getMethode("attach", String.class);
	private static final Method GET_SYSTEM_PROPERTY_METHODE = getMethode("getSystemProperties");
	private static final Method LOAD_AGENT_METHODE = getMethode("loadAgent", String.class);
	private static final Method GET_AGENT_PROPERTIES_METHODE = getMethode("getAgentProperties");

	private Object realVirtualMachine;

	private static Class<?> getVirtualMachineClasse() {
		try {
			//en reflection puisque tools.jar a été ajouté dynamiquement au classpath.
			return Class.forName("com.sun.tools.attach.VirtualMachine");
		} catch (ClassNotFoundException e) {
			LOGGER.info("tools.jar n'est pas dans le classPath, aurait du être chargé dynamiquement", e);
			throw new AspectException("tools.jar n'est pas dans le classPath, aurait du être chargé dynamiquement", e);
		}
	}

	private static Method getMethode(String nomMethode, Class<?>... typesArgument) {
		try {
			return VIRTUAL_MACHINE_CLASSE.getMethod(nomMethode, typesArgument);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new AspectException("impossible de récuperer la méthode " + nomMethode + " de la classe com.sun.tools.attach.VirtualMachine", e);
		}
	}

	//////PROXY

	public VirtualMachine(Object realVirtualMachine) {
		super();
		this.realVirtualMachine = realVirtualMachine;
	}


	public static VirtualMachine attach(String vid) {
		try {
			return new VirtualMachine(ATTACH_METHODE.invoke(null, vid));
		} catch (IllegalAccessException | IllegalArgumentException e) {
			throw new AspectException("interdiction d'appeler la méthode attach de com.sun.tools.attach.VirtualMachine par introspection", e);
		} catch (InvocationTargetException e) {
			throw new AspectException("L'appel de la méthode attach plante", e);
		}
	}

	public Properties getSystemProperties() {
		try {
			return (Properties)GET_SYSTEM_PROPERTY_METHODE.invoke(realVirtualMachine);
		} catch (IllegalAccessException | IllegalArgumentException e) {
			throw new AspectException("interdiction d'appeler la méthode getSystemProperty de com.sun.tools.attach.VirtualMachine par introspection", e);
		} catch (InvocationTargetException e) {
			throw new AspectException("L'appel de la méthode getSystemProperty plante", e);
		}
	}

	public void loadAgent(String agentPath) {
		try{
			LOAD_AGENT_METHODE.invoke(realVirtualMachine, agentPath);
		} catch (IllegalAccessException | IllegalArgumentException e) {
			throw new AspectException("interdiction d'appeler la méthode loadAgent de com.sun.tools.attach.VirtualMachine par introspection", e);
		} catch (InvocationTargetException e) {
			throw new AspectException("L'appel de la méthode loadAgent plante", e);
		}
	}

	public Properties getAgentProperties() {
		try {
			return (Properties)GET_AGENT_PROPERTIES_METHODE.invoke(realVirtualMachine);
		} catch (IllegalAccessException | IllegalArgumentException e) {
			throw new AspectException("impossible de récuperer la méthode getAgentProperties de la classe com.sun.tools.attach.", e);
		} catch (InvocationTargetException e) {
			throw new AspectException("L'appel de la méthode getAgentProperties plante", e);
		}

	}
}
