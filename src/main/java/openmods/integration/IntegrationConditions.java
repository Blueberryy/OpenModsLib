package openmods.integration;

import net.minecraftforge.fml.ModList;
import openmods.conditions.ICondition;
import openmods.reflection.SafeClassLoad;

public class IntegrationConditions extends openmods.conditions.Conditions {

	public static ICondition classExists(String clsName) {
		final SafeClassLoad cls = new SafeClassLoad(clsName);
		return cls::tryLoad;
	}

	public static ICondition modLoaded(final String modName) {
		return () -> ModList.get().isLoaded(modName);
	}

}
