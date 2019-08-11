package org.sparkliang.utils.agent.utils;

import javassist.CtMethod;
import javassist.bytecode.*;

public class ByteCodeUtils {

    /**
     * @param ctMethod
     * @param bytecode
     */
    public static void insertBeforeCertainOpcode(CtMethod ctMethod,int opcode, Bytecode bytecode) throws BadBytecode {

        CodeAttribute codeAttribute = ctMethod.getMethodInfo().getCodeAttribute();
        CodeIterator ci = codeAttribute.iterator();
        while (ci.hasNext()){
            int index = ci.next();
            int op = ci.byteAt(index);
            if(op == opcode){
                ci.insert(index,bytecode.get());
            }
        }
    }

}
