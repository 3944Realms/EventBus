package com.r3944realms.bus;

import com.r3944realms.bus.api.EventListener;

/**
 * 封装监听器以添加检查的监听器。
 */
public interface IWrapperListener {
    EventListener getWithoutCheck();
}
