/**
 * Copyright � Microsoft Open Technologies, Inc.
 *
 * All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
 * ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
 * PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
 *
 * See the Apache License, Version 2.0 for the specific language
 * governing permissions and limitations under the License.
 */
package com.microsoft.office.integration.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.microsoft.exchange.services.odata.model.IMessages;
import com.microsoft.exchange.services.odata.model.Me;
import com.microsoft.exchange.services.odata.model.Messages;
import com.microsoft.exchange.services.odata.model.types.IFolder;
import com.microsoft.exchange.services.odata.model.types.IMessage;

public class MiscellaneousAsyncTestCase extends AbstractAsyncTest {
    public void testFetch() {
        counter = new CountDownLatch(1);
        Futures.addCallback(Me.getDraftsAsync(),  new FutureCallback<IFolder>() {
            public void onFailure(Throwable t) {
                reportError(t);
                counter.countDown();
            }
            
            public void onSuccess(IFolder drafts) {
                try {
                    final IMessages messages = drafts.getMessagesAsync().get();
                    // actual server request for messages will be executed in this line by calling size; response will be cached
                    final int size = messages.size();
                    
                    final IMessage message = Messages.newMessage();
                    message.setSubject("fetch test");
                    Me.flush();
                    
                    // verify that local cache has no changes after flush (size will return old value)
                    assertEquals(size, messages.size());   
                    
                    final CountDownLatch cdl = new CountDownLatch(1);
                    Futures.addCallback(messages.fetchAsync(), new FutureCallback<Void>() {
                        public void onFailure(Throwable t) {
                            reportError(t);
                            cdl.countDown();
                        }
                        
                        public void onSuccess(Void result) {
                            try {
                                assertEquals(size + 1, messages.size());
                                Me.getMessages().delete(message.getId());
                                Me.flush();
                            } catch (Throwable t) {
                                reportError(t);
                            }
                            
                            cdl.countDown();
                        }
                    });
                    
                    cdl.await();
                } catch (Throwable t) {
                    reportError(t);
                }
                
                counter.countDown();
            }
        });
        try {
            if (!counter.await(60000, TimeUnit.MILLISECONDS)) {
                fail("testSize() timed out");
            }
        } catch (InterruptedException e) {
            fail("testSize() has been interrupted");
        }
    }
}
