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
import moe.qingu.nickel.reader.InputReader;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;
import org.junit.jupiter.api.Assertions;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * SNBT 解析测试的公共支持：InputReader 构造、YAML 数据集加载与期望 NBT 结构构造。
 * Claude Generated Tests
 * @author Claude
 * @see moe.qingu.nickel.nbt.SNBTReader
 * @see moe.qingu.nickel.nbt.SNBTScanner
 */
public final class SNBTTestSupport {
    private static final @Nonnull Yaml YAML = new Yaml();

    /**
     * Claude Generated
     * 工具类，禁止实例化
     */
    private SNBTTestSupport(){}

    /**
     * Claude Generated
     * 为给定原文构造 InputReader，并挂上无命令分支的 CommandContext（panic 时抛 NickelRuntimeException）
     */
    @Nonnull
    public static InputReader newInput(final @Nonnull String raw){
        final InputReader reader = new InputReader(raw);
        new CommandContext(reader,null,null,null); //构造器内部会把自身 set 给 reader
        return reader;
    }

    /**
     * Claude Generated
     * 用 ClassGraph 扫描指定资源目录下的所有 YAML 数据文件，读出全部用例（UTF-8）
     */
    @Nonnull
    public static Stream<SNBTCase> loadCases(final @Nonnull String dirPath){
        final List<SNBTCase> cases = new ArrayList<>();
        try (final @Nonnull ScanResult scan = new ClassGraph().acceptPaths(dirPath).scan()){
            final List<Resource> resources = new ArrayList<>(scan.getResourcesWithExtension("yaml"));
            resources.sort(Comparator.comparing(Resource::getPath));
            for(final @Nonnull Resource res:resources){
                try (final @Nonnull Reader reader = new InputStreamReader(res.open(), StandardCharsets.UTF_8)){
                    final List<Map<String,Object>> rawCases = YAML.load(reader);
                    if(rawCases == null) continue;
                    for(final @Nonnull Map<String,Object> raw:rawCases) cases.add(new SNBTCase(res.getPath(),raw));
                }catch (final IOException e){
                    Assertions.fail("error occurred when loading "+res.getPath(),e);
                }
            }
        }
        Assertions.assertFalse(cases.isEmpty(),"no test case found in "+dirPath);
        return cases.stream();
    }

    /**
     * Claude Generated
     * 把 YAML 里的"类型标注节点"（{type:...,value:...}）转换为真实 NBT 对象
     */
    @Nonnull
    public static NBTBase buildExpected(final @Nonnull Object spec){
        final Map<?,?> node = (Map<?,?>) spec;
        final String type = String.valueOf(node.get("type"));
        final Object value = node.get("value");
        switch (type){
            case "byte": return new NBTTagByte(((Number) value).byteValue());
            case "short": return new NBTTagShort(((Number) value).shortValue());
            case "int": return new NBTTagInt(((Number) value).intValue());
            case "long": return new NBTTagLong(((Number) value).longValue());
            case "float": return new NBTTagFloat(((Number) value).floatValue());
            case "double": return new NBTTagDouble(((Number) value).doubleValue());
            case "string": return new NBTTagString((String) value);
            case "list":{
                final NBTTagList list = new NBTTagList();
                for(final Object element:(List<?>) value) list.appendTag(buildExpected(element));
                return list;
            }
            case "compound":{
                final NBTTagCompound compound = new NBTTagCompound();
                for(final Map.Entry<?,?> entry:((Map<?,?>) value).entrySet())
                    compound.setTag(String.valueOf(entry.getKey()),buildExpected(entry.getValue()));
                return compound;
            }
            default:return Assertions.fail("unknown expected node type: "+type);
        }
    }

    /**
     * SNBT 解析测试用例：从 YAML 映射读出的名称、输入、入口模式、期望结构与期望扫描结果。
     * Claude Generated Tests
     * @author Claude
     */
    public static final class SNBTCase {
        /** 用例显示名（数据文件路径 + 用例名） */
        public final @Nonnull String name;
        /** SNBT 输入原文 */
        public final @Nonnull String input;
        /** 解析入口：compound（默认）或 single */
        public final @Nonnull String mode;
        /** 期望 NBT 结构的"类型标注节点"，非法输入用例为 null */
        public final @Nullable Object expected;
        /** 扫描期望结果（ok/eof/error），仅扫描数据集使用 */
        public final @Nullable String outcome;

        /**
         * Claude Generated
         * 从数据文件路径与单个用例的 YAML 映射构造用例
         */
        public SNBTCase(final @Nonnull String file,final @Nonnull Map<String,Object> raw){
            final Object rawName = raw.get("name");
            Assertions.assertNotNull(rawName,"case without name in "+file);
            this.name = file.substring(file.lastIndexOf('/')+1)+"#"+rawName;
            final Object rawInput = raw.get("input");
            Assertions.assertNotNull(rawInput,"case ["+this.name+"] without input");
            this.input = (String) rawInput;
            this.mode = raw.containsKey("mode")?String.valueOf(raw.get("mode")):"compound";
            this.expected = raw.get("expected");
            this.outcome = raw.containsKey("outcome")?String.valueOf(raw.get("outcome")):null;
        }

        /**
         * Claude Generated
         * 返回用例名，美化参数化测试报告
         */
        @Override
        public @Nonnull String toString(){
            return name;
        }
    }
}
