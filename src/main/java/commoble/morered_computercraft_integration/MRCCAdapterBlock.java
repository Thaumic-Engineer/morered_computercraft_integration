package commoble.morered_computercraft_integration;

import java.util.List;

import javax.annotation.Nonnull;

import commoble.morered.api.ChanneledPowerSupplier;
import commoble.morered.api.MoreRedAPI;
import commoble.morered.plate_blocks.PlateBlock;
import commoble.morered.plate_blocks.PlateBlockStateProperties;
import commoble.morered.util.BlockStateUtil;
import commoble.morered.util.DirectionHelper;
import commoble.morered.wire_post.AbstractPostBlock;
import dan200.computercraft.shared.common.IBundledRedstoneBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// block that adapts More Red channeled power to ComputerCraft bundled redstone and back
// The "output" side of the block is the More Red side, the opposite side is the Computercraft side
public class MRCCAdapterBlock extends PlateBlock implements IBundledRedstoneBlock
{
	public static final VoxelShape[][] SHAPES_BY_ATTACHMENT_DIRECTION_AND_ROTATION = Util.make(() ->
	{
		VoxelShape[][] finalShapes = new VoxelShape[6][4];
		
		VoxelShape[] attachmentPlateShapesByDirection = { // DUNSWE, direction of attachment
			Block.box(0, 0, 0, 16, 4, 16),
			Block.box(0, 12, 0, 16, 16, 16),
			Block.box(0, 0, 0, 16, 16, 4),
			Block.box(0, 0, 12, 16, 16, 16),
			Block.box(0, 0, 0, 4, 16, 16),
			Block.box(12, 0, 0, 16, 16, 16)
		};
		
		VoxelShape[] backplateShapesByDirection = {
			Block.box(2, 0, 2, 14, 2, 14),
			Block.box(2, 14, 2, 14, 16, 14),
			Block.box(2, 2, 0, 14, 14, 2),
			Block.box(2, 2, 14, 14, 14, 16),
			Block.box(0, 2, 2, 2, 14, 14),
			Block.box(14, 2, 2, 16, 14, 14)
		};
		for (Direction attachmentDirection : Direction.values())
		{
			int attachmentDirectionIndex = attachmentDirection.ordinal();
			VoxelShape attachmentPlateShape = attachmentPlateShapesByDirection[attachmentDirectionIndex];
			for (int rotationIndex=0; rotationIndex<4; rotationIndex++)
			{
				Direction inputDirection = BlockStateUtil.getOutputDirection(attachmentDirection, rotationIndex).getOpposite();
				int inputDirectionIndex = inputDirection.ordinal();
				VoxelShape backplateShape = backplateShapesByDirection[inputDirectionIndex];
				VoxelShape combinedShape = VoxelShapes.or(attachmentPlateShape, backplateShape);
				finalShapes[attachmentDirectionIndex][rotationIndex] = combinedShape;
			}
		}
		
		return finalShapes;
	});
	public static final VoxelShape[] DOUBLE_PLATE_SHAPES_BY_DIRECTION = { // DUNSWE, direction of attachment
		Block.box(0, 0, 0, 16, 4, 16),
		Block.box(0, 12, 0, 16, 16, 16),
		Block.box(0, 0, 0, 16, 16, 4),
		Block.box(0, 0, 12, 16, 16, 16),
		Block.box(0, 0, 0, 4, 16, 16),
		Block.box(12, 0, 0, 16, 16, 16) };

	public MRCCAdapterBlock(Properties properties)
	{
		super(properties);
	}

	@Override
	public boolean hasTileEntity(BlockState state)
	{
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world)
	{
		return MoreRedComputercraftIntegration.INSTANCE.mrccAdapterBlockEntity.get().create();
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
	{
		if (state.hasProperty(PlateBlock.ATTACHMENT_DIRECTION) && state.hasProperty(PlateBlock.ROTATION))
		{
			return SHAPES_BY_ATTACHMENT_DIRECTION_AND_ROTATION[state.getValue(ATTACHMENT_DIRECTION).ordinal()][state.getValue(PlateBlock.ROTATION)];
		}
		else
		{
			return SHAPES_BY_ATTACHMENT_DIRECTION_AND_ROTATION[0][0];
		}
	}

	@Override
	public boolean getBundledRedstoneConnectivity(World world, BlockPos pos, Direction side)
	{
		return PlateBlockStateProperties.getOutputDirection(world.getBlockState(pos)).getOpposite() == side;
	}

	@Override
	public int getBundledRedstoneOutput(World world, BlockPos thisPos, Direction sideOfThisBlock)
	{
		BlockState thisState = world.getBlockState(thisPos);
		Direction moreRedSide = PlateBlockStateProperties.getOutputDirection(thisState);
		Direction computerCraftSide = moreRedSide.getOpposite();
		if (sideOfThisBlock == computerCraftSide)
		{
			BlockPos moreRedNeighbor = thisPos.relative(moreRedSide);
			TileEntity te = world.getBlockEntity(moreRedNeighbor);
			if (te != null)
			{
				Direction attachmentSide = thisState.getValue(PlateBlock.ATTACHMENT_DIRECTION);
				return te.getCapability(MoreRedAPI.CHANNELED_POWER_CAPABILITY, computerCraftSide)
					.map(powerSupplier -> getBundledRedstoneValueFromPowerSupplier(powerSupplier, world, thisPos, thisState, attachmentSide))
					.orElse(0);
			}
		}
		return 0;
	}

	// forge hook, invoked when a neighbor block's TE data or comparator output changes
	@Override
	public void onNeighborChange(BlockState state, IWorldReader world, BlockPos pos, BlockPos neighbor)
	{
		super.onNeighborChange(state, world, pos, neighbor);
		// propagate neighbor changes to the opposite side in the direction of traffic
		Direction updatePropagationDir = DirectionHelper.getDirectionToNeighborPos(neighbor, pos);
		if (updatePropagationDir != null)
		{
			Direction outputDirection = PlateBlockStateProperties.getOutputDirection(world.getBlockState(pos));
			if (outputDirection.getAxis() == updatePropagationDir.getAxis())
			{
				BlockPos nextNeighbor = pos.relative(updatePropagationDir);
				world.getBlockState(nextNeighbor).onNeighborChange(world, nextNeighbor, pos);
			}
		}
	}
	
	public boolean canConnectToAdjacentCable(@Nonnull IBlockReader world, @Nonnull BlockPos thisPos, @Nonnull BlockState thisState, @Nonnull BlockPos wirePos, @Nonnull BlockState wireState, @Nonnull Direction wireFace, @Nonnull Direction directionToWire)
	{
		Direction plateAttachmentDir = thisState.getValue(AbstractPostBlock.DIRECTION_OF_ATTACHMENT);
		Direction moreRedSide = PlateBlockStateProperties.getOutputDirection(thisState);
		return directionToWire == moreRedSide && plateAttachmentDir == wireFace;
	}
	
	@Override
	@Deprecated
	public void onRemove(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving)
	{
		// if this is just a blockstate change, make sure we tell the TE to invalidate and reset its capability
		if (state.getBlock() == newState.getBlock() && state != newState)
		{
			TileEntity te = worldIn.getBlockEntity(pos);
			if (te instanceof MRCCAdapterBlockEntity)
			{
				((MRCCAdapterBlockEntity)te).resetCapabilities();
			}
		}
		super.onRemove(state, worldIn, pos, newState, isMoving);
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, IBlockReader world, List<ITextComponent> texts, ITooltipFlag flag)
	{
		super.appendHoverText(stack, world, texts, flag);
		texts.add(new TranslationTextComponent("morered_computercraft_integration.tooltips.adapter").withStyle(TextFormatting.GRAY));
	}
	
	public static int getBundledRedstoneValueFromPowerSupplier(ChanneledPowerSupplier powerSupplier, World world, BlockPos thisPos, BlockState thisState, Direction attachmentSide)
	{
		int result = 0;
		for (int channel=0; channel<16; channel++)
		{
			if (powerSupplier.getPowerOnChannel(world, thisPos, thisState, attachmentSide, channel) > 0)
			{
				result |= (1 << channel);
			}
		}
		return result;
	}

}
