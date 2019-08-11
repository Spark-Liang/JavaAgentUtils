package org.sparkliang.utils.agent.utils;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.Assert;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;

@RunWith(JUnit4.class)
public class TestByteCodeUtils {

    public static Object testResult = null;

    private ClassPool cp = ClassPool.getDefault();
    @Before
    public void setUp(){

    }

    @Test
    public void testInsertBeforeReturn() throws Exception{
        //given
        CtClass ctClass = cp.getCtClass("ClassToModifyByteCode");
        CtMethod ctMethod = null;
        for (CtMethod m: ctClass.getMethods()){
            if("testMethod".equals(m.getName())){
                ctMethod = m;
                break;
            }
        }

        CtClass testResultClass = cp.getCtClass(TestByteCodeUtils.class.getName());
        CodeAttribute codeAttribute = ctMethod.getMethodInfo().getCodeAttribute();
        Bytecode bytecode = new Bytecode(codeAttribute.getConstPool(),0,codeAttribute.getMaxLocals());
        bytecode.addOpcode(Bytecode.DUP);
        bytecode.addInvokestatic(
                testResultClass,
                "setTestResult",
                CtClass.voidType,
                new CtClass[]{cp.get("java.lang.Object")}
        );

        //when
        ByteCodeUtils.insertBeforeCertainOpcode(ctMethod,Bytecode.ARETURN,bytecode);
        Class testClass = ctClass.toClass();
        Object testInstance = testClass.newInstance();
        Method testMethod = testClass.getMethod("testMethod",Object.class);
        Object expectedResult = new Object();
        testMethod.invoke(testMethod,expectedResult);

        //then
        ctClass.toBytecode(new DataOutputStream(new FileOutputStream("C:\\Spark\\Java\\ProjectData\\TestJavaAgent\\out\\test_result\\NewClass.class")));
        Assert.assertEquals(expectedResult,testResult);

    }

    public static void setTestResult(Object result ){
        testResult = result;
    }

}

