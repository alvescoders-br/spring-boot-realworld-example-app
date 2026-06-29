package io.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationLifecycleLogger {
  public static final String STARTUP_MARKER = "RealWorldApplication startup complete";
  public static final String SHUTDOWN_MARKER = "RealWorldApplication shutdown complete";

  private static final Logger log = LoggerFactory.getLogger(ApplicationLifecycleLogger.class);

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    log.info(STARTUP_MARKER);
  }

  @EventListener(ContextClosedEvent.class)
  public void onContextClosed() {
    log.info(SHUTDOWN_MARKER);
  }
}
