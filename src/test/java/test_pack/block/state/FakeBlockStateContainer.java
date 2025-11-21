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

import com.google.common.collect.*;
import net.minecraft.block.properties.IProperty;
import org.junit.Assert;
import test_pack.block.FakeBlock;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author QiguaiAAAA
 */
public class FakeBlockStateContainer {
    protected final ImmutableCollection<FakeBlockState> states;
    protected final FakeBlock block;
    protected final FakeBlockState baseState;
    public FakeBlockStateContainer(@Nonnull final FakeBlock block, IProperty<?>... properties) {
        this.block = block;
        if (properties.length == 0) {
            this.states = ImmutableList.of(baseState = new FakeBlockState(block, ImmutableMap.of()).setStateTable(ImmutableTable.of()));
            return;
        }
        final List<FakeBlockState> states = new ArrayList<>();
        final List<Map<IProperty<?>,Comparable<?>>> propertyMaps = new ArrayList<>();
        propertyMaps.add(new HashMap<>());

        final List<Map<IProperty<?>,Comparable<?>>> newMaps = new ArrayList<>();

        for(IProperty<?> property:properties){
            Assert.assertNotNull(property);
            newMaps.clear();
            propertyMaps.forEach(map->{
                Collection<? extends Comparable<?>> allowedValues = property.getAllowedValues();
                Assert.assertTrue(allowedValues.size()>0);
                Iterator<? extends Comparable<?>> iterator = allowedValues.iterator();
                map.put(property,iterator.next());
                while (iterator.hasNext()){
                    Map<IProperty<?>,Comparable<?>> newMap = new HashMap<>(map);
                    newMap.put(property,iterator.next());
                    newMaps.add(newMap);
                }
            });
            propertyMaps.addAll(newMaps);
        }

        final Table<IProperty<?>,Comparable<?>,FakeBlockState> stateTable = HashBasedTable.create();
        propertyMaps.forEach(map->{
            final FakeBlockState state = new FakeBlockState(block,ImmutableMap.copyOf(map));
            states.add(state);
            map.forEach((property,value)-> stateTable.put(property,value,state));
        });
        final ImmutableTable<IProperty<?>,Comparable<?>,FakeBlockState> immutableStateTable = ImmutableTable.copyOf(stateTable);
        states.forEach(state->state.setStateTable(immutableStateTable));
        this.states = ImmutableList.copyOf(states);
        baseState = states.iterator().next();
    }

    @Nonnull
    public FakeBlock getBlock() {
        return block;
    }

    @Nonnull
    public FakeBlockState getBaseState(){
        return baseState;
    }
}
