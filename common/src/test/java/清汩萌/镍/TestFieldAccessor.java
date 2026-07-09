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

package 清汩萌.镍;

import moe.qingu.nickel.util.reflect.FieldAccessor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * @author QGMoe, Claude
 */
public class TestFieldAccessor {
    // ---- static fields ----
    public static String staticString = "hello";
    public static byte staticByte = 1;
    public static short staticShort = 2;
    public static int staticInt = 3;
    public static long staticLong = 4L;
    public static float staticFloat = 5.0f;
    public static double staticDouble = 6.0;
    public static char staticChar = 'A';
    public static boolean staticBoolean = false;

    // ---- instance fields ----
    public String instanceString = "world";
    public byte instanceByte = 10;
    public short instanceShort = 20;
    public int instanceInt = 30;
    public long instanceLong = 40L;
    public float instanceFloat = 50.0f;
    public double instanceDouble = 60.0;
    public char instanceChar = 'Z';
    public boolean instanceBoolean = true;

    protected @Nonnull FieldAccessor accessor(final String name) throws NoSuchFieldException, InvocationTargetException, IllegalAccessException, InstantiationException {
        final Field field = TestFieldAccessor.class.getField(name);
        return FieldAccessor.of(field);
    }

    // ======================== static field: String ========================

    /**
     * Claude Generated
     * 测试静态 String 字段的 get/set
     */
    @Test
    public void testStaticStringGetSet() throws Exception {
        final FieldAccessor acc = accessor("staticString");
        acc.set(null, "changed");
        Assertions.assertEquals("changed", acc.get(null));
        Assertions.assertEquals("changed", staticString);
    }

    // ======================== static field: byte ========================

    /**
     * Claude Generated
     * 测试静态 byte 字段的 getByte/setByte
     */
    @Test
    public void testStaticByteGetSet() throws Exception {
        final FieldAccessor acc = accessor("staticByte");
        acc.setByte(null, (byte) 42);
        Assertions.assertEquals((byte) 42, acc.getByte(null));
        Assertions.assertEquals((byte) 42, staticByte);
    }

    // ======================== static field: short ========================

    /**
     * Claude Generated
     * 测试静态 short 字段的 getShort/setShort
     */
    @Test
    public void testStaticShortGetSet() throws Exception {
        final FieldAccessor acc = accessor("staticShort");
        acc.setShort(null, (short) 256);
        Assertions.assertEquals((short) 256, acc.getShort(null));
        Assertions.assertEquals((short) 256, staticShort);
    }

    // ======================== static field: int ========================

    /**
     * Claude Generated
     * 测试静态 int 字段的 getInt/setInt
     */
    @Test
    public void testStaticIntGetSet() throws Exception {
        final FieldAccessor acc = accessor("staticInt");
        acc.setInt(null, 114514);
        Assertions.assertEquals(114514, acc.getInt(null));
        Assertions.assertEquals(114514, staticInt);
    }

    // ======================== static field: long ========================

    /**
     * Claude Generated
     * 测试静态 long 字段的 getLong/setLong
     */
    @Test
    public void testStaticLongGetSet() throws Exception {
        final FieldAccessor acc = accessor("staticLong");
        acc.setLong(null, 1919810L);
        Assertions.assertEquals(1919810L, acc.getLong(null));
        Assertions.assertEquals(1919810L, staticLong);
    }

    // ======================== static field: float ========================

    /**
     * Claude Generated
     * 测试静态 float 字段的 getFloat/setFloat
     */
    @Test
    public void testStaticFloatGetSet() throws Exception {
        final FieldAccessor acc = accessor("staticFloat");
        acc.setFloat(null, 3.14f);
        Assertions.assertEquals(3.14f, acc.getFloat(null));
        Assertions.assertEquals(3.14f, staticFloat);
    }

    // ======================== static field: double ========================

    /**
     * Claude Generated
     * 测试静态 double 字段的 getDouble/setDouble
     */
    @Test
    public void testStaticDoubleGetSet() throws Exception {
        final FieldAccessor acc = accessor("staticDouble");
        acc.setDouble(null, 2.718281828);
        Assertions.assertEquals(2.718281828, acc.getDouble(null));
        Assertions.assertEquals(2.718281828, staticDouble);
    }

    // ======================== static field: char ========================

    /**
     * Claude Generated
     * 测试静态 char 字段的 getChar/setChar
     */
    @Test
    public void testStaticCharGetSet() throws Exception {
        final FieldAccessor acc = accessor("staticChar");
        acc.setChar(null, '圆');
        Assertions.assertEquals('圆', acc.getChar(null));
        Assertions.assertEquals('圆', staticChar);
    }

    // ======================== static field: boolean ========================

    /**
     * Claude Generated
     * 测试静态 boolean 字段的 getBoolean/setBoolean
     */
    @Test
    public void testStaticBooleanGetSet() throws Exception {
        final FieldAccessor acc = accessor("staticBoolean");
        acc.setBoolean(null, true);
        Assertions.assertTrue(acc.getBoolean(null));
        Assertions.assertTrue(staticBoolean);
    }

    // ======================== instance field: String ========================

    /**
     * Claude Generated
     * 测试实例 String 字段的 get/set
     */
    @Test
    public void testInstanceStringGetSet() throws Exception {
        final FieldAccessor acc = accessor("instanceString");
        acc.set(this, "modified");
        Assertions.assertEquals("modified", acc.get(this));
        Assertions.assertEquals("modified", instanceString);
    }

    // ======================== instance field: byte ========================

    /**
     * Claude Generated
     * 测试实例 byte 字段的 getByte/setByte
     */
    @Test
    public void testInstanceByteGetSet() throws Exception {
        final FieldAccessor acc = accessor("instanceByte");
        acc.setByte(this, (byte) 99);
        Assertions.assertEquals((byte) 99, acc.getByte(this));
        Assertions.assertEquals((byte) 99, instanceByte);
    }

    // ======================== instance field: short ========================

    /**
     * Claude Generated
     * 测试实例 short 字段的 getShort/setShort
     */
    @Test
    public void testInstanceShortGetSet() throws Exception {
        final FieldAccessor acc = accessor("instanceShort");
        acc.setShort(this, (short) 512);
        Assertions.assertEquals((short) 512, acc.getShort(this));
        Assertions.assertEquals((short) 512, instanceShort);
    }

    // ======================== instance field: int ========================

    /**
     * Claude Generated
     * 测试实例 int 字段的 getInt/setInt
     */
    @Test
    public void testInstanceIntGetSet() throws Exception {
        final FieldAccessor acc = accessor("instanceInt");
        acc.setInt(this, 65536);
        Assertions.assertEquals(65536, acc.getInt(this));
        Assertions.assertEquals(65536, instanceInt);
    }

    // ======================== instance field: long ========================

    /**
     * Claude Generated
     * 测试实例 long 字段的 getLong/setLong
     */
    @Test
    public void testInstanceLongGetSet() throws Exception {
        final FieldAccessor acc = accessor("instanceLong");
        acc.setLong(this, Long.MAX_VALUE);
        Assertions.assertEquals(Long.MAX_VALUE, acc.getLong(this));
        Assertions.assertEquals(Long.MAX_VALUE, instanceLong);
    }

    // ======================== instance field: float ========================

    /**
     * Claude Generated
     * 测试实例 float 字段的 getFloat/setFloat
     */
    @Test
    public void testInstanceFloatGetSet() throws Exception {
        final FieldAccessor acc = accessor("instanceFloat");
        acc.setFloat(this, 1.414f);
        Assertions.assertEquals(1.414f, acc.getFloat(this));
        Assertions.assertEquals(1.414f, instanceFloat);
    }

    // ======================== instance field: double ========================

    /**
     * Claude Generated
     * 测试实例 double 字段的 getDouble/setDouble
     */
    @Test
    public void testInstanceDoubleGetSet() throws Exception {
        final FieldAccessor acc = accessor("instanceDouble");
        acc.setDouble(this, Math.PI);
        Assertions.assertEquals(Math.PI, acc.getDouble(this));
        Assertions.assertEquals(Math.PI, instanceDouble);
    }

    // ======================== instance field: char ========================

    /**
     * Claude Generated
     * 测试实例 char 字段的 getChar/setChar
     */
    @Test
    public void testInstanceCharGetSet() throws Exception {
        final FieldAccessor acc = accessor("instanceChar");
        acc.setChar(this, '方');
        Assertions.assertEquals('方', acc.getChar(this));
        Assertions.assertEquals('方', instanceChar);
    }

    // ======================== instance field: boolean ========================

    /**
     * Claude Generated
     * 测试实例 boolean 字段的 getBoolean/setBoolean
     */
    @Test
    public void testInstanceBooleanGetSet() throws Exception {
        final FieldAccessor acc = accessor("instanceBoolean");
        acc.setBoolean(this, false);
        Assertions.assertFalse(acc.getBoolean(this));
        Assertions.assertFalse(instanceBoolean);
    }

    // ======================== generic set() with autoboxing ========================

    /**
     * Claude Generated
     * 测试静态 int 字段通过泛型 set(Object, Object) 设值（自动装箱场景）
     */
    @Test
    public void testStaticIntGenericSet() throws Exception {
        final FieldAccessor acc = accessor("staticInt");
        acc.set(null, 777);
        Assertions.assertEquals(777, acc.get(null));
        Assertions.assertEquals(777, staticInt);
    }

    /**
     * Claude Generated
     * 测试实例 int 字段通过泛型 set(Object, Object) 设值（自动装箱场景）
     */
    @Test
    public void testInstanceIntGenericSet() throws Exception {
        final FieldAccessor acc = accessor("instanceInt");
        acc.set(this, 888);
        Assertions.assertEquals(888, acc.get(this));
        Assertions.assertEquals(888, instanceInt);
    }

    // ======================== null set for reference field ========================

    /**
     * Claude Generated
     * 测试将静态 String 字段设为 null
     */
    @Test
    public void testStaticStringSetNull() throws Exception {
        final FieldAccessor acc = accessor("staticString");
        acc.set(null, null);
        Assertions.assertNull(acc.get(null));
        Assertions.assertNull(staticString);
    }

    /**
     * Claude Generated
     * 测试将实例 String 字段设为 null
     */
    @Test
    public void testInstanceStringSetNull() throws Exception {
        final FieldAccessor acc = accessor("instanceString");
        acc.set(this, null);
        Assertions.assertNull(acc.get(this));
        Assertions.assertNull(instanceString);
    }
}
