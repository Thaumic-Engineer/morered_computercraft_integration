package commoble.morered_computercraft_integration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import commoble.morered.api.ChanneledPowerSupplier;
import commoble.morered.api.MoreRedAPI;
import commoble.morered.plate_blocks.PlateBlockStateProperties;
import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class MRCCAdapterBlockEntity extends TileEntity
{
	protected LazyOptional<ChanneledPowerSupplier> powerHolder = this.makePowerHolder();
	
	public MRCCAdapterBlockEntity()
	{
		super(MoreRedComputercraftIntegration.INSTANCE.mrccAdapterBlockEntity.get());
	}

	public MRCCAdapterBlockEntity(TileEntityType<?> type)
	{
		super(type);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side)
	{
		if (cap == MoreRedAPI.CHANNELED_POWER_CAPABILITY)
			return side == PlateBlockStateProperties.getOutputDirection(this.getBlockState()) ? (LazyOptional<T>) this.powerHolder : LazyOptional.empty();
		return super.getCapability(cap, side);
	}
	
	@Override
	protected void invalidateCaps()
	{
		super.invalidateCaps();
		this.powerHolder.invalidate();
	}
	
	// invalidate and replace capabilities with fresh ones
	// needed if we change the blockstate to one with a different output side
	public void resetCapabilities()
	{
		LazyOptional<ChanneledPowerSupplier> oldPowerHolder = this.powerHolder;
		this.powerHolder = this.makePowerHolder();
		oldPowerHolder.invalidate();
	}
	
	protected LazyOptional<ChanneledPowerSupplier> makePowerHolder()
	{
		return LazyOptional.of(() -> this::getPowerOnChannel);
	}
	
	/**
	 * Gets the per-channel power of the block this wire connector is assigned to.
	 * Internally, cable blocks use 0-31 for power storage (redstone-capable cables output half of this value to non-wires).
	 * @param world The world we're doing power queries in
	 * @param wirePos The position of a wire block
	 * @param wireState The blockstate of the block requesting power. Not guaranteed to be any particular class or have any particular blockstate properties.
	 * @param wireFace The attachment face of the subwire we're supplying power to. Can be null if e.g. querier isn't a wire-like block.
	 * @param channel The channel index we're querying power for. Currently values [0,15] are supported (equivalent to dyecolor ordinals)
	 * @return a power value in the range [0, 31]
	 */
	public int getPowerOnChannel(@Nonnull World world, @Nonnull BlockPos wirePos, @Nonnull BlockState wireState, @Nullable Direction wireFace, int channel)
	{
		BlockState thisState = this.getBlockState();
		Direction directionToMoreRed = PlateBlockStateProperties.getOutputDirection(thisState);
		Direction directionToCC = directionToMoreRed.getOpposite();
		BlockPos ccPos = this.worldPosition.relative(directionToCC);
		int ccBundledRedstoneValue = ComputerCraftAPI.getBundledRedstoneOutput(world, ccPos, directionToMoreRed);
		return ccBundledRedstoneValue > 0 && (ccBundledRedstoneValue & (1 << channel)) > 0 ? 31 : 0;
	}
}
