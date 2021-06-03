package commoble.morered_computercraft_integration.client;

import commoble.morered_computercraft_integration.MoreRedComputercraftIntegration;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientProxy
{
	public static void subscribeClientEvents(IEventBus modBus, IEventBus forgeBus)
	{
		modBus.addListener(ClientProxy::onClientSetup);
	}
	
	private static void onClientSetup(FMLClientSetupEvent event)
	{
		RenderTypeLookup.setRenderLayer(MoreRedComputercraftIntegration.INSTANCE.adapterBlock.get(), RenderType.cutout());
	}
}
