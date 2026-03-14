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

package top.qiguaiaaaa.geocraft_test.world.sandbox;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import net.minecraft.block.state.IBlockState;
import org.junit.jupiter.api.Assertions;
import top.qiguaiaaaa.geocraft_test.GeoCraftTest;
import top.qiguaiaaaa.geocraft_test.block.SerialisedBlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 一个 builder，可以配置中文字符环境，并将传入的中文字符序列转换为 IBlockState[y][z][x]
 * @author QiguaiAAAA
 */
public class MockSandboxEnvBuilder<S extends MockSandboxEnvBuilder<S>> {
    protected final String name;
    protected final BiMap<SerialisedBlockState, IBlockState> characterToState = HashBiMap.create();
    protected final Map<SerialisedBlockState,IBlockState> defaultStates = Maps.newHashMap();

    protected MockSandboxEnvBuilder(@Nonnull final String name){
        this.name = name;
    }

    @Nonnull
    public static MockSandboxEnvBuilder<?> create(final @Nonnull String name){
        return new Impl(name);
    }

    /**
     * 构造一个结构层字符串。
     * <p>
     * 注意：
     * 第一个参数仅用于避免 IntelliJ 的参数提示 `rows:` 影响代码排版，
     * 实际结构内容从第二个参数开始。
     * <p>
     * 示例：
     * layer(
     *     "",
     *     "石石石",
     *     "石〇石",
     *     "石石石"
     * )
     */
    @Nonnull
    public static String layer(@Nonnull final String... rows) {
        return String.join("\n", Arrays.copyOfRange(rows, 1, rows.length));
    }

    @Nonnull
    public static IBlockState[][] layer(@Nonnull final IBlockState[]... rows) {
        return Arrays.stream(Arrays.copyOfRange(rows, 1, rows.length)).toArray(IBlockState[][]::new);
    }

    @Nonnull
    public static IBlockState[] line(@Nonnull final IBlockState... states) {
        return states;
    }


    @Nonnull
    @SuppressWarnings("unchecked")
    public S withStateData(final @Nonnull Class<?> stateDataCls){
        final @Nonnull Field[] fields = stateDataCls.getDeclaredFields();

        for(@Nonnull final Field field:fields){
            if((field.getModifiers() & Modifier.STATIC) == 0) continue;
            if(IBlockState.class.isAssignableFrom(field.getType())){
                field.setAccessible(true);
                try {
                    final IBlockState state = (IBlockState) field.get(null);
                    final String name = field.getName();
                    Assertions.assertNotNull(state);

                    final boolean isDefaultMapping = field.isAnnotationPresent(Sandbox.Default.class);
                    final SerialisedBlockState serialisedState = SerialisedBlockState.of(name);
                    if(isDefaultMapping){
                        Assertions.assertTrue(this.characterToState.containsValue(state));
                        GeoCraftTest.LOGGER.info("{} Putted Default {} -> {} ({}) from class {}",name,serialisedState,state,this.characterToState.inverse().get(state),stateDataCls.getName());
                        registerDefaultStateData(serialisedState,state);
                    }else{
                        GeoCraftTest.LOGGER.info("{} Putted {} -> {} from class {}",name,serialisedState,state,stateDataCls.getName());
                        registerStateData(serialisedState,state);
                    }

                } catch (final IllegalAccessException e) {
                    Assertions.fail(e);
                }
            }
        }

        return (S) this;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public S withStateData(final @Nonnull MockSandboxEnvBuilder<?> anotherBuilder){
        this.characterToState.keySet().forEach(s -> Assertions.assertFalse(anotherBuilder.defaultStates.containsKey(s)));
        anotherBuilder.defaultStates.keySet().forEach(s -> Assertions.assertFalse(this.characterToState.containsKey(s)));
        this.characterToState.putAll(anotherBuilder.characterToState);
        this.defaultStates.putAll(anotherBuilder.defaultStates);
        return (S) this;
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public S registerStateData(final @Nonnull SerialisedBlockState serialised,final @Nonnull IBlockState state){
        Assertions.assertFalse(defaultStates.containsKey(serialised));
        this.characterToState.put(serialised,state);
        return (S) this;
    }

    @Nonnull
    public S registerDefaultStateData(final @Nonnull String serialised,final @Nonnull IBlockState state){
        return this.registerDefaultStateData(SerialisedBlockState.of(serialised),state);
    }

    /**
     * 注册一个单向的默认中文方块状态数据。
     * 什么是单向的状态呢？例如泥土方块有0-4种元数据，但是如果每次输入都要输入泥0、泥1之类的太麻烦了。所以可以规定：
     * 土 -> 土0
     * 这样子输入土就相当于土0了。
     * @param serialised 序列化后的中文方块状态
     * @param state 对应的实际方块状态
     * @return 自身
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public S registerDefaultStateData(final @Nonnull SerialisedBlockState serialised,final @Nonnull IBlockState state){
        Assertions.assertFalse(this.characterToState.containsKey(serialised));
        Assertions.assertTrue(this.characterToState.containsValue(state));
        this.defaultStates.put(serialised,state);
        return (S) this;
    }

    /**
     * 从汉字组成的字符串描述生成一个基本的沙盒结构
     * @param chars 由汉字描述的结构，从低到高，其边长必须是 size 的大小
     * @return 转换后的 IBlockState，索引为 Y,Z,X
     */
    @Nonnull
    public IBlockState[][][] generateFromCharacters(final int size,final @Nonnull String[] chars){
        return generateFromCharacters(size,size,chars);
    }

    /**
     * 从汉字组成的字符串描述生成一个基本的沙盒结构
     * @param zLength 矩形的长
     * @param xLength 矩形的宽
     * @param chars 由汉字描述的结构，从低到高，其边长必须是 size 的大小
     * @return 转换后的 IBlockState，索引为 Y,Z,X
     */
    @Nonnull
    public IBlockState[][][] generateFromCharacters(final int zLength,final int xLength,final @Nonnull String[] chars){
        if(zLength == 0 || xLength == 0) return new IBlockState[0][0][0];
        return Arrays.stream(chars).map(layer -> Arrays.stream(layer.split("\n"))
                .limit(zLength)
                .map(line -> line.codePoints()
                        .filter(c -> !Character.isWhitespace(c))
                        .collect(StringBuilder::new,
                                StringBuilder::appendCodePoint,
                                StringBuilder::append)
                        .toString())
                .map(line ->{
                    final @Nonnull List<SerialisedBlockState> rawStates = parseLine(line);
                    Assertions.assertEquals(rawStates.size(),xLength,"Line "+ line +" has different length of "+xLength);
                    return rawStates.stream()
                            .map(this::getBlockStateBySerialised)
                            .toArray(IBlockState[]::new);
                })
                .toArray(IBlockState[][]::new))
                .toArray(IBlockState[][][]::new);
    }

    @Nonnull
    public IBlockState getBlockStateByName(final @Nonnull String name){
        return getBlockStateBySerialised(SerialisedBlockState.of(name));
    }

    @Nonnull
    public IBlockState getBlockStateBySerialised(final @Nonnull SerialisedBlockState serialisation){
        IBlockState res = characterToState.get(serialisation);
        if(res == null) res = defaultStates.computeIfAbsent(serialisation,k -> Assertions.fail(k + " isn't a known block state!"));
        return res;
    }

    public void assertEqualStructure(final @Nonnull IBlockState[][][] A, final @Nonnull IBlockState[][][] B){
        assertEqualStructure(A,B,null);
    }

    public void assertEqualStructure(final @Nonnull IBlockState[][][] A, final @Nonnull IBlockState[][][] B,final @Nullable String msg){
        Assertions.assertEquals(A.length,B.length,msg);
        for(int y =0;y < A.length; y++){
            Assertions.assertEquals(A[y].length,B[y].length,msg);
            for(int z =0;z < A[y].length; z ++){
                Assertions.assertArrayEquals(A[y][z],B[y][z],"Mismatch at layer(Y)=" + y + " row(Z)=" + z);
            }
        }
    }

    public void assertEqualStructure(final @Nonnull IBlockState[][][] A, final @Nonnull String[] B ,final int size){
        assertEqualStructure(A,B,size,null);
    }

    public void assertEqualStructure(final @Nonnull IBlockState[][][] A, final @Nonnull String[] B ,final int size,final @Nullable String msg){
        Assertions.assertEquals(A.length,B.length);
        if(B.length == 0) return;
        assertEqualStructure(A,generateFromCharacters(size,size,B));
    }

    public void assertEqualStructure(final @Nonnull IBlockState[][][] A, final @Nonnull String[] B ,final int zLength,final int xLength){
        assertEqualStructure(A,B,zLength,xLength,null);
    }

    public void assertEqualStructure(final @Nonnull IBlockState[][][] A, final @Nonnull String[] B ,final int zLength,final int xLength,final @Nullable String msg){
        Assertions.assertEquals(A.length,B.length);
        if(B.length == 0) return;
        assertEqualStructure(A,generateFromCharacters(zLength,xLength,B));
    }

    @Nonnull
    public String[] serialise(final @Nonnull IBlockState[][][] structure){
        final String[] serialised = new String[structure.length];
        for(int y=0;y<structure.length;y++){
            final StringBuilder builder = new StringBuilder();
            for(int z =0;z<structure[y].length;z++){
                for(int x=0;x<structure[y][z].length;x++){
                    builder.append(characterToState.inverse().getOrDefault(structure[y][z][x],SerialisedBlockState.of("□")));
                }
                builder.append('\n');
            }
            serialised[y] = builder.toString();
        }
        return serialised;
    }

    public void print(final @Nonnull IBlockState[][][] structure){
        GeoCraftTest.LOGGER.info("Structure:\n{}",String.join("\n",serialise(structure)));
    }

    @Nonnull
    protected static List<SerialisedBlockState> parseLine(final @Nonnull String line){
        final List<SerialisedBlockState> serialisedBlockStates = new ArrayList<>();
        final int[] codePoints = line.codePoints().toArray();
        int i = 0;

        final @Nonnull StringBuilder curSerialisedState = new StringBuilder();

        while (i < codePoints.length) {
            final int cp = codePoints[i];

            if (curSerialisedState.length() == 0) {
                Assertions.assertFalse(SerialisedBlockState.isSubscript(cp),"The head of a line shouldn't be a subscript! Line: "+line);
                curSerialisedState.appendCodePoint(cp);
                i++;
            } else {
                if (SerialisedBlockState.isSubscript(cp)) { //下标，属于当前序列化状态
                    curSerialisedState.appendCodePoint(cp);
                    i++;
                } else {
                    serialisedBlockStates.add(SerialisedBlockState.of(curSerialisedState.toString()));
                    curSerialisedState.setLength(0);
                }
            }
        }
        if (curSerialisedState.length() > 0) serialisedBlockStates.add(SerialisedBlockState.of(curSerialisedState.toString())); //处理最后一个

        return serialisedBlockStates;
    }

    private static final class Impl extends MockSandboxEnvBuilder<Impl> {
        private Impl(final @Nonnull String name){
            super(name);
        }
    }
}
