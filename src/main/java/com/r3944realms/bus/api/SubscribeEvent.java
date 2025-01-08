package com.r3944realms.bus.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 这是将方法订阅到Event的注解。<br/>
 * 该注解只能应用于单参数方法，其中单参数方法是{@link Event}的子类。<br/>
 * 使用{@link IEventBus}。<br/>
 * <code>register (Object)</code>将Object实例或Class提交到事件总线进行扫描，已生成回调的{@link EventListener}包装器<br/>
 * 事件总线系统生成一个ASM包装器，将其分派给标记方法。
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface SubscribeEvent {
    EventPriority priority() default EventPriority.NORMAL;

    boolean receiveCancelled() default false;
}
