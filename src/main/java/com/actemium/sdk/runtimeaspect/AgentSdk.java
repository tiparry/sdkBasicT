package com.actemium.sdk.runtimeaspect;

import java.lang.instrument.Instrumentation;

public class AgentSdk {

	private static Instrumentation instrumentation;

	private AgentSdk(){}
	
	public static void premain(final String args, final Instrumentation inst) {
		if (isInstrumentationAvailable())
			throw new IllegalStateException("instrumentation is already available, the agent should have been loaded!");
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
		return instrumentation;
	}

}
