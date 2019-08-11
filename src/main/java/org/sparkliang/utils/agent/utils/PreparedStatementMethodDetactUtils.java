package org.sparkliang.utils.agent.utils;

import javassist.CtClass;

import java.util.Arrays;
import java.util.HashSet;

public class PreparedStatementMethodDetactUtils {

    public static boolean isSetNull(String name,CtClass[] paramTypes, CtClass returnType){
        return "setNull".equals(name)
                && Arrays.equals(new CtClass[]{CtClass.intType, CtClass.intType}, paramTypes)
                && CtClass.voidType.equals(returnType);
    }

    public static boolean isSetBoolean(String name,CtClass[] paramTypes, CtClass returnType){
        return "setBoolean".equals(name)
                && Arrays.equals(new CtClass[]{CtClass.intType, CtClass.booleanType}, paramTypes)
                && CtClass.voidType.equals(returnType);
    }

    public static boolean isSetByte(String name,CtClass[] paramTypes, CtClass returnType){
        return "setByte".equals(name)
                && Arrays.equals(new CtClass[]{CtClass.intType, CtClass.byteType}, paramTypes)
                && CtClass.voidType.equals(returnType);
    }

    public static boolean isSetShort(String name,CtClass[] paramTypes, CtClass returnType){
        return "setShort".equals(name)
                && Arrays.equals(new CtClass[]{CtClass.intType, CtClass.shortType}, paramTypes)
                && CtClass.voidType.equals(returnType);
    }

    public static boolean isSetInt(String name,CtClass[] paramTypes, CtClass returnType){
        return "setInt".equals(name)
                && Arrays.equals(new CtClass[]{CtClass.intType, CtClass.intType}, paramTypes)
                && CtClass.voidType.equals(returnType);
    }

    public static boolean isSetLong(String name,CtClass[] paramTypes, CtClass returnType){
        return "setLong".equals(name)
                && Arrays.equals(new CtClass[]{CtClass.intType, CtClass.longType}, paramTypes)
                && CtClass.voidType.equals(returnType);
    }

    public static boolean isSetFloat(String name,CtClass[] paramTypes, CtClass returnType){
        return "setFloat".equals(name)
                && Arrays.equals(new CtClass[]{CtClass.intType, CtClass.floatType}, paramTypes)
                && CtClass.voidType.equals(returnType);
    }

    public static boolean isSetDouble(String name,CtClass[] paramTypes, CtClass returnType){
        return "setDouble".equals(name)
                && Arrays.equals(new CtClass[]{CtClass.intType, CtClass.doubleType}, paramTypes)
                && CtClass.voidType.equals(returnType);
    }

    private static final HashSet<String> SET_OTHER_TYPE_PARAM_METHOD_NAMES = new HashSet<String>(Arrays.asList(
            "setBigDecimal","setString","setBytes","setDate","setTime","setTimestamp","setObject","setRef"
            ,"setBlob","setClob","setArray","setURL","setNString","setNClob","setSQLXML"
    ));
    public static boolean isSetOtherTypeParam(String name,CtClass[] paramTypes, CtClass returnType){
        return SET_OTHER_TYPE_PARAM_METHOD_NAMES.contains(name) 
                && 2 == paramTypes.length;
    }
}
