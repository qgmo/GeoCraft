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

package top.qiguaiaaaa.geocraft.command;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import top.qiguaiaaaa.geocraft.api.atmosphere.accessor.IAtmosphereAccessor;
import top.qiguaiaaaa.geocraft.api.command.builder.CommandBuilder;
import top.qiguaiaaaa.geocraft.api.command.builder.INodeBuilder;
import top.qiguaiaaaa.geocraft.api.command.builder.execute.RelayExecuteNodeBuilder;
import top.qiguaiaaaa.geocraft.api.command.builder.execute.SimpleCommandExecutor;
import top.qiguaiaaaa.geocraft.api.command.context.ExecuteContext;
import top.qiguaiaaaa.geocraft.api.command.node.ISmartNode;
import top.qiguaiaaaa.geocraft.api.command.node.parament.generic.StringNode;
import top.qiguaiaaaa.geocraft.api.configs.value.geo.FluidPhysicsMode;
import top.qiguaiaaaa.geocraft.api.util.AtmosphereUtil;
import top.qiguaiaaaa.geocraft.api.util.FluidUtil;
import top.qiguaiaaaa.geocraft.geography.fluidphysics.reality.MoreRealityFluidPhysicsCore;
import top.qiguaiaaaa.geocraft.util.WaterUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

import static net.minecraft.block.BlockLiquid.LEVEL;
import static top.qiguaiaaaa.geocraft.api.command.Nodes.*;
import static top.qiguaiaaaa.geocraft.command.CommandAtmosphere.AtmosphereCommandContext.ACCESSOR;
import static top.qiguaiaaaa.geocraft.command.CommandFluidPhysics.FluidPhysicsCommandExecutor.CHECK_ATMOSPHERE_ACCESSIBILITY;
import static top.qiguaiaaaa.geocraft.command.CommandFluidPhysics.FluidPhysicsCommandExecutor.GET_LIGHTED_ATMOSPHERE_ACCESSOR;
import static top.qiguaiaaaa.geocraft.command.GeoArguments.*;

/**
 * @author QiguaiAAAA
 */
public class CommandFluidPhysics {
    public static final String FLUID_PHYSICS_COMMAND_NAME = "fluidphysics";
    public static final String FLUIDPHYSICS_PERMISSION_NODE = "geocraft.command.fluidphysics";

    @Nonnull
    public static ICommand create(){
        return new CommandBuilder(FLUID_PHYSICS_COMMAND_NAME)
                .require(2)
                .require(FLUIDPHYSICS_PERMISSION_NODE).allow(DefaultPermissionLevel.OP).register()
                .smart()
                .append(buildQueryCommand()).done()
                .append(buildUtilCommand()).done()
                .done()
                .usage("geocraft.command.fluidphysics.usage")
                .build();

    }

    @Nonnull
    public static INodeBuilder<? extends ISmartNode> buildQueryCommand(){
        return literal("query")
                .then(literals()
                        .when("mode").then(execute(ctx -> ctx.notifyCommandListener("当前流体物理模式："+ FluidPhysicsMode.getCurrentMode()))));
    }

    @Nonnull
    public static INodeBuilder<? extends ISmartNode> buildUtilCommand(){
        return literal("util")
                .then(literals()
                        .when("evaporate").then(pos()
                                .then(doit()
                                        .then(process(CommandFluidPhysics::evaporate,true)))));
    }

    @Nonnull
    public static RelayExecuteNodeBuilder process(@Nonnull final FluidPhysicsCommandExecutor executor,final boolean checkAccessibility){
        return relay(checkAccessibility?GET_LIGHTED_ATMOSPHERE_ACCESSOR.then(CHECK_ATMOSPHERE_ACCESSIBILITY):GET_LIGHTED_ATMOSPHERE_ACCESSOR)
                .keepArguments(false)
                .then(execute(executor))
                .after(ctx -> ctx.<IAtmosphereAccessor>remove(ACCESSOR).close());
    }

    /**
     * @see top.qiguaiaaaa.geocraft.geography.fluidphysics.reality.MoreRealityFluidPhysicsCore#evaporateWater(IBlockState, Random, IAtmosphereAccessor) 
     */
    public static void evaporate(@Nonnull final ExecuteContext ctx,@Nonnull final IAtmosphereAccessor accessor) throws CommandException {
        final @Nonnull BlockPos pos = ctx.getBlockPos(POS);
        final @Nonnull World worldIn = ctx.getWorld();
        final boolean doEvaporate = !ctx.get(DOIT, StringNode.class).isEmpty();
        if(!accessor.getAtmosphereInfo().canWaterEvaporate(pos)){
            ctx.notifyCommandListener("geocraft.command.fluidphysics.unable_to_evaporate",pos.getX(),pos.getY(),pos.getZ());
            return;
        }
        final @Nonnull IBlockState state = getWaterState(worldIn,pos);
        final Object2DoubleArrayMap<String> reasons = new Object2DoubleArrayMap<>();
        final double possibility = gatherEvaporationStatus(state,reasons,accessor);

        final ITextComponent title = new TextComponentTranslation("geocraft.command.fluidphysics.evapration.title",pos.getX(),pos.getY(),pos.getZ());
        title.getStyle().setColor(TextFormatting.YELLOW).setBold(true);
        ctx.getSender().sendMessage(title);
        final ITextComponent possibilityDisplay = new TextComponentTranslation("geocraft.command.fluidphysics.evapration.possibility.pre",possibility*100d);
        final ITextComponent possibilityContent = new TextComponentString(possibility * 100d +" %");
        possibilityContent.getStyle().setColor(TextFormatting.AQUA);
        possibilityDisplay.appendSibling(possibilityContent);
        ctx.getSender().sendMessage(possibilityDisplay);
        reasons.forEach((reason,delta)->{
            final ITextComponent reasonDisplay = new TextComponentTranslation(reason).appendText(" : ");
            reasonDisplay.getStyle().setItalic(true);
            final ITextComponent deltaDisplay = new TextComponentString((delta>=0?"+":"-")+String.format("%.4f %%",delta*100d));
            deltaDisplay.getStyle().setColor(delta>0?TextFormatting.GREEN:delta<0?TextFormatting.RED:TextFormatting.GRAY).setUnderlined(true);
            reasonDisplay.appendSibling(deltaDisplay);
            ctx.getSender().sendMessage(reasonDisplay);
        });
        if(doEvaporate) doEvaporate(state,accessor,ctx);
    }

    static double gatherEvaporationStatus(@Nonnull final IBlockState state, @Nonnull final Object2DoubleArrayMap<String> reasons,@Nonnull final IAtmosphereAccessor accessor){
        final @Nonnull World world = accessor.getWorld();
        final @Nonnull BlockPos pos = accessor.getPos();
        if(!world.isAirBlock(pos.up())){
            reasons.put("geocraft.command.fluidphysics.evapration.reasons.upon_non_air",0);
            return 0;
        }else if(state.getValue(LEVEL) >= 8){
            reasons.put("geocraft.command.fluidphysics.evapration.reasons.invalid_water",1);
            return 1;
        }
        final double raw = WaterUtil.getWaterEvaporatePossibility(accessor);
        reasons.put("geocraft.command.fluidphysics.evapration.reasons.rawPossibility",raw);
        if(raw >= 0.9999d){
            reasons.put("geocraft.command.fluidphysics.evapration.reasons.saturated",1d-raw);
            return 1;
        }else if(useRawEvaporationPossibility(world,pos,state)){
            return raw;
        }

        final double possibility = adjustEvaporationPossibilityByNeighborsAir(world,pos,raw);
        reasons.put("geocraft.command.fluidphysics.evapration.reasons.exposure",possibility-raw);
        return possibility;
    }

    static boolean useRawEvaporationPossibility(@Nonnull final World world,@Nonnull final BlockPos pos,@Nonnull final IBlockState state){
        return state.getValue(LEVEL) <5 || !world.isAreaLoaded(pos,1) || pos.getY() <= 0 || FluidUtil.getFluid(world.getBlockState(pos.down())) == FluidRegistry.WATER;
    }

    static double adjustEvaporationPossibilityByNeighborsAir(@Nonnull final World world,@Nonnull final BlockPos pos,final double possibility){
        byte neighborsAir = 0;
        for(final @Nonnull EnumFacing facing:EnumFacing.HORIZONTALS){
            final @Nonnull BlockPos facingPos = pos.offset(facing);
            if(world.isAirBlock(facingPos)) neighborsAir++;
        }
        if(neighborsAir <= 1) return possibility;
        return Math.min(possibility*(1<<(neighborsAir-1)),1);
    }

    static void doEvaporate(@Nonnull final IBlockState state,@Nonnull final IAtmosphereAccessor accessor,@Nonnull final ExecuteContext ctx){
        final @Nonnull IBlockState newState = MoreRealityFluidPhysicsCore.evaporateWater(state,accessor.getWorld().rand,accessor);
        accessor.getWorld().setBlockState(accessor.getPos(),newState);
        final int quanta = newState == Blocks.AIR.getDefaultState()?Math.max(8-state.getValue(LEVEL),0):(newState.getValue(LEVEL)-state.getValue(LEVEL));
        final ITextComponent evaporatedInfo = new TextComponentTranslation("geocraft.command.fluidphysics.evapration.evaporated",quanta*FluidUtil.ONE_IN_EIGHT_OF_BUCKET_VOLUME,quanta);
        evaporatedInfo.getStyle().setColor(quanta <= 0?TextFormatting.GRAY:TextFormatting.GREEN);
        ctx.getSender().sendMessage(evaporatedInfo);
    }

    @Nonnull
    static IBlockState getWaterState(@Nonnull final World world,@Nonnull final BlockPos pos) throws CommandException{
        final @Nonnull IBlockState state = world.getBlockState(pos);
        if(state.getBlock() != Blocks.WATER && state.getBlock() != Blocks.FLOWING_WATER)
            throw new CommandException("geocraft.command.fluidphysics.not_water",pos.getX(),pos.getY(),pos.getZ(),state);
        return state;
    }

    @Nonnull
    static IAtmosphereAccessor getLightedAtmosphereAccessor(final @Nonnull World world,final @Nonnull BlockPos pos) throws CommandException {
        final @Nullable IAtmosphereAccessor accessor = AtmosphereUtil.getLightedAtmosphereAccessor(world,pos,true);
        if(accessor == null){
            throw new CommandException("geocraft.command.atmosphere.nonexistent",pos);
        }
        return accessor;
    }

    @FunctionalInterface
    interface FluidPhysicsCommandExecutor extends SimpleCommandExecutor{
        SimpleCommandExecutor GET_LIGHTED_ATMOSPHERE_ACCESSOR = ctx -> ctx.put(ACCESSOR,getLightedAtmosphereAccessor(ctx.getWorld(),ctx.getBlockPos(POS)));
        SimpleCommandExecutor CHECK_ATMOSPHERE_ACCESSIBILITY = ctx -> {
            final IAtmosphereAccessor accessor = ctx.get(ACCESSOR);
            if(!accessor.canAccessAtmosphere()) throw new CommandException("geocraft.command.fluidphysics.inaccessibility_to_atmosphere",
                    accessor.getPos().getX(),
                    accessor.getPos().getY(),
                    accessor.getPos().getZ());
        };

        void run(@Nonnull final ExecuteContext ctx,@Nonnull final IAtmosphereAccessor accessor) throws CommandException;

        @Override
        default void run(@Nonnull List<String> args, @Nonnull ExecuteContext context) throws CommandException {
            this.run(context,context.get(ACCESSOR));
        }

        @Override
        default void simplyRun(@Nonnull final ExecuteContext ctx) throws CommandException{
            this.run(ctx,ctx.get(ACCESSOR));
        }
    }
}
