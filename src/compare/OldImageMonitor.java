/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package compare;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.epics.pvaccess.ClientFactory;
import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.Channel.ConnectionState;
import org.epics.pvaccess.client.ChannelGet;
import org.epics.pvaccess.client.ChannelProvider;
import org.epics.pvaccess.client.ChannelProviderRegistry;
import org.epics.pvaccess.client.ChannelProviderRegistryFactory;
import org.epics.pvaccess.client.ChannelRequester;
import org.epics.pvdata.copy.CreateRequest;
import org.epics.pvdata.monitor.Monitor;
import org.epics.pvdata.monitor.MonitorElement;
import org.epics.pvdata.monitor.MonitorRequester;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVShortArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Structure;

/** PVA 'monitor' of Image
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class OldImageMonitor
{
    private static final CountDownLatch done = new CountDownLatch(1);
    private static boolean print = true;

    private static void run_oldclient()
    {
        try
        {
            ClientFactory.start();
            final ChannelProviderRegistry registry = ChannelProviderRegistryFactory.getChannelProviderRegistry();
            final ChannelProvider provider = registry.getProvider("pva");
            final CreateRequest request_creater = CreateRequest.create();
            final PVStructure read_request = request_creater.createRequest("field()");
            final AtomicReference<ChannelGet> get = new AtomicReference<>();

            final MonitorRequester monitor_requester = new MonitorRequester()
            {
                @Override
                public void message(String arg0, MessageType arg1)
                {
                }

                @Override
                public String getRequesterName()
                {
                    return null;
                }

                @Override
                public void unlisten(Monitor arg0)
                {
                }

                @Override
                public void monitorConnect(final Status status, final Monitor monitor,
                        final Structure structure)
                {
                    if (status.isSuccess())
                        monitor.start();
                }

                @Override
                public void monitorEvent(final Monitor monitor)
                {
                    MonitorElement update;
                    while ((update = monitor.poll()) != null)
                    {
                        final PVStructure data = update.getPVStructure();
                        PVShortArray array = data.getUnionField("value").get(PVShortArray.class);
                        if (print)
                            System.out.println("old: " + array.getLength());
                        monitor.release(update);
                    }
                }
            };

            ChannelRequester channel_requester = new ChannelRequester()
            {
                @Override
                public void message(String arg0, MessageType arg1)
                {
                }

                @Override
                public String getRequesterName()
                {
                    return null;
                }

                @Override
                public void channelCreated(final Status status, final Channel channel)
                {
                }

                @Override
                public void channelStateChange(final Channel channel, final ConnectionState state)
                {
                    switch (state)
                    {
                    case CONNECTED:
                        channel.createMonitor(monitor_requester, read_request);
                        break;
                    case DISCONNECTED:
                    default:
                        // System.out.println("Channel " + channel.getChannelName() + " state: " + state);
                    }
                }
            };
            final Channel channel = provider.createChannel("IMAGE", channel_requester , ChannelProvider.PRIORITY_DEFAULT);

            done.await();

            channel.destroy();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException
    {
        new Thread(OldImageMonitor::run_oldclient).start();

        done.await();
//        Thread.sleep(200000);
//        done.countDown();
    }
}
