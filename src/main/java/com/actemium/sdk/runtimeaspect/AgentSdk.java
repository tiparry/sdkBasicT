package com.actemium.sdk.runtimeaspect;

import java.lang.instrument.Instrumentation;
import java.util.Properties;


public class AgentSdk {
	
	private static Instrumentation instrumentation;
	
	private AgentSdk(){}

	public static void premain(final String args, final Instrumentation inst) {
		turnOffSecurity();
		if (isInstrumentationAvailable())
			throw new IllegalStateException("instrumentation is already available, the agent should have been loaded!");
		if(inst == null)
			throw new IllegalArgumentException("pourquoi l'instrumentation est null ?");
		instrumentation = inst;
		System.setProperty("agent.sdk.installed", "true");
	}

	public static void agentmain (final String args, final Instrumentation inst)  {
		premain(args, inst);
	}

	public static boolean isInstrumentationAvailable(){
		return instrumentation != null;
	}

	public static Instrumentation getInstrumentation(){
		if(instrumentation == null)
			System.out.println("c'est null");
		return instrumentation;
	}

	private static void turnOffSecurity() {
		/*
		 * Test if we're inside an applet. We should be inside
		 * an applet if the System property ("package.restrict.access.sun")
		 * is not null and is set to true.
		 */

		boolean restricted = System.getProperty("package.restrict.access.sun") != null;

		/*
		 * If we're in an applet, we need to change the System properties so
		 * as to avoid class restrictions. We go through the current properties
		 * and remove anything related to package restriction.
		 */
		if ( restricted ) {

			Properties newProps = new Properties();

			Properties sysProps = System.getProperties();

			for(String prop : sysProps.stringPropertyNames()) {
				if ( prop != null && ! prop.startsWith("package.restrict.") ) {
					newProps.setProperty(prop,sysProps.getProperty(prop));
				}
			}

			System.setProperties(newProps);
		}

		/*
		 * Should be the final nail in (your) coffin.
		 */
		System.setSecurityManager(null);
	}
}
