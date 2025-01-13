package com.r3944realms.bus;

import com.r3944realms.bus.api.Event;
import com.r3944realms.bus.api.EventListener;
import org.objectweb.asm.*;

import java.lang.constant.ConstantDescs;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 通过使用ASM框架和与 `defineHiddenClass`加载生成包装类，管理带有{@link com.r3944realms.bus.api.SubscribeEvent}的方法的 {@link EventListener}实例生成
 */
public class EventListenerFactory implements Opcodes {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final String HANDLER_DESC = Type.getInternalName(GeneratedEventListener.class);

    private static final String HANDLER_FUNC_DESC = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Event.class));
    private static final String INSTANCE_FUNC_DESC = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object.class), Type.getType(Event.class));

    private static final MethodType STATIC_HANDLER = MethodType.methodType(void.class, Event.class);
    private static final MethodType INSTANCE_HANDLER = MethodType.methodType(void.class, Object.class, Event.class);

    private static final MethodType STATIC_CONSTRUCTOR = MethodType.methodType(void.class);
    private static final MethodType INSTANCE_CONSTRUCTOR = MethodType.methodType(void.class, Object.class);

    private static final ConstantDynamic METHOD_CONSTANT = new ConstantDynamic(
            ConstantDescs.DEFAULT_NAME,
            MethodHandle.class.descriptorString(),
            new Handle(H_INVOKESTATIC, Type.getInternalName(MethodHandle.class), "classData",
                    MethodType.methodType(
                            Object.class,
                            MethodHandles.Lookup.class,
                            String.class,
                            Class.class
                    ).descriptorString(), false));

    private static final LockHelper<Method, MethodHandle> eventListenerFactories = LockHelper.withHashMap();

    private static MethodHandle getEventListenerFactory(Method m) {
        return eventListenerFactories.computeIfAbsent(m, EventListenerFactory::createWrapper0);
    }

    private static MethodHandle createWrapper0(Method callback) {
        try {
            callback.setAccessible(true);

            var handler = LOOKUP.unreflect(callback);
            var isStatic = Modifier.isStatic(callback.getModifiers());

            var boxedHandler = handler.asType(isStatic ? STATIC_HANDLER : INSTANCE_HANDLER);

            var classBytes = makeClass(EventListenerFactory.class.getName() + "$" + callback.getName(), isStatic);
            var classLookUp = LOOKUP.defineHiddenClassWithClassData(classBytes, boxedHandler, true);
            return classLookUp.findConstructor(classLookUp.lookupClass(), isStatic ? STATIC_CONSTRUCTOR : INSTANCE_CONSTRUCTOR);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create listener", e);
        }
    }

    private static byte[] makeClass(String name, boolean isStatic) {
        ClassWriter cv = new ClassWriter(0);

        String desc = name.replace('.', '/');
        cv.visit(V16, ACC_PUBLIC | ACC_FINAL,desc, null, HANDLER_DESC, null);

        cv.visitSource(".dynamic", null);
        if (!isStatic)
            cv.visitField(ACC_PRIVATE | ACC_FINAL, "instance", "Ljava/lang/Object;", null, null).visitEnd();
        {
            MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, HANDLER_DESC, "<init>", "()V", false);
            if (!isStatic) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(PUTFIELD, desc, "instance", "Ljava/lang/Object;");
            }
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "invoke", HANDLER_FUNC_DESC, null, null);
            mv.visitCode();
            mv.visitLdcInsn(METHOD_CONSTANT);
            if (!isStatic) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, desc, "instance", "Ljava/lang/Object;");
            }
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(
                    INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
                    isStatic ? HANDLER_FUNC_DESC : INSTANCE_FUNC_DESC, false
            );
            mv.visitInsn(RETURN);
            mv.visitMaxs(3, 2);
            mv.visitEnd();
        }
        cv.visitEnd();

        return cv.toByteArray();
    }

    public static EventListener create(Method callback, Object target) {
        try {
            var factory = getEventListenerFactory(callback);

            if (Modifier.isStatic(callback.getModifiers()))
                return (EventListener) factory.invoke();
            else
                return (EventListener) factory.invoke(target);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create IEventListener", e);
        }
    }
}
