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

package moe.qingu.nickel.nbt.path;

import com.google.common.collect.Lists;
import moe.qingu.nickel.nbt.NBTUtils;
import net.minecraft.nbt.*;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author QGMoe
 */
public final class NBTPathIndex extends NBTPathNode{
    private final int index;

    public NBTPathIndex(final int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public Collection<NBTBase> apply(final @Nonnull NBTBase nbtBase) {
        if(nbtBase instanceof NBTTagList){
            final NBTTagList list = (NBTTagList) nbtBase;
            if(index<0) return Lists.newArrayList(list);
            return list.tagCount() > index?Collections.singletonList(list.get(index)): Collections.emptyList();
        }else if(nbtBase instanceof NBTTagByteArray){
            final byte[] array = ((NBTTagByteArray) nbtBase).getByteArray();
            if(index<0) {
                final ArrayList<NBTBase> tags = new ArrayList<>();
                for(final byte b:array) tags.add(new NBTTagByte(b));
                return tags;
            }else return array.length>index?Collections.singletonList(new NBTTagByte(array[index])):Collections.emptyList();
        }else if(nbtBase instanceof NBTTagIntArray){
            final int[] array = ((NBTTagIntArray) nbtBase).getIntArray();
            if(index<0) {
                final ArrayList<NBTBase> tags = new ArrayList<>();
                for(final int i:array) tags.add(new NBTTagInt(i));
                return tags;
            }else return array.length>index?Collections.singletonList(new NBTTagInt(array[index])):Collections.emptyList();
        }else if(nbtBase instanceof NBTTagLongArray){
            final long[] array = NBTUtils.streamOf((NBTTagLongArray) nbtBase).toArray();
            if(index<0) {
                final ArrayList<NBTBase> tags = new ArrayList<>();
                for(final long l:array) tags.add(new NBTTagLong(l));
                return tags;
            }else return array.length>index?Collections.singletonList(new NBTTagLong(array[index])):Collections.emptyList();
        }else return Collections.emptyList();
    }
}
