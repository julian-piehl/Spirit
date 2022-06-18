package me.codexadrian.spirit.blocks;

import me.codexadrian.spirit.SpiritConfig;
import me.codexadrian.spirit.blocks.blockentity.PedestalBlockEntity;
import me.codexadrian.spirit.blocks.blockentity.SoulPedestalBlockEntity;
import me.codexadrian.spirit.registry.SpiritBlocks;
import me.codexadrian.spirit.registry.SpiritItems;
import me.codexadrian.spirit.utils.SoulUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CrystalPedestalBlock extends BaseEntityBlock {

    public static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 5, 0, 16, 10, 16),
            Block.box(3, 10, 3, 13, 11, 13),
            Block.box(4, 3, 4, 12, 5, 12),
            Block.box(2, 0, 2, 14, 3, 14)
    );

    public CrystalPedestalBlock(Properties $$0) {
        super($$0);
    }

    public @NotNull InteractionResult use(@NotNull BlockState blockState, @NotNull Level level, @NotNull BlockPos blockPos, @NotNull Player player, @NotNull InteractionHand interactionHand, @NotNull BlockHitResult blockHitResult) {
        if (interactionHand != InteractionHand.OFF_HAND) {
            ItemStack itemStack = player.getMainHandItem();
            if (level.getBlockEntity(blockPos) instanceof SoulPedestalBlockEntity soulPedestal) {
                ItemStack pedestalItem = soulPedestal.getItem(0);
                if (soulPedestal.isEmpty()) {
                    if ((itemStack.is(SpiritItems.SOUL_CRYSTAL.get()) || itemStack.is(SpiritItems.CRUDE_SOUL_CRYSTAL.get()))) {
                        soulPedestal.setItem(0, itemStack.copy());
                        if (!player.getAbilities().instabuild) {
                            itemStack.shrink(1);
                        }
                        soulPedestal.setChanged();
                        level.sendBlockUpdated(blockPos, blockState, blockState, Block.UPDATE_ALL);
                        return InteractionResult.SUCCESS;
                    }
                } else if (itemStack.isEmpty()) {
                    ItemStack soulCrystal = soulPedestal.removeItem(0, Block.UPDATE_ALL);
                    player.getInventory().placeItemBackInInventory(soulCrystal);
                    return InteractionResult.SUCCESS;
                } else if (SoulUtils.getSoulsInCrystal(itemStack) > 0) {
                    if (pedestalItem.is(SpiritItems.CRUDE_SOUL_CRYSTAL.get()) && (itemStack.is(SpiritItems.SOUL_CRYSTAL.get()) || itemStack.is(SpiritItems.CRUDE_SOUL_CRYSTAL.get()))) {
                        if (SoulUtils.canCrystalAcceptSoul(pedestalItem, null)) {
                            int deviateSoulCount = Math.min(SpiritConfig.getCrudeSoulCrystalCap() - SoulUtils.getSoulsInCrystal(pedestalItem), SoulUtils.getSoulsInCrystal(itemStack));
                            combineSoulCrystals(level, blockPos, itemStack, pedestalItem, deviateSoulCount, null);
                            soulPedestal.setChanged();
                            level.sendBlockUpdated(blockPos, blockState, blockState, Block.UPDATE_ALL);
                            return InteractionResult.SUCCESS;
                        }
                    }
                    if (pedestalItem.is(SpiritItems.SOUL_CRYSTAL.get()) && itemStack.is(SpiritItems.SOUL_CRYSTAL.get())) {
                        int maxSouls = SoulUtils.getMaxSouls(pedestalItem, level);
                        int soulsInCrystal = SoulUtils.getSoulsInCrystal(pedestalItem);
                        if ((SoulUtils.doCrystalTypesMatch(pedestalItem, itemStack) && soulsInCrystal < maxSouls) || !pedestalItem.hasTag()) {
                            int deviateSoulCount = Math.min(maxSouls - soulsInCrystal, SoulUtils.getSoulsInCrystal(itemStack));
                            combineSoulCrystals(level, blockPos, itemStack, pedestalItem, deviateSoulCount, SoulUtils.getSoulCrystalType(pedestalItem));
                            soulPedestal.setChanged();
                            level.sendBlockUpdated(blockPos, blockState, blockState, Block.UPDATE_ALL);
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
            }
        }

        return InteractionResult.PASS;
    }

    private void combineSoulCrystals(@NotNull Level level, @NotNull BlockPos blockPos, ItemStack itemStack, ItemStack pedestalItem, int deviateSoulCount, @Nullable String mobType) {
        SoulUtils.deviateSoulCount(pedestalItem, deviateSoulCount, level, mobType);
        SoulUtils.deviateSoulCount(itemStack, -deviateSoulCount, level, mobType);
        if (!level.isClientSide()) {
            ServerLevel sLevel = (ServerLevel) level;
            sLevel.sendParticles(ParticleTypes.SOUL, blockPos.getX() + 0.5, blockPos.getY() + 0.4, blockPos.getZ() + 0.5, 10, 0.5, 0.5, 0.5, 0);
            sLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, 10, 0.5, 0.5, 0.5, 0);
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState blockState, @NotNull BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, SpiritBlocks.SOUL_PEDESTAL_ENTITY.get(), SoulPedestalBlockEntity::tick);
    }


    @Override
    public boolean isOcclusionShapeFullBlock(@NotNull BlockState blockState, @NotNull BlockGetter blockGetter, @NotNull BlockPos blockPos) {
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new SoulPedestalBlockEntity(blockPos, blockState);
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(@NotNull BlockState blockState, @NotNull BlockGetter blockGetter, @NotNull BlockPos blockPos, @NotNull CollisionContext collisionContext) {
        return SHAPE;
    }
}
