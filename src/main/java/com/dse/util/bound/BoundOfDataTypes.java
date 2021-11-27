package com.dse.util.bound;

import auto_testcase_generation.config.PrimitiveBound;
import com.dse.config.IFunctionConfigBound;

import java.util.Map;

public class BoundOfDataTypes {
    public DataSizeModel bounds = new DataSizeModel();

    private static long TWO_POWER_7 = 127;
    private static long TWO_POWER_8 = 255;

    private static long TWO_POWER_15 = 32768l;
    private static long TWO_POWER_16 = 65535l;

    private static long TWO_POWER_31 = 2147483648l;
    private static long TWO_POWER_32 = 4294967295l;

    private static long TWO_POWER_63 = 9223372036854775807l;
    private static double TWO_POWER_64 = 18446744073709551615f;//9223372036854775807l * 2;

    /**
     * Ref: https://en.cppreference.com/w/cpp/language/types
     *
     * @return
     */
    public DataSizeModel createLP32() {
        DataSizeModel bounds = new DataSizeModel();

        // Character types
        bounds.put("char", new PrimitiveBound(0, TWO_POWER_8));
        bounds.put("char8_t", new PrimitiveBound(0, TWO_POWER_7));
        bounds.put("char16_t", new PrimitiveBound(0, TWO_POWER_16));
        bounds.put("char32_t", new PrimitiveBound(0, TWO_POWER_32));
        bounds.put("wchar_t", new PrimitiveBound(0, TWO_POWER_32));

        // Integer types (signed)
        bounds.put("signed char", new PrimitiveBound(-TWO_POWER_7, TWO_POWER_7));

        bounds.put("short", new PrimitiveBound(-TWO_POWER_15, TWO_POWER_15));
        bounds.put("signed short int", bounds.get("short"));
        bounds.put("signed short", bounds.get("short"));
        bounds.put("short int", bounds.get("short"));

        bounds.put("int", new PrimitiveBound(-TWO_POWER_15, TWO_POWER_15));
        bounds.put("signed", bounds.get("int"));
        bounds.put("signed int", bounds.get("int"));

        bounds.put("long", new PrimitiveBound(-TWO_POWER_31, TWO_POWER_31));
        bounds.put("long int", bounds.get("long"));
        bounds.put("signed long", bounds.get("long"));
        bounds.put("signed long int", bounds.get("long"));

        bounds.put("long long", new PrimitiveBound(-TWO_POWER_63, TWO_POWER_63));
        bounds.put("long long int", bounds.get("long long"));
        bounds.put("signed long long", bounds.get("long long"));
        bounds.put("signed long long int", bounds.get("long long"));

        // Integer types (unsigned)
        bounds.put("unsigned char", new PrimitiveBound(0, TWO_POWER_8));//
        bounds.put("unsigned short", new PrimitiveBound(0, TWO_POWER_16));//
        bounds.put("unsigned short int", new PrimitiveBound(0, TWO_POWER_16));//
        bounds.put("unsigned int", new PrimitiveBound(0, TWO_POWER_16));//
        bounds.put("unsigned long int", new PrimitiveBound(0, TWO_POWER_32));//
        bounds.put("unsigned", new PrimitiveBound(0, TWO_POWER_16));
        bounds.put("unsigned long long int", new PrimitiveBound(0 + "", TWO_POWER_64 + ""));//
        bounds.put("unsigned long", new PrimitiveBound(0, TWO_POWER_32));//
        bounds.put("unsigned long long", new PrimitiveBound(0 + "", TWO_POWER_64 + ""));//

        // Floating-point types
        bounds.put("float", new PrimitiveBound(-TWO_POWER_31, TWO_POWER_31));
        bounds.put("double", new PrimitiveBound(-TWO_POWER_63, TWO_POWER_63));
        bounds.put("long double", new PrimitiveBound(-TWO_POWER_63, TWO_POWER_63));

        // Boolean type
        bounds.put("bool", new PrimitiveBound(0, 1));

        return bounds;
    }

    public DataSizeModel getBounds() {
        return bounds;
    }

    public void setBounds(DataSizeModel bounds) {
        this.bounds = bounds;
    }
}
