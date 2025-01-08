import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MyMethodVisitor extends MethodVisitor implements Opcodes {

    protected MyMethodVisitor(MethodVisitor methodVisitor) {
        super(ASM4, methodVisitor);
    }

    @Override
    public void visitCode() {
        super.visitCode();
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn("start");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }

    @Override
    public void visitInsn(int opcode) {
        if(((opcode >= IRETURN) && (opcode <= RETURN)) || (opcode == ATHROW)) {
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("end");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        }
        mv.visitInsn(opcode);
    }
}
