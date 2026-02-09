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

package test_pack.block.state;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.junit.jupiter.api.Assertions;
import test_pack.block.FakeBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author QiguaiAAAA
 */
public class FakeBlockState implements IBlockState {
    protected final FakeBlock block;
    protected final ImmutableMap<IProperty<?>,Comparable<?>> properties;
    protected ImmutableTable<IProperty<?>,Comparable<?>,FakeBlockState> stateTable;

    protected FakeBlockState(@Nonnull FakeBlock block,
                             @Nonnull ImmutableMap<IProperty<?>, Comparable<?>> properties) {
        this.block = block;
        this.properties = properties;
    }

    protected FakeBlockState setStateTable(@Nonnull ImmutableTable<IProperty<?>, Comparable<?>, FakeBlockState> stateTable) {
        this.stateTable = stateTable;
        return this;
    }

    @Nonnull
    @Override
    public Collection<IProperty<?>> getPropertyKeys() {
        return properties.keySet();
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> T getValue(@Nonnull IProperty<T> property) {
        return (T) properties.get(property);
    }

    @Nonnull
    @Override
    public <T extends Comparable<T>, V extends T> FakeBlockState withProperty(@Nonnull IProperty<T> property, @Nonnull V value) {
        final Comparable<?> cur = properties.get(property);
        if(cur == value) return this;
        final FakeBlockState res = stateTable.get(property,value);
        Assertions.assertNotNull(res);
        return res;
    }

    @Nonnull
    @Override
    public <T extends Comparable<T>> FakeBlockState cycleProperty(@Nonnull IProperty<T> property) {
        return this.withProperty(property,cyclePropertyValue(property.getAllowedValues(),this.getValue(property)));
    }

    @Nonnull
    @Override
    public ImmutableMap<IProperty<?>, Comparable<?>> getProperties() {
        return properties;
    }

    @Nonnull
    @Override
    public Block getBlock() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public FakeBlock getFakeBlock(){
        return this.block;
    }

    @Override
    public boolean onBlockEventReceived(@Nonnull World worldIn, @Nonnull BlockPos pos, int id, int param) {
        return false;
    }

    @Override
    public void neighborChanged(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull Block blockIn, @Nonnull BlockPos fromPos) {

    }

    @Nonnull
    @Override
    public Material getMaterial() {
        return block.getMaterial();
    }

    @Override
    public boolean isFullBlock() {
        return false;
    }

    @Override
    public boolean canEntitySpawn(@Nonnull Entity entityIn) {
        return false;
    }

    @Override
    public int getLightOpacity() {
        return 0;
    }

    @Override
    public int getLightOpacity(@Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
        return 0;
    }

    @Override
    public int getLightValue() {
        return 0;
    }

    @Override
    public int getLightValue(@Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
        return 0;
    }

    @Override
    public boolean isTranslucent() {
        return false;
    }

    @Override
    public boolean useNeighborBrightness() {
        return false;
    }

    @Nonnull
    @Override
    public MapColor getMapColor(@Nonnull IBlockAccess p_185909_1_, @Nonnull BlockPos p_185909_2_) {
        return MapColor.AIR;
    }

    @Nonnull
    @Override
    public FakeBlockState withRotation(@Nonnull Rotation rot) {
        return this;
    }

    @Nonnull
    @Override
    public FakeBlockState withMirror(@Nonnull Mirror mirrorIn) {
        return this;
    }

    @Override
    public boolean isFullCube() {
        return false;
    }

    @Override
    public boolean hasCustomBreakingProgress() {
        return false;
    }

    @Nonnull
    @Override
    public EnumBlockRenderType getRenderType() {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public int getPackedLightmapCoords(@Nonnull IBlockAccess source, @Nonnull BlockPos pos) {
        return 0;
    }

    @Override
    public float getAmbientOcclusionLightValue() {
        return 0;
    }

    @Override
    public boolean isBlockNormalCube() {
        return false;
    }

    @Override
    public boolean isNormalCube() {
        return false;
    }

    @Override
    public boolean canProvidePower() {
        return false;
    }

    @Override
    public int getWeakPower(@Nonnull IBlockAccess blockAccess, @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
        return 0;
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return false;
    }

    @Override
    public int getComparatorInputOverride(@Nonnull World worldIn, @Nonnull BlockPos pos) {
        return 0;
    }

    @Override
    public float getBlockHardness(@Nonnull World worldIn, @Nonnull BlockPos pos) {
        return 0;
    }

    @Override
    public float getPlayerRelativeBlockHardness(@Nonnull EntityPlayer player, @Nonnull World worldIn, @Nonnull BlockPos pos) {
        return 0;
    }

    @Override
    public int getStrongPower(@Nonnull IBlockAccess blockAccess, @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
        return 0;
    }

    @Nonnull
    @Override
    public EnumPushReaction getPushReaction() {
        return EnumPushReaction.NORMAL;
    }

    @Nonnull
    @Override
    public FakeBlockState getActualState(@Nonnull IBlockAccess blockAccess, @Nonnull BlockPos pos) {
        return this;
    }

    @Nonnull
    @Override
    public AxisAlignedBB getSelectedBoundingBox(@Nonnull World worldIn, @Nonnull BlockPos pos) {
        return new AxisAlignedBB(0,0,0,1,1,1);
    }

    @Override
    public boolean shouldSideBeRendered(@Nonnull IBlockAccess blockAccess, @Nonnull BlockPos pos, @Nonnull EnumFacing facing) {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(@Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos) {
        return null;
    }

    @Override
    public void addCollisionBoxToList(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox, @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean p_185908_6_) {

    }

    @Nonnull
    @Override
    public AxisAlignedBB getBoundingBox(@Nonnull IBlockAccess blockAccess, @Nonnull BlockPos pos) {
        return new AxisAlignedBB(pos.getX(),pos.getY(),pos.getZ(),pos.getX()+1,pos.getY()+1,pos.getZ()+1);
    }

    @Nonnull
    @Override
    public RayTraceResult collisionRayTrace(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull Vec3d start, @Nonnull Vec3d end) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTopSolid() {
        return false;
    }

    @Override
    public boolean doesSideBlockRendering(@Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
        return false;
    }

    @Override
    public boolean isSideSolid(@Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
        return false;
    }

    @Override
    public boolean doesSideBlockChestOpening(@Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
        return false;
    }

    @Nonnull
    @Override
    public Vec3d getOffset(@Nonnull IBlockAccess access, @Nonnull BlockPos pos) {
        return Vec3d.ZERO;
    }

    @Override
    public boolean causesSuffocation() {
        return false;
    }

    @Nonnull
    @Override
    public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos, @Nonnull EnumFacing facing) {
        return BlockFaceShape.SOLID;
    }

    protected static <T> T cyclePropertyValue(@Nonnull Collection<T> values,@Nonnull T currentValue) {
        Iterator<T> iterator = values.iterator();

        boolean isFirst = true;
        T first = null;
        while (iterator.hasNext()) {
            final T cur = iterator.next();
            if(isFirst){
                first = cur;
                isFirst = false;
            }
            if (cur.equals(currentValue)) {
                if (iterator.hasNext()) {
                    return iterator.next();
                }

                return first;
            }
        }
        return iterator.next();
    }

    @Override
    public String toString() {
        Assertions.assertNotNull(block.getRegistryName());
        StringBuilder builder =  new StringBuilder(block.getRegistryName().toString());
        builder.append("[");
        AtomicBoolean isFirst = new AtomicBoolean(true);
        properties.forEach((property, val) -> builder.append(isFirst.getAndSet(false)?"":",").append(property.getName()).append("=").append(val.toString()));
        builder.append("]");
        return builder.toString();
    }
}
