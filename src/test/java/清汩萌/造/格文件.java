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

package 清汩萌.造;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import 清汩萌.造.工具.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author QiguaiAAAA
 */
public final class 格文件 {
    private static final Function<Object,String> STRIPE = o -> StringUtil.strip(o.toString());
    private static final int[] $关键字 = "略用".codePoints().toArray();
    private static final @Nonnull IntOpenHashSet $关键字集合 = new IntOpenHashSet($关键字,1.0f);

    private @Nullable Map<String,Object> $头部信息;
    private @Nullable String $名称;
    private int $层数;
    private int $行数;
    private int $列数;
    private @Nullable String $默认方块;
    private @Nullable Map<String,Object> $附加数据;

    private String[] $层;

    static {
        $关键字集合.trim();
    }

    private 格文件(){};

    public static boolean 是关键字(final int codePoint){
        return $关键字集合.contains(codePoint);
    }

    public static 格文件 解析(@Nonnull final File file){
        final 格文件 res = new 格文件();
        try (final BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)){
            res.$头部信息 = 解析头部信息(reader);
            res.$层 = 解析层(reader).toArray(new String[0]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        res.初始化元数据();
        return res;
    }

    @Nullable
    public String 获取名称() {
        return $名称;
    }

    public int 获取层数(){
        return $层数;
    }

    public int 获取行数() {
        return $行数;
    }

    public int 获取列数() {
        return $列数;
    }

    @Nullable
    public Map<String, Object> 获取附加数据() {
        return $附加数据;
    }

    @Nullable
    private static Map<String,Object> 解析头部信息(final @Nonnull BufferedReader reader) throws IOException {
        final @Nullable String firstLine = reader.readLine();
        if(firstLine == null) return null;
        final boolean hasFrontmatter = "---".equals(StringUtil.strip(firstLine.split("#",2)[0]));

        if(hasFrontmatter){
            final @Nonnull StringBuilder builder = new StringBuilder();
            while (true){
                final String line = reader.readLine();
                if(line == null) throw new RuntimeException("不完整的头部信息");
                if("---".equals(StringUtil.strip(line.split("#",2)[0]))) break;
                builder.append(line);
            }
            return 造.YAML.load(builder.toString());
        }else return null;
    }

    @Nonnull
    private static List<String> 解析层(final @Nonnull BufferedReader reader) throws IOException {
        final @Nonnull List<String> layers = new ArrayList<>();
        String line;
        StringBuilder builder = new StringBuilder();
        while (true){
            line = reader.readLine();
            if(line == null) break;
            line = StringUtil.strip(line.split("#",2)[0]);
            if("---".equals(line)) break;
            if(line.isEmpty()){
                if(builder != null){
                    layers.add(builder.toString());
                    builder = null;
                }
            }else {
                if(builder == null) builder = new StringBuilder();
                builder.append(line.codePoints()
                        .filter(cp -> !Character.isWhitespace(cp))
                        .collect(StringBuilder::new,
                                StringBuilder::appendCodePoint,
                                StringBuilder::append)
                );
            }
        }
        return layers;
    }

    @SuppressWarnings("unchecked")
    private void 初始化元数据(){
        if($头部信息 != null) {
            $名称 = computeIfNonNull($头部信息.get("name"), STRIPE);
            $默认方块 = computeIfNonNull($头部信息.get("default"),STRIPE);

            @Nullable List<Integer> $大小 = (List<Integer>) $头部信息.get("size");
            if($大小 != null){
                $层数 = $大小.get(0);
                $行数 = $大小.get(1);
                $列数 = $大小.get(2);
            }
            $附加数据 = (Map<String,Object>) $头部信息.get("ext");
        }else{
            throw new RuntimeException(); //TODO
        }
    }

    private static <T> T computeIfNonNull(final Object obj, final @Nonnull Function<Object,T> mapping){
        return obj == null?null:mapping.apply(obj);
    }
}
