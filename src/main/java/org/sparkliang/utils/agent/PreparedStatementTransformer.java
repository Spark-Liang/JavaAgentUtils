package org.sparkliang.utils.agent;

import org.sparkliang.utils.agent.exception.AgentException;
import org.sparkliang.utils.agent.statementlogger.PreparementdStatementLogger;
import org.sparkliang.utils.agent.utils.ByteCodeUtils;
import org.sparkliang.utils.agent.utils.PreparedStatementMethodDetactUtils;
import javassist.*;
import javassist.bytecode.*;
import org.apache.log4j.Logger;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

public class PreparedStatementTransformer implements ClassFileTransformer {
    private static Logger LOGGER = Logger.getLogger(PreparedStatementTransformer.class);

    private final static String GENERATEDCLASS_DUMPPATH = System.getProperty("user.home") + "/tmp/PreparedStatementLogAgent/ClassDump";

    private final static String PREPAREDSTATEMENT_CLASSNAME = PreparedStatement.class.getName();
    private final static String JDBCCONNECTION_CLASSNAME = Connection.class.getName();
    private final Map<Class, CtClass> BASIC_TYPE_MAP = new HashMap<Class, CtClass>();

    private ClassPool cp = ClassPool.getDefault();


    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        LOGGER.info("transform class : " + className);

        String klassName = className.replace('/', '.');
        CtClass ctClass = null;
        try {
            ctClass = cp.get(klassName);
            // skip the abstract method
            if (ctClass.isInterface()) {
                return null;
            }
            if (isInstanceOfPreparedStatement(ctClass)) {
                return transformPreparedStatement(ctClass);
            }
            if (isInstanceOfJDBCConnection(ctClass)) {
                return transformJDBCConnection(ctClass);
            }
        } catch (NotFoundException e) {
            String msg = String.format("class \"%s\" can't found.", klassName);
            LOGGER.info(msg);
            throw new AgentException(msg);
        }

        LOGGER.info(String.format("class \"%s\" not need to transform.", klassName));
        return null;
    }

    private byte[] transformJDBCConnection(CtClass ctClass) {
        LOGGER.info(String.format("Begin transform JDBC Connection Class '%s'.", ctClass.getName()));
        try {
            for (CtMethod ctMethod : ctClass.getMethods()) {
                // skip the abstract method
                if (ctMethod.isEmpty()) {
                    continue;
                }

                if (ctMethod.getName().startsWith("prepare")) {
                    processSetSQL(ctMethod);
                }
            }
            return getClassfileBytesAndWriteToDumpFile(ctClass);

        } catch (CannotCompileException e) {
            String msg = String.format("Failed to modify the method.Reason: '%s'", e.getMessage());
            LOGGER.error(msg);
            throw new AgentException(msg, e);
        } catch (IOException e) {
            String msg = String.format("Failed to get the class bytecode.Reason: '%s'", e.getMessage());
            LOGGER.error(msg);
            throw new AgentException(msg, e);
        }
    }


    private byte[] transformPreparedStatement(CtClass ctClass) {
        LOGGER.info(String.format("Begin transform PreparedStatement Class '%s'.", ctClass.getName()));

        try {
            for (CtMethod ctMethod : ctClass.getMethods()) {
                // skip the abstract method
                if (ctMethod.isEmpty()) {
                    continue;
                }

                processSetParamMethod(ctMethod);

                if (ctMethod.getName().startsWith("execute")) {
                    try {
                        CodeAttribute codeAttribute = ctMethod.getMethodInfo().getCodeAttribute();
                        CodeIterator codeIterator = codeAttribute.iterator();
                        Bytecode bytecode = new Bytecode(codeAttribute.getConstPool(), 0, codeAttribute.getMaxLocals());

                        bytecode.addOpcode(Bytecode.ALOAD_0);
                        bytecode.addInvokestatic(
                                cp.get(PreparementdStatementLogger.class.getName())
                                , "logExecute"
                                , CtClass.voidType
                                , new CtClass[]{cp.get(PreparedStatement.class.getName())}
                        );

                        codeIterator.insertAt(0, bytecode.get());
                    } catch (NotFoundException e) {
                        String msg = String.format("Failed to find the class. Reason :'%s'.", e.getMessage());
                        LOGGER.error(msg);
                        throw new AgentException(msg);
                    } catch (BadBytecode e) {
                        String msg = String.format("Error for bad bytecode. Reason :'%s'.", e.getMessage());
                        LOGGER.error(msg);
                        throw new AgentException(msg);
                    }
                }
            }

            return getClassfileBytesAndWriteToDumpFile(ctClass);
        } catch (CannotCompileException e) {
            String msg = String.format("Failed to modify the method.Reason: '%s'", e.getMessage());
            LOGGER.error(msg);
            throw new AgentException(msg, e);
        } catch (IOException e) {
            String msg = String.format("Failed to get the class bytecode.Reason: '%s'", e.getMessage());
            LOGGER.error(msg);
            throw new AgentException(msg, e);
        }
    }

    private byte[] getClassfileBytesAndWriteToDumpFile(CtClass ctClass) throws IOException, CannotCompileException {
        try (DataOutputStream out = new DataOutputStream(
                new FileOutputStream(GENERATEDCLASS_DUMPPATH + "\\" + ctClass.getName() + ".class"))
        ) {
            ctClass.toBytecode(out);
        } catch (Exception e) {
            LOGGER.info(String.format(
                    "Failed to write the generate classfile to '%s'. \nReason is '%s'.", GENERATEDCLASS_DUMPPATH, e.getMessage()
            ));
        }
        return ctClass.toBytecode();
    }

    /**
     * Insert the following statement before the return statement in the original method:
     * PreparementdStatementLogger.logSQL(the value to return, sql string);
     *
     * @param ctMethod
     */
    private void processSetSQL(CtMethod ctMethod) {
        MethodInfo methodInfo = ctMethod.getMethodInfo();
        CodeAttribute codeAttribute = methodInfo.getCodeAttribute();

        Bytecode bytecode = new Bytecode(codeAttribute.getConstPool(), 0, codeAttribute.getMaxLocals());
        bytecode.addOpcode(Bytecode.DUP);
        bytecode.addOpcode(Bytecode.ALOAD_1);
        String stmtLoggerClassname = PreparementdStatementLogger.class.getName();
        CtClass ctPreparementdStatementLogger = null;
        try {
            ctPreparementdStatementLogger = cp.get(stmtLoggerClassname);
        } catch (NotFoundException e) {
            try {
                cp.appendClassPath(PreparedStatementTransformer.class.getClassLoader().getResource("").toString());
                ctPreparementdStatementLogger = cp.get(stmtLoggerClassname);
            } catch (NotFoundException ex) {
                String msg = String.format(
                        "failed to get the PreparedStatementLogger from ClassPool. Reason : '%s'"
                        , ex.getMessage()
                );
                LOGGER.error(msg);
                throw new AgentException(msg, ex);
            }
        }
        bytecode.addInvokestatic(
                ctPreparementdStatementLogger
                , "logSQL"
                , CtClass.voidType
                , new CtClass[]{
                        cp.getOrNull("java.sql.PreparedStatement"),
                        cp.getOrNull("java.lang.String")
                }
        );

        try {
            ByteCodeUtils.insertBeforeCertainOpcode(ctMethod, Bytecode.ARETURN, bytecode);
        } catch (BadBytecode e) {
            String msg = String.format("Failed to add the log statement. Reason :'%s'.", e.getMessage());
            LOGGER.error(msg);
            throw new AgentException(msg);
        }
    }


    private void processSetParamMethod(CtMethod ctMethod) {
        String methodName = null;
        CtClass[] paramTypes = null;
        CtClass returnType = null;
        try {
            methodName = ctMethod.getName();
            paramTypes = ctMethod.getParameterTypes();
            returnType = ctMethod.getReturnType();
        } catch (NotFoundException e) {
            String msg = "Failed to parsed the method information.";
            LOGGER.info(msg);
            throw new AgentException(msg);
        }

        CodeAttribute codeAttribute = ctMethod.getMethodInfo().getCodeAttribute();
        Bytecode bytecode = new Bytecode(codeAttribute.getConstPool(), 0, codeAttribute.getMaxLocals());

        bytecode.addOpcode(Bytecode.ALOAD_0);
        bytecode.addOpcode(Bytecode.ILOAD_1);


        // handle setvalue
        if (PreparedStatementMethodDetactUtils.isSetBoolean(methodName, paramTypes, returnType)) {
            bytecode.addOpcode(Bytecode.ILOAD_2);
            insertAutoBoxingStatement(bytecode, Boolean.class);
        }
        if (PreparedStatementMethodDetactUtils.isSetByte(methodName, paramTypes, returnType)) {
            bytecode.addOpcode(Bytecode.ILOAD_2);
            insertAutoBoxingStatement(bytecode, Byte.class);
        }
        if (PreparedStatementMethodDetactUtils.isSetShort(methodName, paramTypes, returnType)) {
            bytecode.addOpcode(Bytecode.ILOAD_2);
            insertAutoBoxingStatement(bytecode, Short.class);
        }
        if (PreparedStatementMethodDetactUtils.isSetInt(methodName, paramTypes, returnType)) {
            bytecode.addOpcode(Bytecode.ILOAD_2);
            insertAutoBoxingStatement(bytecode, Integer.class);
        }
        if (PreparedStatementMethodDetactUtils.isSetLong(methodName, paramTypes, returnType)) {
            bytecode.addOpcode(Bytecode.LLOAD_2);
            insertAutoBoxingStatement(bytecode, Long.class);
        }
        if (PreparedStatementMethodDetactUtils.isSetFloat(methodName, paramTypes, returnType)) {
            bytecode.addOpcode(Bytecode.FLOAD_2);
            insertAutoBoxingStatement(bytecode, Float.class);
        }
        if (PreparedStatementMethodDetactUtils.isSetDouble(methodName, paramTypes, returnType)) {
            bytecode.addOpcode(Bytecode.DLOAD_2);
            insertAutoBoxingStatement(bytecode, Double.class);
        }
        if (PreparedStatementMethodDetactUtils.isSetOtherTypeParam(methodName, paramTypes, returnType)) {
            bytecode.addOpcode(Bytecode.ALOAD_2);
        }

        // handle set null
        if (PreparedStatementMethodDetactUtils.isSetNull(methodName, paramTypes, returnType)) {
            bytecode.addOpcode(Bytecode.ACONST_NULL);
        }
        try {
            CtClass preparedStatementLoggerClass = cp.get(PreparementdStatementLogger.class.getName());
            bytecode.addInvokestatic(
                    preparedStatementLoggerClass
                    , "logSetParemeter"
                    , CtClass.voidType
                    , new CtClass[]{
                            cp.get(PreparedStatement.class.getName())
                            , CtClass.intType
                            , cp.get(Object.class.getName())
                    }
            );

            CodeIterator codeIterator = codeAttribute.iterator();
            codeIterator.insertAt(0, bytecode.get());
        } catch (NotFoundException e) {
            String msg = String.format("Failed to add the statement. Reason :'%s'.", e.getMessage());
            LOGGER.error(msg);
            throw new AgentException(msg);
        } catch (BadBytecode e) {
            String msg = String.format("Error for bad bytecode. Reason :'%s'.", e.getMessage());
            LOGGER.error(msg);
            throw new AgentException(msg);
        }
    }

    private void insertAutoBoxingStatement(Bytecode bytecode, Class boxType) {
        CtClass boxTypeCtClass = BASIC_TYPE_MAP.get(boxType);
        bytecode.addInvokestatic(
                boxTypeCtClass
                , "valueOf"
                , boxTypeCtClass
                , new CtClass[]{BASIC_TYPE_MAP.get(boxType)}
        );
    }

    private boolean isInstanceOfJDBCConnection(CtClass klass) {
        if (isImplementInterface(klass, JDBCCONNECTION_CLASSNAME)) return true;
        CtClass parentClass = null;
        try {
            parentClass = klass.getSuperclass();
        } catch (NotFoundException e) {
        }
        if (parentClass != null) {
            return isInstanceOfJDBCConnection(parentClass);
        }
        return false;
    }

    private boolean isInstanceOfPreparedStatement(CtClass klass) {
        if (isImplementInterface(klass, PREPAREDSTATEMENT_CLASSNAME))
            return true;
        CtClass parentClass = null;
        try {
            parentClass = klass.getSuperclass();
        } catch (NotFoundException e) {
        }
        if (parentClass != null) {
            return isInstanceOfPreparedStatement(parentClass);
        }
        return false;
    }

    private boolean isImplementInterface(CtClass klass, String interfaceName) {
        CtClass[] interfaces = null;
        try {
            interfaces = klass.getInterfaces();
        } catch (NotFoundException e) {
        }
        if (interfaces != null) {
            for (CtClass ctInterface : interfaces) {
                if (interfaceName.equals(ctInterface.getName())) {
                    return true;
                }
            }
        }
        return false;
    }


    {
        try {
            BASIC_TYPE_MAP.put(Object.class, cp.get(Object.class.getName()));
            BASIC_TYPE_MAP.put(String.class, cp.get(String.class.getName()));
            BASIC_TYPE_MAP.put(Boolean.class, cp.get(Boolean.class.getName()));
            BASIC_TYPE_MAP.put(Byte.class, cp.get(Byte.class.getName()));
            BASIC_TYPE_MAP.put(Short.class, cp.get(Short.class.getName()));
            BASIC_TYPE_MAP.put(Integer.class, cp.get(Integer.class.getName()));
            BASIC_TYPE_MAP.put(Long.class, cp.get(Long.class.getName()));
            BASIC_TYPE_MAP.put(Float.class, cp.get(Float.class.getName()));
            BASIC_TYPE_MAP.put(Double.class, cp.get(Double.class.getName()));
        } catch (NotFoundException e) {
            String msg = String.format("Failed to find the Basic type. Reason : '%s'.", e.getMessage());
            LOGGER.error(msg);
            throw new AgentException(msg);
        }
    }

    static {
        File file = new File(GENERATEDCLASS_DUMPPATH);
        if (!file.exists()) {
            file.mkdirs();
        }
    }
}
