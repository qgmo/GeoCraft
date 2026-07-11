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

package 清汩萌.镍.snbt;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import moe.qingu.nickel.command.context.CommandContext;
import moe.qingu.nickel.command.exception.NickelRuntimeException;
import moe.qingu.nickel.command.exception.NickelScanEOFSignal;
import moe.qingu.nickel.nbt.path.NBTPath;
import moe.qingu.nickel.nbt.path.NBTPathReader;
import moe.qingu.nickel.nbt.path.NBTPathScanner;
import moe.qingu.nickel.reader.InputReader;
import net.minecraft.command.CommandException;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import org.junit.jupiter.api.Assertions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static 清汩萌.造.造.YAML;

/**
 * NBTPath 测试共用工具：输入流构造、路径解析入口、异常信息键提取与 YAML 数据装载。
 * @author Claude
 */
public final class NBTPathTestSupport {
    private NBTPathTestSupport(){}

    /**
     * Claude Generated
     * 构造带 CommandContext 的 InputReader（带 context 才有组件化报错，供翻译键断言使用）
     */
    @Nonnull
    public static InputReader readerOf(final @Nonnull String raw){
        final InputReader input = new InputReader(raw);
        new CommandContext(input,null,null,null); //构造器会自动 input.setContext(this)，后三参在无分支的 panic 路径上不会被解引用
        return input;
    }

    /**
     * Claude Generated
     * 从字符串解析 NBTPath
     */
    @Nonnull
    public static NBTPath parse(final @Nonnull String raw) throws CommandException {
        return NBTPathReader.readPathFromInput(readerOf(raw));
    }

    /**
     * Claude Generated
     * 对字符串做 NBTPath 语法扫描
     */
    public static void scan(final @Nonnull String raw) throws CommandException, NickelScanEOFSignal {
        NBTPathScanner.scanPathFromInput(readerOf(raw));
    }

    /**
     * Claude Generated
     * 递归收集文本组件树（含格式化参数与兄弟组件）中所有翻译键
     */
    @Nonnull
    public static Set<String> collectKeys(final @Nullable ITextComponent component){
        final Set<String> keys = new HashSet<>();
        walk(component,keys);
        return keys;
    }

    private static void walk(final @Nullable ITextComponent component,final @Nonnull Set<String> keys){
        if(component == null) return;
        if(component instanceof TextComponentTranslation){
            final TextComponentTranslation translation = (TextComponentTranslation) component;
            keys.add(translation.getKey());
            for(final Object arg:translation.getFormatArgs())
                if(arg instanceof ITextComponent) walk((ITextComponent) arg,keys);
        }
        for(final ITextComponent sibling:component.getSiblings()) walk(sibling,keys);
    }

    /**
     * Claude Generated
     * 断言异常为 NickelRuntimeException 且其信息组件树含期望翻译键
     */
    public static void assertInfoHasKey(final @Nonnull CommandException e,final @Nonnull String key){
        Assertions.assertTrue(e instanceof NickelRuntimeException,"期望 NickelRuntimeException，实际 "+e.getClass().getName());
        final Set<String> keys = collectKeys(((NickelRuntimeException) e).getInformation());
        Assertions.assertTrue(keys.contains(key),"信息组件中找不到键 "+key+"，实际键集："+keys);
    }

    /**
     * Claude Generated
     * 用 ClassGraph 扫 classpath 资源并以 UTF-8 装载 YAML 用例表
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static List<Map<String,Object>> loadYamlCases(final @Nonnull String dir,final @Nonnull String leafName){
        try(final @Nonnull ScanResult scan = new ClassGraph().acceptPaths(dir).scan()){
            for(final Resource res:scan.getAllResources()){
                if(!res.getPath().endsWith("/"+leafName) && !res.getPath().equals(dir+leafName)) continue;
                try(final Reader reader = Files.newBufferedReader(Paths.get(res.getURI()),StandardCharsets.UTF_8)){
                    return (List<Map<String,Object>>) YAML.load(reader);
                }
            }
        }catch (final Exception e){
            throw new RuntimeException("装载数据文件 "+dir+leafName+" 失败",e);
        }
        throw new IllegalStateException("找不到数据文件 "+dir+leafName);
    }

    /**
     * Claude Generated
     * 从用例 Map 中取字符串字段（可空）
     */
    @Nullable
    public static String str(final @Nonnull Map<String,Object> map,final @Nonnull String key){
        final Object val = map.get(key);
        return val == null?null:val.toString();
    }
}
