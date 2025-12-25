/*
 * Copyright 2025 QiguaiAAAA
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 版权所有 2025 QiguaiAAAA
 * 根据Apache许可证第2.0版（“本许可证”）许可；
 * 除非符合本许可证的规定，否则你不得使用此文件。
 * 你可以在此获取本许可证的副本：
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * 除非所适用法律要求或经书面同意，在本许可证下分发的软件是“按原样”分发的，
 * 没有任何形式的担保或条件，不论明示或默示。
 * 请查阅本许可证了解有关本许可证下许可和限制的具体要求。
 * 中文译文来自开放原子开源基金会，非官方译文，如有疑议请以英文原文为准
 */

package top.qiguaiaaaa.geocraft.mixin.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.qiguaiaaaa.geocraft.GeoCraft;
import top.qiguaiaaaa.geocraft.api.atmosphere.accessor.IAtmosphereAccessor;
import top.qiguaiaaaa.geocraft.api.block.IBlockStateLayeredFluidHost;
import top.qiguaiaaaa.geocraft.api.fluid.StateOfMatter;
import top.qiguaiaaaa.geocraft.api.property.TemperatureProperty;
import top.qiguaiaaaa.geocraft.api.util.AtmosphereUtil;
import top.qiguaiaaaa.geocraft.api.util.FluidUtil;
import top.qiguaiaaaa.geocraft.block.IBlockSnow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

import static top.qiguaiaaaa.geocraft.api.block.BlockProperties.MIXTURE;
import static top.qiguaiaaaa.geocraft.configs.FluidPhysicsConfig.FLUID_PHYSICS_INFO;
import static top.qiguaiaaaa.geocraft.geography.fluid_physics.FluidPhysicsInfo.CREATE_INFO_FUNC;

@Mixin(value = BlockSnow.class,priority = 1100)
@Deprecated
public class BlockSnowMixin extends Block implements IBlockSnow {
    @Shadow @Final public static PropertyInteger LAYERS;

    public BlockSnowMixin(Material materialIn) {
        super(materialIn);
    }

    @Inject(method = "<init>",at = @At(value = "RETURN"))
    private void injectDefaultState(CallbackInfo ci) {
        this.setTickRandomly(true);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(LAYERS,1)
                .withProperty(MIXTURE,false));
    }

    /**
     * @see top.qiguaiaaaa.geocraft.mixin.reality.block.BlockSnowMixin#updateTick(World, BlockPos, IBlockState, Random, CallbackInfo)
     * @reason 引导到自定义的融化行为
     */
    @Inject(method = "updateTick",at =@At("HEAD"),cancellable = true)
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand, CallbackInfo ci) {
        ci.cancel();
        if(worldIn.isRemote) return;
        trySmelt(worldIn, pos, state, rand);
    }

    @Inject(method = "getStateFromMeta",at = @At(value = "HEAD"),cancellable = true)
    private void getStateFromMeta(int meta, CallbackInfoReturnable<IBlockState> cir) {
        cir.cancel();
        cir.setReturnValue(this.getDefaultState()
                .withProperty(LAYERS,(meta&7)+1)
                .withProperty(MIXTURE,(meta&8) != 0));
    }
    @Inject(method = "getMetaFromState",at = @At(value = "HEAD"),cancellable = true)
    public void getMetaFromState(IBlockState state, CallbackInfoReturnable<Integer> cir) {
        cir.cancel();
        cir.setReturnValue((state.getValue(LAYERS)-1)|(state.getValue(MIXTURE)?8:0));
    }

    @Inject(method = "createBlockState",at = @At(value = "HEAD"),cancellable = true)
    private void createBlockState(CallbackInfoReturnable<BlockStateContainer> cir) {
        cir.cancel();
        cir.setReturnValue(new BlockStateContainer(this,LAYERS,MIXTURE));
    }
}
