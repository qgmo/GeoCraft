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

package moe.qingu.nickel.nbt.matcher;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import moe.qingu.nickel.nbt.NBTUtils;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagLongArray;

import javax.annotation.Nonnull;

/**
 * @author QGMoe
 */
public final class NBTArrayMatcher extends NBTMatcher<NBTBase> {
    private final Class<? extends NBTBase> arrayType;
    private final LongOpenHashSet expectations = new LongOpenHashSet();

    public NBTArrayMatcher(final @Nonnull Class<? extends NBTBase> arrayType) {
        if(arrayType != NBTTagByteArray.class && arrayType != NBTTagIntArray.class && arrayType != NBTTagLongArray.class) throw new IllegalArgumentException();
        this.arrayType = arrayType;
    }

    public void expect(final long num){
        expectations.add(num);
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public Class<NBTBase> getMatchType() {
        return (Class<NBTBase>) arrayType;
    }

    @Override
    protected boolean _match(@Nonnull final NBTBase nbt) {
        final LongOpenHashSet nbts = new LongOpenHashSet();
        if(nbt instanceof NBTTagByteArray){
            for(final byte b: ((NBTTagByteArray)nbt).getByteArray()) nbts.add(b);
        }else if(nbt instanceof NBTTagIntArray){
            for(final int i: ((NBTTagIntArray)nbt).getIntArray()) nbts.add(i);
        }else {
            NBTUtils.streamOf((NBTTagLongArray) nbt)
                    .forEach(nbts::add);
        }
        final LongIterator iterator = expectations.iterator();
        while (iterator.hasNext()) if(!nbts.contains(iterator.nextLong())) return false;
        return true;
    }
}
