package commoble.morered_computercraft_integration;

import java.util.Map;

import commoble.morered.ItemRegistrar;
import commoble.morered.api.MoreRedAPI;
import commoble.morered.api.WireConnector;
import commoble.morered_computercraft_integration.client.ClientProxy;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

@Mod(MoreRedComputercraftIntegration.MODID)
public class MoreRedComputercraftIntegration
{
	public static final String MODID = "morered_computercraft_integration";
	public static MoreRedComputercraftIntegration INSTANCE;
	
	public final RegistryObject<MRCCAdapterBlock> adapterBlock;
	public final RegistryObject<TileEntityType<MRCCAdapterBlockEntity>> mrccAdapterBlockEntity;
	
	public MoreRedComputercraftIntegration()
	{
		INSTANCE = this;
		
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		
		DeferredRegister<Block> blocks = makeDeferredRegister(modBus, ForgeRegistries.BLOCKS);
		DeferredRegister<Item> items = makeDeferredRegister(modBus, ForgeRegistries.ITEMS);
		DeferredRegister<TileEntityType<?>> blockEntities = makeDeferredRegister(modBus, ForgeRegistries.TILE_ENTITIES);
		
		// register blocks, items, etc
		this.adapterBlock = blocks.register(Names.MRCC_ADAPTER,
			() -> new MRCCAdapterBlock(AbstractBlock.Properties.of(Material.STONE, MaterialColor.COLOR_BLUE).strength(2F, 5F)));
		items.register(Names.MRCC_ADAPTER,
			() -> new BlockItem(this.adapterBlock.get(), new Item.Properties().tab(ItemRegistrar.CREATIVE_TAB))); // just stick it in the More Red tab
				
		this.mrccAdapterBlockEntity = blockEntities.register(Names.MRCC_ADAPTER,
			() -> TileEntityType.Builder.of(MRCCAdapterBlockEntity::new,
				this.adapterBlock.get())
			.build(null));
		
		// subscribe events to busses
		modBus.addListener(this::onCommonSetup);
		
		// subscribe client events
		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			ClientProxy.subscribeClientEvents(modBus, forgeBus);
		}
	}
	
	private void onCommonSetup(FMLCommonSetupEvent event)
	{
		// register adapters as being able to have more red cables connect to them
		Map<Block, WireConnector> cableConnectabilityRegistry = MoreRedAPI.getCableConnectabilityRegistry();
		MRCCAdapterBlock adapter = this.adapterBlock.get();
		cableConnectabilityRegistry.put(this.adapterBlock.get(), adapter::canConnectToAdjacentCable);
	}
	
	public static <T extends IForgeRegistryEntry<T>> DeferredRegister<T> makeDeferredRegister(IEventBus modBus, IForgeRegistry<T> registry)
	{
		DeferredRegister<T> register = DeferredRegister.create(registry, MODID);
		register.register(modBus);;
		return register;
	}
}
