package com.r3944realms.bus;

import com.r3944realms.bus.api.EventListener;
import com.r3944realms.bus.api.EventPriority;
import com.r3944realms.bus.api.ICancellableEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

public class ListenerList {
    private boolean rebuild = true;
    private final AtomicReference<EventListener[]> listeners = new AtomicReference<>();
    private final AtomicReference<EventListener[][]> prePhaseListeners = new AtomicReference<>();
    private final ArrayList<ArrayList<EventListener>> priorities;
    private ListenerList parent;
    private List<ListenerList> children;
    private final Semaphore writeLock = new Semaphore(1, true);
    private final boolean canUnwrapListeners;
    private final boolean buildPerPhaseList;

    ListenerList(Class<?> eventClass, boolean buildPerPhaseList) {
        int count = EventPriority.values().length;
        priorities = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            priorities.add(new ArrayList<>());
        }
        // 当事件不可取消时解包装监听器
        canUnwrapListeners = !ICancellableEvent.class.isAssignableFrom(eventClass);
        this.buildPerPhaseList = buildPerPhaseList;
    }

    ListenerList(Class<?> eventClass, ListenerList parent, boolean buildPerPhaseList) {
        this(eventClass, buildPerPhaseList);
        this.parent = parent;
        this.parent.addChild(parent);
    }

    /**
     * 返回一个包含所有该事件监听器的数组列表<br/>
     * 和所有其指定优先级的父事件。
     * <p>
     * 列表首先返回子事件的监听。
     *
     * @param priority 想获取的优先级
     * @return 包含事件监听器的数组列表
     */
    private ArrayList<EventListener> getListeners(EventPriority priority) {
        writeLock.acquireUninterruptibly();
        ArrayList<EventListener> ret = new ArrayList<>(priorities.get(priority.ordinal()));
        writeLock.release();
        if (parent != null) {
            ret.addAll(parent.getListeners(priority));
        }
        return ret;
    }

    /**
     * 返回所有优先级的所有监听器的完整列表 (包括所有父监听器)。<br/>
     * 列表以正确的优先级顺序返回。<br/>
     * 如果内部阵列缓存的信息已过期，则自动重建其。<br/>
     * @return 所有优先级的所有监听器的完整列表
     */
    public EventListener[] getListeners() {
        if (shouldRebuild()) buildCache();
        return listeners.get();
    }

    public EventListener[] getPrePhaseListeners(EventPriority priority) {
        if (!buildPerPhaseList)
            throw new IllegalStateException("buildPerPhaseList is false!");
        if (shouldRebuild()) buildCache();
        return prePhaseListeners.get()[priority.ordinal()];
    }

    protected boolean shouldRebuild() {
        return rebuild;
    }
    protected void forceRebuild() {
        this.rebuild = true;
        if (this.children != null) {
            synchronized (this.children) {
                for (ListenerList child : this.children) {
                    child.forceRebuild();
                }
            }
        }
    }

    private void addChild(ListenerList child) {
        if (this.children == null)
            this.children = Collections.synchronizedList(new ArrayList<>(2));
        this.children.add(child);
    }

    private void unwrapListeners(List<EventListener> ret) {
        if (canUnwrapListeners)
            for (int i = 0; i < ret.size(); ++i)
                if (ret.get(i) instanceof IWrapperListener wrapper)
                    ret.set(i, wrapper.getWithoutCheck());
    }

    /**
     * 重构监听器数组，如果什么事业没做，则返回值需要的时间减小
     */
    private void buildCache() {
        if (parent != null && parent.shouldRebuild())
            parent.buildCache();
        ArrayList<EventListener> ret = new ArrayList<>();
        EventListener[][] perPhaseListeners = buildPerPhaseList ? new EventListener[EventPriority.values().length][] : null;

        for (EventPriority phase : EventPriority.values()) {
            var phaseListeners = getListeners(phase);
            unwrapListeners(phaseListeners);
            ret.addAll(phaseListeners);

            if (perPhaseListeners != null)
                perPhaseListeners[phase.ordinal()] = phaseListeners.toArray(EventListener[]::new);
        }
        this.listeners.set(ret.toArray(new EventListener[0]));
        this.prePhaseListeners.set(perPhaseListeners);

        rebuild = false;
    }

    public void register(EventPriority priority, EventListener listener) {
        writeLock.acquireUninterruptibly();
        priorities.get(priority.ordinal()).add(listener);
        writeLock.release();
        this.forceRebuild();
    }

    public void unregister(EventListener listener) {
        writeLock.acquireUninterruptibly();
        priorities.stream().filter(list -> list.remove(listener)).forEach(list -> this.forceRebuild());
        writeLock.release();
    }
}
