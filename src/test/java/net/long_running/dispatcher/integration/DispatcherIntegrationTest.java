package net.long_running.dispatcher.integration;

import java.nio.ByteBuffer;

import org.junit.Test;

import net.long_running.dispatcher.Dispatcher;
import net.long_running.dispatcher.Dispatchers;
import net.long_running.dispatcher.FragmentHandler;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public class DispatcherIntegrationTest
{

    class Consumer implements FragmentHandler
    {

        int counter = 0;

        @Override
        public void onFragment(DirectBuffer buffer, int offset, int length)
        {
            int newCounter = buffer.getInt(offset);
            if(newCounter  - 1 != counter)
            {
                throw new RuntimeException();
            }
            counter = newCounter;
        }

    }

    @Test
    public void test() throws Exception
    {
        // 1 million 10 K messages
        final int totalWork = 1000000;
        UnsafeBuffer msg = new UnsafeBuffer(ByteBuffer.allocate(1024*10));

        Dispatcher dispatcher = Dispatchers.create("default")
                .bufferSize(1024 * 1024 * 10) // 10 MB buffersize
                .buildAndStart();

        final Consumer consumer = new Consumer();


        Thread consumerThread = new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                while(consumer.counter < totalWork)
                {
                    dispatcher.poll(consumer, Integer.MAX_VALUE);
                }
            }
        });

        consumerThread.start();

        for(int i = 1; i <= totalWork; i++)
        {
            msg.putInt(0, i);
            while (dispatcher.offer(msg) <= 0)
            {
                // spin
            }
        }

        // message published

        consumerThread.join();

        dispatcher.close();
    }

}
