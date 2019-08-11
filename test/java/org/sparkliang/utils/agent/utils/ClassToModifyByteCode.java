package org.sparkliang.utils.agent.utils;

import java.util.Random;

public class ClassToModifyByteCode {

    public static Object testMethod(Object input){
        testMultiParamMethod("a","b");
        if(new Random().nextInt() > 0){
            return input;
        }else
            return input;
    }

    public static void testMultiParamMethod(String s,String s2){
        System.out.println(s + s2);
    }
}
