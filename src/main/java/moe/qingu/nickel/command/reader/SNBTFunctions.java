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

package moe.qingu.nickel.command.reader;

import moe.qingu.nickel.command.exception.NickelRuntimeException;
import moe.qingu.nickel.command.node.parameter.generic.UUIDNode;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagLong;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;

import static moe.qingu.nickel.text.Texts.translation;

/**
 * @author QGMoe
 */
@SuppressWarnings("unused")
public final class SNBTFunctions {
    private static final MethodHandles.Lookup PERMISSION = MethodHandles.lookup();

    @Nonnull
    @SNBTFunc(require = 1)
    public static NBTBase uuid(final @Nonnull String[] args) throws NickelRuntimeException {
        expectedArgCount(1,args);
        try {
            final UUID uuid = UUID.fromString(args[0]);
            final long most = uuid.getMostSignificantBits();
            final long least = uuid.getLeastSignificantBits();
            return new NBTTagIntArray(new int[]{
                    (int)(most >> Integer.SIZE),
                    (int)most,
                    (int)(least >> Integer.SIZE),
                    (int)least
            });
        }catch (final @Nonnull IllegalArgumentException e){
            throw new NickelRuntimeException(UUIDNode.buildUUIDFormatErrorInfo(e,args[0]));
        }
    }

    @Nonnull
    @SNBTFunc(require = 1)
    public static NBTBase uuid_m(final @Nonnull String[] args) throws NickelRuntimeException {
        expectedArgCount(1,args);
        try {
            return new NBTTagLong(UUID.fromString(args[0]).getMostSignificantBits());
        }catch (final @Nonnull IllegalArgumentException e){
            throw new NickelRuntimeException(UUIDNode.buildUUIDFormatErrorInfo(e,args[0]));
        }
    }

    @Nonnull
    @SNBTFunc(require = 1)
    public static NBTBase uuid_l(final @Nonnull String[] args) throws NickelRuntimeException {
        expectedArgCount(1,args);
        try {
            return new NBTTagLong(UUID.fromString(args[0]).getLeastSignificantBits());
        }catch (final @Nonnull IllegalArgumentException e){
            throw new NickelRuntimeException(UUIDNode.buildUUIDFormatErrorInfo(e,args[0]));
        }
    }

    public static void expectedArgCount(final int expected,final @Nonnull String[] args) throws NickelRuntimeException {
        if(args.length != expected) throw new NickelRuntimeException(translation("nickel.command.parameter.nbt.function.count_mismatch",expected,args.length));
    }

    public static void loadFuncs(){
        final Method[] methods = SNBTFunctions.class.getDeclaredMethods();
        for(final Method method:methods){
            if(!Modifier.isStatic(method.getModifiers())) continue;
            if(!method.isAnnotationPresent(SNBTFunc.class)) continue;
            final SNBTFunc annotation = method.getAnnotation(SNBTFunc.class);
            method.setAccessible(true);
            try{
                final MethodHandle handle = PERMISSION.unreflect(method);

                final @Nonnull CallSite site = LambdaMetafactory.metafactory(
                        PERMISSION,
                        "invoke",
                        MethodType.methodType(SNBTReader.NBTFunction.class),
                        SNBTReader.NBTFunction.METHOD_TYPE,
                        handle,
                        SNBTReader.NBTFunction.METHOD_TYPE
                );
                final SNBTReader.NBTFunction func = (SNBTReader.NBTFunction) site.getTarget().invoke();
                SNBTReader.registerSNBTFunc(Pair.of(method.getName(),annotation.require()),func);
            } catch (final @Nonnull Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface SNBTFunc{
        int require();
    }
}
