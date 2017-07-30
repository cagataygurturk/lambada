/*
 * Copyright 2010-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lambadaframework.wagon;

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.SessionEvent;
import org.apache.maven.wagon.events.SessionListener;

import java.util.HashSet;
import java.util.Set;

final class StandardSessionListenerSupport implements SessionListenerSupport {

    private final Wagon wagon;

    private final Set<SessionListener> sessionListeners = new HashSet<>();

    StandardSessionListenerSupport(Wagon wagon) {
        this.wagon = wagon;
    }

    @Override
    public void addSessionListener(SessionListener sessionListener) {
        this.sessionListeners.add(sessionListener);
    }

    @Override
    public void removeSessionListener(SessionListener sessionListener) {
        this.sessionListeners.remove(sessionListener);
    }

    @Override
    public boolean hasSessionListener(SessionListener sessionListener) {
        return this.sessionListeners.contains(sessionListener);
    }

    @Override
    public void fireSessionOpening() {
        SessionEvent event = new SessionEvent(this.wagon, SessionEvent.SESSION_OPENING);
        for (SessionListener sessionListener : this.sessionListeners) {
            sessionListener.sessionOpening(event);
        }
    }

    @Override
    public void fireSessionOpened() {
        SessionEvent event = new SessionEvent(this.wagon, SessionEvent.SESSION_OPENED);
        for (SessionListener sessionListener : this.sessionListeners) {
            sessionListener.sessionOpened(event);
        }
    }

    @Override
    public void fireSessionDisconnecting() {
        SessionEvent event = new SessionEvent(this.wagon, SessionEvent.SESSION_DISCONNECTING);
        for (SessionListener sessionListener : this.sessionListeners) {
            sessionListener.sessionDisconnecting(event);
        }
    }

    @Override
    public void fireSessionDisconnected() {
        SessionEvent event = new SessionEvent(this.wagon, SessionEvent.SESSION_DISCONNECTED);
        for (SessionListener sessionListener : this.sessionListeners) {
            sessionListener.sessionDisconnected(event);
        }
    }

    @Override
    public void fireSessionConnectionRefused() {
        SessionEvent event = new SessionEvent(this.wagon, SessionEvent.SESSION_CONNECTION_REFUSED);
        for (SessionListener sessionListener : this.sessionListeners) {
            sessionListener.sessionConnectionRefused(event);
        }
    }

    @Override
    public void fireSessionLoggedIn() {
        SessionEvent event = new SessionEvent(this.wagon, SessionEvent.SESSION_LOGGED_IN);
        for (SessionListener sessionListener : this.sessionListeners) {
            sessionListener.sessionLoggedIn(event);
        }
    }

    @Override
    public void fireSessionLoggedOff() {
        SessionEvent event = new SessionEvent(this.wagon, SessionEvent.SESSION_LOGGED_OFF);
        for (SessionListener sessionListener : this.sessionListeners) {
            sessionListener.sessionLoggedOff(event);
        }
    }

    @Override
    public void fireSessionError(Exception exception) {
        SessionEvent event = new SessionEvent(this.wagon, exception);
        for (SessionListener sessionListener : this.sessionListeners) {
            sessionListener.sessionError(event);
        }
    }
}
