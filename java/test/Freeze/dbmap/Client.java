// **********************************************************************
//
// Copyright (c) 2003-2005 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

import Freeze.*;

public class Client
{
    static class ReadThread extends Thread
    {
        public void
        run()
        { 
            try
            {
		for(int i = 0; i < 10; ++i)
		{
		    for(;;)
		    {
			java.util.Iterator p = null;

			try
			{
			    java.util.Set entrySet = _map.entrySet();
			    p = entrySet.iterator();
			 
			    while(p.hasNext())
			    {
				java.util.Map.Entry e = (java.util.Map.Entry)p.next();
				byte v = ((Byte)e.getKey()).byteValue();
				test(e.getValue().equals(new Integer(v - (byte)'a')));
			    }
			    break;
			}
			catch(DeadlockException ex)
			{
			    // System.err.print("r");
			    //
			    // Try again
			    //
			}
			finally
			{
			    if(p != null)
			    {
				((Freeze.Map.EntryIterator)p).close();
			    }
			}
		    }
		}
	    }
	    catch(Exception ex)
	    {
		ex.printStackTrace();
		System.err.println(ex);
	    }
	    finally
	    {
		((Freeze.Map) _map).close();
		_connection.close();
	    }
	}
	
	ReadThread(Ice.Communicator communicator, String envName, String dbName)
        {
	    _connection = Freeze.Util.createConnection(communicator, envName);
	    _map = new ByteIntMap(_connection, dbName, true);
	}

	private Freeze.Connection _connection;
	private java.util.Map _map;
    }


    static class WriteThread extends Thread
    {
        public void
        run()
        {
            try
            {
		for(int i = 0; i < 4; ++i)
		{
		    for(;;)
		    {
			java.util.Iterator p = null;

			try
			{
			    java.util.Set entrySet = _map.entrySet();
			    p = entrySet.iterator();
			 
			    while(p.hasNext())
			    {
				java.util.Map.Entry e = (java.util.Map.Entry)p.next();
				int v = ((Integer)e.getValue()).intValue() + 1;
				e.setValue(new Integer(v));
				p.remove();
			    }
			    
			    break;
			}
			catch(DeadlockException ex)
			{
			    // System.err.print("w");
			    //
			    // Try again
			    //
			}
			finally
			{
			    if(p != null)
			    {
				((Freeze.Map.EntryIterator)p).close();
			    }
			}
		    }
		    populateDB(_connection, _map);
		}
	    }
	    catch(Exception ex)
	    {
		ex.printStackTrace();
		System.err.println(ex);
	    }
	    finally
	    {
		((Freeze.Map)_map).close();
		_connection.close();
	    }
	}
	
	WriteThread(Ice.Communicator communicator, String envName, String dbName)
        {
	    _connection = Freeze.Util.createConnection(communicator, envName);
	    _map = new ByteIntMap(_connection, dbName, true);
	}

	private Freeze.Connection _connection;
	private java.util.Map _map;
    }


    static String alphabet = "abcdefghijklmnopqrstuvwxyz";

    private static void
    test(boolean b)
    {
        if(!b)
        {
            throw new RuntimeException();
        }
    }

    private static void
    populateDB(Freeze.Connection connection, java.util.Map m)
	throws DatabaseException
    {
	int length = alphabet.length();

	for(;;)
	{
	   
	    try
	    {
		Transaction tx = connection.beginTransaction();
		for(int j = 0; j < length; ++j)
		{
		    m.put(new Byte((byte)alphabet.charAt(j)), new Integer(j));
		}
		tx.commit();
		break; // for(;;)
	    }
	    catch(Freeze.DeadlockException dx)
	    {
		length = length / 2;
		// System.err.print("t");
		//
		// Try again
		//
	    }
	    finally
	    {
		if(connection.currentTransaction() != null)
		{
		    connection.currentTransaction().rollback();
		}
	    }
	}
    }

    private static int
    run(String[] args, Ice.Communicator communicator, String envName, String dbName)
	throws DatabaseException
    {
	Freeze.Connection connection = Freeze.Util.createConnection(communicator, envName);

	java.util.Map m = new ByteIntMap(connection, dbName, true);

	//
	// Populate the database with the alphabet.
	//
	populateDB(connection, m);

	int j;

	System.out.print("testing populate... ");
        System.out.flush();
	for(j = 0; j < alphabet.length(); ++j)
	{
	    Object value = m.get(new Byte((byte)alphabet.charAt(j)));
	    test(value != null);
	}
	test(m.get(new Byte((byte)'0')) == null);
	for(j = 0; j < alphabet.length(); ++j)
	{
	    test(m.containsKey(new Byte((byte)alphabet.charAt(j))));
	}
	test(!m.containsKey(new Byte((byte)'0')));
	for(j = 0; j < alphabet.length(); ++j)
	{
	    test(m.containsValue(new Integer(j)));
	}
	test(!m.containsValue(new Integer(-1)));
	test(m.size() == alphabet.length());
	test(!m.isEmpty());
	System.out.println("ok");

	System.out.print("testing erase... ");
        System.out.flush();
	m.remove(new Byte((byte)'a'));
	m.remove(new Byte((byte)'b'));
	m.remove(new Byte((byte)'c'));
	for(j = 3; j < alphabet.length(); ++j)
	{
	    Object value = m.get(new Byte((byte)alphabet.charAt(j)));
	    test(value != null);
	}
	test(m.get(new Byte((byte)'a')) == null);
	test(m.get(new Byte((byte)'b')) == null);
	test(m.get(new Byte((byte)'c')) == null);
	System.out.println("ok");
	
	//
	// Re-populate.
	//
	populateDB(connection, m);

	{
	    System.out.print("testing keySet... ");
	    System.out.flush();
	    java.util.Set keys = m.keySet();
	    test(keys.size() == alphabet.length());
	    test(!keys.isEmpty());
	    java.util.Iterator p = keys.iterator();
	    while(p.hasNext())
	    {
		Object o = p.next();
		test(keys.contains(o));

		Byte b = (Byte)o;
		test(m.containsKey(b));
	    }
	    System.out.println("ok");
	}

	{
	    System.out.print("testing values... ");
	    System.out.flush();
	    java.util.Collection values = m.values();
	    test(values.size() == alphabet.length());
	    test(!values.isEmpty());
	    java.util.Iterator p = values.iterator();
	    while(p.hasNext())
	    {
		Object o = p.next();
		test(values.contains(o));

		Integer i = (Integer)o;
		test(m.containsValue(i));
	    }
	    System.out.println("ok");
	}

	{
	    System.out.print("testing entrySet... ");
	    System.out.flush();
	    java.util.Set entrySet = m.entrySet();
	    test(entrySet.size() == alphabet.length());
	    test(!entrySet.isEmpty());
	    java.util.Iterator p = entrySet.iterator();
	    while(p.hasNext())
	    {
		Object o = p.next();
		test(entrySet.contains(o));

		java.util.Map.Entry e = (java.util.Map.Entry)o;
		test(m.containsKey(e.getKey()));
		test(m.containsValue(e.getValue()));
	    }
	    System.out.println("ok");
	}

	{
	    System.out.print("testing iterator.remove... ");
	    System.out.flush();

	    test(m.size() == 26);
	    test(m.get(new Byte((byte)'b')) != null);
	    test(m.get(new Byte((byte)'n')) != null);
	    test(m.get(new Byte((byte)'z')) != null);

	    ((Freeze.Map) m).closeAllIterators();

	    java.util.Set entrySet = m.entrySet();
	    java.util.Iterator p = entrySet.iterator();
	   
	    while(p.hasNext())
	    {
		java.util.Map.Entry e = (java.util.Map.Entry)p.next();
		Byte b = (Byte)e.getKey();
		byte v = b.byteValue();
		if(v == (byte)'b' || v == (byte)'n' || v == (byte)'z')
		{
		    p.remove();
		    try
		    {
			p.remove();
		    }
		    catch(IllegalStateException ex)
		    {
			// Expected.
		    }
		}
	    }
	    ((Freeze.Map) m).closeAllIterators();

	    test(m.size() == 23);
	    test(m.get(new Byte((byte)'b')) == null);
	    test(m.get(new Byte((byte)'n')) == null);
	    test(m.get(new Byte((byte)'z')) == null);

	    //
	    // Re-populate.
	    //
	    populateDB(connection, m);
	    
	    test(m.size() == 26);

	    entrySet = m.entrySet();
	    p = entrySet.iterator();
	    while(p.hasNext())
	    {
		java.util.Map.Entry e = (java.util.Map.Entry)p.next();
		byte v = ((Byte)e.getKey()).byteValue();
		if(v == (byte)'a' || v == (byte)'b' || v == (byte)'c')
		{
		    p.remove();
		}
	    }
	    ((Freeze.Map) m).closeAllIterators();

	    test(m.size() == 23);
	    test(m.get(new Byte((byte)'a')) == null);
	    test(m.get(new Byte((byte)'b')) == null);
	    test(m.get(new Byte((byte)'c')) == null);
	    System.out.println("ok");
	}

        {
            System.out.print("testing entry.setValue... ");
            System.out.flush();

            //
            // Re-populate.
            //
            populateDB(connection, m);

            java.util.Set entrySet = m.entrySet();
            java.util.Iterator p = entrySet.iterator();
            while(p.hasNext())
            {
                java.util.Map.Entry e = (java.util.Map.Entry)p.next();
                byte v = ((Byte)e.getKey()).byteValue();
                if(v == (byte)'b' || v == (byte)'n' || v == (byte)'z')
                {
                    e.setValue(new Integer(v + 100));
                }
            }
	    ((Freeze.Map) m).closeAllIterators();
            test(m.size() == 26);
            test(m.get(new Byte((byte)'b')) != null);
            test(m.get(new Byte((byte)'n')) != null);
            test(m.get(new Byte((byte)'z')) != null);

            p = entrySet.iterator();
            while(p.hasNext())
            {
                java.util.Map.Entry e = (java.util.Map.Entry)p.next();
                byte v = ((Byte)e.getKey()).byteValue();
                if(v == (byte)'b' || v == (byte)'n' || v == (byte)'z')
                {
                    test(e.getValue().equals(new Integer(v + 100)));
                }
                else
                {
                    test(e.getValue().equals(new Integer(v - (byte)'a')));
                }
            }
            System.out.println("ok");
        }

	{
            System.out.print("testing index... ");
            System.out.flush();

	    //
            // Re-populate.
            //
            populateDB(connection, m);

	    ByteIntMap typedM = (ByteIntMap)m;

	    java.util.Map.Entry e;
	    java.util.Iterator p;

	    int length = alphabet.length();

	    for(int k = 0; k < length; ++k)
	    {
		p = typedM.findByValue(k);
		test(p.hasNext());
		e = (java.util.Map.Entry)p.next();
		test(((Byte)e.getKey()).byteValue() == (byte)alphabet.charAt(k));
		test(!p.hasNext());
	    }

	    //
	    // Non-existent index value
	    //
	    p = typedM.findByValue(100);
	    test(!p.hasNext());

	    //
	    // 2 items at 17
	    // 
	    m.put(new Byte((byte)alphabet.charAt(21)), new Integer(17));
	    
	    p = typedM.findByValue(17);
	    
	    test(p.hasNext());
	    e = (java.util.Map.Entry)p.next();
	    byte v = ((Byte)e.getKey()).byteValue();
	    test(v == (byte)alphabet.charAt(17) || v == (byte)alphabet.charAt(21));
	    
	    test(p.hasNext());
	    e = (java.util.Map.Entry)p.next();
	    v = ((Byte)e.getKey()).byteValue();
	    test(v == (byte)alphabet.charAt(17) || v == (byte)alphabet.charAt(21));
	    
	    test(!p.hasNext());
	    test(typedM.valueCount(17) == 2);

	    p = typedM.findByValue(17);
	    test(p.hasNext());
	    p.next();
	    p.remove();
	    test(p.hasNext());
	    e = (java.util.Map.Entry)p.next();
	    v = ((Byte)e.getKey()).byteValue();
	    test(v == (byte)alphabet.charAt(17) || v == (byte)alphabet.charAt(21));
	    test(!p.hasNext());

	    //
	    // We need to close this write iterator before further reads
	    //
	    typedM.closeAllIterators();

	    test(typedM.valueCount(17) == 1);

	    p = typedM.findByValue(17);
	    test(p.hasNext());
	    e = (java.util.Map.Entry)p.next();

	    try
	    {
		e.setValue(new Integer(18));
		test(false);
	    }
	    catch(Freeze.DatabaseException ex)
	    {
		// Expected
	    }

	    v = ((Byte)e.getKey()).byteValue();
	    test(v == (byte)alphabet.charAt(17) || v == (byte)alphabet.charAt(21));
	    test(typedM.valueCount(17) == 1);
            System.out.println("ok");
        }


	((Freeze.Map) m).closeAllIterators();

	{
	    System.out.print("testing concurrent access... ");
	    System.out.flush();
	
	    m.clear();
	    populateDB(connection, m);
	    

	    java.util.List l = new java.util.ArrayList();
	
	    //
	    // Create each thread.
	    //
	    for(int i = 0; i < 5; ++i)
	    {
		l.add(new ReadThread(communicator, envName, dbName));
		l.add(new WriteThread(communicator, envName, dbName));
	    }

	    //
	    // Start each thread.
	    //
	    java.util.Iterator p = l.iterator();
	    while(p.hasNext())
	    {
		Thread thr = (Thread)p.next();
		thr.start();
	    }
	
	    //
	    // Wait for each thread to terminate.
	    //
	    p = l.iterator();
	    while(p.hasNext())
	    {
		Thread thr = (Thread)p.next();
		while(thr.isAlive())
		{
		    try
		    {
			thr.join();
		    }
		    catch(InterruptedException e)
		    {
		    }
		}
	    }

	    System.out.println("ok");
	}
	connection.close();
	
	return 0;
    }

    static public void
    main(String[] args)
    {
	int status;
	Ice.Communicator communicator = null;
	String envName = "db";
      

	try
	{
	    Ice.StringSeqHolder holder = new Ice.StringSeqHolder();
	    holder.value = args;
	    communicator = Ice.Util.initialize(holder);
	    args = holder.value;
	    if(args.length > 0)
	    {
		envName = args[0];
		envName += "/";
		envName += "db";
	    }
	    
	    status = run(args, communicator, envName, "binary");
	}
	catch(DatabaseException ex)
	{
	    System.err.println(args[0] + ": " + ex + ": " + ex.message);
	    status = 1;
	}
	catch(Ice.LocalException ex)
	{
	    System.err.println(args[0] + ": " + ex);
	    status = 1;
	}
	catch(Exception ex)
	{
	    System.err.println(args[0] + ": unknown exception: " + ex);
	    status = 1;
	}

	if(communicator != null)
	{
	    try
	    {
		communicator.destroy();
	    }
	    catch(Exception ex)
	    {
		System.err.println(ex);
		status = 1;
	    }
	}

	System.gc();
	System.exit(status);
    }
}
