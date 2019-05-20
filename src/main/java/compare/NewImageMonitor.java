/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package compare;

import java.util.concurrent.CountDownLatch;

import org.epics.pva.client.ClientChannel;
import org.epics.pva.client.ClientChannelListener;
import org.epics.pva.client.ClientChannelState;
import org.epics.pva.client.PVAClient;
import org.epics.pva.data.PVAShortArray;
import org.epics.pva.data.PVAUnion;

/** PVA 'monitor' of Image
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NewImageMonitor
{
    private static final CountDownLatch done = new CountDownLatch(1);
    private static boolean print = true;

    private static void run_pvaclient()
    {
        try
        {
            PVAClient client = new PVAClient();
            final CountDownLatch connected = new CountDownLatch(1);
            ClientChannelListener listener = (channel, state) ->
            {
                if (state == ClientChannelState.CONNECTED)
                    connected.countDown();
            };
            final ClientChannel channel = client.getChannel("IMAGE", listener);
            connected.await();

            channel.subscribe("", (ch, changes, data) ->
            {
                final PVAUnion value = data.get("value");
                final PVAShortArray array = value.get();
                if (print)
                    System.out.println("new: " + array.get().length);
            });

            done.await();

            channel.close();
            client.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }


    public static void main(String[] args) throws InterruptedException
    {
        new Thread(NewImageMonitor::run_pvaclient).start();

        done.await();
//        Thread.sleep(200000);
//        done.countDown();
    }
}