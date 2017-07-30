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
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.resource.Resource;

import java.util.HashSet;
import java.util.Set;

final class StandardTransferListenerSupport implements TransferListenerSupport {

    private final Wagon wagon;

    private final Set<TransferListener> transferListeners = new HashSet<>();

    StandardTransferListenerSupport(Wagon wagon) {
        this.wagon = wagon;
    }

    @Override
    public void addTransferListener(TransferListener transferListener) {
        if (transferListener != null) {
            this.transferListeners.add(transferListener);
        }
    }

    @Override
    public void removeTransferListener(TransferListener transferListener) {
        this.transferListeners.remove(transferListener);
    }

    @Override
    public boolean hasTransferListener(TransferListener transferListener) {
        return this.transferListeners.contains(transferListener);
    }

    @Override
    public void fireTransferInitiated(Resource resource, int requestType) {
        TransferEvent event = new TransferEvent(this.wagon, resource, TransferEvent.TRANSFER_INITIATED, requestType);
        for (TransferListener transferListener : this.transferListeners) {
            transferListener.transferInitiated(event);
        }
    }

    @Override
    public void fireTransferStarted(Resource resource, int requestType) {
        TransferEvent event = new TransferEvent(this.wagon, resource, TransferEvent.TRANSFER_STARTED, requestType);
        for (TransferListener transferListener : this.transferListeners) {
            transferListener.transferStarted(event);
        }
    }

    @Override
    public void fireTransferProgress(Resource resource, int requestType, byte[] buffer, int length) {
        TransferEvent event = new TransferEvent(this.wagon, resource, TransferEvent.TRANSFER_PROGRESS, requestType);
        for (TransferListener transferListener : this.transferListeners) {
            transferListener.transferProgress(event, buffer, length);
        }
    }

    @Override
    public void fireTransferCompleted(Resource resource, int requestType) {
        TransferEvent event = new TransferEvent(this.wagon, resource, TransferEvent.TRANSFER_COMPLETED, requestType);
        for (TransferListener transferListener : this.transferListeners) {
            transferListener.transferCompleted(event);
        }
    }

    @Override
    public void fireTransferError(Resource resource, int requestType, Exception exception) {
        TransferEvent event = new TransferEvent(this.wagon, resource, exception, requestType);
        for (TransferListener transferListener : this.transferListeners) {
            transferListener.transferError(event);
        }
    }
}
