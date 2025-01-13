module com.r3944realms.bus {
    requires net.jodah.typetools;
    requires org.apache.logging.log4j;
    requires static org.jetbrains.annotations;
    requires org.objectweb.asm.commons;
    requires jdk.unsupported; // required for typetools

    exports com.r3944realms.bus;
    exports com.r3944realms.bus.api;
}